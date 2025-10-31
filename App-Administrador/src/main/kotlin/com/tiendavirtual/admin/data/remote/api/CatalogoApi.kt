package com.tiendavirtual.admin.data.remote.api

import android.util.Log
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.tiendavirtual.admin.core.network.HttpClientFactory
import com.tiendavirtual.admin.data.shared.ApiConfig
import com.tiendavirtual.admin.domain.model.Catalogo
import com.tiendavirtual.admin.domain.model.Producto
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody

class CatalogoApi(
    private val client: OkHttpClient = HttpClientFactory.create(),
    private val gson: Gson = Gson()
) {
    private val baseUrl = ApiConfig.CATALOGOS_URL
    private val mediaType = ApiConfig.CONTENT_TYPE.toMediaType()

    suspend fun obtenerCatalogos(): List<Catalogo> = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder()
                .url(baseUrl)
                .get()
                .addHeader("Accept", ApiConfig.ACCEPT)
                .build()
            
            val response = client.newCall(request).execute()
            if (response.isSuccessful) {
                val json = response.body?.string() ?: "[]"
                val type = object : TypeToken<List<Catalogo>>() {}.type
                gson.fromJson(json, type) ?: emptyList()
            } else emptyList()
        } catch (e: Exception) {
            Log.e("CatalogoApi", "Error al obtener catálogos", e)
            emptyList()
        }
    }

    suspend fun crearCatalogo(catalogo: Catalogo): Catalogo? = withContext(Dispatchers.IO) {
        try {
            val json = gson.toJson(catalogo.copy(
                nombre = catalogo.nombre.trim(),
                descripcion = catalogo.descripcion.trim()
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
                gson.fromJson(response.body?.string(), Catalogo::class.java)
            } else null
        } catch (e: Exception) {
            Log.e("CatalogoApi", "Error al crear catálogo", e)
            null
        }
    }

    suspend fun actualizarCatalogo(id: Int, catalogo: Catalogo): Catalogo? = withContext(Dispatchers.IO) {
        try {
            val json = gson.toJson(catalogo.copy(
                id = id,
                nombre = catalogo.nombre.trim(),
                descripcion = catalogo.descripcion.trim()
            ))
            val body = json.toRequestBody(mediaType)
            val request = Request.Builder()
                .url("$baseUrl/$id")
                .put(body)
                .addHeader("Content-Type", ApiConfig.CONTENT_TYPE)
                .addHeader("Accept", ApiConfig.ACCEPT)
                .build()
            
            val response = client.newCall(request).execute()
            if (response.isSuccessful) {
                gson.fromJson(response.body?.string(), Catalogo::class.java)
            } else null
        } catch (e: Exception) {
            Log.e("CatalogoApi", "Error al actualizar catálogo", e)
            null
        }
    }

    suspend fun eliminarCatalogo(id: Int): Boolean = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder()
                .url("$baseUrl/$id")
                .delete()
                .addHeader("Accept", ApiConfig.ACCEPT)
                .build()
            
            client.newCall(request).execute().isSuccessful
        } catch (e: Exception) {
            Log.e("CatalogoApi", "Error al eliminar catálogo", e)
            false
        }
    }

    suspend fun agregarProductos(catalogoId: Int, productoIds: List<Int>): Boolean = withContext(Dispatchers.IO) {
        try {
            val bodyJson = gson.toJson(mapOf("productoIds" to productoIds))
            val body = bodyJson.toRequestBody(mediaType)
            val request = Request.Builder()
                .url("$baseUrl/$catalogoId/productos")
                .post(body)
                .addHeader("Content-Type", ApiConfig.CONTENT_TYPE)
                .addHeader("Accept", ApiConfig.ACCEPT)
                .build()
            
            client.newCall(request).execute().isSuccessful
        } catch (e: Exception) {
            Log.e("CatalogoApi", "Error al agregar productos", e)
            false
        }
    }

    suspend fun obtenerProductosDeCatalogo(catalogoId: Int): List<Producto> = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder()
                .url("$baseUrl/$catalogoId/productos")
                .get()
                .addHeader("Accept", ApiConfig.ACCEPT)
                .build()
            
            val response = client.newCall(request).execute()
            if (response.isSuccessful) {
                val json = response.body?.string() ?: "[]"
                val type = object : TypeToken<List<Producto>>() {}.type
                gson.fromJson(json, type) ?: emptyList()
            } else emptyList()
        } catch (e: Exception) {
            Log.e("CatalogoApi", "Error al obtener productos del catálogo", e)
            emptyList()
        }
    }

    suspend fun eliminarProductoDeCatalogo(catalogoId: Int, productoId: Int): Boolean = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder()
                .url("$baseUrl/$catalogoId/productos/$productoId")
                .delete()
                .addHeader("Accept", ApiConfig.ACCEPT)
                .build()
            
            client.newCall(request).execute().isSuccessful
        } catch (e: Exception) {
            Log.e("CatalogoApi", "Error al eliminar producto del catálogo", e)
            false
        }
    }
}
