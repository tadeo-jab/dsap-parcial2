package ar.jornadas.ui

import android.app.Application
import android.content.Context
import androidx.lifecycle.*
import ar.jornadas.Servicios
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.io.IOException

class ProgramaModel(app: Application, private val estado: SavedStateHandle) : AndroidViewModel(app) {
    private val servicios = Servicios(app)
    val mesa = servicios.mesa
    private val repo = servicios.agenda
    private val prefs = app.getSharedPreferences("estado", Context.MODE_PRIVATE)
    val charlas = repo.programa.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())
    val dia = estado.getStateFlow("dia", 0)
    val tema = estado.getStateFlow("tema", "Todos")
    val aviso = MutableStateFlow("")
    val cargando = MutableStateFlow(false)
    val hayEdicion = MutableStateFlow(prefs.getBoolean("edicionGuardada", false))

    private var cargadaAutomatica = false
    private var enCurso = false
    private var pendiente = false

    fun cargarAutomatica() {
        if (!cargadaAutomatica) {
            cargadaAutomatica = true
            ejecutar()
        }
    }

    fun recargar() {
        if (!enCurso && !pendiente) {
            ejecutar()
        } else if (enCurso && !pendiente) {
            pendiente = true
        }
    }

    fun elegirDia(d: Int) { estado["dia"] = d }

    fun elegirTema(t: String) { estado["tema"] = t }

    private fun ejecutar(encolada: Boolean = false) {
        enCurso = true
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
                enCurso = false
                if (pendiente && !encolada) {
                    ejecutar(encolada = true)
                } else if (encolada) {
                    pendiente = false
                }
            }
        }
    }

    fun marcar(id: String) { viewModelScope.launch { repo.cambiarFavorita(id) } }
}
