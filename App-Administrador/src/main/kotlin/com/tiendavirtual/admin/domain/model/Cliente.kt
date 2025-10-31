package com.tiendavirtual.admin.domain.model

data class Cliente(
    val id: Int? = null,
    val nombres: String,
    val docIdentidad: String,
    val whatsapp: String,
    val direccion: String
) {
    // Alias para compatibilidad con VentasModule
    val nombre: String get() = nombres
}
