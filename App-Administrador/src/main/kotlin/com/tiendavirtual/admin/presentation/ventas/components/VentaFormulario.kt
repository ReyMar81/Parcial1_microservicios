package com.tiendavirtual.admin.presentation.ventas.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.tiendavirtual.admin.domain.model.Cliente
import com.tiendavirtual.admin.domain.model.DetalleVenta
import com.tiendavirtual.admin.domain.model.Producto

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VentaFormulario(
    clientes: List<Cliente>,
    productos: List<Producto>,
    clienteSeleccionado: Cliente?,
    onClienteSeleccionado: (Cliente) -> Unit,
    productoSeleccionado: Producto?,
    onProductoSeleccionado: (Producto) -> Unit,
    cantidad: String,
    onCantidadChange: (String) -> Unit,
    detalles: List<DetalleVenta>,
    onAgregarProducto: () -> Unit,
    onEliminarDetalle: (DetalleVenta) -> Unit,
    total: Double,
    onGuardar: () -> Unit,
    onCancelar: () -> Unit,
    guardando: Boolean,
    tituloBoton: String = "Guardar Venta",
    modifier: Modifier = Modifier
) {
    var expandedCliente by remember { mutableStateOf(false) }
    var expandedProducto by remember { mutableStateOf(false) }
    
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Selector de Cliente
        item {
            Column {
                Text(
                    text = "Cliente *",
                    style = MaterialTheme.typography.labelLarge
                )
                Spacer(modifier = Modifier.height(8.dp))
                ExposedDropdownMenuBox(
                    expanded = expandedCliente,
                    onExpandedChange = { expandedCliente = !expandedCliente }
                ) {
                    OutlinedTextField(
                        value = clienteSeleccionado?.nombre ?: "Seleccione un cliente",
                        onValueChange = {},
                        readOnly = true,
                        trailingIcon = {
                            Icon(
                                imageVector = Icons.Default.ArrowDropDown,
                                contentDescription = null
                            )
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(),
                        colors = OutlinedTextFieldDefaults.colors()
                    )
                    
                    ExposedDropdownMenu(
                        expanded = expandedCliente,
                        onDismissRequest = { expandedCliente = false }
                    ) {
                        clientes.forEach { cliente ->
                            DropdownMenuItem(
                                text = { Text(cliente.nombre) },
                                onClick = {
                                    onClienteSeleccionado(cliente)
                                    expandedCliente = false
                                }
                            )
                        }
                    }
                }
            }
        }
        
        // Agregar Productos
        item {
            Text(
                text = "Agregar Productos",
                style = MaterialTheme.typography.titleMedium
            )
        }
        
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Selector de Producto
                    ExposedDropdownMenuBox(
                        expanded = expandedProducto,
                        onExpandedChange = { expandedProducto = !expandedProducto }
                    ) {
                        OutlinedTextField(
                            value = productoSeleccionado?.nombre ?: "Seleccione un producto",
                            onValueChange = {},
                            readOnly = true,
                            trailingIcon = {
                                Icon(
                                    imageVector = Icons.Default.ArrowDropDown,
                                    contentDescription = null
                                )
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .menuAnchor()
                        )
                        
                        ExposedDropdownMenu(
                            expanded = expandedProducto,
                            onDismissRequest = { expandedProducto = false }
                        ) {
                            productos.forEach { producto ->
                                DropdownMenuItem(
                                    text = {
                                        Column {
                                            Text(producto.nombre)
                                            Text(
                                                text = "$${"%.2f".format(producto.precio)}",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                        }
                                    },
                                    onClick = {
                                        onProductoSeleccionado(producto)
                                        expandedProducto = false
                                    }
                                )
                            }
                        }
                    }
                    
                    // Cantidad
                    OutlinedTextField(
                        value = cantidad,
                        onValueChange = onCantidadChange,
                        label = { Text("Cantidad") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth()
                    )
                    
                    // Botón Agregar
                    Button(
                        onClick = onAgregarProducto,
                        modifier = Modifier.fillMaxWidth(),
                        enabled = productoSeleccionado != null && cantidad.isNotBlank()
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Agregar Producto")
                    }
                }
            }
        }
        
        // Lista de productos agregados
        if (detalles.isNotEmpty()) {
            item {
                Text(
                    text = "Productos (${detalles.size})",
                    style = MaterialTheme.typography.titleMedium
                )
            }
            
            items(detalles) { detalle ->
                DetalleVentaItem(
                    detalle = detalle,
                    nombreProducto = productos.find { it.id == detalle.productoId || it.codigo == detalle.productoId }?.nombre,
                    onEliminar = { onEliminarDetalle(detalle) }
                )
            }
            
            // Total
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
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
                            text = "TOTAL:",
                            style = MaterialTheme.typography.titleLarge
                        )
                        Text(
                            text = "$${"%.2f".format(total)}",
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }
        
        // Botones de acción
        item {
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = onCancelar,
                    modifier = Modifier.weight(1f),
                    enabled = !guardando
                ) {
                    Text("Cancelar")
                }
                
                Button(
                    onClick = onGuardar,
                    modifier = Modifier.weight(1f),
                    enabled = !guardando && clienteSeleccionado != null && detalles.isNotEmpty()
                ) {
                    if (guardando) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                    Text(if (guardando) "Guardando..." else tituloBoton)
                }
            }
        }
    }
}
