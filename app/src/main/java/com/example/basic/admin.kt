package com.example.basic

import android.app.DatePickerDialog
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.util.Log
import android.util.TypedValue
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Button
import android.widget.DatePicker
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TableLayout
import android.widget.TableRow
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import java.util.Calendar

class admin : AppCompatActivity() {

    private lateinit var sessionManager: SessionManager
    var ip = connect()

    private val cartItems: MutableList<CartItem> by lazy {
        sessionManager.getCartItems().toMutableList()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        sessionManager = SessionManager(this)
        setContentView(R.layout.activity_admin)
        val userSession = user_active(this@admin)
        val userEmail = userSession.getUserEmail()?.toString()
    }




    fun users(view: View) {

        //open the shop page
        val open_shop = Intent(this, adminManageUsers::class.java);
        startActivity(open_shop)

    }

    fun add_clothe(view: View) {

        //open the shop page
        val open_shop = Intent(this, add_clothe()::class.java);
        startActivity(open_shop)

    }

    fun out(view: View) {

        // Code to clear the user session
        val userSession = user_active(this)
        userSession.clearSession()
        Toast.makeText(this, "you have successfully logged-out", Toast.LENGTH_SHORT).show()
        // Open the profile page if session is empty
        val openProfileIntent = Intent(this, MainActivity::class.java)
        startActivity(openProfileIntent)



    }

    fun viewss(view: View) {

        //open the shop page
        val open_shop = Intent(this, viewing()::class.java);
        startActivity(open_shop)

    }


}

