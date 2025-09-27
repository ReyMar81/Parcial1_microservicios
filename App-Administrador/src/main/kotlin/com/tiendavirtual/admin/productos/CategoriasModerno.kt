package com.tiendavirtual.admin.productos

import android.util.Log
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Category
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
data class Categoria(
    val id: Int? = null,
    val nombre: String
)

// ==================== SERVICIO ====================
object CategoriasGatewayService {
    private val client = OkHttpClient.Builder()
        .connectTimeout(ApiConfig.CONNECT_TIMEOUT, TimeUnit.SECONDS)
        .readTimeout(ApiConfig.READ_TIMEOUT, TimeUnit.SECONDS)
        .writeTimeout(ApiConfig.WRITE_TIMEOUT, TimeUnit.SECONDS)
        .build()
    private val gson = Gson()
    private val mediaType = ApiConfig.CONTENT_TYPE.toMediaType()
    private val endpoint = ApiConfig.CATEGORIAS_URL

    suspend fun listar(): List<Categoria> = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder().url(endpoint).get().addHeader("Accept", ApiConfig.ACCEPT).build()
            val response = client.newCall(request).execute()
            if (response.isSuccessful) {
                val json = response.body?.string() ?: "[]"
                val type = object : TypeToken<List<Categoria>>() {}.type
                gson.fromJson<List<Categoria>>(json, type) ?: emptyList()
            } else emptyList()
        } catch (e: Exception) {
            Log.e("CategoriasService", "Error listar", e)
            emptyList()
        }
    }

    suspend fun crear(categoria: Categoria): Categoria? = withContext(Dispatchers.IO) {
        try {
            val json = gson.toJson(categoria.copy(nombre = categoria.nombre.trim()))
            val body = json.toRequestBody(mediaType)
            val request = Request.Builder().url(endpoint).post(body)
                .addHeader("Content-Type", ApiConfig.CONTENT_TYPE)
                .addHeader("Accept", ApiConfig.ACCEPT)
                .build()
            val response = client.newCall(request).execute()
            if (response.isSuccessful) gson.fromJson(response.body?.string(), Categoria::class.java) else null
        } catch (e: Exception) {
            Log.e("CategoriasService", "Error crear", e); null
        }
    }

    suspend fun actualizar(id: Int, categoria: Categoria): Categoria? = withContext(Dispatchers.IO) {
        try {
            val json = gson.toJson(categoria.copy(id = id, nombre = categoria.nombre.trim()))
            val body = json.toRequestBody(mediaType)
            val request = Request.Builder().url("$endpoint/$id")
                .put(body)
                .addHeader("Content-Type", ApiConfig.CONTENT_TYPE)
                .addHeader("Accept", ApiConfig.ACCEPT)
                .build()
            val response = client.newCall(request).execute()
            if (response.isSuccessful) gson.fromJson(response.body?.string(), Categoria::class.java) else null
        } catch (e: Exception) { Log.e("CategoriasService", "Error actualizar", e); null }
    }

    suspend fun eliminar(id: Int): Boolean = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder().url("$endpoint/$id").delete().addHeader("Accept", ApiConfig.ACCEPT).build()
            client.newCall(request).execute().isSuccessful
        } catch (e: Exception) { Log.e("CategoriasService", "Error eliminar", e); false }
    }
}

// ==================== VIEWMODEL ====================
class CategoriasViewModel : ViewModel() {
    private val _categorias = MutableStateFlow<List<Categoria>>(emptyList())
    val categorias: StateFlow<List<Categoria>> = _categorias.asStateFlow()

    private val _loading = MutableStateFlow(false)
    val loading: StateFlow<Boolean> = _loading.asStateFlow()

    private val _mostrarFormulario = MutableStateFlow(false)
    val mostrarFormulario: StateFlow<Boolean> = _mostrarFormulario.asStateFlow()

    private val _categoriaEdit = MutableStateFlow<Categoria?>(null)
    val categoriaEdit: StateFlow<Categoria?> = _categoriaEdit.asStateFlow()

    init { cargar() }

    private fun cargar() {
        viewModelScope.launch {
            _loading.value = true
            _categorias.value = CategoriasGatewayService.listar()
            _loading.value = false
        }
    }

    fun mostrarNuevo() {
        _categoriaEdit.value = null
        _mostrarFormulario.value = true
    }

    fun mostrarEditar(categoria: Categoria) {
        _categoriaEdit.value = categoria
        _mostrarFormulario.value = true
    }

    fun cerrarFormulario() {
        _mostrarFormulario.value = false
        _categoriaEdit.value = null
    }

    fun guardar(categoria: Categoria) {
        viewModelScope.launch {
            _loading.value = true
            val resultado = if (_categoriaEdit.value == null) {
                CategoriasGatewayService.crear(categoria)
            } else {
                CategoriasGatewayService.actualizar(_categoriaEdit.value!!.id!!, categoria)
            }
            if (resultado != null) {
                _categorias.value = if (_categoriaEdit.value == null) {
                    _categorias.value + resultado
                } else {
                    _categorias.value.map { if (it.id == resultado.id) resultado else it }
                }
                cerrarFormulario()
            }
            _loading.value = false
        }
    }

    fun eliminar(categoria: Categoria) {
        viewModelScope.launch {
            _loading.value = true
            if (categoria.id != null && CategoriasGatewayService.eliminar(categoria.id)) {
                _categorias.value = _categorias.value.filter { it.id != categoria.id }
            }
            _loading.value = false
        }
    }
}

// ==================== UI LISTA ====================
@Composable
fun CategoriasScreen(viewModel: CategoriasViewModel = viewModel()) {
    val categorias by viewModel.categorias.collectAsState()
    val loading by viewModel.loading.collectAsState()
    val mostrarFormulario by viewModel.mostrarFormulario.collectAsState()

    if (mostrarFormulario) {
        CategoriaFormularioScreen(viewModel)
    } else {
        Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
            Text(
                text = "Gestión de Categorías",
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
            Text(
                text = "Administrar categorías",
                style = MaterialTheme.typography.titleMedium,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(24.dp))

            Card(
                modifier = Modifier.fillMaxWidth().height(80.dp),
                elevation = CardDefaults.cardElevation(4.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Row(
                    modifier = Modifier.fillMaxSize().padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column { Text("Total Categorías", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimary)
                        Text("${categorias.size} registradas", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onPrimary)
                    }
                    FilledTonalButton(onClick = { viewModel.mostrarNuevo() }, colors = ButtonDefaults.filledTonalButtonColors(
                        containerColor = MaterialTheme.colorScheme.onPrimary,
                        contentColor = MaterialTheme.colorScheme.primary
                    )) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Nueva")
                    }
                }
            }
            Spacer(Modifier.height(16.dp))
            when {
                loading && categorias.isEmpty() -> LoadingCard("Cargando categorías...")
                categorias.isEmpty() -> EmptyCard("No hay categorías", "Pulsa 'Nueva' para crear la primera", Icons.Default.Category)
                else -> LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    items(categorias, key = { it.id ?: 0 }) { cat ->
                        CategoriaCard(cat, onEditar = { viewModel.mostrarEditar(cat) }, onEliminar = { viewModel.eliminar(cat) })
                    }
                }
            }
            if (loading && categorias.isNotEmpty()) {
                Spacer(Modifier.height(16.dp))
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            }
        }
    }
}

@Composable
private fun LoadingCard(texto: String) {
    Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
        Column(Modifier.fillMaxWidth().padding(32.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            CircularProgressIndicator(); Spacer(Modifier.height(16.dp)); Text(texto)
        }
    }
}

@Composable
private fun EmptyCard(title: String, subtitle: String, icon: androidx.compose.ui.graphics.vector.ImageVector) {
    Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
        Column(Modifier.fillMaxWidth().padding(32.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(icon, null, modifier = Modifier.size(48.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(16.dp))
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
        }
    }
}

@Composable
private fun CategoriaCard(categoria: Categoria, onEditar: () -> Unit, onEliminar: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth(), elevation = CardDefaults.cardElevation(4.dp)) {
        Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Category, null, modifier = Modifier.size(48.dp), tint = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.width(16.dp))
            Text(categoria.nombre, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
            IconButton(onClick = onEditar) { Icon(Icons.Default.Edit, null, tint = MaterialTheme.colorScheme.primary) }
            IconButton(onClick = onEliminar) { Icon(Icons.Default.Delete, null, tint = MaterialTheme.colorScheme.error) }
        }
    }
}

// ==================== FORMULARIO ====================
@Composable
fun CategoriaFormularioScreen(viewModel: CategoriasViewModel) {
    val categoriaEdit by viewModel.categoriaEdit.collectAsState()
    val loading by viewModel.loading.collectAsState()
    var nombre by remember(categoriaEdit) { mutableStateOf(categoriaEdit?.nombre ?: "") }
    val valido = nombre.isNotBlank()

    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = { viewModel.cerrarFormulario() }) { Icon(Icons.Default.ArrowBack, null, tint = MaterialTheme.colorScheme.primary) }
            Column(Modifier.weight(1f)) {
                Text(if (categoriaEdit == null) "Nueva Categoría" else "Editar Categoría", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold)
                Text(if (categoriaEdit == null) "Crear nueva categoría" else "Actualizar categoría", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        Spacer(Modifier.height(24.dp))
        Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                OutlinedTextField(value = nombre, onValueChange = { nombre = it }, label = { Text("Nombre") }, singleLine = true)
            }
        }
        Spacer(Modifier.weight(1f))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            OutlinedButton(onClick = { viewModel.cerrarFormulario() }, modifier = Modifier.weight(1f), enabled = !loading) { Text("Cancelar") }
            Button(onClick = { if (valido) viewModel.guardar(Categoria(id = categoriaEdit?.id, nombre = nombre)) }, modifier = Modifier.weight(1f), enabled = valido && !loading) {
                if (loading) CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp, color = MaterialTheme.colorScheme.onPrimary) else Text(if (categoriaEdit == null) "Crear" else "Actualizar")
            }
        }
    }
}

// Wrapper para compatibilidad con navegación previa
@Composable
fun CategoriasModerno() { CategoriasScreen() }
