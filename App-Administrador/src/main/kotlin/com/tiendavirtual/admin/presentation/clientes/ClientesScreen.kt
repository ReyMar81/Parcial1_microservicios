package com.tiendavirtual.admin.presentation.clientes

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Person
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
import com.tiendavirtual.admin.presentation.clientes.components.ClienteCard
import com.tiendavirtual.admin.presentation.clientes.components.ClienteFormulario

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ClientesScreen(viewModel: ClientesViewModel = viewModel()) {
    val uiState by viewModel.uiState.collectAsState()

    if (uiState.mostrarFormulario) {
        ClienteFormulario(
            clienteEditando = uiState.clienteEditando,
            onGuardar = { viewModel.guardarCliente(it) },
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
                            text = "Total de Clientes",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                        Text(
                            text = "${uiState.clientes.size} registrados",
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
            when {
                uiState.isLoading && uiState.clientes.isEmpty() -> {
                    LoadingCard("Cargando clientes...")
                }
                uiState.clientes.isEmpty() -> {
                    EmptyCard(
                        titulo = "No hay clientes registrados",
                        subtitulo = "Presiona 'Nuevo Cliente' para agregar el primero",
                        icono = Icons.Default.Person
                    )
                }
                else -> {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(uiState.clientes, key = { it.id ?: 0 }) { cliente ->
                            ClienteCard(
                                cliente = cliente,
                                onEditar = { viewModel.mostrarFormularioEditar(cliente) },
                                onEliminar = { viewModel.eliminarCliente(cliente) }
                            )
                        }
                    }
                }
            }

            // Loading overlay cuando está actualizando
            if (uiState.isLoading && uiState.clientes.isNotEmpty()) {
                Spacer(modifier = Modifier.height(16.dp))
                LinearProgressIndicator(
                    modifier = Modifier.fillMaxWidth(),
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}
