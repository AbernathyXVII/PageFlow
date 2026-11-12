plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
    id("org.jetbrains.kotlin.plugin.serialization")
}

/**
 * **Le Patch Notes** (dal 30/09/2026, chiesto dall'utente): la Cronologia
 * degli aggiornamenti del README, copiata fra gli asset dell'app a ogni
 * compilazione, e letta da Settings → Patch Notes. Una copia sola, quella
 * del README: l'app non può restare indietro.
 */
abstract class PatchNotesTask : DefaultTask() {
    @get:InputFile
    @get:PathSensitive(PathSensitivity.NONE)
    abstract val readme: RegularFileProperty

    @get:OutputDirectory
    abstract val outputDir: DirectoryProperty

    @TaskAction
    fun copy() {
        val text = readme.get().asFile.readText(Charsets.UTF_8).removePrefix("\uFEFF")
        val start = text.indexOf("## Changelog")
        check(start >= 0) { "Nel README manca la sezione \"## Changelog\"" }
        val end = text.indexOf("\n## ", start + 1).let { if (it < 0) text.length else it }
        val out = outputDir.get().file("patch_notes.md").asFile
        out.parentFile.mkdirs()
        out.writeText(text.substring(start, end), Charsets.UTF_8)
    }
}

/**
 * **Le richieste dell'utente restano sue** (dal 03/11/2026): il README su
 * GitHub non le ha più. La copia completa, con le richieste parola per parola,
 * è `private/README.md`, che resta sul PC. L'app per il telefono dell'utente si
 * compila con `-Ppersonal` e le sue Patch Notes vengono da lì; quella della
 * Release, senza, dal README.
 */
val personalBuild = providers.gradleProperty("personal").isPresent

val patchNotes = tasks.register<PatchNotesTask>("patchNotes") {
    readme.set(rootProject.layout.projectDirectory.file(if (personalBuild) "private/README.md" else "README.md"))
}

// **La versione dell'app è l'ultima delle Patch Notes**: il numero del primo
// titolo "### 🆕 v0.78 — …" della Cronologia, fino a quattro parti (v0.N.M.K,
// dal 21/10/2026). `versionCode` è quante versioni ci sono, così cresce sempre,
// anche quando la Cronologia si rinumera.
val patchVersions = Regex("""^### \S+ v(\d+\.\d+(?:\.\d+){0,2}) — """, RegexOption.MULTILINE)
    .findAll(rootProject.file("README.md").readText(Charsets.UTF_8))
    .map { it.groupValues[1] }
    .toList()

android {
    // Robolectric legge il manifest unito, che contiene l'attività vuota
    // su cui i test disegnano le schermate (ui-test-manifest).
    testOptions {
        unitTests.isIncludeAndroidResources = true
    }

    namespace = "com.gabriele.notionlocal"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.gabriele.notionlocal"
        minSdk = 26
        targetSdk = 35
        versionCode = patchVersions.size.coerceAtLeast(1)
        versionName = patchVersions.firstOrNull() ?: "0.1"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            // **La versione da usare è questa** (dal 18/10/2026): la versione di
            // prova gira molto più lenta (misurato: tre volte, scrivendo in una
            // pagina lunga). Firmata con la stessa chiave della versione di
            // prova, si installa sopra e i dati restano; per leggere il
            // database dal PC si rimette la versione di prova.
            signingConfig = signingConfigs.getByName("debug")
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        compose = true
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
    }
}

androidComponents {
    onVariants { variant ->
        variant.sources.assets?.addGeneratedSourceDirectory(patchNotes, PatchNotesTask::outputDir)
    }
}

dependencies {
    // Il codice comune col PC (dal 04/11/2026): stessi pacchetti di prima,
    // quindi chi lo usa non se ne accorge. Vedi `shared/build.gradle.kts`.
    implementation(project(":shared"))

    // Core Android + Compose
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.4")
    implementation("androidx.activity:activity-compose:1.9.2")

    // Legge l'orientamento scritto dentro le foto (EXIF). Serve alle
    // copertine: le foto scattate col telefono ruotato sono salvate
    // "dritte" con dentro un'etichetta che dice di quanto girarle, e
    // chi non la legge le mostra coricate.
    implementation("androidx.exifinterface:exifinterface:1.3.7")

    val composeBom = platform("androidx.compose:compose-bom:2026.06.01")
    implementation(composeBom)
    androidTestImplementation(composeBom)

    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3:1.4.0")
    implementation("androidx.compose.material:material-icons-extended:1.7.8")

    // I font cinesi e giapponesi delle pagine, scaricati da Google Play
    // Services la prima volta che servono (pesano 10-20 MB l'uno: dentro
    // l'app la farebbero crescere di oltre cento). Vedi `PageFonts.kt`.
    implementation("androidx.compose.ui:ui-text-google-fonts")

    // **La scansione dei documenti e il testo delle foto** (dal 03/11/2026):
    // lo scanner e il lettore di testo di Google (ML Kit), dentro Google Play
    // Services come i font qui sopra: i modelli li scarica lui, l'app non
    // cresce. Il testo si legge in caratteri latini, cinesi, giapponesi e
    // coreani. Vedi `data/ocr/ImageTexts.kt`.
    implementation("com.google.android.gms:play-services-mlkit-document-scanner:16.0.0")
    implementation("com.google.android.gms:play-services-mlkit-text-recognition:19.0.1")
    implementation("com.google.android.gms:play-services-mlkit-text-recognition-chinese:16.0.1")
    implementation("com.google.android.gms:play-services-mlkit-text-recognition-japanese:16.0.1")
    implementation("com.google.android.gms:play-services-mlkit-text-recognition-korean:16.0.1")

    // Navigation tra le schermate

    // ViewModel + Compose integration
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.4")
    // collectAsStateWithLifecycle: raccoglie i Flow rispettando il ciclo
    // di vita della schermata (si ferma quando l'app va in background)
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.4")

    // Room (database SQLite locale) - il cuore del "tutto salvato sul telefono".
    // Dal 04/11/2026 il database e i DAO stanno nel modulo `shared`, che fa
    // anche da sé il codice di Room: qui resta Room per `withTransaction`.
    implementation("androidx.room:room-runtime:2.8.5")
    implementation("androidx.room:room-ktx:2.8.5")

    // Coroutines per operazioni asincrone sul database
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.1")

    // Serializzazione JSON per il rich text (RichTextSpan) e le opzioni SELECT.
    // Era ferma alla 1.6.3 finché il progetto usava Kotlin 1.9 (dal
    // 03/11/2026 è Kotlin 2): la 1.9.0 è quella costruita con Kotlin 2.2,
    // come Compose Multiplatform, che servirà per Windows e Mac.
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.9.0")

    // Debug tooling
    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")

    // Test
    testImplementation("junit:junit:4.13.2")
    // Test del repository su un database vero, senza telefono: Robolectric
    // fa girare Android (e quindi SQLite e Room) sulla JVM. Aggiunti il
    // 24/09/2026 per provare in cloud le operazioni che toccano i dati
    // (spostare, duplicare, annullare), che lì non si possono provare
    // sul telefono. Solo per i test: nell'app non entrano.
    testImplementation("org.robolectric:robolectric:4.17")
    testImplementation("androidx.test:core-ktx:1.7.0")
    // Per disegnare le schermate in un test e guardarle come immagine
    // (i widget della barra laterale, 24/09/2026): in cloud il telefono
    // non c'è, e questo è l'unico modo di vederle.
    testImplementation(composeBom)
    testImplementation("androidx.compose.ui:ui-test-junit4")
    androidTestImplementation("androidx.test.ext:junit:1.2.1")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.6.1")
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")
}
