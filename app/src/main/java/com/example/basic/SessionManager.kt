package com.example.basic

import android.content.Context
import android.content.SharedPreferences
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class SessionManager(context: Context) {

    private val sharedPreferences: SharedPreferences = context.getSharedPreferences("user_session", Context.MODE_PRIVATE)
    private val editor: SharedPreferences.Editor = sharedPreferences.edit()
    private val gson = Gson()

    fun saveCartItems(cartItems: List<CartItem>) {
        val json = gson.toJson(cartItems)
        editor.putString("cart_items", json).apply()
    }

    fun getCartItems(): List<CartItem> {
        val json = sharedPreferences.getString("cart_items", null)
        val type = object : TypeToken<List<CartItem>>() {}.type
        return gson.fromJson(json, type) ?: emptyList()
    }

    fun clearSession() {
        editor.clear().apply()
    }
}
