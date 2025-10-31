package com.tiendavirtual.admin.presentation.categorias.components

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.tiendavirtual.admin.domain.model.Categoria

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoriaFormulario(
    categoriaEditando: Categoria?,
    onGuardar: (Categoria) -> Unit,
    onCancelar: () -> Unit,
    isLoading: Boolean,
    modifier: Modifier = Modifier
) {
    var nombre by remember(categoriaEditando) { mutableStateOf(categoriaEditando?.nombre ?: "") }
    val valido = nombre.isNotBlank()

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onCancelar) {
                Icon(
                    Icons.Default.ArrowBack,
                    contentDescription = "Volver",
                    tint = MaterialTheme.colorScheme.primary
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = if (categoriaEditando == null) "Nueva Categoría" else "Editar Categoría",
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = if (categoriaEditando == null) "Crear nueva categoría" else "Actualizar categoría",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Formulario
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            )
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                OutlinedTextField(
                    value = nombre,
                    onValueChange = { nombre = it },
                    label = { Text("Nombre de la categoría") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        // Botones
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            OutlinedButton(
                onClick = onCancelar,
                modifier = Modifier.weight(1f),
                enabled = !isLoading
            ) {
                Text("Cancelar")
            }

            Button(
                onClick = {
                    if (valido) {
                        onGuardar(
                            Categoria(
                                id = categoriaEditando?.id,
                                nombre = nombre
                            )
                        )
                    }
                },
                modifier = Modifier.weight(1f),
                enabled = valido && !isLoading
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                } else {
                    Text(if (categoriaEditando == null) "Crear" else "Actualizar")
                }
            }
        }
    }
}
