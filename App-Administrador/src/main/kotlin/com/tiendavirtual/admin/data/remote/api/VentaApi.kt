package com.tiendavirtual.admin.data.remote.api

import com.google.gson.Gson
import com.tiendavirtual.admin.core.network.HttpClientFactory
import com.tiendavirtual.admin.data.shared.ApiConfig
import com.tiendavirtual.admin.domain.model.Venta
import com.tiendavirtual.admin.domain.model.CrearVentaRequest
import com.tiendavirtual.admin.domain.model.DetalleVentaRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody

class VentaApi {
    private val client = HttpClientFactory.create()
    private val gson = Gson()
    private val mediaType = "application/json; charset=utf-8".toMediaType()

    suspend fun obtenerVentas(): List<Venta> = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder()
                .url(ApiConfig.VENTAS_URL)
                .get()
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    throw Exception("Error al obtener ventas: ${response.code}")
                }
                val json = response.body?.string() ?: "[]"
                gson.fromJson(json, Array<Venta>::class.java).toList()
            }
        } catch (e: Exception) {
            throw Exception("Error al obtener ventas: ${e.message}", e)
        }
    }

    suspend fun obtenerVenta(id: Int): Venta? = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder()
                .url("${ApiConfig.VENTAS_URL}/$id")
                .get()
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    return@withContext null
                }
                val json = response.body?.string() ?: return@withContext null
                gson.fromJson(json, Venta::class.java)
            }
        } catch (e: Exception) {
            null
        }
    }

    suspend fun crearVenta(venta: Venta): Boolean = withContext(Dispatchers.IO) {
        try {
            val request = CrearVentaRequest(
                clienteId = venta.clienteId,
                fecha = venta.fecha,
                estado = venta.estado,
                total = venta.total,
                detalles = venta.detalles.map { 
                    DetalleVentaRequest(it.productoId, it.cantidad, it.precioUnitario) 
                }
            )
            val json = gson.toJson(request)
            val body = json.toRequestBody(mediaType)

            val httpRequest = Request.Builder()
                .url(ApiConfig.VENTAS_URL)
                .post(body)
                .build()

            client.newCall(httpRequest).execute().use { response ->
                response.isSuccessful
            }
        } catch (e: Exception) {
            throw Exception("Error al crear venta: ${e.message}", e)
        }
    }

    suspend fun actualizarVenta(venta: Venta): Boolean = withContext(Dispatchers.IO) {
        try {
            val request = CrearVentaRequest(
                clienteId = venta.clienteId,
                fecha = venta.fecha,
                estado = venta.estado,
                total = venta.total,
                detalles = venta.detalles.map { 
                    DetalleVentaRequest(it.productoId, it.cantidad, it.precioUnitario) 
                }
            )
            val json = gson.toJson(request)
            val body = json.toRequestBody(mediaType)

            val httpRequest = Request.Builder()
                .url("${ApiConfig.VENTAS_URL}/${venta.id}")
                .put(body)
                .build()

            client.newCall(httpRequest).execute().use { response ->
                response.isSuccessful
            }
        } catch (e: Exception) {
            throw Exception("Error al actualizar venta: ${e.message}", e)
        }
    }

    suspend fun confirmarVenta(id: Int): Boolean = withContext(Dispatchers.IO) {
        try {
            val url = ApiConfig.CONFIRMAR_VENTA_URL.replace("{id}", id.toString())
            val body = "{}".toRequestBody(mediaType)

            val request = Request.Builder()
                .url(url)
                .post(body)
                .build()

            client.newCall(request).execute().use { response ->
                response.isSuccessful
            }
        } catch (e: Exception) {
            throw Exception("Error al confirmar venta: ${e.message}", e)
        }
    }

    suspend fun anularVenta(id: Int): Boolean = withContext(Dispatchers.IO) {
        try {
            val url = ApiConfig.ANULAR_VENTA_URL.replace("{id}", id.toString())
            val body = "{}".toRequestBody(mediaType)

            val request = Request.Builder()
                .url(url)
                .post(body)
                .build()

            client.newCall(request).execute().use { response ->
                response.isSuccessful
            }
        } catch (e: Exception) {
            throw Exception("Error al anular venta: ${e.message}", e)
        }
    }

    suspend fun eliminarVenta(id: Int): Boolean = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder()
                .url("${ApiConfig.VENTAS_URL}/$id")
                .delete()
                .build()

            client.newCall(request).execute().use { response ->
                response.isSuccessful
            }
        } catch (e: Exception) {
            throw Exception("Error al eliminar venta: ${e.message}", e)
        }
    }
}
