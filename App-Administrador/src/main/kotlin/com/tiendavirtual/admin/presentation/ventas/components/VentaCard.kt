package com.tiendavirtual.admin.presentation.ventas.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.tiendavirtual.admin.domain.model.Venta

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VentaCard(
    venta: Venta,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        onClick = onClick,
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Venta #${venta.id}",
                    style = MaterialTheme.typography.titleMedium
                )
                EstadoChip(estado = venta.estado)
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = "Cliente ID: ${venta.clienteId}",
                style = MaterialTheme.typography.bodyMedium
            )
            
            Text(
                text = "Fecha: ${venta.fecha}",
                style = MaterialTheme.typography.bodyMedium
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = "Total: $${"%.2f".format(venta.total)}",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary
            )
            
            if (venta.detalles.isNotEmpty()) {
                Text(
                    text = "${venta.detalles.size} producto(s)",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun EstadoChip(estado: String) {
    val color = when (estado) {
        "CREADA" -> MaterialTheme.colorScheme.secondary
        "CONFIRMADA" -> MaterialTheme.colorScheme.primary
        "ANULADA" -> MaterialTheme.colorScheme.error
        else -> MaterialTheme.colorScheme.surfaceVariant
    }
    
    val emoji = when (estado) {
        "CREADA" -> "📝"
        "CONFIRMADA" -> "✅"
        "ANULADA" -> "❌"
        else -> ""
    }
    
    Surface(
        color = color.copy(alpha = 0.2f),
        shape = MaterialTheme.shapes.small
    ) {
        Text(
            text = "$emoji $estado",
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            style = MaterialTheme.typography.labelSmall,
            color = color
        )
    }
}
