package com.tiendavirtual.clientes.data

import com.tiendavirtual.clientes.negocio.Cliente
import javax.sql.DataSource

/**
 * Capa de Datos - Clientes
 * Maneja el acceso a la base de datos PostgreSQL para clientes
 */
class ClientesData(private val dataSource: DataSource) {

    /**
     * Crear un nuevo cliente en la base de datos
     */
    fun crearCliente(nombres: String, docIdentidad: String, whatsapp: String, direccion: String): Cliente {
        val sql = """
            INSERT INTO clientes (nombres, docIdentidad, whatsapp, direccion) 
            VALUES (?, ?, ?, ?) 
            RETURNING id, nombres, docIdentidad, whatsapp, direccion
        """

        dataSource.connection.use { conn ->
            conn.prepareStatement(sql).use { stmt ->
                stmt.setString(1, nombres)
                stmt.setString(2, docIdentidad)
                stmt.setString(3, whatsapp)
                stmt.setString(4, direccion)

                val rs = stmt.executeQuery()
                if (rs.next()) {
                    return Cliente(
                        id = rs.getInt("id"),
                        nombres = rs.getString("nombres"),
                        docIdentidad = rs.getString("docIdentidad"),
                        whatsapp = rs.getString("whatsapp"),
                        direccion = rs.getString("direccion")
                    )
                } else {
                    throw RuntimeException("Error al crear cliente")
                }
            }
        }
    }

    /**
     * Obtener cliente por ID
     */
    fun obtenerCliente(id: Int): Cliente? {
        val sql = "SELECT id, nombres, docIdentidad, whatsapp, direccion FROM clientes WHERE id = ?"

        dataSource.connection.use { conn ->
            conn.prepareStatement(sql).use { stmt ->
                stmt.setInt(1, id)
                val rs = stmt.executeQuery()

                return if (rs.next()) {
                    Cliente(
                        id = rs.getInt("id"),
                        nombres = rs.getString("nombres"),
                        docIdentidad = rs.getString("docIdentidad"),
                        whatsapp = rs.getString("whatsapp"),
                        direccion = rs.getString("direccion")
                    )
                } else null
            }
        }
    }

    /**
     * Obtener cliente por documento de identidad
     */
    fun obtenerClientePorDoc(docIdentidad: String): Cliente? {
        val sql = "SELECT id, nombres, docIdentidad, whatsapp, direccion FROM clientes WHERE docIdentidad = ?"

        dataSource.connection.use { conn ->
            conn.prepareStatement(sql).use { stmt ->
                stmt.setString(1, docIdentidad)
                val rs = stmt.executeQuery()

                return if (rs.next()) {
                    Cliente(
                        id = rs.getInt("id"),
                        nombres = rs.getString("nombres"),
                        docIdentidad = rs.getString("docIdentidad"),
                        whatsapp = rs.getString("whatsapp"),
                        direccion = rs.getString("direccion")
                    )
                } else null
            }
        }
    }

    /**
     * Obtener todos los clientes
     */
    fun obtenerTodosLosClientes(): List<Cliente> {
        val sql = "SELECT id, nombres, docIdentidad, whatsapp, direccion FROM clientes ORDER BY nombres"
        val clientes = mutableListOf<Cliente>()

        dataSource.connection.use { conn ->
            conn.prepareStatement(sql).use { stmt ->
                val rs = stmt.executeQuery()

                while (rs.next()) {
                    clientes.add(
                        Cliente(
                            id = rs.getInt("id"),
                            nombres = rs.getString("nombres"),
                            docIdentidad = rs.getString("docIdentidad"),
                            whatsapp = rs.getString("whatsapp"),
                            direccion = rs.getString("direccion")
                        )
                    )
                }
            }
        }

        return clientes
    }

    /**
     * Actualizar cliente existente
     */
    fun actualizarCliente(id: Int, nombres: String?, docIdentidad: String?, whatsapp: String?, direccion: String?): Cliente? {
        val campos = mutableListOf<String>()
        val valores = mutableListOf<Any>()

        nombres?.let {
            campos.add("nombres = ?")
            valores.add(it)
        }
        docIdentidad?.let {
            campos.add("docIdentidad = ?")
            valores.add(it)
        }
        whatsapp?.let {
            campos.add("whatsapp = ?")
            valores.add(it)
        }
        direccion?.let {
            campos.add("direccion = ?")
            valores.add(it)
        }

        if (campos.isEmpty()) {
            return obtenerCliente(id) // No hay nada que actualizar
        }

        val sql = """
            UPDATE clientes 
            SET ${campos.joinToString(", ")} 
            WHERE id = ? 
            RETURNING id, nombres, docIdentidad, whatsapp, direccion
        """

        dataSource.connection.use { conn ->
            conn.prepareStatement(sql).use { stmt ->
                valores.forEachIndexed { index, valor ->
                    stmt.setObject(index + 1, valor)
                }
                stmt.setInt(valores.size + 1, id)

                val rs = stmt.executeQuery()
                return if (rs.next()) {
                    Cliente(
                        id = rs.getInt("id"),
                        nombres = rs.getString("nombres"),
                        docIdentidad = rs.getString("docIdentidad"),
                        whatsapp = rs.getString("whatsapp"),
                        direccion = rs.getString("direccion")
                    )
                } else null
            }
        }
    }

    /**
     * Eliminar cliente
     */
    fun eliminarCliente(id: Int): Boolean {
        val sql = "DELETE FROM clientes WHERE id = ?"

        dataSource.connection.use { conn ->
            conn.prepareStatement(sql).use { stmt ->
                stmt.setInt(1, id)
                return stmt.executeUpdate() > 0
            }
        }
    }
}
