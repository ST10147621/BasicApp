package com.example.basic

import android.content.Context
import android.content.SharedPreferences

class user_active(context: Context) {

        private val sharedPreferences: SharedPreferences = context.getSharedPreferences("user_session", Context.MODE_PRIVATE)
        private val editor: SharedPreferences.Editor = sharedPreferences.edit()

        fun saveUserEmail(email: String) {
            editor.putString("user_email", email).apply()
        }

        fun getUserEmail(): String? {
            return sharedPreferences.getString("user_email", null)
        }

        fun clearSession() {
            editor.clear().apply()
        }
}