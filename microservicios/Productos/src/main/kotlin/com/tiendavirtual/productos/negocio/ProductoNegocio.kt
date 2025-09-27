package com.tiendavirtual.productos.negocio

import com.tiendavirtual.productos.data.CategoriasData
import com.tiendavirtual.productos.data.ProductosData
import com.tiendavirtual.productos.data.CatalogosData

// Entidades de dominio (similar a Cliente en microservicio Clientes)
data class Categoria(
    val id: Int,
    val nombre: String
)

data class Producto(
    val codigo: Int,
    val nombre: String,
    val descripcion: String,
    val imagen: String,
    val precio: Double,
    val stock: Int,
    val categoriaId: Int
)

data class Catalogo(
    val id: Int,
    val nombre: String,
    val descripcion: String
)

class CategoriaNegocio(private val categoriasData: CategoriasData) {
    fun crearCategoria(nombre: String): Categoria {
        if (nombre.isBlank()) throw IllegalArgumentException("El nombre de la categoría no puede estar vacío")
        val existente = categoriasData.obtenerCategoriaPorNombre(nombre)
        if (existente != null) throw IllegalArgumentException("Ya existe una categoría con ese nombre")
        return categoriasData.crearCategoria(nombre)
    }
    fun listarCategorias(): List<Categoria> = categoriasData.listarCategorias()
    fun actualizarCategoria(id: Int, nombre: String): Categoria? {
        if (nombre.isBlank()) throw IllegalArgumentException("El nombre de la categoría no puede estar vacío")
        return categoriasData.actualizarCategoria(id, nombre)
    }
    fun eliminarCategoria(id: Int): Boolean = categoriasData.eliminarCategoria(id)
}

class ProductoNegocio(private val productosData: ProductosData) {
    fun crearProducto(nombre: String, descripcion: String, imagen: String, precio: Double, stock: Int, categoriaId: Int): Producto {
        validarDatosProducto(nombre, descripcion, imagen, precio, stock)
        return productosData.crearProducto(nombre, descripcion, imagen, precio, stock, categoriaId)
    }
    fun listarProductos(): List<Producto> = productosData.listarProductos()
    fun obtenerProducto(codigo: Int): Producto? = productosData.obtenerProducto(codigo)
    fun actualizarProducto(codigo: Int, nombre: String?, descripcion: String?, imagen: String?, precio: Double?, stock: Int?, categoriaId: Int?): Producto? {
        return productosData.actualizarProducto(codigo, nombre, descripcion, imagen, precio, stock, categoriaId)
    }
    fun eliminarProducto(codigo: Int): Boolean = productosData.eliminarProducto(codigo)

    private fun validarDatosProducto(nombre: String, descripcion: String, imagen: String, precio: Double, stock: Int) {
        if (nombre.isBlank()) throw IllegalArgumentException("El nombre no puede estar vacío")
        if (descripcion.isBlank()) throw IllegalArgumentException("La descripción no puede estar vacía")
        if (imagen.isBlank()) throw IllegalArgumentException("La imagen no puede estar vacía")
        if (precio < 0) throw IllegalArgumentException("El precio no puede ser negativo")
        if (stock < 0) throw IllegalArgumentException("El stock no puede ser negativo")
    }
}

class CatalogoNegocio(private val catalogosData: CatalogosData) {
    fun crearCatalogo(nombre: String, descripcion: String): Catalogo {
        if (nombre.isBlank()) throw IllegalArgumentException("El nombre del catálogo no puede estar vacío")
        return catalogosData.crearCatalogo(nombre, descripcion)
    }
    fun listarCatalogos(): List<Catalogo> = catalogosData.listarCatalogos()
    fun actualizarCatalogo(id: Int, nombre: String, descripcion: String): Catalogo? {
        if (nombre.isBlank()) throw IllegalArgumentException("El nombre del catálogo no puede estar vacío")
        return catalogosData.actualizarCatalogo(id, nombre, descripcion)
    }
    fun eliminarCatalogo(id: Int): Boolean = catalogosData.eliminarCatalogo(id)
}
