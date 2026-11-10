import org.jetbrains.compose.desktop.application.dsl.TargetFormat
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

/**
 * **PageFlow per Windows e Mac** (dal 04/11/2026). Compose Multiplatform per
 * il PC: usa il codice comune del modulo `shared`, lo stesso del telefono.
 * Si prova con `gradle :desktop:run`.
 */
plugins {
    id("org.jetbrains.kotlin.jvm")
    id("org.jetbrains.kotlin.plugin.compose")
    id("org.jetbrains.compose")
}

kotlin {
    compilerOptions { jvmTarget.set(JvmTarget.JVM_17) }
}

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

dependencies {
    implementation(project(":shared"))
    implementation(compose.desktop.currentOs)
    // Le schermate del PC disegnate in un'immagine, senza aprire finestre.
    testImplementation("junit:junit:4.13.2")
    // Per preparare nei test un database di una versione vecchia (DesktopBackupTest): la stessa di `shared`.
    testImplementation("androidx.sqlite:sqlite-bundled:2.6.2")
    // Per leggere nei test il PDF fatto sul PC (`DesktopPdfTest`).
    testImplementation("org.apache.pdfbox:pdfbox:3.0.5")
}

/**
 * **Le Patch Notes anche sul PC**: come sul telefono (vedi `PatchNotesTask` in
 * `app/build.gradle.kts`), la Cronologia del README copiata fra le risorse a
 * ogni compilazione; con `-Ppersonal` quella di `private/README.md`.
 */
val personalBuild = providers.gradleProperty("personal").isPresent
val readmeFile = rootProject.layout.projectDirectory.file(if (personalBuild) "private/README.md" else "README.md")
val patchNotesDir = layout.buildDirectory.dir("generated/patchNotes")
val patchNotes = tasks.register("patchNotes") {
    inputs.file(readmeFile)
    outputs.dir(patchNotesDir)
    doLast {
        val text = readmeFile.asFile.readText(Charsets.UTF_8).removePrefix("﻿")
        val start = text.indexOf("## Cronologia degli aggiornamenti")
        check(start >= 0) { "Nel README manca la Cronologia degli aggiornamenti" }
        val end = text.indexOf("\n## ", start + 1).let { if (it < 0) text.length else it }
        val out = patchNotesDir.get().file("patch_notes.md").asFile
        out.parentFile.mkdirs()
        out.writeText(text.substring(start, end), Charsets.UTF_8)
    }
}

sourceSets.main {
    // Le Patch Notes e l'icona (la stessa del telefono, cartella `branding`).
    resources.srcDir(patchNotes)
    resources.srcDir(rootProject.file("branding"))
    // I file che il telefono ha negli asset (le emoji del selettore, i
    // dizionari…), gli stessi: vedi `readAppAsset` (dal 04/11/2026).
    resources.srcDir(rootProject.file("app/src/main/assets"))
}

/**
 * **Il numero di versione per Windows** (dal 09/11/2026): quello dell'ultima
 * voce del README, ma Windows ne vuole tre ("0.153.14" diventa 0.153.1400,
 * "0.153.11.1" diventa 0.153.1101): così cresce sempre, e l'installatore
 * riconosce un aggiornamento.
 */
// Per le prove fra una versione e l'altra: -PinstallerVersion=0.153.2401. Con lo stesso numero di quello
// installato, l'installatore di Windows non fa niente (successo il 10/11/2026).
val installerVersion: String = providers.gradleProperty("installerVersion").orNull ?: run {
    val readme = rootProject.file("README.md").readText(Charsets.UTF_8)
    val latest = Regex("""^### \S+ v(\d+)\.(\d+)(?:\.(\d+))?(?:\.(\d+))? — """, RegexOption.MULTILINE).find(readme)
        ?: error("Nel README non c'è una versione")
    val (major, minor, change, fix) = latest.destructured
    "$major.$minor.${(change.toIntOrNull() ?: 0) * 100 + (fix.toIntOrNull() ?: 0)}"
}

compose.desktop {
    application {
        mainClass = "com.gabriele.notionlocal.desktop.MainKt"
        /**
         * **L'installatore per Windows** (dal 09/11/2026): `gradle :desktop:packageExe`,
         * in `desktop/build/compose/binaries/main/exe`. Si installa per l'utente
         * (senza permessi da amministratore), con la voce nel menu Start e il
         * collegamento sul desktop. I dati restano in `%APPDATA%\PageFlow`.
         */
        nativeDistributions {
            targetFormats(TargetFormat.Exe)
            // Quelli che trova `suggestRuntimeModules` (java.net.http: il Web Clipper parla con Edge), più quelli che si caricano per nome:
            // date e mesi nelle otto lingue, le connessioni sicure (meteo, sport…), le pagine web non UTF-8.
            modules("java.instrument", "java.net.http", "jdk.unsupported", "jdk.localedata", "jdk.crypto.ec", "jdk.charsets")
            packageName = "PageFlow"
            packageVersion = installerVersion
            description = "PageFlow"
            vendor = "PageFlow"
            windows {
                iconFile.set(rootProject.file("branding/pageflow.ico"))
                menuGroup = "PageFlow"
                menu = true
                shortcut = true
                perUserInstall = true
                // Sempre lo stesso: è così che Windows riconosce un aggiornamento della stessa app.
                upgradeUuid = "8c0f3c8e-6923-461c-ad01-e3f35518e769"
            }
        }
    }
}
