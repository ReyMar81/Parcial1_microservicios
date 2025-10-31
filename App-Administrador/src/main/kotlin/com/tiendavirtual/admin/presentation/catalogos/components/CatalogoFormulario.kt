package com.tiendavirtual.admin.presentation.catalogos.components

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.tiendavirtual.admin.domain.model.Catalogo

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CatalogoFormulario(
    catalogoEditando: Catalogo?,
    onGuardar: (Catalogo) -> Unit,
    onCancelar: () -> Unit,
    isLoading: Boolean,
    modifier: Modifier = Modifier
) {
    var nombre by remember(catalogoEditando) { mutableStateOf(catalogoEditando?.nombre ?: "") }
    var descripcion by remember(catalogoEditando) { mutableStateOf(catalogoEditando?.descripcion ?: "") }

    val valido = nombre.isNotBlank() && descripcion.isNotBlank()

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Row(
            Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onCancelar) {
                Icon(
                    Icons.Default.ArrowBack,
                    contentDescription = "Volver",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
            Column(Modifier.weight(1f)) {
                Text(
                    if (catalogoEditando == null) "Nuevo Catálogo" else "Editar Catálogo",
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    if (catalogoEditando == null) "Crear nuevo catálogo" else "Actualizar catálogo",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Spacer(Modifier.height(24.dp))

        Card(
            Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            )
        ) {
            Column(
                Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                OutlinedTextField(
                    value = nombre,
                    onValueChange = { nombre = it },
                    label = { Text("Nombre") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                OutlinedTextField(
                    value = descripcion,
                    onValueChange = { descripcion = it },
                    label = { Text("Descripción") },
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 3
                )
            }
        }

        Spacer(Modifier.weight(1f))

        Row(
            Modifier.fillMaxWidth(),
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
                            Catalogo(
                                id = catalogoEditando?.id,
                                nombre = nombre,
                                descripcion = descripcion
                            )
                        )
                    }
                },
                modifier = Modifier.weight(1f),
                enabled = valido && !isLoading
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        Modifier.size(20.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                } else {
                    Text(if (catalogoEditando == null) "Crear" else "Actualizar")
                }
            }
        }
    }
}
