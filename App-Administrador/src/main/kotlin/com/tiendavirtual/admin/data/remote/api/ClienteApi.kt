package com.tiendavirtual.admin.data.remote.api

import android.util.Log
import com.tiendavirtual.admin.core.network.HttpClientFactory
import com.tiendavirtual.admin.data.shared.ApiConfig
import com.tiendavirtual.admin.domain.model.Cliente
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody

class ClienteApi(
    private val client: OkHttpClient = HttpClientFactory.create(),
    private val gson: Gson = Gson()
) {
    private val baseUrl = ApiConfig.CLIENTES_URL
    private val mediaType = ApiConfig.CONTENT_TYPE.toMediaType()

    suspend fun obtenerClientes(): List<Cliente> = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder()
                .url(baseUrl)
                .addHeader("Accept", ApiConfig.ACCEPT)
                .build()
            
            val response = client.newCall(request).execute()
            if (response.isSuccessful) {
                val json = response.body?.string() ?: "[]"
                val type = object : TypeToken<List<Cliente>>() {}.type
                gson.fromJson(json, type) ?: emptyList()
            } else {
                emptyList()
            }
        } catch (e: Exception) {
            Log.e("ClienteApi", "Error al obtener clientes", e)
            emptyList()
        }
    }

    suspend fun crearCliente(cliente: Cliente): Cliente? = withContext(Dispatchers.IO) {
        try {
            val json = gson.toJson(cliente.copy(
                nombres = cliente.nombres.trim(),
                docIdentidad = cliente.docIdentidad.trim(),
                whatsapp = cliente.whatsapp.replace("[\\s()-]".toRegex(), ""),
                direccion = cliente.direccion.trim()
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
                gson.fromJson(response.body?.string(), Cliente::class.java)
            } else {
                null
            }
        } catch (e: Exception) {
            Log.e("ClienteApi", "Error al crear cliente", e)
            null
        }
    }

    suspend fun actualizarCliente(id: Int, cliente: Cliente): Cliente? = withContext(Dispatchers.IO) {
        try {
            val json = gson.toJson(cliente.copy(
                id = id,
                nombres = cliente.nombres.trim(),
                docIdentidad = cliente.docIdentidad.trim(),
                whatsapp = cliente.whatsapp.replace("[\\s()-]".toRegex(), ""),
                direccion = cliente.direccion.trim()
            ))
            
            val body = json.toRequestBody(mediaType)
            val request = Request.Builder()
                .url("$baseUrl/$id")
                .patch(body)
                .addHeader("Content-Type", ApiConfig.CONTENT_TYPE)
                .addHeader("Accept", ApiConfig.ACCEPT)
                .build()
            
            val response = client.newCall(request).execute()
            if (response.isSuccessful) {
                gson.fromJson(response.body?.string(), Cliente::class.java)
            } else {
                null
            }
        } catch (e: Exception) {
            Log.e("ClienteApi", "Error al actualizar cliente", e)
            null
        }
    }

    suspend fun eliminarCliente(id: Int): Boolean = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder()
                .url("$baseUrl/$id")
                .delete()
                .addHeader("Accept", ApiConfig.ACCEPT)
                .build()
            
            client.newCall(request).execute().isSuccessful
        } catch (e: Exception) {
            Log.e("ClienteApi", "Error al eliminar cliente", e)
            false
        }
    }
}
