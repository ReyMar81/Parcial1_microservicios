package com.tiendavirtual.admin.productos

import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
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
import java.io.ByteArrayOutputStream
import java.util.concurrent.TimeUnit

// ==================== MODELO ====================
data class Producto(
    val codigo: Int = 0, // Cambiar de Int? a Int con valor por defecto
    val nombre: String,
    val descripcion: String,
    val imagen: String,
    val precio: Double,
    val stock: Int,
    val categoriaId: Int
)

// ==================== SERVICIO ====================
object ProductosGatewayService {
    private val client = OkHttpClient.Builder()
        .connectTimeout(ApiConfig.CONNECT_TIMEOUT, TimeUnit.SECONDS)
        .readTimeout(ApiConfig.READ_TIMEOUT, TimeUnit.SECONDS)
        .writeTimeout(ApiConfig.WRITE_TIMEOUT, TimeUnit.SECONDS)
        .build()
    private val gson = Gson()
    private val mediaType = ApiConfig.CONTENT_TYPE.toMediaType()
    private val endpoint = ApiConfig.PRODUCTOS_URL + "/productos"

    suspend fun listar(): List<Producto> = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder().url(endpoint).get().addHeader("Accept", ApiConfig.ACCEPT).build()
            val response = client.newCall(request).execute()
            if (response.isSuccessful) {
                val json = response.body?.string() ?: "[]"
                val type = object : TypeToken<List<Producto>>() {}.type
                gson.fromJson<List<Producto>>(json, type) ?: emptyList()
            } else emptyList()
        } catch (e: Exception) { Log.e("ProductosService", "Error listar", e); emptyList() }
    }

    suspend fun crear(producto: Producto): Producto? = withContext(Dispatchers.IO) {
        try {
            val json = gson.toJson(producto.copy(
                nombre = producto.nombre.trim(),
                descripcion = producto.descripcion.trim(),
                imagen = producto.imagen.trim()
            ))
            val body = json.toRequestBody(mediaType)
            val request = Request.Builder().url(endpoint).post(body)
                .addHeader("Content-Type", ApiConfig.CONTENT_TYPE)
                .addHeader("Accept", ApiConfig.ACCEPT)
                .build()
            val response = client.newCall(request).execute()
            if (response.isSuccessful) gson.fromJson(response.body?.string(), Producto::class.java) else null
        } catch (e: Exception) { Log.e("ProductosService", "Error crear", e); null }
    }

    suspend fun actualizar(codigo: Int, producto: Producto): Producto? = withContext(Dispatchers.IO) {
        try {
            val json = gson.toJson(producto.copy(
                codigo = codigo,
                nombre = producto.nombre.trim(),
                descripcion = producto.descripcion.trim(),
                imagen = producto.imagen.trim()
            ))
            val body = json.toRequestBody(mediaType)
            val request = Request.Builder().url("$endpoint/$codigo")
                .put(body)
                .addHeader("Content-Type", ApiConfig.CONTENT_TYPE)
                .addHeader("Accept", ApiConfig.ACCEPT)
                .build()
            val response = client.newCall(request).execute()
            if (response.isSuccessful) gson.fromJson(response.body?.string(), Producto::class.java) else null
        } catch (e: Exception) { Log.e("ProductosService", "Error actualizar", e); null }
    }

    suspend fun eliminar(codigo: Int): Boolean = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder().url("$endpoint/$codigo").delete().addHeader("Accept", ApiConfig.ACCEPT).build()
            client.newCall(request).execute().isSuccessful
        } catch (e: Exception) { Log.e("ProductosService", "Error eliminar", e); false }
    }
}

// ==================== VIEWMODEL ====================
class ProductosViewModel : ViewModel() {
    private val _productos = MutableStateFlow<List<Producto>>(emptyList())
    val productos: StateFlow<List<Producto>> = _productos.asStateFlow()

    private val _loading = MutableStateFlow(false)
    val loading: StateFlow<Boolean> = _loading.asStateFlow()

    private val _mostrarFormulario = MutableStateFlow(false)
    val mostrarFormulario: StateFlow<Boolean> = _mostrarFormulario.asStateFlow()

    private val _productoEdit = MutableStateFlow<Producto?>(null)
    val productoEdit: StateFlow<Producto?> = _productoEdit.asStateFlow()

    // Estado para las categorías del dropdown
    private val _categorias = MutableStateFlow<List<com.tiendavirtual.admin.productos.Categoria>>(emptyList())
    val categorias: StateFlow<List<com.tiendavirtual.admin.productos.Categoria>> = _categorias.asStateFlow()

    private val _loadingCategorias = MutableStateFlow(false)
    val loadingCategorias: StateFlow<Boolean> = _loadingCategorias.asStateFlow()

    init {
        cargar()
        cargarCategorias()
    }

    private fun cargar() {
        viewModelScope.launch {
            _loading.value = true
            _productos.value = ProductosGatewayService.listar()
            _loading.value = false
        }
    }

    fun cargarCategorias() {
        viewModelScope.launch {
            _loadingCategorias.value = true
            _categorias.value = com.tiendavirtual.admin.productos.CategoriasGatewayService.listar()
            _loadingCategorias.value = false
        }
    }

    fun mostrarNuevo() {
        _productoEdit.value = null
        _mostrarFormulario.value = true
        cargarCategorias() // Cargar categorías cuando se abre el formulario
    }

    fun mostrarEditar(p: Producto) {
        _productoEdit.value = p
        _mostrarFormulario.value = true
        cargarCategorias() // Cargar categorías cuando se abre el formulario
    }

    fun cerrarFormulario() { _mostrarFormulario.value = false; _productoEdit.value = null }

    //Funcion para guardar o actualizar un producto
    fun guardar(producto: Producto) { viewModelScope.launch {
        _loading.value = true
        val resultado = if (_productoEdit.value == null) ProductosGatewayService.crear(producto) else ProductosGatewayService.actualizar(_productoEdit.value!!.codigo, producto)
        if (resultado != null) {
            _productos.value = if (_productoEdit.value == null) _productos.value + resultado else _productos.value.map { if (it.codigo == resultado.codigo) resultado else it }
            cerrarFormulario()
        }
        _loading.value = false
    } }

    fun eliminar(producto: Producto) { viewModelScope.launch {
        _loading.value = true
        if (ProductosGatewayService.eliminar(producto.codigo)) {
            _productos.value = _productos.value.filter { it.codigo != producto.codigo }
        }
        _loading.value = false
    } }
}

// ==================== UI LISTA ====================
@Composable
fun ProductosScreen(viewModel: ProductosViewModel = viewModel()) {
    val productos by viewModel.productos.collectAsState()
    val loading by viewModel.loading.collectAsState()
    val mostrarFormulario by viewModel.mostrarFormulario.collectAsState()
    val categorias by viewModel.categorias.collectAsState() // Obtener categorías para pasar a ProductoCard

    if (mostrarFormulario) {
        ProductoFormularioScreen(viewModel)
    } else {
        Column(Modifier.fillMaxSize().padding(16.dp)) {
            Text("Gestión de Productos", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
            Text("Administrar catálogo de productos", style = MaterialTheme.typography.titleMedium, textAlign = TextAlign.Center, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(24.dp))
            Card(Modifier.fillMaxWidth().height(80.dp), elevation = CardDefaults.cardElevation(4.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary)) {
                Row(Modifier.fillMaxSize().padding(16.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Column { Text("Total Productos", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimary); Text("${productos.size} registrados", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onPrimary) }
                    FilledTonalButton(onClick = { viewModel.mostrarNuevo() }, colors = ButtonDefaults.filledTonalButtonColors(containerColor = MaterialTheme.colorScheme.onPrimary, contentColor = MaterialTheme.colorScheme.primary)) { Icon(Icons.Default.Add, null, modifier = Modifier.size(18.dp)); Spacer(Modifier.width(8.dp)); Text("Nuevo") }
                }
            }
            Spacer(Modifier.height(16.dp))
            when {
                loading && productos.isEmpty() -> LoadingCard("Cargando productos...")
                productos.isEmpty() -> EmptyCard("No hay productos", "Pulsa 'Nuevo' para crear el primero", Icons.Default.Inventory2)
                else -> LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    items(productos, key = { it.codigo }) { prod -> ProductoCard(prod, categorias, onEditar = { viewModel.mostrarEditar(prod) }, onEliminar = { viewModel.eliminar(prod) }) }
                }
            }
            if (loading && productos.isNotEmpty()) { Spacer(Modifier.height(16.dp)); LinearProgressIndicator(Modifier.fillMaxWidth()) }
        }
    }
}

private fun esImagenBase64(s: String) = s.startsWith("data:image/") && s.contains(",")

@Composable
private fun ProductoCard(
    producto: Producto,
    categorias: List<com.tiendavirtual.admin.productos.Categoria>,
    onEditar: () -> Unit,
    onEliminar: () -> Unit
) {
    val nombreCategoria = categorias.find { it.id == producto.categoriaId }?.nombre ?: "Sin categoría"
    val imageBitmap = remember(producto.imagen) {
        if (esImagenBase64(producto.imagen)) {
            try {
                val base64 = producto.imagen.substringAfter(",")
                val imageBytes = Base64.decode(base64, Base64.DEFAULT)
                BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)?.asImageBitmap()
            } catch (e: Exception) {
                null
            }
        } else {
            null
        }
    }

    Card(Modifier.fillMaxWidth(), elevation = CardDefaults.cardElevation(4.dp)) {
        Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            if (imageBitmap != null) {
                Image(
                    bitmap = imageBitmap,
                    contentDescription = null,
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(8.dp)),
                    contentScale = ContentScale.Crop
                )
            } else {
                // Placeholder cuando no hay imagen válida
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Inventory2, null, tint = MaterialTheme.colorScheme.primary)
                }
            }

            Spacer(Modifier.width(16.dp))
            Column(Modifier.weight(1f)) {
                Text(producto.nombre, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text("Precio: ${"%.2f".format(producto.precio)}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text("Stock: ${producto.stock} | Cat: $nombreCategoria", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
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
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProductoFormularioScreen(viewModel: ProductosViewModel) {
    val productoEdit by viewModel.productoEdit.collectAsState()
    val loading by viewModel.loading.collectAsState()
    val categorias by viewModel.categorias.collectAsState()
    val loadingCategorias by viewModel.loadingCategorias.collectAsState()

    var nombre by remember(productoEdit) { mutableStateOf(productoEdit?.nombre ?: "") }
    var descripcion by remember(productoEdit) { mutableStateOf(productoEdit?.descripcion ?: "") }
    var imagen by remember(productoEdit) { mutableStateOf(productoEdit?.imagen ?: "") }
    var precioTxt by remember(productoEdit) { mutableStateOf(productoEdit?.precio?.toString() ?: "") }
    var stockTxt by remember(productoEdit) { mutableStateOf(productoEdit?.stock?.toString() ?: "") }

    // Estado para el dropdown de categorías
    var categoriaSeleccionada by remember(productoEdit) {
        val productoActual = productoEdit
        mutableStateOf(
            if (productoActual != null)
                categorias.find { it.id == productoActual.categoriaId }
            else null
        )
    }
    var expanded by remember { mutableStateOf(false) }

    // Actualizar categoría seleccionada cuando se cargan las categorías y hay un producto para editar
    LaunchedEffect(categorias, productoEdit) {
        val productoActual = productoEdit
        if (productoActual != null && categorias.isNotEmpty()) {
            categoriaSeleccionada = categorias.find { it.id == productoActual.categoriaId }
        }
    }

    val precioVal = precioTxt.toDoubleOrNull()
    val stockVal = stockTxt.toIntOrNull()

    val valido = nombre.isNotBlank() && descripcion.isNotBlank() && precioVal != null && precioVal >= 0 &&
            stockVal != null && stockVal >= 0 && categoriaSeleccionada != null && categoriaSeleccionada!!.id != null && imagen.isNotBlank()

    Column(Modifier.fillMaxSize()) {
        // Header fijo
        Row(
            Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = { viewModel.cerrarFormulario() }) {
                Icon(Icons.Default.ArrowBack, null, tint = MaterialTheme.colorScheme.primary)
            }
            Column(Modifier.weight(1f)) {
                Text(
                    if (productoEdit == null) "Nuevo Producto" else "Editar Producto",
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    if (productoEdit == null) "Crear nuevo producto" else "Actualizar producto",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // Contenido scrolleable
        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Card(
                    Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        OutlinedTextField(
                            value = nombre,
                            onValueChange = { nombre = it },
                            label = { Text("Nombre") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = descripcion,
                            onValueChange = { descripcion = it },
                            label = { Text("Descripción") },
                            modifier = Modifier.fillMaxWidth(),
                            maxLines = 3
                        )

                        // Sección de imagen simplificada - solo selección de archivo
                        Text("Imagen del Producto", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)

                        val context = LocalContext.current
                        val launcher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
                            if (uri != null) {
                                try {
                                    val inputStream = context.contentResolver.openInputStream(uri)
                                    val bytes = inputStream?.readBytes()
                                    if (bytes != null) {
                                        if (bytes.size > 400_000) { // ~400KB límite
                                            // Si excede el límite, no asignamos y mostramos mensaje
                                            imagen = "" // limpiar para obligar re-selección
                                        } else {
                                            // Detectar mime simple (solo diferenciamos png / jpg)
                                            val header = bytes.take(8).toByteArray()
                                            val mime = when {
                                                header.sliceArray(0..1).contentEquals(byteArrayOf(0xFF.toByte(), 0xD8.toByte())) -> "jpeg"
                                                header.contentEquals(byteArrayOf(0x89.toByte(),0x50,0x4E,0x47,0x0D,0x0A,0x1A,0x0A)) -> "png"
                                                else -> "jpeg"
                                            }
                                            val base64Image = Base64.encodeToString(bytes, Base64.NO_WRAP)
                                            imagen = "data:image/$mime;base64,$base64Image"
                                        }
                                    }
                                } catch (_: Exception) { imagen = "" }
                            }
                        }

                        Row(
                            Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            if (imagen.isNotBlank() && imagen.startsWith("data:image/")) {
                                val imageBitmap = remember(imagen) {
                                    try {
                                        val base64 = imagen.substringAfter(",")
                                        val imageBytes = Base64.decode(base64, Base64.DEFAULT)
                                        BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)?.asImageBitmap()
                                    } catch (_: Exception) { null }
                                }

                                if (imageBitmap != null) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Image(
                                            bitmap = imageBitmap,
                                            contentDescription = null,
                                            modifier = Modifier
                                                .size(80.dp)
                                                .clip(RoundedCornerShape(8.dp)),
                                            contentScale = ContentScale.Crop
                                        )
                                        Spacer(Modifier.width(12.dp))
                                        Column {
                                            val sizeKB = (imagen.length * 0.75 / 1024).toInt()
                                            Text("Imagen cargada", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                                            Text("$sizeKB KB", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                            if (sizeKB > 390) {
                                                Text("Casi excede el límite (400KB)", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
                                            }
                                            Spacer(Modifier.height(4.dp))
                                            TextButton(onClick = { launcher.launch("image/*") }) { Text("Cambiar") }
                                        }
                                    }
                                } else {
                                    Text("Error en la imagen", color = MaterialTheme.colorScheme.error)
                                }
                            } else {
                                // No hay imagen - mostrar botón para seleccionar
                                Card(
                                    onClick = { launcher.launch("image/*") },
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = CardDefaults.cardColors(
                                        containerColor = MaterialTheme.colorScheme.surface
                                    ),
                                    elevation = CardDefaults.cardElevation(2.dp)
                                ) {
                                    Column(
                                        Modifier.padding(24.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Icon(
                                            Icons.Default.Image,
                                            null,
                                            modifier = Modifier.size(48.dp),
                                            tint = MaterialTheme.colorScheme.primary
                                        )
                                        Spacer(Modifier.height(8.dp))
                                        Text("Tocar para seleccionar imagen", style = MaterialTheme.typography.bodyMedium)
                                        Text("JPEG/PNG < 400KB", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                }
                            }
                        }
                        if (imagen.isBlank()) {
                            Text("Debes seleccionar una imagen válida (JPEG/PNG < 400KB)", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                        }

                        OutlinedTextField(
                            value = precioTxt,
                            onValueChange = { precioTxt = it },
                            label = { Text("Precio") },
                            modifier = Modifier.fillMaxWidth(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = stockTxt,
                            onValueChange = { stockTxt = it },
                            label = { Text("Stock") },
                            modifier = Modifier.fillMaxWidth(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true
                        )

                        // Dropdown para seleccionar categoría
                        ExposedDropdownMenuBox(
                            expanded = expanded,
                            onExpandedChange = { expanded = !expanded && !loadingCategorias },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            OutlinedTextField(
                                value = categoriaSeleccionada?.nombre ?: if (loadingCategorias) "Cargando..." else "Seleccionar categoría",
                                onValueChange = { },
                                readOnly = true,
                                label = { Text("Categoría") },
                                trailingIcon = {
                                    ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
                                },
                                enabled = !loadingCategorias,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .menuAnchor()
                            )

                            ExposedDropdownMenu(
                                expanded = expanded,
                                onDismissRequest = { expanded = false },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                if (categorias.isEmpty() && !loadingCategorias) {
                                    DropdownMenuItem(
                                        text = { Text("No hay categorías disponibles") },
                                        onClick = { expanded = false },
                                        enabled = false
                                    )
                                } else {
                                    categorias.forEach { categoria ->
                                        DropdownMenuItem(
                                            text = { Text(categoria.nombre) },
                                            onClick = {
                                                categoriaSeleccionada = categoria
                                                expanded = false
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Botones fijos en la parte inferior
        Card(
            Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(8.dp)
        ) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                OutlinedButton(
                    onClick = { viewModel.cerrarFormulario() },
                    modifier = Modifier.weight(1f),
                    enabled = !loading
                ) {
                    Text("Cancelar")
                }
                Button(
                    onClick = {
                        if (valido && categoriaSeleccionada != null) {
                            viewModel.guardar(
                                Producto(
                                    codigo = productoEdit?.codigo ?: 0, // Usar 0 para productos nuevos
                                    nombre = nombre,
                                    descripcion = descripcion,
                                    imagen = imagen,
                                    precio = precioVal!!,
                                    stock = stockVal!!,
                                    categoriaId = categoriaSeleccionada!!.id!!
                                )
                            )
                        }
                    },
                    modifier = Modifier.weight(1f),
                    enabled = valido && !loading
                ) {
                    if (loading) {
                        CircularProgressIndicator(
                            Modifier.size(20.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    } else {
                        Text(
                            when {
                                !imagen.startsWith("data:image/") -> "Selecciona imagen"
                                productoEdit == null -> "Crear"
                                else -> "Actualizar"
                            }
                        )
                    }
                }
            }
        }
    }
}

// Wrapper compatibilidad
@Composable
fun ProductosModerno() { ProductosScreen() }
