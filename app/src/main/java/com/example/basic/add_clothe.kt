package com.example.basic

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.provider.MediaStore
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL

class add_clothe : AppCompatActivity() {

    private val IMAGE_REQUEST_CODE = 100
    private var imageUri: Uri? = null
    private var ip = connect()

    private lateinit var inputItemName: EditText
    private lateinit var inputItemPrice: EditText
    private lateinit var inputStockNumber: EditText
    private lateinit var uploadImageButton: Button
    private lateinit var submitButton: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_clothe)

        inputItemName = findViewById(R.id.input_item_name)
        inputItemPrice = findViewById(R.id.input_item_price)
        inputStockNumber = findViewById(R.id.input_stock_number)
        uploadImageButton = findViewById(R.id.upload_image_button)
        submitButton = findViewById(R.id.submit_button)

        uploadImageButton.setOnClickListener {
            openImageChooser()
        }

        submitButton.setOnClickListener {
            val name = inputItemName.text.toString()
            val price = inputItemPrice.text.toString().toDoubleOrNull()
            val stock = inputStockNumber.text.toString().toIntOrNull()

            if (name.isNotEmpty() && price != null && stock != null && imageUri != null) {
                readyStore(name, price, stock)
            } else {
                Toast.makeText(this, "Please fill all fields and select an image", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun openImageChooser() {
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        startActivityForResult(intent, IMAGE_REQUEST_CODE)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == IMAGE_REQUEST_CODE && resultCode == Activity.RESULT_OK) {
            imageUri = data?.data
            uploadImageButton.text = "Image Selected"
        }
    }
    private fun readyStore(name: String, price: Double, stock: Int) {
        val thread = Thread {
            try {
                val inputStream: InputStream? = contentResolver.openInputStream(imageUri!!)
                val url = URL("http://${ip.IP()}/basic/stores.php")
                val connection = url.openConnection() as HttpURLConnection
                connection.requestMethod = "POST"
                connection.doOutput = true

                // Unique boundary for separating form data parts
                val boundary = "Boundary-${System.currentTimeMillis()}"
                connection.setRequestProperty("Content-Type", "multipart/form-data; boundary=$boundary")

                val outputStream = connection.outputStream
                val writer = outputStream.bufferedWriter()

                // Write form data fields
                writer.apply {
                    write("--$boundary\r\n")
                    write("Content-Disposition: form-data; name=\"title\"\r\n\r\n")
                    write("$name\r\n")

                    write("--$boundary\r\n")
                    write("Content-Disposition: form-data; name=\"price\"\r\n\r\n")
                    write("$price\r\n")

                    write("--$boundary\r\n")
                    write("Content-Disposition: form-data; name=\"total_added\"\r\n\r\n")
                    write("$stock\r\n")

                    // Image file data
                    write("--$boundary\r\n")
                    write("Content-Disposition: form-data; name=\"image_url\"; filename=\"${System.currentTimeMillis()}.jpg\"\r\n")
                    write("Content-Type: image/jpeg\r\n\r\n")
                    flush()
                }

                // Write image content
                inputStream?.copyTo(outputStream)
                outputStream.flush()

                writer.write("\r\n--$boundary--\r\n")
                writer.flush()

                // Close all streams
                inputStream?.close()
                writer.close()
                outputStream.close()

                // Handle response
                val responseCode = connection.responseCode
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    runOnUiThread {
                        Toast.makeText(this, "Item added successfully", Toast.LENGTH_SHORT).show()
                        clearForm()
                    }
                } else {
                    runOnUiThread {
                        Toast.makeText(this, "Failed to add item: $responseCode", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                runOnUiThread {
                    Toast.makeText(this, "Error occurred: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
        thread.start()
    }


    private fun clearForm() {
        inputItemName.text.clear()
        inputItemPrice.text.clear()
        inputStockNumber.text.clear()
        uploadImageButton.text = "Choose Image"
        imageUri = null
    }

    fun onBackPressed(view: View) {
        // Open the shop page
        val openShop = Intent(this, admin::class.java)
        startActivity(openShop)
    }
}
