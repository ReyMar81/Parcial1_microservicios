package com.tiendavirtual.admin.core.util

import android.util.Base64

object ImageUtil {
    fun esImagenBase64(imagen: String): Boolean {
        return imagen.startsWith("data:image/") && imagen.contains(",")
    }

    fun decodificarBase64(imagenBase64: String): ByteArray? {
        return try {
            if (esImagenBase64(imagenBase64)) {
                val base64 = imagenBase64.substringAfter(",")
                Base64.decode(base64, Base64.DEFAULT)
            } else {
                null
            }
        } catch (e: Exception) {
            null
        }
    }

    fun codificarABase64(bytes: ByteArray, mime: String = "jpeg"): String {
        val base64Image = Base64.encodeToString(bytes, Base64.NO_WRAP)
        return "data:image/$mime;base64,$base64Image"
    }

    fun detectarMimeType(bytes: ByteArray): String {
        val header = bytes.take(8).toByteArray()
        return when {
            header.sliceArray(0..1).contentEquals(byteArrayOf(0xFF.toByte(), 0xD8.toByte())) -> "jpeg"
            header.contentEquals(byteArrayOf(0x89.toByte(), 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A)) -> "png"
            else -> "jpeg"
        }
    }

    fun calcularTamanoKB(imagenBase64: String): Int {
        return (imagenBase64.length * 0.75 / 1024).toInt()
    }
}
