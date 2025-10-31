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
import com.tiendavirtual.admin.presentation.clientes.ClientesScreen
import com.tiendavirtual.admin.presentation.categorias.CategoriasScreen
import com.tiendavirtual.admin.presentation.productos.ProductosScreen
import com.tiendavirtual.admin.presentation.catalogos.CatalogosScreen
import com.tiendavirtual.admin.presentation.ventas.VentasScreen

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
            ProductosScreen()
        }

        // Categorías
        composable("categorias") {
            CategoriasScreen()
        }

        // Catálogos
        composable("catalogos") {
            CatalogosScreen()
        }

        // Ventas
        composable("ventas") {
            VentasScreen()
        }
    }
}
