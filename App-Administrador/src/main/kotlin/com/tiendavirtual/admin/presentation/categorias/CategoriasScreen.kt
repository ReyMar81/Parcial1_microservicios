package com.tiendavirtual.admin.presentation.categorias

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Category
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
import com.tiendavirtual.admin.presentation.categorias.components.CategoriaCard
import com.tiendavirtual.admin.presentation.categorias.components.CategoriaFormulario

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoriasScreen(viewModel: CategoriasViewModel = viewModel()) {
    val uiState by viewModel.uiState.collectAsState()

    if (uiState.mostrarFormulario) {
        CategoriaFormulario(
            categoriaEditando = uiState.categoriaEditando,
            onGuardar = { viewModel.guardarCategoria(it) },
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
                text = "Gestión de Categorías",
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )

            Text(
                text = "Administrar categorías",
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
                            text = "Total Categorías",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                        Text(
                            text = "${uiState.categorias.size} registradas",
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
                            contentDescription = "Nueva Categoría",
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Nueva")
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Lista de categorías
            when {
                uiState.isLoading && uiState.categorias.isEmpty() -> {
                    LoadingCard("Cargando categorías...")
                }
                uiState.categorias.isEmpty() -> {
                    EmptyCard(
                        titulo = "No hay categorías",
                        subtitulo = "Pulsa 'Nueva' para crear la primera",
                        icono = Icons.Default.Category
                    )
                }
                else -> {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(uiState.categorias, key = { it.id ?: 0 }) { categoria ->
                            CategoriaCard(
                                categoria = categoria,
                                onEditar = { viewModel.mostrarFormularioEditar(categoria) },
                                onEliminar = { viewModel.eliminarCategoria(categoria) }
                            )
                        }
                    }
                }
            }

            if (uiState.isLoading && uiState.categorias.isNotEmpty()) {
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
fun CategoriasModerno() {
    CategoriasScreen()
}
