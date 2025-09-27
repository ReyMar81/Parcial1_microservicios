package com.tiendavirtual.admin.clientes

import android.util.Log
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.concurrent.TimeUnit

// ============== MODELO ===============
data class Cliente(
    val id: Int? = null,
    val nombres: String,
    val docIdentidad: String,
    val whatsapp: String,
    val direccion: String
)

// ============== SERVICIO ===============
object ClientesService {
    private val client = OkHttpClient.Builder()
        .connectTimeout(ApiConfig.CONNECT_TIMEOUT, TimeUnit.SECONDS)
        .readTimeout(ApiConfig.READ_TIMEOUT, TimeUnit.SECONDS)
        .writeTimeout(ApiConfig.WRITE_TIMEOUT, TimeUnit.SECONDS)
        .build()

    private val gson = Gson()

    suspend fun obtenerClientes(): List<Cliente> = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder()
                .url(ApiConfig.CLIENTES_URL)
                .addHeader("Accept", ApiConfig.ACCEPT)
                .build()
            val response = client.newCall(request).execute()
            if (response.isSuccessful) {
                val json = response.body?.string() ?: "[]"
                val type = object : TypeToken<List<Cliente>>() {}.type
                gson.fromJson(json, type) ?: emptyList()
            } else emptyList()
        } catch (e: Exception) {
            Log.e("ClientesService", "Error al obtener clientes", e)
            emptyList()
        }
    }

    suspend fun crearCliente(cliente: Cliente): Cliente? = withContext(Dispatchers.IO) {
        try {
            val json = gson.toJson(cliente.copy(
                nombres = cliente.nombres.trim(),
                docIdentidad = cliente.docIdentidad.trim(),
                whatsapp = cliente.whatsapp.replace("[\\s()-]".toRegex(), ""),
                direccion = cliente.direccion.trim()
            ))
            val body = json.toRequestBody(ApiConfig.CONTENT_TYPE.toMediaType())
            val request = Request.Builder()
                .url(ApiConfig.CLIENTES_URL)
                .post(body)
                .addHeader("Content-Type", ApiConfig.CONTENT_TYPE)
                .addHeader("Accept", ApiConfig.ACCEPT)
                .build()
            val response = client.newCall(request).execute()
            if (response.isSuccessful) {
                gson.fromJson(response.body?.string(), Cliente::class.java)
            } else null
        } catch (e: Exception) {
            Log.e("ClientesService", "Error al crear cliente", e)
            null
        }
    }

    suspend fun actualizarCliente(id: Int, cliente: Cliente): Cliente? = withContext(Dispatchers.IO) {
        try {
            val json = gson.toJson(cliente.copy(
                id = id,
                nombres = cliente.nombres.trim(),
                docIdentidad = cliente.docIdentidad.trim(),
                whatsapp = cliente.whatsapp.replace("[\\s()-]".toRegex(), ""),
                direccion = cliente.direccion.trim()
            ))
            val body = json.toRequestBody(ApiConfig.CONTENT_TYPE.toMediaType())
            val request = Request.Builder()
                .url("${ApiConfig.CLIENTES_URL}/$id")
                .patch(body)
                .addHeader("Content-Type", ApiConfig.CONTENT_TYPE)
                .addHeader("Accept", ApiConfig.ACCEPT)
                .build()
            val response = client.newCall(request).execute()
            if (response.isSuccessful) {
                gson.fromJson(response.body?.string(), Cliente::class.java)
            } else null
        } catch (e: Exception) {
            Log.e("ClientesService", "Error al actualizar cliente", e)
            null
        }
    }

    suspend fun eliminarCliente(id: Int): Boolean = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder()
                .url("${ApiConfig.CLIENTES_URL}/$id")
                .delete()
                .addHeader("Accept", ApiConfig.ACCEPT)
                .build()
            client.newCall(request).execute().isSuccessful
        } catch (e: Exception) {
            Log.e("ClientesService", "Error al eliminar cliente", e)
            false
        }
    }
}

// ============== VIEWMODEL ===============
class ClientesViewModel : ViewModel() {
    private val _clientes = MutableStateFlow<List<Cliente>>(emptyList())
    val clientes: StateFlow<List<Cliente>> = _clientes.asStateFlow()

    private val _loading = MutableStateFlow(false)
    val loading: StateFlow<Boolean> = _loading.asStateFlow()

    private val _mostrarFormulario = MutableStateFlow(false)
    val mostrarFormulario: StateFlow<Boolean> = _mostrarFormulario.asStateFlow()

    private val _clienteEditando = MutableStateFlow<Cliente?>(null)
    val clienteEditando: StateFlow<Cliente?> = _clienteEditando.asStateFlow()

    init {
        cargarClientes()
    }

    private fun cargarClientes() {
        viewModelScope.launch {
            _loading.value = true
            _clientes.value = ClientesService.obtenerClientes()
            _loading.value = false
        }
    }

    fun mostrarFormularioNuevo() {
        _clienteEditando.value = null
        _mostrarFormulario.value = true
    }

    fun mostrarFormularioEditar(cliente: Cliente) {
        _clienteEditando.value = cliente
        _mostrarFormulario.value = true
    }

    fun cerrarFormulario() {
        _mostrarFormulario.value = false
        _clienteEditando.value = null
    }

    fun guardarCliente(cliente: Cliente) {
        viewModelScope.launch {
            _loading.value = true
            val resultado = if (_clienteEditando.value == null) {
                ClientesService.crearCliente(cliente)
            } else {
                ClientesService.actualizarCliente(_clienteEditando.value!!.id!!, cliente)
            }

            if (resultado != null) {
                // Actualizar lista localmente para mejor UX
                _clientes.value = if (_clienteEditando.value == null) {
                    _clientes.value + resultado
                } else {
                    _clientes.value.map { if (it.id == resultado.id) resultado else it }
                }
                cerrarFormulario()
            }
            _loading.value = false
        }
    }

    fun eliminarCliente(cliente: Cliente) {
        viewModelScope.launch {
            _loading.value = true
            if (ClientesService.eliminarCliente(cliente.id!!)) {
                _clientes.value = _clientes.value.filter { it.id != cliente.id }
            }
            _loading.value = false
        }
    }
}

// ============== PANTALLA PRINCIPAL (Lista de Clientes) ===============
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ClientesScreen(viewModel: ClientesViewModel = viewModel()) {
    val clientes by viewModel.clientes.collectAsState()
    val loading by viewModel.loading.collectAsState()
    val mostrarFormulario by viewModel.mostrarFormulario.collectAsState()

    if (mostrarFormulario) {
        ClienteFormularioScreen(viewModel)
    } else {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            // Header homogéneo con MainScreen
            Text(
                text = "Gestión de Clientes",
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )

            Text(
                text = "Administrar clientes registrados",
                style = MaterialTheme.typography.titleMedium,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Botón de acción principal (como el grid de MainScreen)
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(80.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primary
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Total de Clientes",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                        Text(
                            text = "${clientes.size} registrados",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    }

                    FilledTonalButton(
                        onClick = { viewModel.mostrarFormularioNuevo() },
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = MaterialTheme.colorScheme.onPrimary,
                            contentColor = MaterialTheme.colorScheme.primary
                        )
                    ) {
                        Icon(
                            Icons.Default.Add,
                            contentDescription = "Nuevo Cliente",
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Nuevo Cliente")
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Lista de clientes
            if (loading && clientes.isEmpty()) {
                // Loading inicial
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        CircularProgressIndicator()
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            "Cargando clientes...",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            } else if (clientes.isEmpty()) {
                // Estado vacío (similar a los módulos deshabilitados en MainScreen)
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            Icons.Default.Person,
                            contentDescription = null,
                            modifier = Modifier.size(48.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            "No hay clientes registrados",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            "Presiona 'Nuevo Cliente' para agregar el primero",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            } else {
                // Lista de clientes con cards similares a MainScreen
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(clientes, key = { it.id ?: 0 }) { cliente ->
                        ClienteCard(
                            cliente = cliente,
                            onEditar = { viewModel.mostrarFormularioEditar(cliente) },
                            onEliminar = { viewModel.eliminarCliente(cliente) }
                        )
                    }
                }
            }

            // Loading overlay cuando está actualizando
            if (loading && clientes.isNotEmpty()) {
                Spacer(modifier = Modifier.height(16.dp))
                LinearProgressIndicator(
                    modifier = Modifier.fillMaxWidth(),
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

// ============== TARJETA DE CLIENTE (Estilo homogéneo con ModuleCard) ===============
@Composable
fun ClienteCard(
    cliente: Cliente,
    onEditar: () -> Unit,
    onEliminar: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Icono (similar al estilo de MainScreen)
            Icon(
                imageVector = Icons.Default.Person,
                contentDescription = null,
                modifier = Modifier.size(48.dp),
                tint = MaterialTheme.colorScheme.primary
            )

            Spacer(modifier = Modifier.width(16.dp))

            // Información del cliente
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = cliente.nombres,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Doc: ${cliente.docIdentidad}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "Tel: ${cliente.whatsapp}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Botones de acción
            Row {
                IconButton(onClick = onEditar) {
                    Icon(
                        Icons.Default.Edit,
                        contentDescription = "Editar",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
                IconButton(onClick = onEliminar) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = "Eliminar",
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
}

// ============== FORMULARIO (Estilo homogéneo con MainScreen) ===============
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ClienteFormularioScreen(viewModel: ClientesViewModel) {
    val clienteEditando by viewModel.clienteEditando.collectAsState()
    val loading by viewModel.loading.collectAsState()

    var nombres by remember(clienteEditando) { mutableStateOf(clienteEditando?.nombres ?: "") }
    var docIdentidad by remember(clienteEditando) { mutableStateOf(clienteEditando?.docIdentidad ?: "") }
    var whatsapp by remember(clienteEditando) { mutableStateOf(clienteEditando?.whatsapp ?: "") }
    var direccion by remember(clienteEditando) { mutableStateOf(clienteEditando?.direccion ?: "") }

    fun esValido() = nombres.isNotBlank() && docIdentidad.isNotBlank() &&
                    whatsapp.isNotBlank() && direccion.isNotBlank()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Header homogéneo con MainScreen y ClientesScreen
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = { viewModel.cerrarFormulario() }) {
                Icon(
                    Icons.Default.ArrowBack,
                    contentDescription = "Volver",
                    tint = MaterialTheme.colorScheme.primary
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = if (clienteEditando == null) "Nuevo Cliente" else "Editar Cliente",
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = if (clienteEditando == null) "Agregar cliente al sistema" else "Actualizar información",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Formulario en Cards (estilo homogéneo)
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            )
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "Información Personal",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                OutlinedTextField(
                    value = nombres,
                    onValueChange = { nombres = it },
                    label = { Text("Nombres completos") },
                    leadingIcon = { Icon(Icons.Default.Person, null) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                OutlinedTextField(
                    value = docIdentidad,
                    onValueChange = { docIdentidad = it },
                    label = { Text("Documento de identidad") },
                    leadingIcon = { Icon(Icons.Default.Info, null) },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            )
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "Información de Contacto",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                OutlinedTextField(
                    value = whatsapp,
                    onValueChange = { whatsapp = it },
                    label = { Text("WhatsApp") },
                    leadingIcon = { Icon(Icons.Default.Phone, null) },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    singleLine = true
                )

                OutlinedTextField(
                    value = direccion,
                    onValueChange = { direccion = it },
                    label = { Text("Dirección") },
                    leadingIcon = { Icon(Icons.Default.LocationOn, null) },
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 2
                )
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        // Botones de acción (estilo homogéneo)
        Row(
            modifier = Modifier.fillMaxWidth(),
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
                    if (esValido()) {
                        viewModel.guardarCliente(
                            Cliente(
                                id = clienteEditando?.id,
                                nombres = nombres,
                                docIdentidad = docIdentidad,
                                whatsapp = whatsapp,
                                direccion = direccion
                            )
                        )
                    }
                },
                modifier = Modifier.weight(1f),
                enabled = !loading && esValido()
            ) {
                if (loading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                } else {
                    Text(if (clienteEditando == null) "Crear Cliente" else "Actualizar")
                }
            }
        }
    }
}

