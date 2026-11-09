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

compose.desktop {
    application {
        mainClass = "com.gabriele.notionlocal.desktop.MainKt"
    }
}
