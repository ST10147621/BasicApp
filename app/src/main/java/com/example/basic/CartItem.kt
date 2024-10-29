package com.example.basic

data class CartItem(
    val title: String,
    val size: String,
    var quantity: Int,
    var totalPrice: Double,
    var pricePerUnit :Double
)
