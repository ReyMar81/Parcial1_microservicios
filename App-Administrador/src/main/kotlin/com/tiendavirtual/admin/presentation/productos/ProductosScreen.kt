package com.tiendavirtual.admin.presentation.productos

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.tiendavirtual.admin.core.ui.components.EmptyCard
import com.tiendavirtual.admin.core.ui.components.LoadingCard
import com.tiendavirtual.admin.presentation.productos.components.ProductoCard
import com.tiendavirtual.admin.presentation.productos.components.ProductoFormulario

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProductosScreen(viewModel: ProductosViewModel = viewModel()) {
    val uiState by viewModel.uiState.collectAsState()

    if (uiState.mostrarFormulario) {
        ProductoFormulario(
            productoEditando = uiState.productoEditando,
            categorias = uiState.categorias,
            isLoadingCategorias = uiState.isLoadingCategorias,
            onGuardar = { viewModel.guardarProducto(it) },
            onCancelar = { viewModel.cerrarFormulario() },
            isLoading = uiState.isLoading
        )
    } else {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            // Header
            Text(
                text = "Gestión de Productos",
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )

            Text(
                text = "Administrar catálogo de productos",
                style = MaterialTheme.typography.titleMedium,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Botón de acción principal
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
                            text = "Total Productos",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                        Text(
                            text = "${uiState.productos.size} registrados",
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
                            contentDescription = "Nuevo Producto",
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Nuevo")
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Lista de productos
            when {
                uiState.isLoading && uiState.productos.isEmpty() -> {
                    LoadingCard("Cargando productos...")
                }
                uiState.productos.isEmpty() -> {
                    EmptyCard(
                        titulo = "No hay productos",
                        subtitulo = "Pulsa 'Nuevo' para crear el primero",
                        icono = Icons.Default.Inventory2
                    )
                }
                else -> {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(uiState.productos, key = { it.codigo }) { producto ->
                            ProductoCard(
                                producto = producto,
                                categorias = uiState.categorias,
                                onEditar = { viewModel.mostrarFormularioEditar(producto) },
                                onEliminar = { viewModel.eliminarProducto(producto) }
                            )
                        }
                    }
                }
            }

            if (uiState.isLoading && uiState.productos.isNotEmpty()) {
                Spacer(modifier = Modifier.height(16.dp))
                LinearProgressIndicator(
                    modifier = Modifier.fillMaxWidth(),
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

// Función de compatibilidad con la navegación actual
@Composable
fun ProductosModerno() {
    ProductosScreen()
}
