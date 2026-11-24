package com.gabriele.notionlocal.data.entity

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.math.BigDecimal

/**
 * Le colonne Number: cifre decimali, valute e percentuale, barra e anello,
 * e i calcoli sotto la tabella.
 */
class NumberFormatTest {

    @Test
    fun decimalsHideTheExtraDigitsWithoutRoundingAndPadTheMissingOnes() {
        // L'esempio dell'utente: 45.76 con una cifra mostra 45.7, con tre 45.760.
        assertEquals("45.7", numberWithDecimals("45.76", 1))
        assertEquals("45.760", numberWithDecimals("45.76", 3))
        assertEquals("45", numberWithDecimals("45.76", 0))
        assertEquals("45.76", numberWithDecimals("45.76", null))
        assertEquals(null, numberWithDecimals("ciao", 2))
    }

    @Test
    fun currenciesAndPercentGoAroundTheNumber() {
        assertEquals("$1,000", NumberFormatKind.USD.decorate("1,000"))
        assertEquals("-€5", NumberFormatKind.EUR.decorate("-5"))
        assertEquals("100 kr", NumberFormatKind.SEK.decorate("100"))
        assertEquals("100 zł", NumberFormatKind.PLN.decorate("100"))
        assertEquals("45%", NumberFormatKind.PERCENT.decorate("45"))
        assertEquals("45", NumberFormatKind.NUMBER.decorate("45"))
        // Un nome che l'app non conosce non la fa chiudere: torna Number.
        assertEquals(NumberFormatKind.NUMBER, NumberFormatKind.of("BTC"))
    }

    @Test
    fun theCurrenciesAreInTheOrderTheUserAskedFor() {
        assertEquals(
            listOf("USD", "EUR", "GBP", "CHF", "CNY", "JPY", "KRW", "AUD", "CAD", "SGD", "RUB", "INR", "HKD", "NZD", "SEK", "NOK", "MXN", "TWD", "DKK", "PLN"),
            NumberFormatKind.entries.filter { it.isCurrency }.map { it.name }
        )
    }

    @Test
    fun barAndRingAreTheNumberOverAHundred() {
        assertEquals(0.72f, numberFill("72"), 0.0001f)
        assertEquals(1f, numberFill("150"), 0f)
        assertEquals(0f, numberFill("-3"), 0f)
        assertEquals(0f, numberFill(""), 0f)
    }

    @Test
    fun theCalculationsOfTheUserScreenshots() {
        val numbers = listOf("72", "99", "3", "41", "17", "20", "56", "1")
        fun number(calc: Calculation) = (calculate(calc, ColumnType.NUMBER, numbers) as CalculationResult.Number).value
        assertEquals(BigDecimal(309), number(Calculation.SUM))
        assertEquals(BigDecimal(99), number(Calculation.MAX))
        assertEquals(BigDecimal(1), number(Calculation.MIN))
        assertEquals(BigDecimal(98), number(Calculation.RANGE))
        assertEquals(0, BigDecimal("30.5").compareTo(number(Calculation.MEDIAN)))
        assertEquals(0, BigDecimal("38.63").compareTo(number(Calculation.AVERAGE)))

        val values = listOf("8", "", "16", "32")
        assertEquals(CalculationResult.Count(4), calculate(Calculation.COUNT_ALL, ColumnType.NUMBER, values))
        assertEquals(CalculationResult.Count(1), calculate(Calculation.COUNT_EMPTY, ColumnType.NUMBER, values))
        assertEquals(CalculationResult.Percent(75.0), calculate(Calculation.PERCENT_NOT_EMPTY, ColumnType.NUMBER, values))
    }

    /**
     * **La ~ davanti a un numero lo toglie dai calcoli** (dal 24/11/2026, chiesto dall'utente per i
     * prezzi della Wishlist): somma e media contano solo gli altri, ma la barra lo legge.
     */
    @Test
    fun anEstimateIsLeftOutOfTheCalculations() {
        val prices = listOf("10", "~25.50", "30")
        assertEquals(CalculationResult.Number(BigDecimal(40)), calculate(Calculation.SUM, ColumnType.NUMBER, prices))
        assertEquals(0, BigDecimal(20).compareTo((calculate(Calculation.AVERAGE, ColumnType.NUMBER, prices) as CalculationResult.Number).value))
        assertEquals(CalculationResult.Count(3), calculate(Calculation.COUNT_NOT_EMPTY, ColumnType.NUMBER, prices))
        assertEquals(0.255f, numberFill("~25.50"), 0.0001f)
        assertEquals("25.50", "~ 25.50".withoutEstimate())
    }

    @Test
    fun checkboxesAndMultiSelectCountTheirOwnWay() {
        val boxes = listOf("true", "false", "", "true")
        assertEquals(CalculationResult.Count(2), calculate(Calculation.CHECKED, ColumnType.CHECKBOX, boxes))
        assertEquals(CalculationResult.Count(2), calculate(Calculation.UNCHECKED, ColumnType.CHECKBOX, boxes))
        assertEquals(listOf(Calculation.COUNT_ALL, Calculation.CHECKED, Calculation.UNCHECKED), calculationsFor(ColumnType.CHECKBOX)[CalculationGroup.COUNT])

        val tags = listOf("PS5|PC", "PC", "", "XBOX")
        assertEquals(CalculationResult.Count(4), calculate(Calculation.COUNT_VALUES, ColumnType.MULTI_SELECT, tags))
        assertEquals(CalculationResult.Count(3), calculate(Calculation.COUNT_UNIQUE, ColumnType.MULTI_SELECT, tags))
    }

    @Test
    fun onlyDatesHaveDateAndOnlyNumbersHaveMoreOptions() {
        assertTrue(CalculationGroup.DATE in calculationsFor(ColumnType.DATE))
        assertFalse(CalculationGroup.MORE in calculationsFor(ColumnType.DATE))
        assertTrue(CalculationGroup.MORE in calculationsFor(ColumnType.NUMBER))
        assertFalse(CalculationGroup.DATE in calculationsFor(null))

        val day = 86_400_000L
        val dates = listOf("1000", "", "${1000 + 12 * day}")
        val range = calculate(Calculation.DATE_RANGE, ColumnType.DATE, dates, startOfDate = { it.toLongOrNull() })
        assertEquals(CalculationResult.Days(12), range)
    }

    @Test
    fun theCalculationsOfAViewAreSavedAndReadBack() {
        val calcs = mapOf(NAME_CALCULATION_KEY to Calculation.COUNT_ALL, "c1" to Calculation.SUM)
        assertEquals(calcs, parseViewCalculations(encodeViewCalculations(calcs)))
        assertEquals(null, encodeViewCalculations(emptyMap()))
        assertEquals(emptyMap<String, Calculation>(), parseViewCalculations("c1=NON_ESISTE"))
    }

    @Test
    fun whereEachResultStandsIsSavedNextToTheCalculations() {
        val calcs = mapOf("c1" to Calculation.SUM)
        val aligns = mapOf("c1" to CellAlignment.CENTER, NAME_CALCULATION_KEY to CellAlignment.LEFT)
        val text = encodeViewCalculations(calcs, aligns)
        assertEquals(calcs, parseViewCalculations(text))
        assertEquals(aligns, parseCalculationAlignments(text))
        // I calcoli salvati prima non hanno allineamenti: stanno a destra.
        assertEquals(emptyMap<String, CellAlignment>(), parseCalculationAlignments("c1=SUM"))
    }
}
