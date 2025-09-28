package com.tiendavirtual.productos.negocio

import com.tiendavirtual.productos.data.CategoriasData
import com.tiendavirtual.productos.data.ProductosData
import com.tiendavirtual.productos.data.CatalogosData

// Entidades de dominio
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
    fun crearCategoria(nombre: String): Categoria = categoriasData.crearCategoria(nombre)
    fun listarCategorias(): List<Categoria> = categoriasData.listarCategorias()
    fun actualizarCategoria(id: Int, nombre: String): Categoria? = categoriasData.actualizarCategoria(id, nombre)
    fun eliminarCategoria(id: Int): Boolean = categoriasData.eliminarCategoria(id)
}

class ProductoNegocio(private val productosData: ProductosData) {
    fun crearProducto(nombre: String, descripcion: String, imagen: String, precio: Double, stock: Int, categoriaId: Int): Producto =
        productosData.crearProducto(nombre, descripcion, imagen, precio, stock, categoriaId)
    fun listarProductos(): List<Producto> = productosData.listarProductos()
    fun obtenerProducto(codigo: Int): Producto? = productosData.obtenerProducto(codigo)
    fun actualizarProducto(codigo: Int, nombre: String?, descripcion: String?, imagen: String?, precio: Double?, stock: Int?, categoriaId: Int?): Producto? =
        productosData.actualizarProducto(codigo, nombre, descripcion, imagen, precio, stock, categoriaId)
    fun eliminarProducto(codigo: Int): Boolean = productosData.eliminarProducto(codigo)
}

class CatalogoNegocio(private val catalogosData: CatalogosData) {
    fun crearCatalogo(nombre: String, descripcion: String): Catalogo = catalogosData.crearCatalogo(nombre, descripcion)
    fun listarCatalogos(): List<Catalogo> = catalogosData.listarCatalogos()
    fun actualizarCatalogo(id: Int, nombre: String, descripcion: String): Catalogo? = catalogosData.actualizarCatalogo(id, nombre, descripcion)
    fun eliminarCatalogo(id: Int): Boolean = catalogosData.eliminarCatalogo(id)

    // Asociación catálogo-productos
    fun listarProductosDeCatalogo(catalogoId: Int): List<Producto> = catalogosData.listarProductosDeCatalogo(catalogoId)

    fun agregarProductosACatalogo(catalogoId: Int, productoIds: List<Int>): Int {
        if (productoIds.isEmpty()) return 0
        val existentes = listarProductosDeCatalogo(catalogoId).map { it.codigo }.toHashSet()
        val nuevos = productoIds.filter { it !in existentes }.distinct()
        if (nuevos.isEmpty()) return 0
        var agregados = 0
        val ds = catalogosData.obtenerDataSource()
        ds.connection.use { conn ->
            conn.autoCommit = false
            try {
                nuevos.forEach { pid -> catalogosData.insertarVinculoCatalogoProducto(conn, catalogoId, pid); agregados++ }
                conn.commit()
            } catch (e: Exception) {
                conn.rollback(); throw e
            } finally { conn.autoCommit = true }
        }
        return agregados
    }

    fun reemplazarProductosDeCatalogo(catalogoId: Int, productoIds: List<Int>): Int {
        val ds = catalogosData.obtenerDataSource()
        val distintos = productoIds.distinct()
        var total = 0
        ds.connection.use { conn ->
            conn.autoCommit = false
            try {
                catalogosData.eliminarVinculosDeCatalogo(conn, catalogoId)
                distintos.forEach { pid -> catalogosData.insertarVinculoCatalogoProducto(conn, catalogoId, pid); total++ }
                conn.commit()
            } catch (e: Exception) {
                conn.rollback(); throw e
            } finally { conn.autoCommit = true }
        }
        return total
    }

    fun eliminarProductoDeCatalogo(catalogoId: Int, productoId: Int): Boolean {
        val ds = catalogosData.obtenerDataSource()
        var eliminado: Boolean
        ds.connection.use { conn ->
            conn.autoCommit = false
            try {
                eliminado = catalogosData.eliminarVinculoIndividual(conn, catalogoId, productoId)
                conn.commit()
            } catch (e: Exception) {
                conn.rollback(); throw e
            } finally { conn.autoCommit = true }
        }
        return eliminado
    }
}
