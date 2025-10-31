package com.tiendavirtual.admin.presentation.clientes.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.tiendavirtual.admin.domain.model.Cliente

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ClienteFormulario(
    clienteEditando: Cliente?,
    onGuardar: (Cliente) -> Unit,
    onCancelar: () -> Unit,
    isLoading: Boolean,
    modifier: Modifier = Modifier
) {
    var nombres by remember(clienteEditando) { mutableStateOf(clienteEditando?.nombres ?: "") }
    var docIdentidad by remember(clienteEditando) { mutableStateOf(clienteEditando?.docIdentidad ?: "") }
    var whatsapp by remember(clienteEditando) { mutableStateOf(clienteEditando?.whatsapp ?: "") }
    var direccion by remember(clienteEditando) { mutableStateOf(clienteEditando?.direccion ?: "") }

    fun esValido() = nombres.isNotBlank() && docIdentidad.isNotBlank() &&
                    whatsapp.isNotBlank() && direccion.isNotBlank()

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
                    text = if (clienteEditando == null) "Nuevo Cliente" else "Editar Cliente",
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = if (clienteEditando == null) "Agregar cliente al sistema" else "Actualizar información",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Formulario - Información Personal
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
                Text(
                    text = "Información Personal",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                OutlinedTextField(
                    value = nombres,
                    onValueChange = { nombres = it },
                    label = { Text("Nombres completos") },
                    leadingIcon = { Icon(Icons.Default.Person, null) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                OutlinedTextField(
                    value = docIdentidad,
                    onValueChange = { docIdentidad = it },
                    label = { Text("Documento de identidad") },
                    leadingIcon = { Icon(Icons.Default.Info, null) },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Formulario - Información de Contacto
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
                Text(
                    text = "Información de Contacto",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                OutlinedTextField(
                    value = whatsapp,
                    onValueChange = { whatsapp = it },
                    label = { Text("WhatsApp") },
                    leadingIcon = { Icon(Icons.Default.Phone, null) },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    singleLine = true
                )

                OutlinedTextField(
                    value = direccion,
                    onValueChange = { direccion = it },
                    label = { Text("Dirección") },
                    leadingIcon = { Icon(Icons.Default.LocationOn, null) },
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 2
                )
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        // Botones de acción
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
                    if (esValido()) {
                        onGuardar(
                            Cliente(
                                id = clienteEditando?.id,
                                nombres = nombres,
                                docIdentidad = docIdentidad,
                                whatsapp = whatsapp,
                                direccion = direccion
                            )
                        )
                    }
                },
                modifier = Modifier.weight(1f),
                enabled = !isLoading && esValido()
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                } else {
                    Text(if (clienteEditando == null) "Crear Cliente" else "Actualizar")
                }
            }
        }
    }
}
