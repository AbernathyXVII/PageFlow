import org.jetbrains.kotlin.gradle.dsl.JvmTarget

/**
 * **Il codice comune al telefono e al PC** (dal 04/11/2026, verso Windows e Mac).
 *
 * Kotlin Multiplatform con due destinazioni, entrambe sulla JVM: `android`
 * (l'app del telefono, modulo `app`) e `desktop` (Windows e Mac, modulo
 * `desktop`). Il codice sta in `commonMain` e i pacchetti sono quelli di
 * prima (`com.gabriele.notionlocal…`): chi lo usa non vede differenze.
 * Visto che le due destinazioni sono JVM, `commonMain` può usare anche Java
 * (`java.time`, `java.util.UUID`…). Quello che cambia fra telefono e PC
 * (dove si salvano le impostazioni, per esempio) sta in `androidMain` e
 * `desktopMain`, con `expect`/`actual`.
 *
 * Il codice ci arriva un pezzo per volta dal modulo `app`: vedi la Cronologia
 * nel README.
 */
plugins {
    id("org.jetbrains.kotlin.multiplatform")
    id("com.android.library")
    id("org.jetbrains.kotlin.plugin.compose")
    id("org.jetbrains.compose")
    id("org.jetbrains.kotlin.plugin.serialization")
    // Room scrive da sé il codice del database (DAO e tabelle), per ogni piattaforma.
    id("com.google.devtools.ksp")
}

kotlin {
    androidTarget {
        compilerOptions { jvmTarget.set(JvmTarget.JVM_17) }
    }
    jvm("desktop") {
        compilerOptions { jvmTarget.set(JvmTarget.JVM_17) }
    }
    compilerOptions {
        // `expect class` (vedi `PlatformContext`) è ancora "Beta" per Kotlin.
        freeCompilerArgs.add("-Xexpect-actual-classes")
    }

    sourceSets {
        commonMain.dependencies {
            // Compose Multiplatform 1.11: sul telefono sono le librerie Compose
            // 1.11 di Google, le stesse dell'app (material3 1.9.0 = 1.4.0 di Google).
            api("org.jetbrains.compose.runtime:runtime:1.11.1")
            api("org.jetbrains.compose.foundation:foundation:1.11.1")
            api("org.jetbrains.compose.ui:ui:1.11.1")
            api("org.jetbrains.compose.material3:material3:1.9.0")
            api("org.jetbrains.compose.material:material-icons-extended:1.7.3")
            api("org.jetbrains.kotlinx:kotlinx-serialization-json:1.9.0")
            api("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.10.2")
            // Room multipiattaforma: le tabelle, i DAO e `AppDatabase` (dal
            // 04/11/2026 qui e non più nel modulo `app`).
            api("androidx.room:room-runtime:2.8.5")
            // I ViewModel (dal 04/11/2026): sul telefono è lifecycle 2.9.4 di
            // Google, la stessa che l'app usava già.
            api("org.jetbrains.androidx.lifecycle:lifecycle-viewmodel:2.9.5")
            // Le schermate: `collectAsStateWithLifecycle` e `viewModel(...)`.
            api("org.jetbrains.androidx.lifecycle:lifecycle-runtime-compose:2.9.5")
            api("org.jetbrains.androidx.lifecycle:lifecycle-viewmodel-compose:2.9.5")
            // La navigazione fra le schermate (dal 09/11/2026): la stessa di Android, multipiattaforma
            // (sul telefono è navigation-compose di Google). 2.9.1 va con lifecycle 2.9.5.
            api("org.jetbrains.androidx.navigation:navigation-compose:2.9.1")
        }
        androidMain.dependencies {
            // L'orientamento scritto dentro le foto (EXIF): vedi `PageImageStore.android.kt`.
            implementation("androidx.exifinterface:exifinterface:1.3.7")
            // **Dal 04/11/2026 qui c'è tutta l'app del telefono** (schermate,
            // notifiche, backup…), tranne le attività: le stesse librerie che
            // aveva il modulo `app`, alle stesse versioni.
            implementation(project.dependencies.platform("androidx.compose:compose-bom:2026.06.01"))
            implementation("androidx.compose.ui:ui-text-google-fonts")
            implementation("androidx.compose.ui:ui-tooling-preview")
            implementation("androidx.core:core-ktx:1.13.1")
            implementation("androidx.activity:activity-compose:1.9.2")
            implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.4")
            implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.4")
            implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.4")
            implementation("androidx.room:room-ktx:2.8.5")
            implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.1")
            implementation("com.google.android.gms:play-services-mlkit-document-scanner:16.0.0")
            implementation("com.google.android.gms:play-services-mlkit-text-recognition:19.0.1")
            implementation("com.google.android.gms:play-services-mlkit-text-recognition-chinese:16.0.1")
            implementation("com.google.android.gms:play-services-mlkit-text-recognition-japanese:16.0.1")
            implementation("com.google.android.gms:play-services-mlkit-text-recognition-korean:16.0.1")
        }
        getByName("desktopMain").dependencies {
            // Il "filo principale" dei ViewModel sul PC è quello di Swing, la
            // finestra: senza, `viewModelScope` non sa dove lavorare.
            implementation("org.jetbrains.kotlinx:kotlinx-coroutines-swing:1.10.2")
            // Sul PC l'SQLite viaggia dentro l'app (su Windows non ce n'è uno di sistema).
            implementation("androidx.sqlite:sqlite-bundled:2.6.2")
            // Il tasto Esc come "indietro" (vedi `BackHandler.desktop.kt`): la stessa che usa già la navigazione.
            implementation("org.jetbrains.compose.ui:ui-backhandler:1.11.1")
            // Il PDF sul PC (vedi `PdfBackend.desktop.kt`): il telefono lo scrive con Android.
            implementation("org.apache.pdfbox:pdfbox:3.0.5")
        }
    }
}

dependencies {
    add("kspAndroid", "androidx.room:room-compiler:2.8.5")
    add("kspDesktop", "androidx.room:room-compiler:2.8.5")
}

android {
    namespace = "com.gabriele.notionlocal.shared"
    compileSdk = 35
    defaultConfig {
        minSdk = 26
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}
