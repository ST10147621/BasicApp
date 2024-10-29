package com.example.basic

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

class ProfileActivity : AppCompatActivity() {

    private lateinit var userEditTextReg: EditText
    private lateinit var emailEditText: EditText
    private lateinit var regPasswordEditText: EditText
    private lateinit var retypePasswordEditText: EditText
    private lateinit var btnCreate: Button
    private lateinit var alreadyUser: TextView
    var ip = connect() // Assuming you have a function to get the IP

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_profile)

        // Initialize views
        userEditTextReg = findViewById(R.id.userEditTextReg)
        emailEditText = findViewById(R.id.emailEditText)
        regPasswordEditText = findViewById(R.id.phone)
        retypePasswordEditText = findViewById(R.id.retypePasswordEditText)
        btnCreate = findViewById(R.id.btnCreate)
        alreadyUser = findViewById(R.id.clickToNextPage)

        // Set up button listeners
        btnCreate.setOnClickListener {
            registerUser()
        }

        alreadyUser.setOnClickListener {
            val intent = Intent(this, Login::class.java)
            startActivity(intent)

            Toast.makeText(this@ProfileActivity,"Redirecting to login...",Toast.LENGTH_SHORT).show()
        }
    }

    // Method to register the user
    private fun registerUser() {
        val username = userEditTextReg.text.toString().trim()
        val email = emailEditText.text.toString().trim()
        val password = regPasswordEditText.text.toString().trim()
        val retypePassword = retypePasswordEditText.text.toString().trim()

        // Validate input fields
        if (username.isEmpty() || email.isEmpty() || password.isEmpty() || retypePassword.isEmpty()) {
            Toast.makeText(this@ProfileActivity,"Please fill in all fields", Toast.LENGTH_SHORT).show()
            return
        }

        // Launch coroutine to perform the registration operation
        lifecycleScope.launch {
            val result = sendRegistrationData(this@ProfileActivity,username, email, password, retypePassword)

        }
    }
    private fun sendRegistrationData(context: Context, name: String, email: String, phone: String, password: String) {
        CoroutineScope(Dispatchers.Main).launch {
            val result = withContext(Dispatchers.IO) {
                try {
                    val url = URL("http://${ip.IP()}/basic/new_user.php")
                    val connection = url.openConnection() as HttpURLConnection
                    connection.requestMethod = "POST"
                    connection.doOutput = true
                    connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded")

                    // Prepare POST data
                    val postData = "name=${URLEncoder.encode(name, "UTF-8")}" +
                            "&email=${URLEncoder.encode(email, "UTF-8")}" +
                            "&phone=${URLEncoder.encode(phone.toString(), "UTF-8")}" +
                            "&password=${URLEncoder.encode(password, "UTF-8")}"

                    // Send the POST request
                    connection.outputStream.use { it.write(postData.toByteArray()) }

                    // Read the response
                    val reader = BufferedReader(InputStreamReader(connection.inputStream))
                    val response = reader.readText()
                    reader.close()

                    // Debugging: print the response
                    println(response)

                    // Check if the response is successful
                    if (connection.responseCode == HttpURLConnection.HTTP_OK) {
                        response
                    } else {
                        "Error: ${connection.responseCode}"
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                    "Error: ${e.message}"
                }
            }

            // Display the result or error as a toast on the main thread
            if (result.startsWith("Error")) {
                Toast.makeText(context, result, Toast.LENGTH_LONG).show()
            } else {
                Toast.makeText(context, "Registration successful!", Toast.LENGTH_LONG).show()

                val intent = Intent(this@ProfileActivity, Login::class.java)
                startActivity(intent)

                Toast.makeText(this@ProfileActivity,"Redirecting to login...",Toast.LENGTH_SHORT).show()
            }
        }
    }



}
