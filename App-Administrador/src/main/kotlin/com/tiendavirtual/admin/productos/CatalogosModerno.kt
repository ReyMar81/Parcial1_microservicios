package com.tiendavirtual.admin.productos

import android.util.Log
import android.util.Base64
import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.material3.AssistChip
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
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
    private val endpoint = ApiConfig.CATALOGOS_URL

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
            val json = gson.toJson(catalogo.copy(
                nombre = catalogo.nombre.trim(),
                descripcion = catalogo.descripcion.trim()
            ))
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
            val json = gson.toJson(catalogo.copy(
                id = id,
                nombre = catalogo.nombre.trim(),
                descripcion = catalogo.descripcion.trim()
            ))
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

    // Asociación de productos a catálogo - AGREGAR productos (POST)
    suspend fun agregarProductos(catalogoId: Int, productoIds: List<Int>): Boolean = withContext(Dispatchers.IO) {
        try {
            val bodyJson = gson.toJson(mapOf("productoIds" to productoIds))
            val body = bodyJson.toRequestBody(mediaType)
            val request = Request.Builder().url("$endpoint/$catalogoId/productos")
                .post(body)
                .addHeader("Content-Type", ApiConfig.CONTENT_TYPE)
                .addHeader("Accept", ApiConfig.ACCEPT)
                .build()
            client.newCall(request).execute().isSuccessful
        } catch (e: Exception) { Log.e("CatalogosService", "Error agregar productos", e); false }
    }

    // LISTAR productos de un catálogo (GET)
    suspend fun listarProductosDeCatalogo(catalogoId: Int): List<Producto> = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder().url("$endpoint/$catalogoId/productos")
                .get()
                .addHeader("Accept", ApiConfig.ACCEPT)
                .build()
            val response = client.newCall(request).execute()
            if (response.isSuccessful) {
                val json = response.body?.string() ?: "[]"
                val type = object : TypeToken<List<Producto>>() {}.type
                gson.fromJson<List<Producto>>(json, type) ?: emptyList()
            } else emptyList()
        } catch (e: Exception) { Log.e("CatalogosService", "Error listar productos catalogo", e); emptyList() }
    }

    // ELIMINAR un producto específico de un catálogo (DELETE)
    suspend fun eliminarProductoDeCatalogo(catalogoId: Int, productoId: Int): Boolean = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder().url("$endpoint/$catalogoId/productos/$productoId")
                .delete()
                .addHeader("Accept", ApiConfig.ACCEPT)
                .build()
            client.newCall(request).execute().isSuccessful
        } catch (e: Exception) { Log.e("CatalogosService", "Error eliminar producto catalogo", e); false }
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

    // Nuevo estado: selección de productos para un catálogo
    private val _catalogoSeleccionado = MutableStateFlow<Catalogo?>(null)
    val catalogoSeleccionado: StateFlow<Catalogo?> = _catalogoSeleccionado.asStateFlow()
    private val _mostrarSeleccionProductos = MutableStateFlow(false)
    val mostrarSeleccionProductos: StateFlow<Boolean> = _mostrarSeleccionProductos.asStateFlow()

    // Estados para gestión de productos en catálogo
    private val _productosDisponibles = MutableStateFlow<List<Producto>>(emptyList())
    val productosDisponibles: StateFlow<List<Producto>> = _productosDisponibles.asStateFlow()

    private val _productosEnCatalogo = MutableStateFlow<List<Producto>>(emptyList())
    val productosEnCatalogo: StateFlow<List<Producto>> = _productosEnCatalogo.asStateFlow()

    private val _mostrarVistaProductos = MutableStateFlow(false)
    val mostrarVistaProductos: StateFlow<Boolean> = _mostrarVistaProductos.asStateFlow()

    init { cargar() }

    private fun cargar() { viewModelScope.launch { _loading.value = true; _catalogos.value = CatalogosGatewayService.listar(); _loading.value = false } }
    fun mostrarNuevo() { _catalogoEdit.value = null; _mostrarFormulario.value = true }
    fun mostrarEditar(c: Catalogo) { _catalogoEdit.value = c; _mostrarFormulario.value = true }
    fun cerrarFormulario() { _mostrarFormulario.value = false; _catalogoEdit.value = null }

    fun abrirSeleccionProductos(catalogo: Catalogo) {
        _catalogoSeleccionado.value = catalogo
        _mostrarVistaProductos.value = false // Asegurar que no siga mostrando la vista anterior
        _mostrarSeleccionProductos.value = true
        cargarProductosDisponibles()
        catalogo.id?.let { cargarProductosDeCatalogo(it) }
    }

    fun cerrarSeleccionProductos() { _mostrarSeleccionProductos.value = false; _catalogoSeleccionado.value = null }

    fun abrirVistaProductos(catalogo: Catalogo) {
        _catalogoSeleccionado.value = catalogo
        _mostrarVistaProductos.value = true
        cargarProductosDisponibles()
        cargarProductosDeCatalogo(catalogo.id!!)
    }

    fun cerrarVistaProductos() {
        _mostrarVistaProductos.value = false
        _catalogoSeleccionado.value = null
        _mostrarSeleccionProductos.value = false
    }

    private fun cargarProductosDisponibles() {
        viewModelScope.launch {
            _loading.value = true
            _productosDisponibles.value = ProductosGatewayService.listar()
            _loading.value = false
        }
    }

    private fun cargarProductosDeCatalogo(catalogoId: Int) {
        viewModelScope.launch {
            _loading.value = true
            _productosEnCatalogo.value = CatalogosGatewayService.listarProductosDeCatalogo(catalogoId)
            _loading.value = false
        }
    }

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

    fun agregarProductosACatalogo(catalogoId: Int, productoIds: List<Int>, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            _loading.value = true
            val exito = CatalogosGatewayService.agregarProductos(catalogoId, productoIds)
            if (exito) cargarProductosDeCatalogo(catalogoId)
            _loading.value = false
            onResult(exito)
        }
    }

    fun eliminarProductoDeCatalogo(catalogoId: Int, productoId: Int, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            _loading.value = true
            val exito = CatalogosGatewayService.eliminarProductoDeCatalogo(catalogoId, productoId)
            if (exito) cargarProductosDeCatalogo(catalogoId)
            _loading.value = false
            onResult(exito)
        }
    }
}

// ==================== UI LISTA / NAV ====================
@Composable
fun CatalogosScreen(viewModel: CatalogosViewModel = viewModel()) {
    val catalogos by viewModel.catalogos.collectAsState()
    val loading by viewModel.loading.collectAsState()
    val mostrarFormulario by viewModel.mostrarFormulario.collectAsState()
    val mostrarSeleccionProductos by viewModel.mostrarSeleccionProductos.collectAsState()
    val mostrarVistaProductos by viewModel.mostrarVistaProductos.collectAsState()
    val catalogoSeleccionado by viewModel.catalogoSeleccionado.collectAsState()

    when {
        mostrarFormulario -> CatalogoFormularioScreen(viewModel)
        mostrarVistaProductos && catalogoSeleccionado != null -> VistaProductosCatalogoScreen(viewModel, catalogoSeleccionado!!)
        mostrarSeleccionProductos && catalogoSeleccionado != null -> SeleccionProductosCatalogoScreen(viewModel, catalogoSeleccionado!!)
        else -> CatalogosListaScreen(catalogos, loading, viewModel)
    }
}

@Composable
private fun CatalogosListaScreen(catalogos: List<Catalogo>, loading: Boolean, viewModel: CatalogosViewModel) {
    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Text("Gestión de Catálogos", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
        Text("Administrar catálogos", style = MaterialTheme.typography.titleMedium, textAlign = TextAlign.Center, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.height(24.dp))
        Card(Modifier.fillMaxWidth().height(80.dp), elevation = CardDefaults.cardElevation(4.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary)) {
            Row(Modifier.fillMaxSize().padding(16.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column { Text("Total Catálogos", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimary); Text("${catalogos.size} registrados", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onPrimary) }
                FilledTonalButton(onClick = { viewModel.mostrarNuevo() }, colors = ButtonDefaults.filledTonalButtonColors(containerColor = MaterialTheme.colorScheme.onPrimary, contentColor = MaterialTheme.colorScheme.primary)) { Icon(Icons.Default.Add, null, modifier = Modifier.size(18.dp)); Spacer(Modifier.width(8.dp)); Text("Nuevo") }
            }
        }
        Spacer(Modifier.height(16.dp))
        when {
            loading && catalogos.isEmpty() -> LoadingCard("Cargando catálogos...")
            catalogos.isEmpty() -> EmptyCard("No hay catálogos", "Pulsa 'Nuevo' para crear el primero", Icons.Default.Book)
            else -> LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                items(catalogos, key = { it.id ?: -1 }) { cat ->
                    CatalogoCard(
                        catalogo = cat,
                        onSeleccionarProductos = { viewModel.abrirVistaProductos(cat) },
                        onEditar = { viewModel.mostrarEditar(cat) },
                        onEliminar = { viewModel.eliminar(cat) }
                    )
                }
            }
        }
        if (loading && catalogos.isNotEmpty()) { Spacer(Modifier.height(16.dp)); LinearProgressIndicator(Modifier.fillMaxWidth()) }
    }
}

@Composable
private fun CatalogoCard(
    catalogo: Catalogo,
    onSeleccionarProductos: () -> Unit,
    onEditar: () -> Unit,
    onEliminar: () -> Unit
) {
    Card(Modifier.fillMaxWidth(), elevation = CardDefaults.cardElevation(4.dp)) {
        Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Book, null, modifier = Modifier.size(48.dp), tint = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.width(16.dp))
            Column(Modifier.weight(1f).clickable { onSeleccionarProductos() }) {
                Text(catalogo.nombre, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text(catalogo.descripcion, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 2)
                Text("Tap para gestionar productos", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
            }
            IconButton(onClick = onEditar) { Icon(Icons.Default.Edit, null, tint = MaterialTheme.colorScheme.primary) }
            IconButton(onClick = onEliminar) { Icon(Icons.Default.Delete, null, tint = MaterialTheme.colorScheme.error) }
        }
    }
}

@Composable
private fun LoadingCard(texto: String) { Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) { Column(Modifier.fillMaxWidth().padding(32.dp), horizontalAlignment = Alignment.CenterHorizontally) { CircularProgressIndicator(); Spacer(Modifier.height(16.dp)); Text(texto) } } }

@Composable
private fun EmptyCard(title: String, subtitle: String, icon: androidx.compose.ui.graphics.vector.ImageVector) { Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) { Column(Modifier.fillMaxWidth().padding(32.dp), horizontalAlignment = Alignment.CenterHorizontally) { Icon(icon, null, modifier = Modifier.size(48.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant); Spacer(Modifier.height(16.dp)); Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant); Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center) } } }

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

// ==================== VISTA DE PRODUCTOS DE UN CATÁLOGO ====================
@Composable
private fun VistaProductosCatalogoScreen(viewModel: CatalogosViewModel, catalogo: Catalogo) {
    val loading by viewModel.loading.collectAsState()
    val productosEnCatalogo by viewModel.productosEnCatalogo.collectAsState()
    LaunchedEffect(catalogo.id) {}
    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = { viewModel.cerrarVistaProductos() }) {
                Icon(Icons.Default.ArrowBack, null, tint = MaterialTheme.colorScheme.primary)
            }
            Column(Modifier.weight(1f)) {
                Text("Productos del Catálogo", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Text(catalogo.nombre, style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
            }
            // Botón para agregar productos
            FilledTonalButton(
                onClick = { viewModel.abrirSeleccionProductos(catalogo) },
                colors = ButtonDefaults.filledTonalButtonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                )
            ) {
                Icon(Icons.Default.Add, null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("Agregar")
            }
        }

        Spacer(Modifier.height(16.dp))

        // Estadísticas
        Card(
            Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
        ) {
            Row(Modifier.padding(16.dp)) {
                Column {
                    Text("Productos Asociados", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    Text("${productosEnCatalogo.size} productos", style = MaterialTheme.typography.bodyMedium)
                }
            }
        }

        Spacer(Modifier.height(16.dp))

        // Lista de productos
        when {
            loading -> LoadingCard("Cargando productos...")
            productosEnCatalogo.isEmpty() -> EmptyCard(
                "Sin productos",
                "Este catálogo no tiene productos asociados.\nUsa el botón 'Agregar' para añadir algunos.",
                Icons.Default.Add
            )
            else -> LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(productosEnCatalogo, key = { it.codigo }) { producto ->
                    ProductoEnCatalogoCard(
                        producto = producto,
                        onEliminar = {
                            if (catalogo.id != null) {
                                viewModel.eliminarProductoDeCatalogo(catalogo.id, producto.codigo) { _ ->
                                }
                            }
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun ProductoEnCatalogoCard(
    producto: Producto,
    onEliminar: () -> Unit
) {
    Card(Modifier.fillMaxWidth(), elevation = CardDefaults.cardElevation(2.dp)) {
        Row(
            Modifier.fillMaxWidth().padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Imagen del producto
            if (producto.imagen.isNotEmpty()) {
                ProductImage(
                    base64Image = producto.imagen,
                    modifier = Modifier.size(48.dp)
                )
            } else {
                Box(
                    Modifier.size(48.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Book, null, modifier = Modifier.size(32.dp), tint = MaterialTheme.colorScheme.outline)
                }
            }

            Spacer(Modifier.width(12.dp))

            // Info del producto
            Column(Modifier.weight(1f)) {
                Text(producto.nombre, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                Text("Stock: ${producto.stock}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text("$${producto.precio}", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold)
            }

            // Botón eliminar
            IconButton(onClick = onEliminar) {
                Icon(Icons.Default.Delete, null, tint = MaterialTheme.colorScheme.error)
            }
        }
    }
}

// ==================== SELECCIÓN DE PRODUCTOS PARA CATÁLOGO ====================
@Composable
private fun SeleccionProductosCatalogoScreen(viewModel: CatalogosViewModel, catalogo: Catalogo) {
    val loading by viewModel.loading.collectAsState()
    val productosDisponibles by viewModel.productosDisponibles.collectAsState()
    val productosEnCatalogo by viewModel.productosEnCatalogo.collectAsState()

    // Snackbar
    val snackbarHostState = remember { SnackbarHostState() }
    var ultimoAgregados by remember { mutableStateOf(0) }

    // Filtro de búsqueda
    var filtro by remember { mutableStateOf("") }

    // Lista de productos seleccionados localmente
    val seleccionados = remember { mutableStateListOf<Int>() }

    // IDs ya existentes
    val productosYaEnCatalogo = productosEnCatalogo.map { it.codigo }.toSet()

    // Aplicar filtro
    val productosFiltrados = remember(productosDisponibles, filtro) {
        if (filtro.isBlank()) productosDisponibles else productosDisponibles.filter { it.nombre.contains(filtro, ignoreCase = true) }
    }

    LaunchedEffect(ultimoAgregados) {
        if (ultimoAgregados > 0) {
            snackbarHostState.showSnackbar("$ultimoAgregados producto(s) agregados al catálogo")
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).padding(16.dp)) {
            // Header
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = { viewModel.cerrarSeleccionProductos() }) {
                    Icon(Icons.Default.ArrowBack, null, tint = MaterialTheme.colorScheme.primary)
                }
                Column(Modifier.weight(1f)) {
                    Text("Añadir productos", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Text(catalogo.nombre, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                }
            }
            Spacer(Modifier.height(8.dp))
            Text(
                "Selecciona productos para incorporarlos al catálogo. Los que ya están aparecerán deshabilitados.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(16.dp))

            // Búsqueda
            OutlinedTextField(
                value = filtro,
                onValueChange = { filtro = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Buscar producto") },
                singleLine = true,
                trailingIcon = {
                    if (filtro.isNotBlank()) {
                        TextButton(onClick = { filtro = "" }) { Text("Limpiar") }
                    }
                }
            )

            Spacer(Modifier.height(16.dp))

            // Seleccionados resumen pequeño chips
            if (seleccionados.isNotEmpty()) {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    items(seleccionados.toList()) { idSel ->
                        val p = productosDisponibles.find { it.codigo == idSel }
                        if (p != null) {
                            AssistChip(
                                onClick = { seleccionados.remove(idSel) },
                                label = { Text(p.nombre, maxLines = 1) }
                            )
                        }
                    }
                }
                Spacer(Modifier.height(12.dp))
            }

            // Lista principal
            when {
                loading && productosDisponibles.isEmpty() -> LoadingCard("Cargando productos...")
                productosDisponibles.isEmpty() -> EmptyCard("Sin productos", "No hay productos disponibles", Icons.Default.Add)
                productosFiltrados.isEmpty() -> EmptyCard("Sin coincidencias", "No hay resultados para '$filtro'", Icons.Default.Book)
                else -> LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(productosFiltrados, key = { it.codigo }) { producto ->
                        val yaEnCatalogo = productosYaEnCatalogo.contains(producto.codigo)
                        val seleccionado = seleccionados.contains(producto.codigo)
                        ProductoSeleccionableCard(
                            producto = producto,
                            seleccionado = seleccionado,
                            yaEnCatalogo = yaEnCatalogo,
                            onToggleSeleccion = {
                                if (seleccionado) seleccionados.remove(producto.codigo)
                                else if (!yaEnCatalogo) seleccionados.add(producto.codigo)
                            }
                        )
                    }
                }
            }

            // Barra de acciones inferior
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                OutlinedButton(
                    onClick = { viewModel.cerrarSeleccionProductos() },
                    modifier = Modifier.weight(1f),
                    enabled = !loading
                ) { Text("Cerrar") }
                Button(
                    onClick = {
                        val ids = seleccionados.toList()
                        if (catalogo.id != null && ids.isNotEmpty()) {
                            viewModel.agregarProductosACatalogo(catalogo.id, ids) { ok ->
                                if (ok) {
                                    ultimoAgregados = ids.size
                                    seleccionados.clear()
                                }
                            }
                        }
                    },
                    modifier = Modifier.weight(1f),
                    enabled = seleccionados.isNotEmpty() && !loading
                ) {
                    if (loading) {
                        CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp, color = MaterialTheme.colorScheme.onPrimary)
                    } else {
                        Text("Agregar (${seleccionados.size})")
                    }
                }
            }
        }
    }
}

@Composable
private fun ProductoSeleccionableCard(
    producto: Producto,
    seleccionado: Boolean,
    yaEnCatalogo: Boolean,
    onToggleSeleccion: () -> Unit
) {
    val cardColors = when {
        yaEnCatalogo -> CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        seleccionado -> CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
        else -> CardDefaults.cardColors()
    }

    Card(
        Modifier.fillMaxWidth().clickable(enabled = !yaEnCatalogo) { onToggleSeleccion() },
        elevation = CardDefaults.cardElevation(if (seleccionado) 8.dp else 2.dp),
        colors = cardColors
    ) {
        Row(
            Modifier.fillMaxWidth().padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Checkbox o indicador
            when {
                yaEnCatalogo -> Icon(
                    Icons.Default.Book,
                    null,
                    tint = MaterialTheme.colorScheme.outline,
                    modifier = Modifier.size(24.dp)
                )
                else -> Checkbox(
                    checked = seleccionado,
                    onCheckedChange = { onToggleSeleccion() }
                )
            }

            Spacer(Modifier.width(12.dp))

            // Imagen del producto
            if (producto.imagen.isNotEmpty()) {
                ProductImage(
                    base64Image = producto.imagen,
                    modifier = Modifier.size(48.dp)
                )
            } else {
                Box(
                    Modifier.size(48.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Book, null, modifier = Modifier.size(32.dp), tint = MaterialTheme.colorScheme.outline)
                }
            }

            Spacer(Modifier.width(12.dp))

            // Info del producto
            Column(Modifier.weight(1f)) {
                Text(
                    producto.nombre,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = if (yaEnCatalogo) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface
                )
                Text(
                    "Stock: ${producto.stock}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    "$${producto.precio}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (yaEnCatalogo) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold
                )

                if (yaEnCatalogo) {
                    Text(
                        "Ya en catálogo",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.outline
                    )
                }
            }
        }
    }
}

@Composable
fun ProductImage(base64Image: String, modifier: Modifier = Modifier) {
    // Verificar si es una imagen Base64 válida
    if (!base64Image.startsWith("data:image/") || !base64Image.contains(",")) {
        // No es una imagen Base64 válida, mostrar placeholder
        Box(
            modifier = modifier,
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Default.Book, null, tint = MaterialTheme.colorScheme.outline)
        }
        return
    }

    // Decodificar la imagen
    val imageBitmap = remember(base64Image) {
        try {
            val base64Data = base64Image.substringAfter(",")
            val imageBytes = Base64.decode(base64Data, Base64.DEFAULT)
            BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)?.asImageBitmap()
        } catch (e: Exception) {
            Log.e("ProductImage", "Error decodificando imagen Base64", e)
            null
        }
    }

    // Mostrar la imagen o placeholder
    if (imageBitmap != null) {
        Image(
            bitmap = imageBitmap,
            contentDescription = null,
            modifier = modifier.clip(RoundedCornerShape(4.dp)),
            contentScale = ContentScale.Crop
        )
    } else {
        Box(
            modifier = modifier,
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Default.Book, null, tint = MaterialTheme.colorScheme.outline)
        }
    }
}
