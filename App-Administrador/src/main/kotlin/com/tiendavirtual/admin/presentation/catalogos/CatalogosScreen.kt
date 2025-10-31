package com.tiendavirtual.admin.presentation.catalogos

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Book
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
import com.tiendavirtual.admin.presentation.catalogos.components.CatalogoCard
import com.tiendavirtual.admin.presentation.catalogos.components.CatalogoFormulario

@Composable
fun CatalogosScreen(viewModel: CatalogosViewModel = viewModel()) {
    val uiState by viewModel.uiState.collectAsState()

    if (uiState.mostrarFormulario) {
        CatalogoFormulario(
            catalogoEditando = uiState.catalogoEditando,
            onGuardar = { viewModel.guardarCatalogo(it) },
            onCancelar = { viewModel.cerrarFormulario() },
            isLoading = uiState.isLoading
        )
    } else {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            Text(
                text = "Gestión de Catálogos",
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )

            Text(
                text = "Administrar catálogos de productos",
                style = MaterialTheme.typography.titleMedium,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(24.dp))

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
                            text = "Total Catálogos",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                        Text(
                            text = "${uiState.catalogos.size} registrados",
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
                            contentDescription = "Nuevo Catálogo",
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Nuevo")
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            when {
                uiState.isLoading && uiState.catalogos.isEmpty() -> {
                    LoadingCard("Cargando catálogos...")
                }
                uiState.catalogos.isEmpty() -> {
                    EmptyCard(
                        titulo = "No hay catálogos",
                        subtitulo = "Pulsa 'Nuevo' para crear el primero",
                        icono = Icons.Default.Book
                    )
                }
                else -> {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(uiState.catalogos, key = { it.id ?: 0 }) { catalogo ->
                            CatalogoCard(
                                catalogo = catalogo,
                                onEditar = { viewModel.mostrarFormularioEditar(catalogo) },
                                onEliminar = { viewModel.eliminarCatalogo(catalogo) },
                                onGestionarProductos = { viewModel.mostrarGestionProductos(catalogo) }
                            )
                        }
                    }
                }
            }

            if (uiState.isLoading && uiState.catalogos.isNotEmpty()) {
                Spacer(modifier = Modifier.height(16.dp))
                LinearProgressIndicator(
                    modifier = Modifier.fillMaxWidth(),
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

// Compatibilidad
@Composable
fun CatalogosModerno() { CatalogosScreen() }
