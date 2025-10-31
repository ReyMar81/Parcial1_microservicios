package com.tiendavirtual.admin.presentation.ventas

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.tiendavirtual.admin.core.ui.components.EmptyCard
import com.tiendavirtual.admin.core.ui.components.ErrorCard
import com.tiendavirtual.admin.core.ui.components.LoadingCard
import com.tiendavirtual.admin.presentation.ventas.components.VentaCard
import com.tiendavirtual.admin.presentation.ventas.components.VentaFormulario

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VentasScreen(
    viewModel: VentasViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    
    // Mostrar diálogo de detalle de venta
    if (uiState.mostrarDetalleVenta && uiState.ventaSeleccionada != null) {
        DetalleVentaDialog(
            venta = uiState.ventaSeleccionada!!,
            productos = uiState.productos,
            onDismiss = { viewModel.cerrarDetalleVenta() },
            onEditar = { viewModel.mostrarEditarVenta() },
            onConfirmar = { viewModel.confirmarVenta(it) },
            onAnular = { viewModel.anularVenta(it) },
            onEliminar = { viewModel.eliminarVenta(it) },
            procesando = uiState.procesandoAccion
        )
    }
    
    // Mostrar formulario de crear venta
    if (uiState.mostrarCrearVenta) {
        AlertDialog(
            onDismissRequest = { viewModel.cerrarCrearVenta() },
            confirmButton = {},
            title = { Text("Crear Venta") },
            text = {
                VentaFormulario(
                    clientes = uiState.clientes,
                    productos = uiState.productos,
                    clienteSeleccionado = uiState.clienteSeleccionado,
                    onClienteSeleccionado = { viewModel.seleccionarCliente(it) },
                    productoSeleccionado = uiState.productoSeleccionado,
                    onProductoSeleccionado = { viewModel.seleccionarProducto(it) },
                    cantidad = uiState.cantidadProducto,
                    onCantidadChange = { viewModel.actualizarCantidad(it) },
                    detalles = uiState.detallesVenta,
                    onAgregarProducto = { viewModel.agregarProductoADetalle() },
                    onEliminarDetalle = { viewModel.eliminarDetalleVenta(it) },
                    total = uiState.totalVenta,
                    onGuardar = { viewModel.crearVenta() },
                    onCancelar = { viewModel.cerrarCrearVenta() },
                    guardando = uiState.procesandoAccion
                )
            },
            modifier = Modifier.fillMaxWidth(0.95f)
        )
    }
    
    // Mostrar formulario de editar venta
    if (uiState.mostrarEditarVenta && uiState.ventaSeleccionada != null) {
        AlertDialog(
            onDismissRequest = { viewModel.cerrarEditarVenta() },
            confirmButton = {},
            title = { Text("Editar Venta #${uiState.ventaSeleccionada?.id}") },
            text = {
                VentaFormulario(
                    clientes = uiState.clientes,
                    productos = uiState.productos,
                    clienteSeleccionado = uiState.clienteSeleccionado,
                    onClienteSeleccionado = { viewModel.seleccionarCliente(it) },
                    productoSeleccionado = uiState.productoSeleccionado,
                    onProductoSeleccionado = { viewModel.seleccionarProducto(it) },
                    cantidad = uiState.cantidadProducto,
                    onCantidadChange = { viewModel.actualizarCantidad(it) },
                    detalles = uiState.detallesVenta,
                    onAgregarProducto = { viewModel.agregarProductoADetalle() },
                    onEliminarDetalle = { viewModel.eliminarDetalleVenta(it) },
                    total = uiState.totalVenta,
                    onGuardar = { viewModel.actualizarVenta() },
                    onCancelar = { viewModel.cerrarEditarVenta() },
                    guardando = uiState.procesandoAccion,
                    tituloBoton = "Actualizar Venta"
                )
            },
            modifier = Modifier.fillMaxWidth(0.95f)
        )
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Ventas") }
            )
        },
        floatingActionButton = {
            if (!uiState.isLoading && uiState.error == null) {
                FloatingActionButton(
                    onClick = { viewModel.mostrarCrearVenta() }
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Crear venta")
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Mensaje de éxito/error
            uiState.mensaje?.let { mensaje ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (mensaje.contains("Error") || mensaje.contains("error"))
                            MaterialTheme.colorScheme.errorContainer
                        else
                            MaterialTheme.colorScheme.primaryContainer
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = mensaje,
                            modifier = Modifier.weight(1f)
                        )
                        TextButton(onClick = { viewModel.limpiarMensaje() }) {
                            Text("OK")
                        }
                    }
                }
            }
            
            when {
                uiState.isLoading -> {
                    LoadingCard(
                        message = "Cargando ventas...",
                        modifier = Modifier.padding(16.dp)
                    )
                }
                uiState.error != null -> {
                    ErrorCard(
                        message = uiState.error!!,
                        onRetry = { viewModel.cargarDatos() },
                        modifier = Modifier.padding(16.dp)
                    )
                }
                uiState.ventas.isEmpty() -> {
                    EmptyCard(
                        message = "No hay ventas registradas",
                        modifier = Modifier.padding(16.dp)
                    )
                }
                else -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(uiState.ventas) { venta ->
                            VentaCard(
                                venta = venta,
                                onClick = { viewModel.seleccionarVenta(venta) }
                            )
                        }
                    }
                }
            }
        }
    }
}

