package ar.jornadas.ui

import android.app.Application
import android.content.Context
import androidx.lifecycle.*
import ar.jornadas.Servicios
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.io.IOException

class ProgramaModel(app: Application) : AndroidViewModel(app) {
    private val servicios = Servicios(app)
    val mesa = servicios.mesa
    private val repo = servicios.agenda
    private val prefs = app.getSharedPreferences("estado", Context.MODE_PRIVATE)
    val charlas = repo.programa.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())
    val dia = MutableStateFlow(0)
    val aviso = MutableStateFlow("")
    val cargando = MutableStateFlow(false)
    val hayEdicion = MutableStateFlow(prefs.getBoolean("edicionGuardada", false))

    fun recargar() {
        viewModelScope.launch {
            cargando.value = true
            try {
                repo.sincronizar()
                hayEdicion.value = true
                prefs.edit().putBoolean("edicionGuardada", true).apply()
                aviso.value = ""
            } catch (e: IOException) {
                aviso.value = if (hayEdicion.value) "Sin señal: se muestran datos guardados"
                              else "No se pudo consultar el programa"
            } finally {
                cargando.value = false
            }
        }
    }

    fun marcar(id: String) { viewModelScope.launch { repo.cambiarFavorita(id) } }
}
