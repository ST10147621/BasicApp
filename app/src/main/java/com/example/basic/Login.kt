package com.example.basic

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONException
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

class Login : AppCompatActivity() {

    private lateinit var emailEditText: EditText
    private lateinit var passwordEditText: EditText
    private lateinit var btnLogin: Button
    private lateinit var registerTextView: TextView
    var ip = connect() // Assuming you have a function to get the IP

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        emailEditText = findViewById(R.id.emailEditText)
        passwordEditText = findViewById(R.id.passwordEditText)
        btnLogin = findViewById(R.id.btnLogin)
        registerTextView = findViewById(R.id.registerTextView)

        btnLogin.setOnClickListener {
            performLogin() // Call your login function
        }

        registerTextView.setOnClickListener {
            val intent = Intent(this, ProfileActivity::class.java)
            startActivity(intent)
            Toast.makeText(this, "Redirecting to registration...", Toast.LENGTH_SHORT).show()
        }
    }

    private fun performLogin() {
        val email = emailEditText.text.toString().trim()
        val password = passwordEditText.text.toString().trim()

        // Validate email and password
        if (email.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Please fill in both fields", Toast.LENGTH_SHORT).show()
            return
        }

        // Launch coroutine to perform the login operation
        lifecycleScope.launch {
            val result = performLogins(email, password)

            // Parse the JSON response
            try {
                val jsonResponse = JSONObject(result)
                if (jsonResponse.has("error")) {
                    // Display the error message
                    Toast.makeText(this@Login, jsonResponse.getString("error"), Toast.LENGTH_SHORT).show()
                } else {
                    // Login successful
                    Toast.makeText(this@Login, "Login successful", Toast.LENGTH_SHORT).show()

                    // Retrieve role and other info if available
                    val role = jsonResponse.optString("role", "User") // Default to "User" if role is not provided
                    if(role=="admin"){

                        Toast.makeText(this@Login, "welcome  "+role, Toast.LENGTH_SHORT).show()
                        // Save the user email and role in session
                        val userSession = user_active(this@Login)
                        userSession.saveUserEmail(email)
                        // Start the ShopActivity
                        val intent = Intent(this@Login, admin::class.java)
                        intent.putExtra("role", role) // Pass role to the next activity if needed
                        startActivity(intent)
                        finish()
                    }else{
                        // Save the user email and role in session
                        val userSession = user_active(this@Login)
                        userSession.saveUserEmail(email)
                        // Start the ShopActivity
                        val intent = Intent(this@Login, ShopActivity::class.java)
                        intent.putExtra("uaer", role) // Pass role to the next activity if needed
                        startActivity(intent)
                        finish()
                    }







                    // Reset fields after processing
                    emailEditText.text.clear()
                    passwordEditText.text.clear()
                }
            } catch (e: JSONException) {
                Toast.makeText(this@Login, "Error parsing response", Toast.LENGTH_SHORT).show()
            }

        }
    }

    // Login function
    suspend fun performLogins(email: String, password: String): String {
        return withContext(Dispatchers.IO) {
            try {
                val url = URL("http://${ip.IP()}/basic/login_app.php")
                val connection = url.openConnection() as HttpURLConnection
                connection.requestMethod = "POST"
                connection.doOutput = true

                val postData = "email=${URLEncoder.encode(email, "UTF-8")}" +
                        "&password=${URLEncoder.encode(password, "UTF-8")}"

                val writer = OutputStreamWriter(connection.outputStream)
                writer.write(postData)
                writer.flush()

                val responseCode = connection.responseCode
                return@withContext if (responseCode == HttpURLConnection.HTTP_OK) {
                    val reader = BufferedReader(InputStreamReader(connection.inputStream))
                    val response = StringBuilder()
                    var line: String?
                    while (reader.readLine().also { line = it } != null) {
                        response.append(line)
                    }
                    reader.close()

                    // Log the raw response for debugging
                    Log.d("LoginResponse", response.toString())

                    response.toString()
                } else {
                    "Error: $responseCode"
                }
            } catch (e: Exception) {
                e.printStackTrace()
                "Error: ${e.message}"
            }
        }
    }
}
