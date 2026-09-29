import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.android.application)
    // AGP 9 ya compila Kotlin por sí mismo: no hace falta el plugin org.jetbrains.kotlin.android
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "com.example.appbiblialeeer"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.example.appbiblialeeer.septiembre"
        minSdk = 25
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"
    }

    androidResources {
        // La app está en español: no empaqueta las traducciones de las librerías a otros idiomas
        localeFilters += "es"
    }

    buildTypes {
        release {
            // R8: elimina el código y los recursos que no se usan y optimiza el resto
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            // Firmado con la clave de depuración para poder instalar el release directamente.
            // Para publicar en Google Play habría que usar una clave propia.
            signingConfig = signingConfigs.getByName("debug")
            // No mete en el APK la info del control de versiones (META-INF/version-control-info.textproto)
            vcsInfo {
                include = false
            }
            // Reglas de R8 de estas librerías que obligan a conservar código que aquí no se usa:
            // - startup-runtime conserva TODOS los inicializadores, también los que se quitan del
            //   manifest (EmojiCompat, ProcessLifecycle). Sus reglas se copian en proguard-rules.pro
            //   sin esos dos.
            // - versionedparcelable conserva IconCompat/RemoteActionCompat (notificaciones,
            //   accesos directos, multimedia), que la app no usa.
            optimization {
                keepRules {
                    ignoreFrom(
                        "androidx.startup:startup-runtime",
                        "androidx.versionedparcelable:versionedparcelable"
                    )
                }
            }
        }
    }
    dependenciesInfo {
        // El APK se instala directamente: sin la lista cifrada de dependencias en el bloque de firma
        // (≈4 KB). Se mantiene en el App Bundle porque Google Play la usa para avisos de SDK.
        includeInApk = false
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildFeatures {
        compose = true
    }
}

kotlin {
    compilerOptions {
        jvmTarget = JvmTarget.JVM_11
    }
}

// Strong skipping (Compose se salta más recomposiciones innecesarias) ya viene activado por defecto desde Kotlin 2.0.20

androidComponents {
    onVariants(selector().withBuildType("release")) { variant ->
        // Metadatos de las librerías que la app no necesita para funcionar
        variant.packaging.resources.excludes.addAll(
            "/META-INF/*.version",
            "/META-INF/*.kotlin_module",
            "/META-INF/{AL2.0,LGPL2.1}",
            "DebugProbesKt.bin",
            "kotlin-tooling-metadata.json",
            // Solo los lee kotlin-reflect, que la app no usa
            "/kotlin/**.kotlin_builtins"
        )
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.material.icons.core)
    // El AndroidManifest.xml configura su InitializationProvider (ya venía con las librerías)
    implementation(libs.androidx.startup.runtime)
    testImplementation(libs.junit)
    debugImplementation(libs.androidx.ui.tooling)
}
