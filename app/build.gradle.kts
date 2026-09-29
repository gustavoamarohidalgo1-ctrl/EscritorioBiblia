import org.jetbrains.compose.desktop.application.dsl.TargetFormat
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.compose.multiplatform)
}

// Compose Desktop necesita Java 17 o superior (jpackage empaqueta la app con ese JDK)
java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

kotlin {
    compilerOptions {
        jvmTarget = JvmTarget.JVM_17
    }
}

dependencies {
    // Compose para el sistema operativo donde se compila (en un Mac: macOS arm64 o x64)
    implementation(compose.desktop.currentOs) {
        // ✅ Trae Material 2 (≈1,5 MB) y la app solo usa Material 3
        exclude(group = "org.jetbrains.compose.material", module = "material")
    }
    implementation(libs.compose.material3)
    // Dispatchers.Main en escritorio (hilo de Swing)
    implementation(libs.kotlinx.coroutines.swing)
    testImplementation(libs.junit)
}

compose.desktop {
    application {
        mainClass = "com.example.appbiblialeeer.MainKt"

        nativeDistributions {
            // ./gradlew packageDmg → app/build/compose/binaries/main/dmg/
            targetFormats(TargetFormat.Dmg)
            packageName = "Mes de Septiembre"
            packageVersion = "1.0.0"
            description = "Plan de lectura bíblica de septiembre"
            // Módulos de Java: basta con los de Compose por defecto. java.util.prefs (el progreso) ya
            // entra porque java.desktop lo necesita; jdeps confirma que no hace falta ningún otro

            macOS {
                bundleID = "com.example.appbiblialeeer.septiembre"
                iconFile.set(project.file("icono.icns"))
            }
        }

        // ./gradlew packageReleaseDmg: la versión optimizada para instalar
        buildTypes.release.proguard {
            // ✅ ProGuard quita el código de las librerías (Compose, Kotlin, coroutines) que la app no usa
            isEnabled.set(true)
            optimize.set(true)
            // Sin ofuscar: si algo falla, el informe de error muestra los nombres reales
            obfuscate.set(false)
            // ✅ Un solo .jar: menos archivos que abrir y leer al arrancar
            joinOutputJars.set(true)
        }
    }
}
