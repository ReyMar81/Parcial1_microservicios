package com.tiendavirtual.admin.domain.model

data class Producto(
    val codigo: Int = 0,
    val nombre: String,
    val descripcion: String,
    val imagen: String,
    val precio: Double,
    val stock: Int,
    val categoriaId: Int
)
