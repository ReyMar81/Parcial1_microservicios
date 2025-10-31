package com.tiendavirtual.admin.presentation.ventas.state

import com.tiendavirtual.admin.domain.model.Cliente
import com.tiendavirtual.admin.domain.model.DetalleVenta
import com.tiendavirtual.admin.domain.model.Producto
import com.tiendavirtual.admin.domain.model.Venta

data class VentasUiState(
    val isLoading: Boolean = false,
    val ventas: List<Venta> = emptyList(),
    val clientes: List<Cliente> = emptyList(),
    val productos: List<Producto> = emptyList(),
    val ventaSeleccionada: Venta? = null,
    val mostrarDetalleVenta: Boolean = false,
    val mostrarCrearVenta: Boolean = false,
    val mostrarEditarVenta: Boolean = false,
    val error: String? = null,
    val mensaje: String? = null,
    
    // Estado del formulario de crear/editar venta
    val clienteSeleccionado: Cliente? = null,
    val detallesVenta: List<DetalleVenta> = emptyList(),
    val productoSeleccionado: Producto? = null,
    val cantidadProducto: String = "",
    
    val procesandoAccion: Boolean = false
) {
    val totalVenta: Double
        get() = detallesVenta.sumOf { it.subtotal }
}
