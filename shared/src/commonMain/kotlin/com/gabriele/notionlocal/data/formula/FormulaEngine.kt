package com.gabriele.notionlocal.data.formula

import com.gabriele.notionlocal.data.entity.ColumnType
import com.gabriele.notionlocal.data.entity.DatabaseColumnEntity
import com.gabriele.notionlocal.data.entity.DatabaseRowEntity
import com.gabriele.notionlocal.data.entity.DatabaseSnapshot
import com.gabriele.notionlocal.data.entity.MULTI_VALUE_SEPARATOR
import com.gabriele.notionlocal.data.entity.RollupFormatter
import com.gabriele.notionlocal.data.entity.durationMillisOfCell
import com.gabriele.notionlocal.data.entity.RollupValue
import com.gabriele.notionlocal.data.entity.parseColorCell
import com.gabriele.notionlocal.data.entity.parseDateRange
import com.gabriele.notionlocal.data.entity.relationConfig
import com.gabriele.notionlocal.data.entity.relationTitles
import java.math.BigDecimal
import java.math.MathContext
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.time.temporal.IsoFields
import java.util.Locale
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.ln
import kotlin.math.log10
import kotlin.math.log2
import kotlin.math.pow
import kotlin.math.sign
import kotlin.math.sqrt

/** Quello che una formula legge mentre si calcola per una riga. */
interface FormulaScope {
    /** Il valore della proprietà [name] in questa riga: lancia `FormulaException` se non c'è. */
    fun prop(name: String): FormulaValue
    val now: Long
    val zone: ZoneId
    val format: RollupFormatter
    /** La lingua dei nomi dei giorni e dei mesi in `formatDate`. */
    val locale: Locale get() = Locale.getDefault()
}

/**
 * **Il calcolo delle formule** (dal 28/10/2026): operatori, funzioni e le
 * proprietà di ogni riga. Una formula sbagliata non ferma niente: la sua
 * cella dice l'errore, le altre si calcolano lo stesso.
 */
object FormulaEngine {

    /** Il nome con cui una formula legge il nome della riga, prop("Name"), oltre a quello dato alla colonna. */
    const val NAME = "Name"

    /** Il valore di [expr] in [scope]; un errore diventa `FormulaValue.Error`. */
    fun evaluate(expr: Expr, scope: FormulaScope): FormulaValue =
        try {
            Evaluation(scope).eval(expr)
        } catch (e: FormulaException) {
            FormulaValue.Error(e.error)
        } catch (e: ArithmeticException) {
            FormulaValue.Error(FormulaError(FormulaError.Kind.TYPE, e.message.orEmpty()))
        } catch (e: NumberFormatException) {
            FormulaValue.Error(FormulaError(FormulaError.Kind.TYPE, e.message.orEmpty()))
        } catch (e: StackOverflowError) {
            // Una formula annidata migliaia di volte: meglio un errore che l'app chiusa.
            FormulaValue.Error(FormulaError(FormulaError.Kind.SYNTAX, "too deep"))
        }

    /** Legge e calcola [expression]: per l'anteprima della finestra. */
    fun evaluate(expression: String, scope: FormulaScope): FormulaValue {
        val expr = try {
            FormulaParser.parse(expression)
        } catch (e: FormulaException) {
            return FormulaValue.Error(e.error)
        } ?: return FormulaValue.Empty
        return evaluate(expr, scope)
    }

    // --- Tutte le righe di un database ---

    /**
     * **Le formule di tutte le righe**: (riga, colonna) → valore. Una formula
     * può leggere un'altra formula; due che si leggono a vicenda dicono
     * "circolare" invece di girare per sempre. [nameTitles]: i nomi con cui si
     * legge il nome della riga ("Name", e quello dato alla colonna del nome).
     */
    fun computeAll(
        columns: List<DatabaseColumnEntity>,
        rows: List<DatabaseRowEntity>,
        cells: Map<Pair<String, String>, String>,
        rollups: Map<Pair<String, String>, RollupValue>,
        related: Map<String, DatabaseSnapshot>,
        nameTitles: Set<String>,
        now: Long,
        zone: ZoneId,
        format: RollupFormatter,
        locale: Locale = Locale.getDefault()
    ): Map<Pair<String, String>, FormulaValue> {
        val formulas = columns.filter { it.type == ColumnType.FORMULA }
        if (formulas.isEmpty()) return emptyMap()
        val parsed = formulas.associate { column ->
            column.id to runCatching { FormulaParser.parse(FormulaConfig.parse(column.optionsJson).expression) }
        }
        val result = HashMap<Pair<String, String>, FormulaValue>()
        for (row in rows) {
            val scope = RowScope(row, columns, cells, rollups, related, nameTitles, parsed, now, zone, format, locale)
            for (column in formulas) {
                result[row.id to column.id] = scope.formula(column)
            }
        }
        return result
    }

    private class RowScope(
        private val row: DatabaseRowEntity,
        columns: List<DatabaseColumnEntity>,
        private val cells: Map<Pair<String, String>, String>,
        private val rollups: Map<Pair<String, String>, RollupValue>,
        private val related: Map<String, DatabaseSnapshot>,
        private val nameTitles: Set<String>,
        private val parsed: Map<String, Result<Expr?>>,
        override val now: Long,
        override val zone: ZoneId,
        override val format: RollupFormatter,
        override val locale: Locale
    ) : FormulaScope {
        private val byName = columns.associateBy { it.name }
        private val byLowerName = columns.associateBy { it.name.lowercase() }
        private val memo = HashMap<String, FormulaValue>()
        private val visiting = HashSet<String>()

        fun formula(column: DatabaseColumnEntity): FormulaValue {
            memo[column.id]?.let { return it }
            if (!visiting.add(column.id)) return FormulaValue.Error(FormulaError(FormulaError.Kind.CIRCULAR, column.name))
            val value = parsed[column.id]?.fold(
                onSuccess = { expr -> if (expr == null) FormulaValue.Empty else evaluate(expr, this) },
                onFailure = { e ->
                    FormulaValue.Error((e as? FormulaException)?.error ?: FormulaError(FormulaError.Kind.SYNTAX, e.message.orEmpty()))
                }
            ) ?: FormulaValue.Empty
            visiting.remove(column.id)
            memo[column.id] = value
            return value
        }

        override fun prop(name: String): FormulaValue {
            val column = byName[name] ?: byLowerName[name.lowercase()]
            if (column == null) {
                if (nameTitles.any { it.equals(name, ignoreCase = true) }) return text(row.title)
                throw FormulaException(FormulaError(FormulaError.Kind.UNKNOWN_PROPERTY, name))
            }
            val raw = cells[row.id to column.id].orEmpty()
            return when (column.type) {
                ColumnType.TEXT, ColumnType.URL, ColumnType.EMAIL, ColumnType.PHONE, ColumnType.SELECT -> text(raw)
                ColumnType.NUMBER -> raw.trim().replace(',', '.').toDoubleOrNull()?.let { FormulaValue.Num(it) } ?: FormulaValue.Empty
                ColumnType.CHECKBOX -> FormulaValue.Bool(raw == "true")
                ColumnType.MULTI_SELECT, ColumnType.PEOPLE -> text(raw.split(MULTI_VALUE_SEPARATOR).filter { it.isNotBlank() }.joinToString(", "))
                ColumnType.DATE -> parseDateRange(raw)?.let { FormulaValue.Date(it.start, it.hasTime, it.end) } ?: FormulaValue.Empty
                // Una durata vale i minuti fra l'inizio e la fine.
                ColumnType.DURATION -> durationMillisOfCell(raw)?.let { FormulaValue.Num((it / 60_000).toDouble()) } ?: FormulaValue.Empty
                ColumnType.COLOR -> text(parseColorCell(raw).text)
                ColumnType.CREATED_TIME -> FormulaValue.Date(row.createdAt, hasTime = true)
                ColumnType.LAST_EDITED_TIME -> FormulaValue.Date(row.updatedAt ?: row.createdAt, hasTime = true)
                ColumnType.RELATION -> text(relationTitles(raw, column.relationConfig()?.let { related[it.database] }).joinToString(", "))
                ColumnType.ROLLUP -> rollups[row.id to column.id]?.let { r ->
                    when {
                        r.date != null -> FormulaValue.Date(r.date)
                        r.number != null -> FormulaValue.Num(r.number)
                        else -> text(r.text)
                    }
                } ?: FormulaValue.Empty
                ColumnType.FORMULA -> formula(column)
                ColumnType.PAGE_LINK, ColumnType.IMAGE, ColumnType.BUTTON -> FormulaValue.Empty
            }
        }

        private fun text(value: String) = if (value.isEmpty()) FormulaValue.Empty else FormulaValue.Text(value)
    }

    // --- Il calcolo ---

    private class Evaluation(private val scope: FormulaScope) {

        fun eval(expr: Expr): FormulaValue = when (expr) {
            is Expr.Num -> FormulaValue.Num(expr.value)
            is Expr.Str -> FormulaValue.Text(expr.value)
            is Expr.Bool -> FormulaValue.Bool(expr.value)
            is Expr.Prop -> scope.prop(expr.name).also { if (it is FormulaValue.Error) throw FormulaException(it.error) }
            is Expr.Cond -> if (truthy(eval(expr.condition))) eval(expr.then) else eval(expr.otherwise)
            is Expr.Unary -> when (expr.op) {
                "-" -> FormulaValue.Num(-number(eval(expr.operand), "-"))
                else -> FormulaValue.Bool(!truthy(eval(expr.operand)))
            }
            is Expr.Binary -> binary(expr)
            is Expr.Call -> call(expr.name, expr.args)
        }

        private fun binary(expr: Expr.Binary): FormulaValue {
            when (expr.op) {
                "and" -> return FormulaValue.Bool(truthy(eval(expr.left)) && truthy(eval(expr.right)))
                "or" -> return FormulaValue.Bool(truthy(eval(expr.left)) || truthy(eval(expr.right)))
            }
            val a = eval(expr.left)
            val b = eval(expr.right)
            return when (expr.op) {
                "+" -> if (a is FormulaValue.Text || b is FormulaValue.Text) {
                    FormulaValue.Text(text(a) + text(b))
                } else {
                    FormulaValue.Num(number(a, "+") + number(b, "+"))
                }
                "-" -> FormulaValue.Num(number(a, "-") - number(b, "-"))
                "*" -> FormulaValue.Num(number(a, "*") * number(b, "*"))
                "/" -> {
                    val d = number(b, "/")
                    if (d == 0.0) throw FormulaException(FormulaError(FormulaError.Kind.DIVISION_BY_ZERO))
                    FormulaValue.Num(number(a, "/") / d)
                }
                "%" -> {
                    val d = number(b, "%")
                    if (d == 0.0) throw FormulaException(FormulaError(FormulaError.Kind.DIVISION_BY_ZERO))
                    FormulaValue.Num(number(a, "%") % d)
                }
                "^" -> FormulaValue.Num(number(a, "^").pow(number(b, "^")))
                "==" -> FormulaValue.Bool(same(a, b))
                "!=" -> FormulaValue.Bool(!same(a, b))
                else -> FormulaValue.Bool(compare(a, b, expr.op))
            }
        }

        private fun compare(a: FormulaValue, b: FormulaValue, op: String): Boolean {
            val c = when {
                a is FormulaValue.Date && b is FormulaValue.Date -> a.millis.compareTo(b.millis)
                a is FormulaValue.Text && b is FormulaValue.Text -> a.value.compareTo(b.value, ignoreCase = true)
                (a is FormulaValue.Num || a is FormulaValue.Empty) && (b is FormulaValue.Num || b is FormulaValue.Empty) ->
                    number(a, op).compareTo(number(b, op))
                a is FormulaValue.Empty || b is FormulaValue.Empty -> return false
                else -> typeError(op)
            }
            return when (op) {
                "<" -> c < 0
                "<=" -> c <= 0
                ">" -> c > 0
                else -> c >= 0
            }
        }

        // --- Le funzioni ---

        private fun call(name: String, args: List<Expr>): FormulaValue {
            fun arity(min: Int, max: Int = min) {
                if (args.size < min || args.size > max) throw FormulaException(FormulaError(FormulaError.Kind.WRONG_ARGUMENTS, name))
            }
            fun arg(i: Int) = eval(args[i])
            fun num(i: Int) = number(arg(i), name)
            fun str(i: Int) = text(arg(i))
            fun all() = args.map { eval(it) }
            return when (name.lowercase()) {
                // Logica
                "if" -> { arity(3); if (truthy(arg(0))) arg(1) else arg(2) }
                "ifs" -> {
                    if (args.size < 3 || args.size % 2 == 0) throw FormulaException(FormulaError(FormulaError.Kind.WRONG_ARGUMENTS, name))
                    var i = 0
                    while (i + 1 < args.size) {
                        if (truthy(arg(i))) return arg(i + 1)
                        i += 2
                    }
                    arg(args.lastIndex)
                }
                "and" -> { arity(1, 99); FormulaValue.Bool(args.all { truthy(eval(it)) }) }
                "or" -> { arity(1, 99); FormulaValue.Bool(args.any { truthy(eval(it)) }) }
                "not" -> { arity(1); FormulaValue.Bool(!truthy(arg(0))) }
                "empty" -> { arity(1); FormulaValue.Bool(isEmpty(arg(0))) }
                "equal" -> { arity(2); FormulaValue.Bool(same(arg(0), arg(1))) }
                "unequal" -> { arity(2); FormulaValue.Bool(!same(arg(0), arg(1))) }
                // Numeri
                "abs" -> { arity(1); FormulaValue.Num(abs(num(0))) }
                "round" -> {
                    arity(1, 2)
                    val places = if (args.size == 2) num(1).toInt() else 0
                    // valueOf, non il costruttore: 2.345 è 2.345, non 2.34499999…, e si arrotonda a 2.35.
                    val n = num(0)
                    FormulaValue.Num(if (n.isFinite()) BigDecimal.valueOf(n).setScale(places.coerceIn(-15, 15), java.math.RoundingMode.HALF_UP).toDouble() else n)
                }
                "floor" -> { arity(1); FormulaValue.Num(floor(num(0))) }
                "ceil" -> { arity(1); FormulaValue.Num(ceil(num(0))) }
                "sqrt" -> { arity(1); FormulaValue.Num(sqrt(num(0))) }
                "pow" -> { arity(2); FormulaValue.Num(num(0).pow(num(1))) }
                "mod" -> {
                    arity(2)
                    val d = num(1)
                    if (d == 0.0) throw FormulaException(FormulaError(FormulaError.Kind.DIVISION_BY_ZERO))
                    FormulaValue.Num(num(0) % d)
                }
                "sign" -> { arity(1); FormulaValue.Num(num(0).sign) }
                "exp" -> { arity(1); FormulaValue.Num(kotlin.math.exp(num(0))) }
                "ln" -> { arity(1); FormulaValue.Num(ln(num(0))) }
                "log10" -> { arity(1); FormulaValue.Num(log10(num(0))) }
                "log2" -> { arity(1); FormulaValue.Num(log2(num(0))) }
                "min" -> { arity(1, 99); FormulaValue.Num(numbersOf(all(), name).minOrNull() ?: 0.0) }
                "max" -> { arity(1, 99); FormulaValue.Num(numbersOf(all(), name).maxOrNull() ?: 0.0) }
                "sum" -> { arity(1, 99); FormulaValue.Num(numbersOf(all(), name).sum()) }
                "mean", "average" -> { arity(1, 99); numbersOf(all(), name).let { if (it.isEmpty()) FormulaValue.Empty else FormulaValue.Num(it.average()) } }
                "tonumber" -> {
                    arity(1)
                    when (val v = arg(0)) {
                        is FormulaValue.Num -> v
                        is FormulaValue.Bool -> FormulaValue.Num(if (v.value) 1.0 else 0.0)
                        is FormulaValue.Date -> FormulaValue.Num(v.millis.toDouble())
                        is FormulaValue.Text -> v.value.trim().replace(',', '.').toDoubleOrNull()?.let { FormulaValue.Num(it) } ?: FormulaValue.Empty
                        else -> FormulaValue.Empty
                    }
                }
                "pi" -> { arity(0); FormulaValue.Num(Math.PI) }
                "e" -> { arity(0); FormulaValue.Num(Math.E) }
                // Testo
                "concat" -> { arity(1, 99); FormulaValue.Text(all().joinToString("") { text(it) }) }
                "join" -> {
                    // join(", ", a, b, c): i valori non vuoti, col separatore.
                    arity(2, 99)
                    val sep = str(0)
                    FormulaValue.Text(args.drop(1).map { eval(it) }.filterNot { isEmpty(it) }.joinToString(sep) { text(it) })
                }
                "length" -> { arity(1); FormulaValue.Num(str(0).length.toDouble()) }
                "lower" -> { arity(1); FormulaValue.Text(str(0).lowercase()) }
                "upper" -> { arity(1); FormulaValue.Text(str(0).uppercase()) }
                "trim" -> { arity(1); FormulaValue.Text(str(0).trim()) }
                "contains" -> { arity(2); FormulaValue.Bool(str(0).contains(str(1), ignoreCase = true)) }
                "startswith" -> { arity(2); FormulaValue.Bool(str(0).startsWith(str(1), ignoreCase = true)) }
                "endswith" -> { arity(2); FormulaValue.Bool(str(0).endsWith(str(1), ignoreCase = true)) }
                "test" -> { arity(2); FormulaValue.Bool(regex(str(1)).containsMatchIn(str(0))) }
                "replace" -> { arity(3); FormulaValue.Text(regex(str(1)).replaceFirst(str(0), str(2))) }
                "replaceall" -> { arity(3); FormulaValue.Text(regex(str(1)).replace(str(0), str(2))) }
                "slice", "substring" -> {
                    arity(2, 3)
                    val s = str(0)
                    val start = num(1).toInt().coerceIn(0, s.length)
                    val end = if (args.size == 3) num(2).toInt().coerceIn(start, s.length) else s.length
                    FormulaValue.Text(s.substring(start, end))
                }
                "repeat" -> { arity(2); FormulaValue.Text(str(0).repeat(num(1).toInt().coerceIn(0, 1000))) }
                "format", "tostring" -> { arity(1); FormulaValue.Text(text(arg(0))) }
                // Date
                "now" -> { arity(0); FormulaValue.Date(scope.now, hasTime = true) }
                "today" -> { arity(0); FormulaValue.Date(startOfDay(scope.now), hasTime = false) }
                "dateadd" -> { arity(3); shift(date(arg(0), name), num(1).toLong(), str(2)) }
                "datesubtract" -> { arity(3); shift(date(arg(0), name), -num(1).toLong(), str(2)) }
                "datebetween" -> {
                    arity(3)
                    val a = date(arg(0), name)
                    val b = date(arg(1), name)
                    FormulaValue.Num(between(b.millis, a.millis, str(2)).toDouble())
                }
                "datestart" -> { arity(1); date(arg(0), name).let { FormulaValue.Date(it.millis, it.hasTime) } }
                "dateend" -> { arity(1); date(arg(0), name).let { FormulaValue.Date(it.end ?: it.millis, it.hasTime) } }
                "formatdate" -> { arity(2); FormulaValue.Text(formatDate(date(arg(0), name), str(1))) }
                "year" -> { arity(1); FormulaValue.Num(zoned(date(arg(0), name).millis).year.toDouble()) }
                "month" -> { arity(1); FormulaValue.Num(zoned(date(arg(0), name).millis).monthValue.toDouble()) }
                "date" -> { arity(1); FormulaValue.Num(zoned(date(arg(0), name).millis).dayOfMonth.toDouble()) }
                // Il giorno della settimana: 1 il lunedì, 7 la domenica.
                "day" -> { arity(1); FormulaValue.Num(zoned(date(arg(0), name).millis).dayOfWeek.value.toDouble()) }
                "week" -> { arity(1); FormulaValue.Num(zoned(date(arg(0), name).millis).get(IsoFields.WEEK_OF_WEEK_BASED_YEAR).toDouble()) }
                "hour" -> { arity(1); FormulaValue.Num(zoned(date(arg(0), name).millis).hour.toDouble()) }
                "minute" -> { arity(1); FormulaValue.Num(zoned(date(arg(0), name).millis).minute.toDouble()) }
                "timestamp" -> { arity(1); FormulaValue.Num(date(arg(0), name).millis.toDouble()) }
                "fromtimestamp" -> { arity(1); FormulaValue.Date(num(0).toLong(), hasTime = true) }
                "parsedate" -> {
                    arity(1)
                    val day = runCatching { LocalDate.parse(str(0).trim()) }.getOrNull() ?: throw FormulaException(FormulaError(FormulaError.Kind.TYPE, name))
                    FormulaValue.Date(day.atStartOfDay(scope.zone).toInstant().toEpochMilli())
                }
                else -> throw FormulaException(FormulaError(FormulaError.Kind.UNKNOWN_FUNCTION, name))
            }
        }

        // --- I tipi ---

        private fun typeError(where: String): Nothing = throw FormulaException(FormulaError(FormulaError.Kind.TYPE, where))

        private fun number(v: FormulaValue, where: String): Double = when (v) {
            is FormulaValue.Num -> v.value
            is FormulaValue.Empty -> 0.0
            is FormulaValue.Bool -> if (v.value) 1.0 else 0.0
            else -> typeError(where)
        }

        private fun numbersOf(values: List<FormulaValue>, where: String): List<Double> =
            values.filterNot { it is FormulaValue.Empty }.map { number(it, where) }

        private fun date(v: FormulaValue, where: String): FormulaValue.Date =
            v as? FormulaValue.Date ?: if (v is FormulaValue.Empty) throw FormulaException(FormulaError(FormulaError.Kind.TYPE, where)) else typeError(where)

        fun text(v: FormulaValue): String = when (v) {
            is FormulaValue.Text -> v.value
            is FormulaValue.Num -> scope.format.number(clean(v.value))
            is FormulaValue.Bool -> v.value.toString()
            is FormulaValue.Date -> if (v.hasTime) scope.format.dateTime(v.millis) else scope.format.date(v.millis)
            is FormulaValue.Empty -> ""
            is FormulaValue.Error -> throw FormulaException(v.error)
        }

        private fun isEmpty(v: FormulaValue) = when (v) {
            is FormulaValue.Empty -> true
            is FormulaValue.Text -> v.value.isEmpty()
            is FormulaValue.Num -> v.value == 0.0
            is FormulaValue.Bool -> !v.value
            else -> false
        }

        fun truthy(v: FormulaValue): Boolean = when (v) {
            is FormulaValue.Bool -> v.value
            is FormulaValue.Num -> v.value != 0.0
            is FormulaValue.Text -> v.value.isNotEmpty()
            is FormulaValue.Date -> true
            is FormulaValue.Empty -> false
            is FormulaValue.Error -> throw FormulaException(v.error)
        }

        private fun same(a: FormulaValue, b: FormulaValue): Boolean = when {
            a is FormulaValue.Num && b is FormulaValue.Num -> abs(a.value - b.value) < 1e-9
            a is FormulaValue.Text && b is FormulaValue.Text -> a.value == b.value
            a is FormulaValue.Bool && b is FormulaValue.Bool -> a.value == b.value
            a is FormulaValue.Date && b is FormulaValue.Date -> a.millis == b.millis
            a is FormulaValue.Empty -> isEmpty(b)
            b is FormulaValue.Empty -> isEmpty(a)
            else -> false
        }

        private fun regex(pattern: String): Regex =
            runCatching { Regex(pattern) }.getOrElse { Regex(Regex.escape(pattern)) }

        // --- Le date ---

        private fun zoned(millis: Long): ZonedDateTime = Instant.ofEpochMilli(millis).atZone(scope.zone)

        private fun startOfDay(millis: Long): Long = zoned(millis).toLocalDate().atStartOfDay(scope.zone).toInstant().toEpochMilli()

        /** L'unità di misura di un nome ("days", "giorni"…) e quante ne vale: un trimestre sono tre mesi. */
        private fun unit(name: String): Pair<ChronoUnit, Long> = when (name.trim().lowercase()) {
            "years", "year", "anni", "anno" -> ChronoUnit.YEARS to 1L
            "quarters", "quarter", "trimestri", "trimestre" -> ChronoUnit.MONTHS to 3L
            "months", "month", "mesi", "mese" -> ChronoUnit.MONTHS to 1L
            "weeks", "week", "settimane", "settimana" -> ChronoUnit.WEEKS to 1L
            "days", "day", "giorni", "giorno" -> ChronoUnit.DAYS to 1L
            "hours", "hour", "ore", "ora" -> ChronoUnit.HOURS to 1L
            "minutes", "minute", "minuti", "minuto" -> ChronoUnit.MINUTES to 1L
            else -> throw FormulaException(FormulaError(FormulaError.Kind.TYPE, name))
        }

        private fun between(from: Long, to: Long, unitName: String): Long {
            val (u, size) = unit(unitName)
            return u.between(zoned(from), zoned(to)) / size
        }

        private fun shift(d: FormulaValue.Date, amount: Long, unitName: String): FormulaValue.Date {
            val (u, size) = unit(unitName)
            val moved = zoned(d.millis).plus(amount * size, u).toInstant().toEpochMilli()
            return FormulaValue.Date(moved, d.hasTime || u == ChronoUnit.HOURS || u == ChronoUnit.MINUTES)
        }

        /** I segni di Notion (Day.js): YYYY, MM, DD, dddd, HH, mm, A…; quello fra [ ] resta com'è. */
        private fun formatDate(d: FormulaValue.Date, pattern: String): String {
            val tokens = listOf(
                "YYYY" to "yyyy", "YY" to "yy", "MMMM" to "MMMM", "MMM" to "MMM", "MM" to "MM", "M" to "M",
                "dddd" to "EEEE", "ddd" to "EEE", "DD" to "dd", "D" to "d", "HH" to "HH", "H" to "H",
                "hh" to "hh", "h" to "h", "mm" to "mm", "m" to "m", "ss" to "ss", "s" to "s", "A" to "a", "a" to "a"
            )
            val java = StringBuilder()
            var i = 0
            var literal = StringBuilder()
            fun flush() {
                if (literal.isNotEmpty()) {
                    java.append('\'').append(literal.toString().replace("'", "''")).append('\'')
                    literal = StringBuilder()
                }
            }
            while (i < pattern.length) {
                // [testo]: si scrive com'è, come in Day.js ("[Scade il] D").
                if (pattern[i] == '[') {
                    val close = pattern.indexOf(']', i + 1)
                    if (close > i) {
                        literal.append(pattern, i + 1, close)
                        i = close + 1
                        continue
                    }
                }
                val match = tokens.firstOrNull { pattern.startsWith(it.first, i) }
                if (match != null) {
                    flush()
                    java.append(match.second)
                    i += match.first.length
                } else {
                    literal.append(pattern[i])
                    i++
                }
            }
            flush()
            return runCatching {
                DateTimeFormatter.ofPattern(java.toString(), scope.locale).format(zoned(d.millis))
            }.getOrElse { throw FormulaException(FormulaError(FormulaError.Kind.TYPE, "formatDate")) }
        }
    }

    /**
     * **Una proprietà rinominata**: in [expression] ogni `prop("[old]")`
     * diventa `prop("[new]")`, così la formula continua a leggerla. Le
     * maiuscole del nome vecchio non contano, come quando si calcola.
     */
    fun renameProperty(expression: String, old: String, new: String): String {
        if (old == new || old.isEmpty()) return expression
        val quotes = "[\"“”']"
        val pattern = Regex("prop\\s*\\(\\s*$quotes${Regex.escape(old)}$quotes\\s*\\)", RegexOption.IGNORE_CASE)
        // Con la funzione il testo nuovo si scrive com'è: "$" e "\" non sono segni da leggere.
        val escaped = new.replace("\\", "\\\\").replace("\"", "\\\"")
        return pattern.replace(expression) { "prop(\"$escaped\")" }
    }

    // --- Come si legge e come si ordina ---

    /** Senza i decimali sbagliati dei calcoli (0,1 + 0,2 fa 0,3), e senza "-0". */
    fun clean(value: Double): Double {
        if (value.isNaN() || value.isInfinite()) return value
        val rounded = BigDecimal(value).round(MathContext(12)).toDouble()
        return if (rounded == 0.0) 0.0 else rounded
    }

    /**
     * Il testo di un risultato, come si legge nelle celle, nelle schede e
     * nella ricerca. Una casella spuntata è "✓"; un errore non ha testo.
     */
    fun display(value: FormulaValue, format: RollupFormatter): String = when (value) {
        is FormulaValue.Num -> format.number(clean(value.value))
        is FormulaValue.Text -> value.value
        is FormulaValue.Bool -> if (value.value) "✓" else ""
        is FormulaValue.Date -> (if (value.hasTime) format.dateTime(value.millis) else format.date(value.millis)) +
            (value.end?.let { " → " + if (value.hasTime) format.dateTime(it) else format.date(it) } ?: "")
        is FormulaValue.Empty, is FormulaValue.Error -> ""
    }

    /** Il numero con cui si ordina: il numero, l'istante della data, 1 o 0 per una casella. */
    fun sortNumber(value: FormulaValue?): Double? = when (value) {
        is FormulaValue.Num -> value.value
        is FormulaValue.Date -> value.millis.toDouble()
        is FormulaValue.Bool -> if (value.value) 1.0 else 0.0
        else -> null
    }}
