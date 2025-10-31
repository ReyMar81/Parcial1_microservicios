package com.tiendavirtual.admin.data.remote.api

import android.util.Log
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.tiendavirtual.admin.core.network.HttpClientFactory
import com.tiendavirtual.admin.data.shared.ApiConfig
import com.tiendavirtual.admin.domain.model.Producto
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody

class ProductoApi(
    private val client: OkHttpClient = HttpClientFactory.create(),
    private val gson: Gson = Gson()
) {
    private val baseUrl = "${ApiConfig.PRODUCTOS_URL}/productos"
    private val mediaType = ApiConfig.CONTENT_TYPE.toMediaType()

    suspend fun obtenerProductos(): List<Producto> = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder()
                .url(baseUrl)
                .get()
                .addHeader("Accept", ApiConfig.ACCEPT)
                .build()
            
            val response = client.newCall(request).execute()
            if (response.isSuccessful) {
                val json = response.body?.string() ?: "[]"
                val type = object : TypeToken<List<Producto>>() {}.type
                gson.fromJson(json, type) ?: emptyList()
            } else {
                emptyList()
            }
        } catch (e: Exception) {
            Log.e("ProductoApi", "Error al obtener productos", e)
            emptyList()
        }
    }

    suspend fun crearProducto(producto: Producto): Producto? = withContext(Dispatchers.IO) {
        try {
            val json = gson.toJson(producto.copy(
                nombre = producto.nombre.trim(),
                descripcion = producto.descripcion.trim(),
                imagen = producto.imagen.trim()
            ))
            val body = json.toRequestBody(mediaType)
            val request = Request.Builder()
                .url(baseUrl)
                .post(body)
                .addHeader("Content-Type", ApiConfig.CONTENT_TYPE)
                .addHeader("Accept", ApiConfig.ACCEPT)
                .build()
            
            val response = client.newCall(request).execute()
            if (response.isSuccessful) {
                gson.fromJson(response.body?.string(), Producto::class.java)
            } else {
                null
            }
        } catch (e: Exception) {
            Log.e("ProductoApi", "Error al crear producto", e)
            null
        }
    }

    suspend fun actualizarProducto(codigo: Int, producto: Producto): Producto? = withContext(Dispatchers.IO) {
        try {
            val json = gson.toJson(producto.copy(
                codigo = codigo,
                nombre = producto.nombre.trim(),
                descripcion = producto.descripcion.trim(),
                imagen = producto.imagen.trim()
            ))
            val body = json.toRequestBody(mediaType)
            val request = Request.Builder()
                .url("$baseUrl/$codigo")
                .put(body)
                .addHeader("Content-Type", ApiConfig.CONTENT_TYPE)
                .addHeader("Accept", ApiConfig.ACCEPT)
                .build()
            
            val response = client.newCall(request).execute()
            if (response.isSuccessful) {
                gson.fromJson(response.body?.string(), Producto::class.java)
            } else {
                null
            }
        } catch (e: Exception) {
            Log.e("ProductoApi", "Error al actualizar producto", e)
            null
        }
    }

    suspend fun eliminarProducto(codigo: Int): Boolean = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder()
                .url("$baseUrl/$codigo")
                .delete()
                .addHeader("Accept", ApiConfig.ACCEPT)
                .build()
            
            client.newCall(request).execute().isSuccessful
        } catch (e: Exception) {
            Log.e("ProductoApi", "Error al eliminar producto", e)
            false
        }
    }
}
