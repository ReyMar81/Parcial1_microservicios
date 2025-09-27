package com.tiendavirtual.admin.productos

import android.util.Log
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.tiendavirtual.admin.data.shared.ApiConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.MediaType.Companion.toMediaType
import java.util.concurrent.TimeUnit

// ==================== MODELO ====================
data class Catalogo(
    val id: Int? = null,
    val nombre: String,
    val descripcion: String
)

// ==================== SERVICIO ====================
object CatalogosGatewayService {
    private val client = OkHttpClient.Builder()
        .connectTimeout(ApiConfig.CONNECT_TIMEOUT, TimeUnit.SECONDS)
        .readTimeout(ApiConfig.READ_TIMEOUT, TimeUnit.SECONDS)
        .writeTimeout(ApiConfig.WRITE_TIMEOUT, TimeUnit.SECONDS)
        .build()
    private val gson = Gson()
    private val mediaType = ApiConfig.CONTENT_TYPE.toMediaType()
    private val endpoint = ApiConfig.CATALOGOS_URL // ahora /api/productos/catalogos

    suspend fun listar(): List<Catalogo> = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder().url(endpoint).get().addHeader("Accept", ApiConfig.ACCEPT).build()
            val response = client.newCall(request).execute()
            if (response.isSuccessful) {
                val json = response.body?.string() ?: "[]"
                val type = object : TypeToken<List<Catalogo>>() {}.type
                gson.fromJson<List<Catalogo>>(json, type) ?: emptyList()
            } else emptyList()
        } catch (e: Exception) { Log.e("CatalogosService", "Error listar", e); emptyList() }
    }

    suspend fun crear(catalogo: Catalogo): Catalogo? = withContext(Dispatchers.IO) {
        try {
            val json = gson.toJson(catalogo.copy(nombre = catalogo.nombre.trim(), descripcion = catalogo.descripcion.trim()))
            val body = json.toRequestBody(mediaType)
            val request = Request.Builder().url(endpoint).post(body)
                .addHeader("Content-Type", ApiConfig.CONTENT_TYPE)
                .addHeader("Accept", ApiConfig.ACCEPT)
                .build()
            val response = client.newCall(request).execute()
            if (response.isSuccessful) gson.fromJson(response.body?.string(), Catalogo::class.java) else null
        } catch (e: Exception) { Log.e("CatalogosService", "Error crear", e); null }
    }

    suspend fun actualizar(id: Int, catalogo: Catalogo): Catalogo? = withContext(Dispatchers.IO) {
        try {
            val json = gson.toJson(catalogo.copy(id = id, nombre = catalogo.nombre.trim(), descripcion = catalogo.descripcion.trim()))
            val body = json.toRequestBody(mediaType)
            val request = Request.Builder().url("$endpoint/$id")
                .put(body)
                .addHeader("Content-Type", ApiConfig.CONTENT_TYPE)
                .addHeader("Accept", ApiConfig.ACCEPT)
                .build()
            val response = client.newCall(request).execute()
            if (response.isSuccessful) gson.fromJson(response.body?.string(), Catalogo::class.java) else null
        } catch (e: Exception) { Log.e("CatalogosService", "Error actualizar", e); null }
    }

    suspend fun eliminar(id: Int): Boolean = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder().url("$endpoint/$id").delete().addHeader("Accept", ApiConfig.ACCEPT).build()
            client.newCall(request).execute().isSuccessful
        } catch (e: Exception) { Log.e("CatalogosService", "Error eliminar", e); false }
    }
}

// ==================== VIEWMODEL ====================
class CatalogosViewModel : ViewModel() {
    private val _catalogos = MutableStateFlow<List<Catalogo>>(emptyList())
    val catalogos: StateFlow<List<Catalogo>> = _catalogos.asStateFlow()

    private val _loading = MutableStateFlow(false)
    val loading: StateFlow<Boolean> = _loading.asStateFlow()

    private val _mostrarFormulario = MutableStateFlow(false)
    val mostrarFormulario: StateFlow<Boolean> = _mostrarFormulario.asStateFlow()

    private val _catalogoEdit = MutableStateFlow<Catalogo?>(null)
    val catalogoEdit: StateFlow<Catalogo?> = _catalogoEdit.asStateFlow()

    init { cargar() }

    private fun cargar() { viewModelScope.launch { _loading.value = true; _catalogos.value = CatalogosGatewayService.listar(); _loading.value = false } }
    fun mostrarNuevo() { _catalogoEdit.value = null; _mostrarFormulario.value = true }
    fun mostrarEditar(c: Catalogo) { _catalogoEdit.value = c; _mostrarFormulario.value = true }
    fun cerrarFormulario() { _mostrarFormulario.value = false; _catalogoEdit.value = null }

    fun guardar(catalogo: Catalogo) { viewModelScope.launch {
        _loading.value = true
        val resultado = if (_catalogoEdit.value == null) CatalogosGatewayService.crear(catalogo) else CatalogosGatewayService.actualizar(_catalogoEdit.value!!.id!!, catalogo)
        if (resultado != null) {
            _catalogos.value = if (_catalogoEdit.value == null) _catalogos.value + resultado else _catalogos.value.map { if (it.id == resultado.id) resultado else it }
            cerrarFormulario()
        }
        _loading.value = false
    } }

    fun eliminar(catalogo: Catalogo) { viewModelScope.launch {
        _loading.value = true
        if (catalogo.id != null && CatalogosGatewayService.eliminar(catalogo.id)) {
            _catalogos.value = _catalogos.value.filter { it.id != catalogo.id }
        }
        _loading.value = false
    } }
}

// ==================== UI LISTA ====================
@Composable
fun CatalogosScreen(viewModel: CatalogosViewModel = viewModel()) {
    val catalogos by viewModel.catalogos.collectAsState()
    val loading by viewModel.loading.collectAsState()
    val mostrarFormulario by viewModel.mostrarFormulario.collectAsState()

    if (mostrarFormulario) {
        CatalogoFormularioScreen(viewModel)
    } else {
        Column(Modifier.fillMaxSize().padding(16.dp)) {
            Text("Gestión de Catálogos", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
            Text("Administrar catálogos", style = MaterialTheme.typography.titleMedium, textAlign = TextAlign.Center, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(24.dp))
            Card(Modifier.fillMaxWidth().height(80.dp), elevation = CardDefaults.cardElevation(4.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary)) {
                Row(Modifier.fillMaxSize().padding(16.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Column { Text("Total Catálogos", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimary); Text("${catalogos.size} registrados", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onPrimary) }
                    FilledTonalButton(onClick = { viewModel.mostrarNuevo() }, colors = ButtonDefaults.filledTonalButtonColors(containerColor = MaterialTheme.colorScheme.onPrimary, contentColor = MaterialTheme.colorScheme.primary)) {
                        Icon(Icons.Default.Add, null, modifier = Modifier.size(18.dp)); Spacer(Modifier.width(8.dp)); Text("Nuevo")
                    }
                }
            }
            Spacer(Modifier.height(16.dp))
            when {
                loading && catalogos.isEmpty() -> LoadingCard("Cargando catálogos...")
                catalogos.isEmpty() -> EmptyCard("No hay catálogos", "Pulsa 'Nuevo' para crear el primero", Icons.Default.Book)
                else -> LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    items(catalogos, key = { it.id ?: 0 }) { cat -> CatalogoCard(cat, onEditar = { viewModel.mostrarEditar(cat) }, onEliminar = { viewModel.eliminar(cat) }) }
                }
            }
            if (loading && catalogos.isNotEmpty()) { Spacer(Modifier.height(16.dp)); LinearProgressIndicator(Modifier.fillMaxWidth()) }
        }
    }
}

@Composable
private fun CatalogoCard(catalogo: Catalogo, onEditar: () -> Unit, onEliminar: () -> Unit) {
    Card(Modifier.fillMaxWidth(), elevation = CardDefaults.cardElevation(4.dp)) {
        Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Book, null, modifier = Modifier.size(48.dp), tint = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.width(16.dp))
            Column(Modifier.weight(1f)) {
                Text(catalogo.nombre, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text(catalogo.descripcion, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 2)
            }
            IconButton(onClick = onEditar) { Icon(Icons.Default.Edit, null, tint = MaterialTheme.colorScheme.primary) }
            IconButton(onClick = onEliminar) { Icon(Icons.Default.Delete, null, tint = MaterialTheme.colorScheme.error) }
        }
    }
}

// Reutilizo LoadingCard y EmptyCard de Categorías para consistencia (copiadas mínimamente)
@Composable
private fun LoadingCard(texto: String) {
    Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
        Column(Modifier.fillMaxWidth().padding(32.dp), horizontalAlignment = Alignment.CenterHorizontally) { CircularProgressIndicator(); Spacer(Modifier.height(16.dp)); Text(texto) }
    }
}

@Composable
private fun EmptyCard(title: String, subtitle: String, icon: androidx.compose.ui.graphics.vector.ImageVector) {
    Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
        Column(Modifier.fillMaxWidth().padding(32.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(icon, null, modifier = Modifier.size(48.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(16.dp))
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
        }
    }
}

// ==================== FORMULARIO ====================
@Composable
fun CatalogoFormularioScreen(viewModel: CatalogosViewModel) {
    val catalogoEdit by viewModel.catalogoEdit.collectAsState()
    val loading by viewModel.loading.collectAsState()
    var nombre by remember(catalogoEdit) { mutableStateOf(catalogoEdit?.nombre ?: "") }
    var descripcion by remember(catalogoEdit) { mutableStateOf(catalogoEdit?.descripcion ?: "") }
    val valido = nombre.isNotBlank() && descripcion.isNotBlank()

    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = { viewModel.cerrarFormulario() }) { Icon(Icons.Default.ArrowBack, null, tint = MaterialTheme.colorScheme.primary) }
            Column(Modifier.weight(1f)) {
                Text(if (catalogoEdit == null) "Nuevo Catálogo" else "Editar Catálogo", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold)
                Text(if (catalogoEdit == null) "Crear nuevo catálogo" else "Actualizar catálogo", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        Spacer(Modifier.height(24.dp))
        Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                OutlinedTextField(value = nombre, onValueChange = { nombre = it }, label = { Text("Nombre") }, singleLine = true)
                OutlinedTextField(value = descripcion, onValueChange = { descripcion = it }, label = { Text("Descripción") }, maxLines = 4)
            }
        }
        Spacer(Modifier.weight(1f))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            OutlinedButton(onClick = { viewModel.cerrarFormulario() }, modifier = Modifier.weight(1f), enabled = !loading) { Text("Cancelar") }
            Button(onClick = { if (valido) viewModel.guardar(Catalogo(id = catalogoEdit?.id, nombre = nombre, descripcion = descripcion)) }, modifier = Modifier.weight(1f), enabled = valido && !loading) {
                if (loading) CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp, color = MaterialTheme.colorScheme.onPrimary) else Text(if (catalogoEdit == null) "Crear" else "Actualizar")
            }
        }
    }
}

// Wrapper compatibilidad
@Composable
fun CatalogosModerno() { CatalogosScreen() }
