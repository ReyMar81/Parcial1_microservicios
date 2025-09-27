package com.tiendavirtual.admin.ui.main

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.clickable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

/**
 * Pantalla principal de la aplicación
 * Muestra el menú de todos los módulos de microservicios
 */
@Composable
fun MainScreen(
    onNavigateToClientes: () -> Unit = {},
    onNavigateToProductos: () -> Unit = {},
    onNavigateToCategorias: () -> Unit = {},
    onNavigateToCatalogos: () -> Unit = {},
    onNavigateToVentas: () -> Unit = {}
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Header
        Text(
            text = "Tienda Virtual",
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )

        Text(
            text = "Panel de Administración",
            style = MaterialTheme.typography.titleMedium,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(32.dp))

        // Grid de módulos
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            // Módulo de Clientes
            item {
                ModuleCard(
                    title = "Clientes",
                    description = "Gestionar clientes registrados",
                    icon = Icons.Default.Person,
                    isEnabled = true,
                    onClick = onNavigateToClientes
                )
            }

            // Módulo de Productos
            item {
                ModuleCard(
                    title = "Productos",
                    description = "Catálogo de productos",
                    icon = Icons.Default.ShoppingCart,
                    isEnabled = true,
                    onClick = onNavigateToProductos
                )
            }

            // Módulo de Categorías
            item {
                ModuleCard(
                    title = "Categorías",
                    description = "Gestión de categorías",
                    icon = Icons.Default.Settings,
                    isEnabled = true,
                    onClick = onNavigateToCategorias
                )
            }

            // Módulo de Catálogos
            item {
                ModuleCard(
                    title = "Catálogos",
                    description = "Gestión de catálogos",
                    icon = Icons.Default.Settings,
                    isEnabled = true,
                    onClick = onNavigateToCatalogos
                )
            }

            // Módulo de Ventas
            item {
                ModuleCard(
                    title = "Ventas",
                    description = "Gestión de ventas y facturas",
                    icon = Icons.Default.Person,
                    isEnabled = false,
                    onClick = onNavigateToVentas
                )
            }

            // Configuración
            item {
                ModuleCard(
                    title = "Configuración",
                    description = "Ajustes del sistema",
                    icon = Icons.Default.Settings,
                    isEnabled = false,
                    onClick = { /* TODO */ }
                )
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        // Información del sistema
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            )
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = "Estado del Sistema",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        StatusIndicator(
                            label = "API Gateway",
                            isOnline = true
                        )
                        StatusIndicator(
                            label = "MS Clientes",
                            isOnline = true
                        )
                    }

                    Column {
                        StatusIndicator(
                            label = "MS Productos",
                            isOnline = false
                        )
                        StatusIndicator(
                            label = "MS Ventas",
                            isOnline = false
                        )
                    }
                }
            }
        }
    }
}

/**
 * Card de módulo individual
 */
@Composable
fun ModuleCard(
    title: String,
    description: String,
    icon: ImageVector,
    isEnabled: Boolean,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(140.dp)
            .clickable(enabled = isEnabled) {
                if (isEnabled) onClick()
            },
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (isEnabled) 4.dp else 2.dp
        ),
        colors = CardDefaults.cardColors(
            containerColor = if (isEnabled)
                MaterialTheme.colorScheme.surface
            else
                MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                modifier = Modifier.size(48.dp),
                tint = if (isEnabled)
                    MaterialTheme.colorScheme.primary
                else
                    MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                color = if (isEnabled)
                    MaterialTheme.colorScheme.onSurface
                else
                    MaterialTheme.colorScheme.onSurfaceVariant
            )

            Text(
                text = if (isEnabled) description else "Próximamente",
                style = MaterialTheme.typography.bodySmall,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/**
 * Indicador de estado de servicios
 */
@Composable
fun StatusIndicator(
    label: String,
    isOnline: Boolean
) {
    Row(
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .padding(end = 4.dp)
        ) {
            Card(
                modifier = Modifier.fillMaxSize(),
                colors = CardDefaults.cardColors(
                    containerColor = if (isOnline)
                        MaterialTheme.colorScheme.primary
                    else
                        MaterialTheme.colorScheme.error
                )
            ) {}
        }

        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
