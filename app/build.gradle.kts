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
    implementation(compose.desktop.currentOs)
    implementation(libs.compose.material3)
    implementation(libs.compose.material.icons.core)
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
            // El progreso se guarda con java.util.prefs, que no entra en el runtime mínimo por defecto
            modules("java.prefs")

            macOS {
                bundleID = "com.example.appbiblialeeer.septiembre"
                iconFile.set(project.file("icono.icns"))
            }
        }
    }
}
