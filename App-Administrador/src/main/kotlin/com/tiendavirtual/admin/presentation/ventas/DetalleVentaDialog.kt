package com.tiendavirtual.admin.presentation.ventas

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.tiendavirtual.admin.domain.model.Producto
import com.tiendavirtual.admin.domain.model.Venta
import com.tiendavirtual.admin.presentation.ventas.components.DetalleVentaItem
import com.tiendavirtual.admin.presentation.ventas.components.EstadoChip

@Composable
fun DetalleVentaDialog(
    venta: Venta,
    productos: List<Producto>,
    onDismiss: () -> Unit,
    onEditar: () -> Unit,
    onConfirmar: (Int) -> Unit,
    onAnular: (Int) -> Unit,
    onEliminar: (Int) -> Unit,
    procesando: Boolean
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.9f)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Venta #${venta.id}",
                        style = MaterialTheme.typography.headlineSmall
                    )
                    EstadoChip(estado = venta.estado)
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Información general
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
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        InfoRow("Cliente ID:", venta.clienteId.toString())
                        InfoRow("Fecha:", venta.fecha)
                        InfoRow("Total:", "$${"%.2f".format(venta.total)}")
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Productos
                Text(
                    text = "Productos (${venta.detalles.size})",
                    style = MaterialTheme.typography.titleMedium
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(venta.detalles) { detalle ->
                        DetalleVentaItem(
                            detalle = detalle,
                            nombreProducto = productos.find { it.id == detalle.productoId || it.codigo == detalle.productoId }?.nombre
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Botones de acción según estado
                when (venta.estado) {
                    "CREADA" -> {
                        // Botones para venta creada
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = onEditar,
                                modifier = Modifier.weight(1f),
                                enabled = !procesando
                            ) {
                                Text("Editar")
                            }
                            Button(
                                onClick = { onConfirmar(venta.id!!) },
                                modifier = Modifier.weight(1f),
                                enabled = !procesando
                            ) {
                                Text(if (procesando) "Confirmando..." else "Confirmar")
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedButton(
                                onClick = { onAnular(venta.id!!) },
                                modifier = Modifier.weight(1f),
                                enabled = !procesando,
                                colors = ButtonDefaults.outlinedButtonColors(
                                    contentColor = MaterialTheme.colorScheme.error
                                )
                            ) {
                                Text(if (procesando) "Anulando..." else "Anular")
                            }
                            OutlinedButton(
                                onClick = { onEliminar(venta.id!!) },
                                modifier = Modifier.weight(1f),
                                enabled = !procesando,
                                colors = ButtonDefaults.outlinedButtonColors(
                                    contentColor = MaterialTheme.colorScheme.error
                                )
                            ) {
                                Text(if (procesando) "Eliminando..." else "Eliminar")
                            }
                        }
                    }
                    "CONFIRMADA" -> {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer
                            )
                        ) {
                            Text(
                                text = "✅ Venta confirmada - Solo lectura",
                                modifier = Modifier.padding(16.dp),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                    "ANULADA" -> {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.errorContainer
                            )
                        ) {
                            Text(
                                text = "❌ Venta anulada - Solo lectura",
                                modifier = Modifier.padding(16.dp),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                
                // Botón cerrar
                OutlinedButton(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !procesando
                ) {
                    Text("Cerrar")
                }
            }
        }
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium
        )
    }
}
