package com.tiendavirtual.admin.data.remote.api

import android.util.Log
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.tiendavirtual.admin.core.network.HttpClientFactory
import com.tiendavirtual.admin.data.shared.ApiConfig
import com.tiendavirtual.admin.domain.model.Categoria
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody

class CategoriaApi(
    private val client: OkHttpClient = HttpClientFactory.create(),
    private val gson: Gson = Gson()
) {
    private val baseUrl = ApiConfig.CATEGORIAS_URL
    private val mediaType = ApiConfig.CONTENT_TYPE.toMediaType()

    suspend fun obtenerCategorias(): List<Categoria> = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder()
                .url(baseUrl)
                .get()
                .addHeader("Accept", ApiConfig.ACCEPT)
                .build()
            
            val response = client.newCall(request).execute()
            if (response.isSuccessful) {
                val json = response.body?.string() ?: "[]"
                val type = object : TypeToken<List<Categoria>>() {}.type
                gson.fromJson(json, type) ?: emptyList()
            } else {
                emptyList()
            }
        } catch (e: Exception) {
            Log.e("CategoriaApi", "Error al obtener categorías", e)
            emptyList()
        }
    }

    suspend fun crearCategoria(categoria: Categoria): Categoria? = withContext(Dispatchers.IO) {
        try {
            val json = gson.toJson(categoria.copy(nombre = categoria.nombre.trim()))
            val body = json.toRequestBody(mediaType)
            val request = Request.Builder()
                .url(baseUrl)
                .post(body)
                .addHeader("Content-Type", ApiConfig.CONTENT_TYPE)
                .addHeader("Accept", ApiConfig.ACCEPT)
                .build()
            
            val response = client.newCall(request).execute()
            if (response.isSuccessful) {
                gson.fromJson(response.body?.string(), Categoria::class.java)
            } else {
                null
            }
        } catch (e: Exception) {
            Log.e("CategoriaApi", "Error al crear categoría", e)
            null
        }
    }

    suspend fun actualizarCategoria(id: Int, categoria: Categoria): Categoria? = withContext(Dispatchers.IO) {
        try {
            val json = gson.toJson(categoria.copy(id = id, nombre = categoria.nombre.trim()))
            val body = json.toRequestBody(mediaType)
            val request = Request.Builder()
                .url("$baseUrl/$id")
                .put(body)
                .addHeader("Content-Type", ApiConfig.CONTENT_TYPE)
                .addHeader("Accept", ApiConfig.ACCEPT)
                .build()
            
            val response = client.newCall(request).execute()
            if (response.isSuccessful) {
                gson.fromJson(response.body?.string(), Categoria::class.java)
            } else {
                null
            }
        } catch (e: Exception) {
            Log.e("CategoriaApi", "Error al actualizar categoría", e)
            null
        }
    }

    suspend fun eliminarCategoria(id: Int): Boolean = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder()
                .url("$baseUrl/$id")
                .delete()
                .addHeader("Accept", ApiConfig.ACCEPT)
                .build()
            
            client.newCall(request).execute().isSuccessful
        } catch (e: Exception) {
            Log.e("CategoriaApi", "Error al eliminar categoría", e)
            false
        }
    }
}
