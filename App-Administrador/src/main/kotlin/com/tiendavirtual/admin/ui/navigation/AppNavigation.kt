package com.tiendavirtual.admin.ui.navigation

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.tiendavirtual.admin.ui.main.MainScreen
import com.tiendavirtual.admin.clientes.ClientesScreen
import com.tiendavirtual.admin.productos.CategoriasModerno
import com.tiendavirtual.admin.productos.ProductosModerno
import com.tiendavirtual.admin.productos.CatalogosScreen

/**
 * Navegación
 */
@Composable
fun AppNavigation() {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = "main"
    ) {
        // Pantalla principal
        composable("main") {
            MainScreen(
                onNavigateToClientes = { navController.navigate("clientes") },
                onNavigateToProductos = { navController.navigate("productos") },
                onNavigateToCategorias = { navController.navigate("categorias") },
                onNavigateToCatalogos = { navController.navigate("catalogos") },
                onNavigateToVentas = { navController.navigate("ventas") }
            )
        }

        // Clientes
        composable("clientes") {
            ClientesScreen()
        }

        // Productos
        composable("productos") {
            ProductosModerno()
        }

        // Categorías
        composable("categorias") {
            CategoriasModerno()
        }

        // Catálogos
        composable("catalogos") {
            CatalogosScreen()
        }

        // Placeholder para ventas
        composable("ventas") {
            PlaceholderScreen("Ventas", "Módulo en desarrollo") {
                navController.popBackStack()
            }
        }
    }
}

@Composable
fun PlaceholderScreen(
    title: String,
    message: String,
    onBack: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.Settings,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = title,
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )

        Text(
            text = message,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(24.dp))

        Button(onClick = onBack) {
            Text("Volver al Menú Principal")
        }
    }
}
