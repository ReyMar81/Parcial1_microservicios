package com.tiendavirtual.admin.presentation.productos.components

import android.graphics.BitmapFactory
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Image
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.tiendavirtual.admin.core.util.ImageUtil
import com.tiendavirtual.admin.domain.model.Categoria
import com.tiendavirtual.admin.domain.model.Producto

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProductoFormulario(
    productoEditando: Producto?,
    categorias: List<Categoria>,
    isLoadingCategorias: Boolean,
    onGuardar: (Producto) -> Unit,
    onCancelar: () -> Unit,
    isLoading: Boolean,
    modifier: Modifier = Modifier
) {
    var nombre by remember(productoEditando) { mutableStateOf(productoEditando?.nombre ?: "") }
    var descripcion by remember(productoEditando) { mutableStateOf(productoEditando?.descripcion ?: "") }
    var imagen by remember(productoEditando) { mutableStateOf(productoEditando?.imagen ?: "") }
    var precioTxt by remember(productoEditando) { mutableStateOf(productoEditando?.precio?.toString() ?: "") }
    var stockTxt by remember(productoEditando) { mutableStateOf(productoEditando?.stock?.toString() ?: "") }

    var categoriaSeleccionada by remember(productoEditando) {
        mutableStateOf(
            if (productoEditando != null)
                categorias.find { it.id == productoEditando.categoriaId }
            else null
        )
    }
    var expanded by remember { mutableStateOf(false) }

    LaunchedEffect(categorias, productoEditando) {
        if (productoEditando != null && categorias.isNotEmpty()) {
            categoriaSeleccionada = categorias.find { it.id == productoEditando.categoriaId }
        }
    }

    val precioVal = precioTxt.toDoubleOrNull()
    val stockVal = stockTxt.toIntOrNull()

    val valido = nombre.isNotBlank() && descripcion.isNotBlank() && 
                precioVal != null && precioVal >= 0 &&
                stockVal != null && stockVal >= 0 && 
                categoriaSeleccionada != null && categoriaSeleccionada!!.id != null && 
                imagen.isNotBlank() && ImageUtil.esImagenBase64(imagen)

    Column(modifier = modifier.fillMaxSize()) {
        // Header
        Row(
            Modifier
                .fillMaxWidth()
                .padding(16.dp),
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
                    if (productoEditando == null) "Nuevo Producto" else "Editar Producto",
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    if (productoEditando == null) "Crear nuevo producto" else "Actualizar producto",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // Formulario scrolleable
        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
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

                        // Sección de imagen
                        Text(
                            "Imagen del Producto",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )

                        val context = LocalContext.current
                        val launcher = rememberLauncherForActivityResult(
                            ActivityResultContracts.GetContent()
                        ) { uri: Uri? ->
                            if (uri != null) {
                                try {
                                    val inputStream = context.contentResolver.openInputStream(uri)
                                    val bytes = inputStream?.readBytes()
                                    if (bytes != null) {
                                        if (bytes.size > 400_000) {
                                            imagen = ""
                                        } else {
                                            val mime = ImageUtil.detectarMimeType(bytes)
                                            imagen = ImageUtil.codificarABase64(bytes, mime)
                                        }
                                    }
                                } catch (_: Exception) {
                                    imagen = ""
                                }
                            }
                        }

                        if (ImageUtil.esImagenBase64(imagen)) {
                            val imageBitmap = remember(imagen) {
                                ImageUtil.decodificarBase64(imagen)?.let { bytes ->
                                    BitmapFactory.decodeByteArray(bytes, 0, bytes.size)?.asImageBitmap()
                                }
                            }

                            if (imageBitmap != null) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Image(
                                        bitmap = imageBitmap,
                                        contentDescription = null,
                                        modifier = Modifier
                                            .size(80.dp)
                                            .clip(RoundedCornerShape(8.dp)),
                                        contentScale = ContentScale.Crop
                                    )
                                    Spacer(Modifier.width(12.dp))
                                    Column {
                                        val sizeKB = ImageUtil.calcularTamanoKB(imagen)
                                        Text(
                                            "Imagen cargada",
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            "$sizeKB KB",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        if (sizeKB > 390) {
                                            Text(
                                                "Casi excede el límite (400KB)",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.error
                                            )
                                        }
                                        Spacer(Modifier.height(4.dp))
                                        TextButton(onClick = { launcher.launch("image/*") }) {
                                            Text("Cambiar")
                                        }
                                    }
                                }
                            } else {
                                Text("Error en la imagen", color = MaterialTheme.colorScheme.error)
                            }
                        } else {
                            Card(
                                onClick = { launcher.launch("image/*") },
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surface
                                ),
                                elevation = CardDefaults.cardElevation(2.dp)
                            ) {
                                Column(
                                    Modifier.padding(24.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Icon(
                                        Icons.Default.Image,
                                        contentDescription = null,
                                        modifier = Modifier.size(48.dp),
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                    Spacer(Modifier.height(8.dp))
                                    Text(
                                        "Tocar para seleccionar imagen",
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                    Text(
                                        "JPEG/PNG < 400KB",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }

                        if (!ImageUtil.esImagenBase64(imagen) && imagen.isBlank()) {
                            Text(
                                "Debes seleccionar una imagen válida (JPEG/PNG < 400KB)",
                                color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }

                        OutlinedTextField(
                            value = precioTxt,
                            onValueChange = { precioTxt = it },
                            label = { Text("Precio") },
                            modifier = Modifier.fillMaxWidth(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = stockTxt,
                            onValueChange = { stockTxt = it },
                            label = { Text("Stock") },
                            modifier = Modifier.fillMaxWidth(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true
                        )

                        // Dropdown de categorías
                        ExposedDropdownMenuBox(
                            expanded = expanded,
                            onExpandedChange = { expanded = !expanded && !isLoadingCategorias },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            OutlinedTextField(
                                value = categoriaSeleccionada?.nombre ?: 
                                       if (isLoadingCategorias) "Cargando..." else "Seleccionar categoría",
                                onValueChange = { },
                                readOnly = true,
                                label = { Text("Categoría") },
                                trailingIcon = {
                                    ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
                                },
                                enabled = !isLoadingCategorias,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .menuAnchor()
                            )

                            ExposedDropdownMenu(
                                expanded = expanded,
                                onDismissRequest = { expanded = false },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                if (categorias.isEmpty() && !isLoadingCategorias) {
                                    DropdownMenuItem(
                                        text = { Text("No hay categorías disponibles") },
                                        onClick = { expanded = false },
                                        enabled = false
                                    )
                                } else {
                                    categorias.forEach { categoria ->
                                        DropdownMenuItem(
                                            text = { Text(categoria.nombre) },
                                            onClick = {
                                                categoriaSeleccionada = categoria
                                                expanded = false
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Botones
        Card(
            Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            elevation = CardDefaults.cardElevation(8.dp)
        ) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
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
                        if (valido && categoriaSeleccionada != null) {
                            onGuardar(
                                Producto(
                                    codigo = productoEditando?.codigo ?: 0,
                                    nombre = nombre,
                                    descripcion = descripcion,
                                    imagen = imagen,
                                    precio = precioVal!!,
                                    stock = stockVal!!,
                                    categoriaId = categoriaSeleccionada!!.id!!
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
                        Text(
                            when {
                                !ImageUtil.esImagenBase64(imagen) -> "Selecciona imagen"
                                productoEditando == null -> "Crear"
                                else -> "Actualizar"
                            }
                        )
                    }
                }
            }
        }
    }
}
