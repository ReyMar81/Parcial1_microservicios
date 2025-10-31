package com.tiendavirtual.admin.presentation.ventas

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tiendavirtual.admin.data.remote.api.ClienteApi
import com.tiendavirtual.admin.data.remote.api.ProductoApi
import com.tiendavirtual.admin.data.repository.VentaRepositoryImpl
import com.tiendavirtual.admin.domain.model.Cliente
import com.tiendavirtual.admin.domain.model.DetalleVenta
import com.tiendavirtual.admin.domain.model.Producto
import com.tiendavirtual.admin.domain.model.Venta
import com.tiendavirtual.admin.domain.repository.VentaRepository
import com.tiendavirtual.admin.presentation.ventas.state.VentasUiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class VentasViewModel(
    private val repository: VentaRepository = VentaRepositoryImpl(),
    private val clienteApi: ClienteApi = ClienteApi(),
    private val productoApi: ProductoApi = ProductoApi()
) : ViewModel() {

    private val _uiState = MutableStateFlow(VentasUiState())
    val uiState: StateFlow<VentasUiState> = _uiState.asStateFlow()

    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

    init {
        cargarDatos()
    }

    fun cargarDatos() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            try {
                val ventas = repository.obtenerVentas()
                val clientes = clienteApi.obtenerClientes()
                val productos = productoApi.obtenerProductos()
                
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        ventas = ventas,
                        clientes = clientes,
                        productos = productos
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = "Error al cargar datos: ${e.message}"
                    )
                }
            }
        }
    }

    fun seleccionarVenta(venta: Venta) {
        _uiState.update {
            it.copy(
                ventaSeleccionada = venta,
                mostrarDetalleVenta = true
            )
        }
    }

    fun cerrarDetalleVenta() {
        _uiState.update {
            it.copy(
                ventaSeleccionada = null,
                mostrarDetalleVenta = false,
                mostrarEditarVenta = false
            )
        }
    }

    fun mostrarCrearVenta() {
        _uiState.update {
            it.copy(
                mostrarCrearVenta = true,
                clienteSeleccionado = null,
                detallesVenta = emptyList(),
                productoSeleccionado = null,
                cantidadProducto = ""
            )
        }
    }

    fun cerrarCrearVenta() {
        _uiState.update {
            it.copy(
                mostrarCrearVenta = false,
                clienteSeleccionado = null,
                detallesVenta = emptyList(),
                productoSeleccionado = null,
                cantidadProducto = ""
            )
        }
    }

    fun mostrarEditarVenta() {
        val venta = _uiState.value.ventaSeleccionada
        if (venta != null && venta.estado == "CREADA") {
            _uiState.update {
                it.copy(
                    mostrarEditarVenta = true,
                    clienteSeleccionado = it.clientes.find { c -> c.id == venta.clienteId },
                    detallesVenta = venta.detalles
                )
            }
        }
    }

    fun cerrarEditarVenta() {
        _uiState.update {
            it.copy(
                mostrarEditarVenta = false,
                clienteSeleccionado = null,
                detallesVenta = emptyList()
            )
        }
    }

    fun seleccionarCliente(cliente: Cliente) {
        _uiState.update { it.copy(clienteSeleccionado = cliente) }
    }

    fun seleccionarProducto(producto: Producto) {
        _uiState.update { it.copy(productoSeleccionado = producto) }
    }

    fun actualizarCantidad(cantidad: String) {
        _uiState.update { it.copy(cantidadProducto = cantidad) }
    }

    fun agregarProductoADetalle() {
        val producto = _uiState.value.productoSeleccionado
        val cantidadStr = _uiState.value.cantidadProducto
        
        if (producto != null && cantidadStr.isNotBlank()) {
            val cantidad = cantidadStr.toIntOrNull()
            if (cantidad != null && cantidad > 0) {
                val detalle = DetalleVenta(
                    productoId = producto.id ?: producto.codigo,
                    cantidad = cantidad,
                    precioUnitario = producto.precio
                )
                _uiState.update {
                    it.copy(
                        detallesVenta = it.detallesVenta + detalle,
                        productoSeleccionado = null,
                        cantidadProducto = ""
                    )
                }
            }
        }
    }

    fun eliminarDetalleVenta(detalle: DetalleVenta) {
        _uiState.update {
            it.copy(detallesVenta = it.detallesVenta - detalle)
        }
    }

    fun crearVenta() {
        val cliente = _uiState.value.clienteSeleccionado
        val detalles = _uiState.value.detallesVenta
        
        if (cliente == null || detalles.isEmpty()) {
            _uiState.update { it.copy(mensaje = "Seleccione un cliente y agregue productos") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(procesandoAccion = true, mensaje = null) }
            try {
                val venta = Venta(
                    clienteId = cliente.id!!,
                    fecha = dateFormat.format(Date()),
                    estado = "CREADA",
                    total = _uiState.value.totalVenta,
                    detalles = detalles
                )
                
                val success = repository.crearVenta(venta)
                if (success) {
                    _uiState.update {
                        it.copy(
                            procesandoAccion = false,
                            mostrarCrearVenta = false,
                            mensaje = "Venta creada exitosamente",
                            clienteSeleccionado = null,
                            detallesVenta = emptyList()
                        )
                    }
                    cargarDatos()
                } else {
                    _uiState.update {
                        it.copy(
                            procesandoAccion = false,
                            mensaje = "Error al crear venta"
                        )
                    }
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        procesandoAccion = false,
                        mensaje = "Error: ${e.message}"
                    )
                }
            }
        }
    }

    fun actualizarVenta() {
        val venta = _uiState.value.ventaSeleccionada
        val cliente = _uiState.value.clienteSeleccionado
        val detalles = _uiState.value.detallesVenta
        
        if (venta == null || cliente == null || detalles.isEmpty()) {
            _uiState.update { it.copy(mensaje = "Datos incompletos") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(procesandoAccion = true, mensaje = null) }
            try {
                val ventaActualizada = venta.copy(
                    clienteId = cliente.id!!,
                    total = _uiState.value.totalVenta,
                    detalles = detalles
                )
                
                val success = repository.actualizarVenta(ventaActualizada)
                if (success) {
                    _uiState.update {
                        it.copy(
                            procesandoAccion = false,
                            mostrarEditarVenta = false,
                            mensaje = "Venta actualizada exitosamente"
                        )
                    }
                    cargarDatos()
                    cerrarDetalleVenta()
                } else {
                    _uiState.update {
                        it.copy(
                            procesandoAccion = false,
                            mensaje = "Error al actualizar venta"
                        )
                    }
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        procesandoAccion = false,
                        mensaje = "Error: ${e.message}"
                    )
                }
            }
        }
    }

    fun confirmarVenta(ventaId: Int) {
        viewModelScope.launch {
            _uiState.update { it.copy(procesandoAccion = true, mensaje = null) }
            try {
                val success = repository.confirmarVenta(ventaId)
                if (success) {
                    _uiState.update {
                        it.copy(
                            procesandoAccion = false,
                            mensaje = "Venta confirmada exitosamente"
                        )
                    }
                    cargarDatos()
                    cerrarDetalleVenta()
                } else {
                    _uiState.update {
                        it.copy(
                            procesandoAccion = false,
                            mensaje = "Error al confirmar venta"
                        )
                    }
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        procesandoAccion = false,
                        mensaje = "Error: ${e.message}"
                    )
                }
            }
        }
    }

    fun anularVenta(ventaId: Int) {
        viewModelScope.launch {
            _uiState.update { it.copy(procesandoAccion = true, mensaje = null) }
            try {
                val success = repository.anularVenta(ventaId)
                if (success) {
                    _uiState.update {
                        it.copy(
                            procesandoAccion = false,
                            mensaje = "Venta anulada exitosamente"
                        )
                    }
                    cargarDatos()
                    cerrarDetalleVenta()
                } else {
                    _uiState.update {
                        it.copy(
                            procesandoAccion = false,
                            mensaje = "Error al anular venta"
                        )
                    }
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        procesandoAccion = false,
                        mensaje = "Error: ${e.message}"
                    )
                }
            }
        }
    }

    fun eliminarVenta(ventaId: Int) {
        viewModelScope.launch {
            _uiState.update { it.copy(procesandoAccion = true, mensaje = null) }
            try {
                val success = repository.eliminarVenta(ventaId)
                if (success) {
                    _uiState.update {
                        it.copy(
                            procesandoAccion = false,
                            mensaje = "Venta eliminada exitosamente"
                        )
                    }
                    cargarDatos()
                    cerrarDetalleVenta()
                } else {
                    _uiState.update {
                        it.copy(
                            procesandoAccion = false,
                            mensaje = "Error al eliminar venta"
                        )
                    }
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        procesandoAccion = false,
                        mensaje = "Error: ${e.message}"
                    )
                }
            }
        }
    }

    fun limpiarMensaje() {
        _uiState.update { it.copy(mensaje = null) }
    }
}
