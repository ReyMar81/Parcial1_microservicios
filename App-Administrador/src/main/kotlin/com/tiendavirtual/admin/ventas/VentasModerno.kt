package com.tiendavirtual.admin.ventas

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.Alignment
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.clickable
import com.tiendavirtual.admin.data.shared.ApiConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.net.HttpURLConnection
import java.net.URL
import java.text.SimpleDateFormat
import java.util.*

@Serializable
data class Venta(
    val id: Int,
    val fecha: String,
    val estado: String,
    val total: Double,
    val clienteId: Int,
    val detalles: List<DetalleVenta> = emptyList()
)

@Serializable
data class DetalleVenta(
    val id: Int,
    val productoId: Int,
    val cantidad: Int,
    val precioUnitario: Double
)

@Serializable
data class Cliente(
    val id: Int,
    val nombre: String
)

@Serializable
data class Producto(
    val id: Int,
    val nombre: String,
    val precio: Double
)

@Serializable
data class CrearVentaRequest(
    val clienteId: Int,
    val fecha: String,
    val estado: String,
    val total: Double,
    val detalles: List<DetalleVentaRequest>
)

@Serializable
data class DetalleVentaRequest(
    val productoId: Int,
    val cantidad: Int,
    val precioUnitario: Double
)

class VentasRepository(private val baseUrl: String = ApiConfig.VENTAS_URL) {
    private suspend fun doRequest(
        urlStr: String,
        method: String = "GET",
        body: String? = null,
        contentType: String = ApiConfig.CONTENT_TYPE
    ): String = withContext(Dispatchers.IO) {
        val url = URL(urlStr)
        val conn = (url.openConnection() as HttpURLConnection).apply {
            requestMethod = method
            setRequestProperty("Accept", ApiConfig.ACCEPT)
            if (body != null) {
                doOutput = true
                setRequestProperty("Content-Type", contentType)
            }
            connectTimeout = (ApiConfig.CONNECT_TIMEOUT * 1000).toInt()
            readTimeout = (ApiConfig.READ_TIMEOUT * 1000).toInt()
        }
        try {
            body?.let { conn.outputStream.use { os -> os.write(body.toByteArray()) } }
            val code = conn.responseCode
            val stream = if (code in 200..299) conn.inputStream else conn.errorStream
            val text = stream?.bufferedReader()?.readText() ?: ""
            if (code !in 200..299) throw RuntimeException("HTTP $code -> $text")
            text
        } finally { conn.disconnect() }
    }

    suspend fun listarVentas(): List<Venta> = try {
        val jsonStr = doRequest(baseUrl)
        Json.decodeFromString(jsonStr)
    } catch (e: Exception) {
        throw RuntimeException("Fallo al listar ventas: ${e.message}")
    }

    suspend fun guardarVenta(venta: Venta): Boolean = try {
        val request = CrearVentaRequest(
            clienteId = venta.clienteId,
            fecha = venta.fecha,
            estado = venta.estado,
            total = venta.total,
            detalles = venta.detalles.map { DetalleVentaRequest(it.productoId, it.cantidad, it.precioUnitario) }
        )
        val body = Json.encodeToString(CrearVentaRequest.serializer(), request)
        doRequest(baseUrl, "POST", body)
        true
    } catch (e: Exception) { throw RuntimeException("Fallo al crear venta: ${e.message}") }

    suspend fun actualizarVenta(venta: Venta): Boolean = try {
        val request = CrearVentaRequest(
            clienteId = venta.clienteId,
            fecha = venta.fecha,
            estado = venta.estado,
            total = venta.total,
            detalles = venta.detalles.map { DetalleVentaRequest(it.productoId, it.cantidad, it.precioUnitario) }
        )
        val body = Json.encodeToString(CrearVentaRequest.serializer(), request)
        doRequest("$baseUrl/${venta.id}", "PUT", body)
        true
    } catch (e: Exception) { throw RuntimeException("Fallo al actualizar venta: ${e.message}") }

    suspend fun confirmarVenta(ventaId: Int): Boolean = try {
        doRequest(ApiConfig.CONFIRMAR_VENTA_URL.replace("{id}", ventaId.toString()), "POST", "{}")
        true
    } catch (e: Exception) { throw RuntimeException("Fallo al confirmar venta: ${e.message}") }

    suspend fun anularVenta(ventaId: Int): Boolean = try {
        doRequest(ApiConfig.ANULAR_VENTA_URL.replace("{id}", ventaId.toString()), "POST", "{}")
        true
    } catch (e: Exception) { throw RuntimeException("Fallo al anular venta: ${e.message}") }

    suspend fun eliminarVenta(ventaId: Int): Boolean = try {
        doRequest("$baseUrl/$ventaId", "DELETE")
        true
    } catch (e: Exception) { throw RuntimeException("Fallo al eliminar venta: ${e.message}") }
}

class ClientesRepository(private val baseUrl: String = ApiConfig.CLIENTES_URL) {
    suspend fun listarClientes(): List<Cliente> {
        val url = URL(baseUrl)
        val conn = url.openConnection() as HttpURLConnection
        conn.requestMethod = "GET"
        conn.setRequestProperty("Accept", ApiConfig.ACCEPT)
        conn.connectTimeout = ApiConfig.CONNECT_TIMEOUT.toInt() * 1000
        conn.readTimeout = ApiConfig.READ_TIMEOUT.toInt() * 1000
        return try {
            val response = conn.inputStream.bufferedReader().readText()
            Json.decodeFromString(response)
        } finally {
            conn.disconnect()
        }
    }
}

class ProductosRepository(private val baseUrl: String = ApiConfig.PRODUCTOS_URL) {
    suspend fun listarProductos(): List<Producto> {
        val url = URL(baseUrl)
        val conn = url.openConnection() as HttpURLConnection
        conn.requestMethod = "GET"
        conn.setRequestProperty("Accept", ApiConfig.ACCEPT)
        conn.connectTimeout = ApiConfig.CONNECT_TIMEOUT.toInt() * 1000
        conn.readTimeout = ApiConfig.READ_TIMEOUT.toInt() * 1000
        return try {
            val response = conn.inputStream.bufferedReader().readText()
            Json.decodeFromString(response)
        } finally {
            conn.disconnect()
        }
    }
}

@Composable
fun VentasScreen(
    ventas: List<Venta>,
    onRefresh: () -> Unit,
    onVerDetalle: (Venta) -> Unit
) {
    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text("Ventas", style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.height(8.dp))
        Button(onClick = onRefresh) { Text("Actualizar") }
        Spacer(Modifier.height(8.dp))
        LazyColumn {
            items(ventas) { venta ->
                VentaItem(venta = venta, onVerDetalle = onVerDetalle)
                Divider()
            }
        }
    }
}

@Composable
fun VentaItem(venta: Venta, onVerDetalle: (Venta) -> Unit) {
    Column(modifier = Modifier.fillMaxWidth().padding(8.dp)) {
        Text("Venta #${venta.id}", style = MaterialTheme.typography.titleMedium)
        Text("Cliente: ${venta.clienteId}")
        Text("Fecha: ${venta.fecha}")
        Text("Estado: ${venta.estado}")
        Text("Total: $${venta.total}")
        Button(onClick = { onVerDetalle(venta) }, modifier = Modifier.padding(top = 4.dp)) {
            Text("Ver Detalle")
        }
    }
}

@Composable
fun DetalleVentaScreen(
    venta: Venta,
    detalles: List<DetalleVenta>,
    clientes: List<Cliente>,
    productos: List<Producto>,
    onBack: () -> Unit,
    onVentaModificada: () -> Unit
) {
    val scope = rememberCoroutineScope()
    val ventasRepo = VentasRepository(ApiConfig.VENTAS_URL)
    var procesando by remember { mutableStateOf(false) }
    var mensaje by remember { mutableStateOf("") }
    var mostrarEditar by remember { mutableStateOf(false) }

    if (mostrarEditar && venta.estado == "CREADA") {
        EditarVentaScreen(
            venta = venta,
            clientes = clientes,
            productos = productos,
            onVentaActualizada = {
                mostrarEditar = false
                onVentaModificada()
            },
            onCancelar = { mostrarEditar = false }
        )
    } else {
        Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
            Text("Detalle de Venta #${venta.id}", style = MaterialTheme.typography.headlineMedium)
            Text("Cliente: ${venta.clienteId}")
            Text("Fecha: ${venta.fecha}")
            Text("Estado: ${venta.estado}")
            Text("Total: $${venta.total}")
            Spacer(Modifier.height(8.dp))

            Text("Productos:", style = MaterialTheme.typography.titleMedium)
            LazyColumn(modifier = Modifier.weight(1f)) {
                items(detalles) { detalle ->
                    DetalleVentaItem(detalle)
                    Divider()
                }
            }

            Spacer(Modifier.height(16.dp))

            // Botones de acciones según el estado
            when (venta.estado) {
                "CREADA" -> {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = { mostrarEditar = true },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Editar")
                        }
                        Button(
                            onClick = {
                                procesando = true
                                mensaje = ""
                                scope.launch {
                                    val ok = ventasRepo.confirmarVenta(venta.id)
                                    if (ok) {
                                        mensaje = "Venta confirmada exitosamente"
                                        onVentaModificada()
                                    } else {
                                        mensaje = "Error al confirmar venta"
                                    }
                                    procesando = false
                                }
                            },
                            enabled = !procesando,
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(if (procesando) "Confirmando..." else "Confirmar")
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = {
                                procesando = true
                                mensaje = ""
                                scope.launch {
                                    val ok = ventasRepo.anularVenta(venta.id)
                                    if (ok) {
                                        mensaje = "Venta anulada exitosamente"
                                        onVentaModificada()
                                    } else {
                                        mensaje = "Error al anular venta"
                                    }
                                    procesando = false
                                }
                            },
                            enabled = !procesando,
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(if (procesando) "Anulando..." else "Anular", color = MaterialTheme.colorScheme.onError)
                        }
                        Button(
                            onClick = {
                                procesando = true
                                mensaje = ""
                                scope.launch {
                                    val ok = ventasRepo.eliminarVenta(venta.id)
                                    if (ok) {
                                        mensaje = "Venta eliminada exitosamente"
                                        onBack()
                                        onVentaModificada()
                                    } else {
                                        mensaje = "Error al eliminar venta"
                                    }
                                    procesando = false
                                }
                            },
                            enabled = !procesando,
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(if (procesando) "Eliminando..." else "Eliminar", color = MaterialTheme.colorScheme.onError)
                        }
                    }
                }
                "CONFIRMADA" -> {
                    Text("✅ Venta confirmada - Solo lectura", color = MaterialTheme.colorScheme.primary)
                }
                "ANULADA" -> {
                    Text("❌ Venta anulada - Solo lectura", color = MaterialTheme.colorScheme.error)
                }
            }

            if (mensaje.isNotBlank()) {
                Spacer(Modifier.height(8.dp))
                Text(mensaje, color = if (mensaje.contains("Error")) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary)
            }

            Spacer(Modifier.height(16.dp))
            Button(onClick = onBack, modifier = Modifier.fillMaxWidth()) {
                Text("Volver")
            }
        }
    }
}

@Composable
fun DetalleVentaItem(detalle: DetalleVenta) {
    Row(modifier = Modifier.fillMaxWidth().padding(8.dp), horizontalArrangement = Arrangement.SpaceBetween) {
        Text("Producto: ${detalle.productoId}")
        Text("Cantidad: ${detalle.cantidad}")
        Text("Precio: $${detalle.precioUnitario}")
    }
}

@Composable
fun CrearVentaScreen(
    clientes: List<Cliente>,
    productos: List<Producto>,
    onVentaGuardada: () -> Unit
) {
    var clienteSeleccionado by remember { mutableStateOf<Cliente?>(null) }
    var productoSeleccionado by remember { mutableStateOf<Producto?>(null) }
    var cantidad by remember { mutableStateOf("") }
    var detalles by remember { mutableStateOf(listOf<DetalleVenta>()) }
    var expandedCliente by remember { mutableStateOf(false) }
    var expandedProducto by remember { mutableStateOf(false) }
    val total = detalles.sumOf { it.cantidad * it.precioUnitario }
    val scope = rememberCoroutineScope()
    val ventasRepo = VentasRepository(ApiConfig.VENTAS_URL)
    var guardando by remember { mutableStateOf(false) }
    var mensaje by remember { mutableStateOf("") }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text("Crear Venta", style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.height(8.dp))
        // Selección de cliente
        Box {
            TextField(
                value = clienteSeleccionado?.nombre ?: "Selecciona cliente",
                onValueChange = {},
                readOnly = true,
                modifier = Modifier.fillMaxWidth().clickable { expandedCliente = true },
                label = { Text("Cliente") }
            )
            DropdownMenu(expanded = expandedCliente, onDismissRequest = { expandedCliente = false }) {
                clientes.forEach { cliente ->
                    DropdownMenuItem(text = { Text(cliente.nombre) }, onClick = {
                        clienteSeleccionado = cliente
                        expandedCliente = false
                    })
                }
            }
        }
        Spacer(Modifier.height(8.dp))
        // Selección de producto y cantidad
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.weight(1f)) {
                TextField(
                    value = productoSeleccionado?.nombre ?: "Selecciona producto",
                    onValueChange = {},
                    readOnly = true,
                    modifier = Modifier.fillMaxWidth().clickable { expandedProducto = true },
                    label = { Text("Producto") }
                )
                DropdownMenu(expanded = expandedProducto, onDismissRequest = { expandedProducto = false }) {
                    productos.forEach { producto ->
                        DropdownMenuItem(text = { Text(producto.nombre) }, onClick = {
                            productoSeleccionado = producto
                            expandedProducto = false
                        })
                    }
                }
            }
            Spacer(Modifier.width(8.dp))
            TextField(
                value = cantidad,
                onValueChange = { cantidad = it.filter { c -> c.isDigit() } },
                label = { Text("Cantidad") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.width(100.dp)
            )
            Spacer(Modifier.width(8.dp))
            IconButton(onClick = {
                if (productoSeleccionado != null && cantidad.isNotBlank() && cantidad.toInt() > 0) {
                    detalles = detalles + DetalleVenta(
                        id = detalles.size + 1,
                        productoId = productoSeleccionado!!.id,
                        cantidad = cantidad.toInt(),
                        precioUnitario = productoSeleccionado!!.precio
                    )
                    cantidad = ""
                    productoSeleccionado = null
                }
            }) {
                Icon(Icons.Filled.Add, contentDescription = "Agregar producto")
            }
        }
        Spacer(Modifier.height(8.dp))
        // Lista de detalles
        Text("Detalle de venta:", style = MaterialTheme.typography.titleMedium)
        LazyColumn {
            items(detalles) { detalle ->
                Row(modifier = Modifier.fillMaxWidth().padding(4.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("${detalle.productoId}")
                    Text("Cantidad: ${detalle.cantidad}")
                    Text("Precio: Bs. ${detalle.precioUnitario}")
                    IconButton(onClick = {
                        detalles = detalles.filter { it.id != detalle.id }
                    }) {
                        Icon(Icons.Filled.Delete, contentDescription = "Eliminar")
                    }
                }
            }
        }
        Spacer(Modifier.height(8.dp))
        Text("Total: Bs. ${total}", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(16.dp))
        Button(
            onClick = {
                guardando = true
                mensaje = ""
                scope.launch {
                    if (clienteSeleccionado != null && detalles.isNotEmpty()) {
                        val venta = Venta(
                            id = 0,
                            fecha = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date()),
                            estado = "CREADA",
                            total = total,
                            clienteId = clienteSeleccionado!!.id,
                            detalles = detalles
                        )
                        val ok = ventasRepo.guardarVenta(venta)
                        if (ok) {
                            mensaje = "Venta guardada correctamente"
                            detalles = emptyList()
                            clienteSeleccionado = null
                            onVentaGuardada()
                        } else {
                            mensaje = "Error al guardar venta"
                        }
                    } else {
                        mensaje = "Selecciona cliente y agrega productos"
                    }
                    guardando = false
                }
            },
            enabled = !guardando && clienteSeleccionado != null && detalles.isNotEmpty(),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(if (guardando) "Guardando..." else "Guardar Venta")
        }
        if (mensaje.isNotBlank()) {
            Text(mensaje, color = if (mensaje.contains("Error")) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary)
        }
    }
}

@Composable
fun EditarVentaScreen(
    venta: Venta,
    clientes: List<Cliente>,
    productos: List<Producto>,
    onVentaActualizada: () -> Unit,
    onCancelar: () -> Unit
) {
    var clienteSeleccionado by remember { mutableStateOf(clientes.find { it.id == venta.clienteId }) }
    var detalles by remember {
        mutableStateOf(venta.detalles.map {
            DetalleVenta(it.id, it.productoId, it.cantidad, it.precioUnitario)
        })
    }
    var productoSeleccionado by remember { mutableStateOf<Producto?>(null) }
    var cantidad by remember { mutableStateOf("") }
    var expandedCliente by remember { mutableStateOf(false) }
    var expandedProducto by remember { mutableStateOf(false) }
    val total = detalles.sumOf { it.cantidad * it.precioUnitario }
    val scope = rememberCoroutineScope()
    val ventasRepo = VentasRepository(ApiConfig.VENTAS_URL)
    var guardando by remember { mutableStateOf(false) }
    var mensaje by remember { mutableStateOf("") }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text("Editar Venta #${venta.id}", style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.height(8.dp))

        // Selección de cliente
        Box {
            TextField(
                value = clienteSeleccionado?.nombre ?: "Selecciona cliente",
                onValueChange = {},
                readOnly = true,
                modifier = Modifier.fillMaxWidth().clickable { expandedCliente = true },
                label = { Text("Cliente") }
            )
            DropdownMenu(expanded = expandedCliente, onDismissRequest = { expandedCliente = false }) {
                clientes.forEach { cliente ->
                    DropdownMenuItem(text = { Text(cliente.nombre) }, onClick = {
                        clienteSeleccionado = cliente
                        expandedCliente = false
                    })
                }
            }
        }
        Spacer(Modifier.height(8.dp))

        // Agregar nuevos productos
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.weight(1f)) {
                TextField(
                    value = productoSeleccionado?.nombre ?: "Agregar producto",
                    onValueChange = {},
                    readOnly = true,
                    modifier = Modifier.fillMaxWidth().clickable { expandedProducto = true },
                    label = { Text("Producto") }
                )
                DropdownMenu(expanded = expandedProducto, onDismissRequest = { expandedProducto = false }) {
                    productos.forEach { producto ->
                        DropdownMenuItem(text = { Text(producto.nombre) }, onClick = {
                            productoSeleccionado = producto
                            expandedProducto = false
                        })
                    }
                }
            }
            Spacer(Modifier.width(8.dp))
            TextField(
                value = cantidad,
                onValueChange = { cantidad = it.filter { c -> c.isDigit() } },
                label = { Text("Cantidad") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.width(100.dp)
            )
            Spacer(Modifier.width(8.dp))
            IconButton(onClick = {
                if (productoSeleccionado != null && cantidad.isNotBlank() && cantidad.toInt() > 0) {
                    detalles = detalles + DetalleVenta(
                        id = detalles.size + 1,
                        productoId = productoSeleccionado!!.id,
                        cantidad = cantidad.toInt(),
                        precioUnitario = productoSeleccionado!!.precio
                    )
                    cantidad = ""
                    productoSeleccionado = null
                }
            }) {
                Icon(Icons.Filled.Add, contentDescription = "Agregar producto")
            }
        }
        Spacer(Modifier.height(8.dp))

        // Lista de detalles editables
        Text("Productos en la venta:", style = MaterialTheme.typography.titleMedium)
        LazyColumn(modifier = Modifier.weight(1f)) {
            items(detalles) { detalle ->
                Row(modifier = Modifier.fillMaxWidth().padding(4.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("ID: ${detalle.productoId}")
                    Text("Cant: ${detalle.cantidad}")
                    Text("$${detalle.precioUnitario}")
                    IconButton(onClick = {
                        detalles = detalles.filter { it.id != detalle.id }
                    }) {
                        Icon(Icons.Filled.Delete, contentDescription = "Eliminar")
                    }
                }
            }
        }

        Text("Total: $${total}", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(16.dp))

        // Botones de acción
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(
                onClick = onCancelar,
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
            ) {
                Text("Cancelar")
            }
            Button(
                onClick = {
                    guardando = true
                    mensaje = ""
                    scope.launch {
                        if (clienteSeleccionado != null && detalles.isNotEmpty()) {
                            val ventaActualizada = Venta(
                                id = venta.id,
                                fecha = venta.fecha,
                                estado = "CREADA",
                                total = total,
                                clienteId = clienteSeleccionado!!.id,
                                detalles = detalles
                            )
                            val ok = ventasRepo.actualizarVenta(ventaActualizada)
                            if (ok) {
                                mensaje = "Venta actualizada correctamente"
                                onVentaActualizada()
                            } else {
                                mensaje = "Error al actualizar venta"
                            }
                        } else {
                            mensaje = "Selecciona cliente y agrega productos"
                        }
                        guardando = false
                    }
                },
                enabled = !guardando && clienteSeleccionado != null && detalles.isNotEmpty(),
                modifier = Modifier.weight(1f)
            ) {
                Text(if (guardando) "Guardando..." else "Guardar Cambios")
            }
        }

        if (mensaje.isNotBlank()) {
            Spacer(Modifier.height(8.dp))
            Text(mensaje, color = if (mensaje.contains("Error")) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary)
        }
    }
}

@Composable
fun VentasModuleScreen() {
    // Estados principales
    val ventasRepo = remember { VentasRepository(ApiConfig.VENTAS_URL) }
    val clientesRepo = remember { ClientesRepository(ApiConfig.CLIENTES_URL) }
    val productosRepo = remember { ProductosRepository(ApiConfig.PRODUCTOS_URL) }

    var ventas by remember { mutableStateOf<List<Venta>>(emptyList()) }
    var detalles by remember { mutableStateOf<List<DetalleVenta>>(emptyList()) }
    var clientes by remember { mutableStateOf<List<Cliente>>(emptyList()) }
    var productos by remember { mutableStateOf<List<Producto>>(emptyList()) }
    var ventaSeleccionada by remember { mutableStateOf<Venta?>(null) }
    var mostrarCrearVenta by remember { mutableStateOf(false) }
    var cargando by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var rawError by remember { mutableStateOf<String?>(null) }

    val scope = rememberCoroutineScope()

    fun recargar() {
        cargando = true
        error = null
        rawError = null
        scope.launch {
            try {
                val clientesData = clientesRepo.listarClientes()
                val productosData = productosRepo.listarProductos()
                val ventasData = ventasRepo.listarVentas()
                clientes = clientesData
                productos = productosData
                ventas = ventasData
            } catch (e: Exception) {
                error = "Error cargando datos (ver detalles)"
                rawError = e.message
            } finally {
                cargando = false
            }
        }
    }

    LaunchedEffect(Unit) { recargar() }

    when {
        cargando -> {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        }
        error != null -> {
            Column(
                Modifier.fillMaxSize().padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(error!!, color = MaterialTheme.colorScheme.error)
                rawError?.let { Text(it, style = MaterialTheme.typography.bodySmall) }
                Spacer(Modifier.height(12.dp))
                Text(
                    "Checklist:\n1. ¿Gateway arriba? http://192.168.0.10:8080/health\n2. ¿Ruta /api/ventas responde?\n3. ¿Contenedor MS-Ventas levantado? (docker ps)\n4. ¿DB ventas creada? (docker exec -it DB-Ventas psql -U app -d ventas -c 'select count(*) from ventas;')\n5. Si usas emulador usa 10.0.2.2 en ApiConfig.",
                    style = MaterialTheme.typography.bodySmall
                )
                Spacer(Modifier.height(12.dp))
                Button(onClick = { recargar() }) { Text("Reintentar") }
            }
        }
        mostrarCrearVenta -> {
            CrearVentaScreen(
                clientes = clientes,
                productos = productos,
                onVentaGuardada = {
                    mostrarCrearVenta = false
                    recargar()
                }
            )
        }
        ventaSeleccionada == null -> {
            Column(Modifier.fillMaxSize()) {
                Button(onClick = { mostrarCrearVenta = true }, modifier = Modifier.padding(16.dp)) { Text("Nueva Venta") }
                VentasScreen(
                    ventas = ventas,
                    onRefresh = { recargar() },
                    onVerDetalle = { venta ->
                        ventaSeleccionada = venta
                        detalles = venta.detalles
                    }
                )
            }
        }
        else -> {
            DetalleVentaScreen(
                venta = ventaSeleccionada!!,
                detalles = detalles,
                clientes = clientes,
                productos = productos,
                onBack = { ventaSeleccionada = null },
                onVentaModificada = {
                    recargar()
                    scope.launch {
                        val v = ventasRepo.listarVentas().find { it.id == ventaSeleccionada!!.id }
                        if (v != null) {
                            ventaSeleccionada = v
                            detalles = v.detalles
                        }
                    }
                }
            )
        }
    }
}

class VentasModerno : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            VentasModuleScreen()
        }
    }
}
