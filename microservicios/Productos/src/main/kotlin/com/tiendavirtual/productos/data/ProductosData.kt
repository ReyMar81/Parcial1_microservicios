package com.tiendavirtual.productos.data

import com.tiendavirtual.productos.negocio.Categoria
import com.tiendavirtual.productos.negocio.Producto
import com.tiendavirtual.productos.negocio.Catalogo
import javax.sql.DataSource

// Data para Categorías
class CategoriasData(private val dataSource: DataSource) {
    fun crearCategoria(nombre: String): Categoria {
        val sql = "INSERT INTO categorias (nombre) VALUES (?) RETURNING id, nombre"
        dataSource.connection.use { conn ->
            conn.prepareStatement(sql).use { stmt ->
                stmt.setString(1, nombre)
                val rs = stmt.executeQuery()
                if (rs.next()) return Categoria(rs.getInt("id"), rs.getString("nombre"))
            }
        }
        throw RuntimeException("Error al crear categoría")
    }
    fun listarCategorias(): List<Categoria> {
        val sql = "SELECT id, nombre FROM categorias ORDER BY nombre"
        val lista = mutableListOf<Categoria>()
        dataSource.connection.use { conn ->
            conn.prepareStatement(sql).use { stmt ->
                val rs = stmt.executeQuery()
                while (rs.next()) lista.add(Categoria(rs.getInt("id"), rs.getString("nombre")))
            }
        }
        return lista
    }
    fun actualizarCategoria(id: Int, nombre: String): Categoria? {
        val sql = "UPDATE categorias SET nombre = ? WHERE id = ? RETURNING id, nombre"
        dataSource.connection.use { conn ->
            conn.prepareStatement(sql).use { stmt ->
                stmt.setString(1, nombre)
                stmt.setInt(2, id)
                val rs = stmt.executeQuery()
                if (rs.next()) return Categoria(rs.getInt("id"), rs.getString("nombre"))
            }
        }
        return null
    }
    fun eliminarCategoria(id: Int): Boolean {
        val sql = "DELETE FROM categorias WHERE id = ?"
        dataSource.connection.use { conn ->
            conn.prepareStatement(sql).use { stmt ->
                stmt.setInt(1, id)
                return stmt.executeUpdate() > 0
            }
        }
    }
}
// Data para Productos
class ProductosData(private val dataSource: DataSource) {
    fun crearProducto(nombre: String, descripcion: String, imagen: String, precio: Double, stock: Int, categoriaId: Int): Producto {
        val sql = """
            INSERT INTO productos (nombre, descripcion, imagen, precio, stock, categoria_id)
            VALUES (?, ?, ?, ?, ?, ?) RETURNING codigo, nombre, descripcion, imagen, precio, stock, categoria_id
        """.trimIndent()
        dataSource.connection.use { conn ->
            conn.prepareStatement(sql).use { stmt ->
                stmt.setString(1, nombre)
                stmt.setString(2, descripcion)
                stmt.setString(3, imagen)
                stmt.setDouble(4, precio)
                stmt.setInt(5, stock)
                stmt.setInt(6, categoriaId)
                val rs = stmt.executeQuery()
                if (rs.next()) return Producto(
                    rs.getInt("codigo"), rs.getString("nombre"), rs.getString("descripcion"),
                    rs.getString("imagen"), rs.getDouble("precio"), rs.getInt("stock"), rs.getInt("categoria_id")
                )
            }
        }
        throw RuntimeException("Error al crear producto")
    }
    fun listarProductos(): List<Producto> {
        val sql = "SELECT codigo, nombre, descripcion, imagen, precio, stock, categoria_id FROM productos ORDER BY codigo DESC"
        val lista = mutableListOf<Producto>()
        dataSource.connection.use { conn ->
            conn.prepareStatement(sql).use { stmt ->
                val rs = stmt.executeQuery()
                while (rs.next()) lista.add(
                    Producto(
                        rs.getInt("codigo"), rs.getString("nombre"), rs.getString("descripcion"),
                        rs.getString("imagen"), rs.getDouble("precio"), rs.getInt("stock"), rs.getInt("categoria_id")
                    )
                )
            }
        }
        return lista
    }
    fun obtenerProducto(codigo: Int): Producto? {
        val sql = "SELECT codigo, nombre, descripcion, imagen, precio, stock, categoria_id FROM productos WHERE codigo = ?"
        dataSource.connection.use { conn ->
            conn.prepareStatement(sql).use { stmt ->
                stmt.setInt(1, codigo)
                val rs = stmt.executeQuery()
                if (rs.next()) return Producto(
                    rs.getInt("codigo"), rs.getString("nombre"), rs.getString("descripcion"),
                    rs.getString("imagen"), rs.getDouble("precio"), rs.getInt("stock"), rs.getInt("categoria_id")
                )
            }
        }
        return null
    }
    fun actualizarProducto(codigo: Int, nombre: String?, descripcion: String?, imagen: String?, precio: Double?, stock: Int?, categoriaId: Int?): Producto? {
        val sql = """
            UPDATE productos SET
              nombre = COALESCE(?, nombre),
              descripcion = COALESCE(?, descripcion),
              imagen = COALESCE(?, imagen),
              precio = COALESCE(?, precio),
              stock = COALESCE(?, stock),
              categoria_id = COALESCE(?, categoria_id)
            WHERE codigo = ?
            RETURNING codigo, nombre, descripcion, imagen, precio, stock, categoria_id
        """.trimIndent()
        dataSource.connection.use { conn ->
            conn.prepareStatement(sql).use { stmt ->
                stmt.setString(1, nombre)
                stmt.setString(2, descripcion)
                stmt.setString(3, imagen)
                if (precio != null) stmt.setDouble(4, precio) else stmt.setNull(4, java.sql.Types.DOUBLE)
                if (stock != null) stmt.setInt(5, stock) else stmt.setNull(5, java.sql.Types.INTEGER)
                if (categoriaId != null) stmt.setInt(6, categoriaId) else stmt.setNull(6, java.sql.Types.INTEGER)
                stmt.setInt(7, codigo)
                val rs = stmt.executeQuery()
                if (rs.next()) return Producto(
                    rs.getInt("codigo"), rs.getString("nombre"), rs.getString("descripcion"),
                    rs.getString("imagen"), rs.getDouble("precio"), rs.getInt("stock"), rs.getInt("categoria_id")
                )
            }
        }
        return null
    }
    fun eliminarProducto(codigo: Int): Boolean {
        val sql = "DELETE FROM productos WHERE codigo = ?"
        dataSource.connection.use { conn ->
            conn.prepareStatement(sql).use { stmt ->
                stmt.setInt(1, codigo)
                return stmt.executeUpdate() > 0
            }
        }
    }
}

class CatalogosData(private val dataSource: DataSource) {
    fun crearCatalogo(nombre: String, descripcion: String): Catalogo {
        val sql = "INSERT INTO catalogos (nombre, descripcion) VALUES (?, ?) RETURNING id, nombre, descripcion"
        dataSource.connection.use { conn ->
            conn.prepareStatement(sql).use { stmt ->
                stmt.setString(1, nombre)
                stmt.setString(2, descripcion)
                val rs = stmt.executeQuery()
                if (rs.next()) return Catalogo(rs.getInt("id"), rs.getString("nombre"), rs.getString("descripcion"))
            }
        }
        throw RuntimeException("Error al crear catálogo")
    }
    fun listarCatalogos(): List<Catalogo> {
        val sql = "SELECT id, nombre, descripcion FROM catalogos ORDER BY id DESC"
        val lista = mutableListOf<Catalogo>()
        dataSource.connection.use { conn ->
            conn.prepareStatement(sql).use { stmt ->
                val rs = stmt.executeQuery()
                while (rs.next()) lista.add(Catalogo(rs.getInt("id"), rs.getString("nombre"), rs.getString("descripcion")))
            }
        }
        return lista
    }

    fun actualizarCatalogo(id: Int, nombre: String, descripcion: String): Catalogo? {
        val sql = "UPDATE catalogos SET nombre = ?, descripcion = ? WHERE id = ? RETURNING id, nombre, descripcion"
        dataSource.connection.use { conn ->
            conn.prepareStatement(sql).use { stmt ->
                stmt.setString(1, nombre)
                stmt.setString(2, descripcion)
                stmt.setInt(3, id)
                val rs = stmt.executeQuery()
                if (rs.next()) return Catalogo(rs.getInt("id"), rs.getString("nombre"), rs.getString("descripcion"))
            }
        }
        return null
    }
    fun eliminarCatalogo(id: Int): Boolean {
        val sql = "DELETE FROM catalogos WHERE id = ?"
        dataSource.connection.use { conn ->
            conn.prepareStatement(sql).use { stmt ->
                stmt.setInt(1, id)
                return stmt.executeUpdate() > 0
            }
        }
    }

    // ================== Asociación catálogo-productos ==================
    fun listarProductosDeCatalogo(catalogoId: Int): List<Producto> {
        val sql = """
            SELECT p.codigo, p.nombre, p.descripcion, p.imagen, p.precio, p.stock, p.categoria_id
            FROM catalogo_producto cp
            JOIN productos p ON p.codigo = cp.producto_id
            WHERE cp.catalogo_id = ?
            ORDER BY p.codigo DESC
        """.trimIndent()
        val lista = mutableListOf<Producto>()
        dataSource.connection.use { conn ->
            conn.prepareStatement(sql).use { stmt ->
                stmt.setInt(1, catalogoId)
                val rs = stmt.executeQuery()
                while (rs.next()) {
                    lista.add(
                        Producto(
                            rs.getInt("codigo"),
                            rs.getString("nombre"),
                            rs.getString("descripcion"),
                            rs.getString("imagen"),
                            rs.getDouble("precio"),
                            rs.getInt("stock"),
                            rs.getInt("categoria_id")
                        )
                    )
                }
            }
        }
        return lista
    }

    // ================== PRIMITIVAS catálogo-productos ==================
    fun insertarVinculoCatalogoProducto(conn: java.sql.Connection, catalogoId: Int, productoId: Int) {
        conn.prepareStatement("INSERT INTO catalogo_producto (catalogo_id, producto_id) VALUES (?, ?) ON CONFLICT DO NOTHING").use { st ->
            st.setInt(1, catalogoId)
            st.setInt(2, productoId)
            st.executeUpdate()
        }
    }
    fun eliminarVinculosDeCatalogo(conn: java.sql.Connection, catalogoId: Int) {
        conn.prepareStatement("DELETE FROM catalogo_producto WHERE catalogo_id = ?").use { st ->
            st.setInt(1, catalogoId)
            st.executeUpdate()
        }
    }
    fun eliminarVinculoIndividual(conn: java.sql.Connection, catalogoId: Int, productoId: Int): Boolean {
        conn.prepareStatement("DELETE FROM catalogo_producto WHERE catalogo_id = ? AND producto_id = ?").use { st ->
            st.setInt(1, catalogoId)
            st.setInt(2, productoId)
            return st.executeUpdate() > 0
        }
    }
    fun obtenerDataSource(): DataSource = dataSource
}
