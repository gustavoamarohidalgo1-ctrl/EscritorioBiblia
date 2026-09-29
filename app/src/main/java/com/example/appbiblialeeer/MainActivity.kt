package com.example.appbiblialeeer

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.core.content.res.ResourcesCompat
import com.example.appbiblialeeer.storage.planPrefs
import com.example.appbiblialeeer.ui.AppBiblialectura
import com.example.appbiblialeeer.ui.theme.AppBibliaTheme
import kotlin.concurrent.thread

// Una vez por proceso (al girar el teléfono ya está todo cargado)
private var precargado = false

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        precargarPrimerFotograma()

        setContent {
            AppBibliaTheme {
                AppBiblialectura()
            }
        }
    }

    /**
     * ✅ Mientras Android termina de crear la ventana, un hilo aparte deja listo lo que el primer
     * fotograma pedía en el hilo principal: el progreso guardado (se lee del disco) y la fuente Lora
     * (se descomprime del APK). Ambas quedan en cachés del sistema seguras entre hilos
     * (SharedPreferences y ResourcesCompat), así que la pantalla solo las encuentra ya cargadas.
     */
    private fun precargarPrimerFotograma() {
        if (precargado) return
        precargado = true
        val actividad = this
        val app = applicationContext
        thread(name = "precarga") {
            // Solo es una precarga: si algo falla, la pantalla lo carga como siempre
            runCatching {
                // Empieza a leer PlanLectura.xml (el mismo objeto que usa la pantalla)
                planPrefs(actividad)
                // Títulos y tarjetas usan la negrita; los versículos, la normal.
                // Mismo contexto que usa Compose para cargar Font(R.font...), así se reutiliza la caché
                ResourcesCompat.getFont(app, R.font.lora_bold)
                ResourcesCompat.getFont(app, R.font.lora_regular)
            }
        }
    }
}
