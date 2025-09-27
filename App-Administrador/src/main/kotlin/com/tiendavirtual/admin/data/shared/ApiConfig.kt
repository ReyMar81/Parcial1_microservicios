package com.tiendavirtual.admin.data.shared

import com.tiendavirtual.admin.BuildConfig

/**
 * Configuración para comunicación con API Gateway
 */
object ApiConfig {
    // URL base del API Gateway
    //const val GATEWAY_BASE_URL = "http://10.0.2.2:8080/api/"  // Para emulador
    const val GATEWAY_BASE_URL = "http://192.168.0.16:8080/api"
    const val CLIENTES_URL = "${GATEWAY_BASE_URL}/clientes"
    private const val PRODUCTOS_PREFIX = "$GATEWAY_BASE_URL/productos"
    const val PRODUCTOS_URL = PRODUCTOS_PREFIX
    const val CATEGORIAS_URL = "$PRODUCTOS_PREFIX/categorias"
    const val CATALOGOS_URL  = "$PRODUCTOS_PREFIX/catalogos"


    const val CONNECT_TIMEOUT = 30L
    const val READ_TIMEOUT = 30L
    const val WRITE_TIMEOUT = 30L

    const val CONTENT_TYPE = "application/json"
    const val ACCEPT = "application/json"
}
