package com.example.basic

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.text.Editable
import android.text.InputType
import android.text.TextUtils
import android.text.TextWatcher
import android.util.Log
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.HorizontalScrollView
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.Spinner
import android.widget.TableLayout
import android.widget.TableRow
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import com.bumptech.glide.Glide

class ShopActivity : AppCompatActivity() {

    data class Product(
        val image_url: String,
        val title: String,
        val price: String,
        val total_added:String
    )
    // Data class for user billing information
    data class UserBillingInfo(
        val id: Int,
        val user_id: Int,
        val full_name: String,
        val email: String,
        val card_number: String,
        val exp_month: String,
        val exp_year: String,
        val cvv: String
    )data class PaymentFields(
        val fullNameField: EditText,
        val emailField: EditText,
        val cardNumberField: EditText,
        val expiryDateField: EditText,
        val cvvField: EditText
    )

    private lateinit var sessionManager: SessionManager
    var ip = connect()
    private lateinit var productAdapter: ProductAdapter

    private val cartItems: MutableList<CartItem> by lazy {
        sessionManager.getCartItems().toMutableList()
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_shop)
        sessionManager = SessionManager(this)

        val recyclerView: RecyclerView = findViewById(R.id.recyclerView)
        recyclerView.layoutManager = GridLayoutManager(this, 2)

        productAdapter = ProductAdapter(emptyList(), listOf("S", "M", "L"))
        recyclerView.adapter = productAdapter

        // Fetch the product list from the server
        fetchProductList()
    }

    // Function to fetch product list from the server
    private fun fetchProductList() {
        CoroutineScope(Dispatchers.Main).launch {
            val products = withContext(Dispatchers.IO) {
                try {
                    val url = URL("http://${ip.IP()}/basic/get_products.php")
                    val connection = url.openConnection() as HttpURLConnection
                    connection.requestMethod = "POST"
                    connection.doOutput = true
                    connection.setRequestProperty("Content-Type", "application/json; utf-8")

                    // Create JSON object to send (if you need to send any parameters)
                    val jsonInputString = "{}" // Modify if parameters are needed
                    connection.outputStream.write(jsonInputString.toByteArray())

                    // Read the response
                    val reader = BufferedReader(InputStreamReader(connection.inputStream))
                    val jsonResponse = reader.readText()
                    reader.close()

                    // Print JSON response for debugging
                    println(jsonResponse)

                    // Parse the JSON response into Product objects
                    Gson().fromJson(jsonResponse, Array<Product>::class.java).toList()
                } catch (e: Exception) {
                    e.printStackTrace()
                    emptyList<Product>()
                }
            }

            // Update the UI with the product list
            updateUIWithProducts(products)
        }
    }

    // Function to update RecyclerView with fetched products
    private fun updateUIWithProducts(products: List<Product>) {
        productAdapter.updateData(products)
        productAdapter.notifyDataSetChanged()
    }

    private inner class ProductAdapter(
        private var products: List<Product>,
        private val sizes: List<String>
    ) : RecyclerView.Adapter<ProductAdapter.ProductViewHolder>() {

        inner class ProductViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            val cardView: CardView = itemView as CardView
        }

        // Method to update product list in the adapter
        fun updateData(newProducts: List<Product>) {
            products = newProducts
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ProductViewHolder {
            val cardView = CardView(parent.context).apply {
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                )
                setCardElevation(4f)
                setCardBackgroundColor(Color.WHITE)
                setContentPadding(8, 8, 8, 8)
            }

            val linearLayout = LinearLayout(parent.context).apply {
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                )
                orientation = LinearLayout.VERTICAL
                setPadding(8, 8, 8, 8)
            }

            val imageView = ImageView(parent.context).apply {
                layoutParams = LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    500
                )
                scaleType = ImageView.ScaleType.CENTER_CROP
            }

            val titleView = TextView(parent.context).apply {
                layoutParams = LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                ).apply {
                    marginStart = 4
                    topMargin = 4
                }
                setTextColor(Color.BLACK)
                setTypeface(null, Typeface.BOLD)
                gravity = Gravity.START
            }

            val priceView = TextView(parent.context).apply {
                layoutParams = LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                )
                setTextColor(Color.DKGRAY)
                gravity = Gravity.START
            }

            // Set up the spinner with sizes
            val sizeSpinner = Spinner(parent.context).apply {
                adapter = ArrayAdapter(parent.context, android.R.layout.simple_spinner_item, sizes).apply {
                    setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                }
                layoutParams = LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                )
            }

            // Button to add to cart
            val addButton = Button(parent.context).apply {
                text = "Add to Cart"
                setOnClickListener {
                    // Get selected size from the spinner
                    val selectedSize = sizeSpinner.selectedItem?.toString() ?: "No Size Selected"
                    // Get product title and price
                    val productTitle = titleView.text.toString()
                    val productPrice = priceView.text.toString()
                    addToCart(productTitle, selectedSize, productPrice.toDouble())
                    Toast.makeText(parent.context, "Added to Cart - Product: $productTitle, Price: $productPrice, Size: $selectedSize", Toast.LENGTH_SHORT).show()
                }
            }

            // Button to view cart
            val viewButton = Button(parent.context).apply {
                text = "View Cart"
                setOnClickListener {
                    showCartDialog(parent.context) // Call the function to show cart dialog
                }
            }

            // Add views to LinearLayout and then to CardView
            linearLayout.addView(imageView)
            linearLayout.addView(titleView)
            linearLayout.addView(priceView)
            linearLayout.addView(sizeSpinner)
            linearLayout.addView(addButton)
            linearLayout.addView(viewButton)
            cardView.addView(linearLayout)

            return ProductViewHolder(cardView)
        }

        override fun onBindViewHolder(holder: ProductViewHolder, position: Int) {
            val product = products[position]
            val layout = holder.cardView.getChildAt(0) as LinearLayout
            val imageView = layout.getChildAt(0) as ImageView
            val titleView = layout.getChildAt(1) as TextView
            val priceView = layout.getChildAt(2) as TextView

            // Construct the image URL
            val imageUrl = "http://${ip.IP()}/basic/${product.image_url}"
            //Toast.makeText(this@ShopActivity, ""+product.image_url, Toast.LENGTH_SHORT).show()

            // Log the image URL to the console
            Log.d("ProductImage", "Loading image from URL: $imageView")

            // Use Glide to load the image from the URL
            Glide.with(holder.itemView.context)
                .load(imageUrl)
                .into(imageView)

            // Set the title and price
            if(product.total_added=="0"){

                titleView.text = product.title +" [ out of stock ]"
            }else{

                titleView.text = product.title
            }

            priceView.text = product.price
        }


        override fun getItemCount(): Int {
            return products.size
        }

        private fun addToCart(title: String, size: String, price: Double) {
            if (title.isNotBlank() && size.isNotBlank()) {
                val existingItem = cartItems.find { it.title == title && it.size == size }
                if (existingItem != null) {
                    // Update existing item
                    existingItem.quantity++
                    existingItem.totalPrice += price
                } else {
                    var prices = price
                    // Add new item
                    cartItems.add(CartItem(title, size, 1, price,prices))
                }
                // Save cart items
                sessionManager.saveCartItems(cartItems)
            } else {
                // Handle invalid input
                Toast.makeText(this@ShopActivity, "Invalid item details", Toast.LENGTH_SHORT).show()
            }
        }



        private fun showCartDialog(context: Context) {
            // Create an AlertDialog builder
            val builder = AlertDialog.Builder(context)
            builder.setTitle("Cart Items")

            // If cart is empty, show a message
            if (cartItems.isEmpty()) {
                builder.setMessage("Your cart is empty.")
                builder.setPositiveButton("OK", null)
            } else {
                // Create a TableLayout to display cart items in a grid manner
                val tableLayout = TableLayout(context).apply {
                    setPadding(16, 16, 16, 16)
                }

                // Add table headers
                val headerRow = TableRow(context).apply {
                    setPadding(8, 8, 8, 8)
                }

                // Headers: Title, Size, Qty, Total, Edit (for quantity)
                val headers = arrayOf("Title", "Size", "Qty", "Total", "Edit")
                headers.forEach { headerText ->
                    val header = TextView(context).apply {
                        text = headerText
                        setTypeface(null, Typeface.BOLD)
                        setTextColor(Color.BLACK)
                        layoutParams = TableRow.LayoutParams(TableRow.LayoutParams.WRAP_CONTENT)
                    }
                    headerRow.addView(header)
                }

                tableLayout.addView(headerRow)

                // Loop through cart items and add them as rows to the table layout
                cartItems.forEach { cartItem ->
                    val itemRow = TableRow(context).apply {
                        setPadding(8, 8, 8, 8)
                    }

                    // Create TextViews for Title, Size, Qty, and Total
                    val titleView = TextView(context).apply {
                        text = cartItem.title
                        maxWidth = 200 // Set max width to prevent overflow
                        ellipsize = TextUtils.TruncateAt.END // Add ellipsis for long text
                        isSingleLine = true // Keep title in a single line
                    }
                    val sizeView = TextView(context).apply {
                        text = cartItem.size
                    }
                    val quantityView = TextView(context).apply {
                        text = cartItem.quantity.toString()
                    }
                    val totalView = TextView(context).apply {
                        text = "R${cartItem.totalPrice}"
                    }

                    // Create buttons for increasing/decreasing quantity
                    val minusButton = Button(context).apply {
                        text = "-"
                        setOnClickListener {
                            if (cartItem.quantity > 1) {
                                cartItem.quantity--
                                cartItem.totalPrice = cartItem.pricePerUnit * cartItem.quantity
                                sessionManager.saveCartItems(cartItems)
                                showCartDialog(context) // Refresh dialog to reflect changes
                            }
                        }
                    }

                    val plusButton = Button(context).apply {
                        text = "+"
                        setOnClickListener {
                            cartItem.quantity++
                            cartItem.totalPrice = cartItem.pricePerUnit * cartItem.quantity
                            sessionManager.saveCartItems(cartItems)
                            showCartDialog(context) // Refresh dialog to reflect changes
                        }
                    }

                    // Add the views to the row
                    itemRow.addView(titleView)
                    itemRow.addView(sizeView)
                    itemRow.addView(quantityView)
                    itemRow.addView(totalView)

                    // Add minus and plus buttons in a horizontal layout
                    val editLayout = LinearLayout(context).apply {
                        orientation = LinearLayout.HORIZONTAL
                        setPadding(8, 8, 8, 8)
                    }
                    editLayout.addView(minusButton)
                    editLayout.addView(plusButton)

                    itemRow.addView(editLayout)

                    // Add the row to the table layout
                    tableLayout.addView(itemRow)
                }

                // Calculate total price of all items
                val totalPrice = cartItems.sumOf { it.totalPrice }

                // Create a row for displaying the total price
                val totalRow = TableRow(context).apply {
                    setPadding(8, 8, 8, 8)
                }

                val totalLabel = TextView(context).apply {
                    text = "Total: "
                    setTypeface(null, Typeface.BOLD)
                }

                val totalValue = TextView(context).apply {
                    text = "R$totalPrice"
                    setTypeface(null, Typeface.BOLD)
                }

                totalRow.addView(totalLabel)
                totalRow.addView(totalValue)

                // Add the total row to the table layout
                tableLayout.addView(totalRow)

                // Create a horizontal ScrollView for the TableLayout
                val horizontalScrollView = HorizontalScrollView(context).apply {
                    addView(tableLayout)
                }

                // Create a layout to hold the checkout button
                val buttonLayout = LinearLayout(context).apply {
                    orientation = LinearLayout.VERTICAL
                    setPadding(16, 16, 16, 16)
                }

                // Create a Checkout button
                val checkoutButton = Button(context).apply {
                    text = "Checkout"
                    setOnClickListener {
                        // Handle checkout process here
                        Toast.makeText(context, "Proceeding to checkout...", Toast.LENGTH_SHORT).show()

                        // Show the payment form
                        showPaymentForm(context, totalPrice)
                    }
                }

                buttonLayout.addView(checkoutButton)

                // Set the custom view to the dialog (scrollable table + buttons)
                val containerLayout = LinearLayout(context).apply {
                    orientation = LinearLayout.VERTICAL
                }
                containerLayout.addView(horizontalScrollView) // Add horizontal scroll view
                containerLayout.addView(buttonLayout) // Add button layout

                builder.setView(containerLayout)

                // Add an "OK" button to close the dialog
                builder.setPositiveButton("OK", null)

                // Optionally add a button to clear the cart
                builder.setNegativeButton("Clear Cart") { dialog, _ ->
                    cartItems.clear()
                    sessionManager.saveCartItems(cartItems) // Update session
                    Toast.makeText(context, "Cart cleared.", Toast.LENGTH_SHORT).show()
                    dialog.dismiss()
                }
            }

            // Show the dialog
            builder.create().show()
        }







        private fun showPaymentForm(context: Context, totalPrice: Double, initialEmail: String? = null) {
            val paymentFormBuilder = AlertDialog.Builder(context)
            paymentFormBuilder.setTitle("Payment Details")
            val formLayout = LinearLayout(context).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(16, 16, 16, 16)
            }

// Create fields
            val fullNameField = EditText(context).apply {
                hint = "Full Name"
                inputType = InputType.TYPE_CLASS_TEXT
            }
            formLayout.addView(fullNameField)

            val emailField = EditText(context).apply {
                hint = "Email"
                inputType = InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS
                setText(initialEmail)  // Set initial email if provided
            }
            formLayout.addView(emailField)

            val cardNumberField = EditText(context).apply {
                hint = "Card Number"
                inputType = InputType.TYPE_CLASS_TEXT
            }
            formLayout.addView(cardNumberField)

            val expiryDateField = EditText(context).apply {
                hint = "Expiry Date (MM/YY)"
                inputType = InputType.TYPE_CLASS_TEXT
            }
            formLayout.addView(expiryDateField)

            val cvvField = EditText(context).apply {
                hint = "CVV"
                inputType = InputType.TYPE_CLASS_TEXT
            }
            formLayout.addView(cvvField)

            //formLayout.addView(cvvField)

            paymentFormBuilder.setView(formLayout)

            // Load initial billing info automatically if initialEmail is provided
            initialEmail?.let { email ->
                loadBillingInfo(context, email, fullNameField, cardNumberField, expiryDateField, cvvField)
            }

            val userSession = user_active(this@ShopActivity)
            val userEmail = userSession.getUserEmail()?.toString()
             loadBillingInfo(context, userEmail.toString(), fullNameField, cardNumberField, expiryDateField, cvvField)
            emailField.setText(userEmail)
            // Payment button to proceed with the entered details
            paymentFormBuilder.setPositiveButton("Pay") { _, _ ->
                showConfirmationDialog(context, emailField.text.toString(), totalPrice)
            }
            paymentFormBuilder.setNegativeButton("Cancel", null)

            paymentFormBuilder.create().show()
        }

        private fun loadBillingInfo(
            context: Context,
            email: String,
            fullNameField: EditText,
            cardNumberField: EditText,
            expiryDateField: EditText,
            cvvField: EditText
        ) {
            CoroutineScope(Dispatchers.Main).launch {

                val billingInfo = withContext(Dispatchers.IO) {
                    try {
                        val url = URL("http://${ip.IP()}/basic/get_billing_info.php")
                        val connection = url.openConnection() as HttpURLConnection
                        connection.requestMethod = "POST"
                        connection.doOutput = true
                        connection.setRequestProperty("Content-Type", "application/json; utf-8")

                        val jsonInputString = "{\"email\": \"$email\"}"
                        connection.outputStream.use { it.write(jsonInputString.toByteArray()) }

                        val reader = BufferedReader(InputStreamReader(connection.inputStream))
                        val jsonResponse = reader.readText()
                        reader.close()

                        Gson().fromJson(jsonResponse, UserBillingInfo::class.java)
                    } catch (e: Exception) {
                        e.printStackTrace()
                        null
                    }
                }

                billingInfo?.let {
                    fullNameField.setText(it.full_name)
                    cardNumberField.setText(it.card_number)
                    expiryDateField.setText("${it.exp_month}/${it.exp_year}")
                    cvvField.setText(it.cvv)
                } ?: Toast.makeText(context, "No billing information found for $email", Toast.LENGTH_SHORT).show()
            }
        }



        // Function to show payment confirmation dialog
        private fun showConfirmationDialog(context: Context, email: String, totalPrice: Double) {
            val confirmationBuilder = AlertDialog.Builder(context)
            confirmationBuilder.setTitle("Confirm Payment")

            // Confirmation message
            confirmationBuilder.setMessage("Confirm your payment details:\n\nEmail: $email\nTotal: R$totalPrice")

            confirmationBuilder.setPositiveButton("Confirm") { _, _ ->


                // Ensure email is not empty
                if (email.isNotEmpty()) {
                    lifecycleScope.launch {
                        // Call storeAdded and pass email and products
                        val result = storeAdded(email, cartItems)
                        Toast.makeText(this@ShopActivity, result, Toast.LENGTH_SHORT).show()
                        cartItems.clear()
                        sessionManager.saveCartItems(cartItems) // Update session
                        val intent = Intent(this@ShopActivity,ShopActivity::class.java)
                        startActivity(intent)
                        Toast.makeText(context, "Cart cleared.", Toast.LENGTH_SHORT).show()
                        // Payment processing logic can be placed here
                        Toast.makeText(context, "Payment successful!", Toast.LENGTH_SHORT).show()
                    }
                }else {
                        Toast.makeText(context, "Please enter a valid email.", Toast.LENGTH_SHORT)
                            .show()
                    }
                }


            confirmationBuilder.setNegativeButton("Cancel", null)
            confirmationBuilder.create().show()
        }
        suspend fun storeAdded(email: String, products: MutableList<CartItem>): String {
            return withContext(Dispatchers.IO) {
                try {
                    // Open a single connection for all products
                    val url = URL("http://${ip.IP()}/basic/order_app.php")
                    val connection = url.openConnection() as HttpURLConnection
                    connection.requestMethod = "POST"
                    connection.doOutput = true
                    connection.setRequestProperty("Content-Type", "application/json; utf-8")

                    // Create a JSON object with email and products
                    val json = JSONObject().apply {
                        put("email", email)
                        put("products", JSONArray().apply {
                            products.forEach { product ->
                                put(JSONObject().apply {
                                    put("title", product.title)
                                    put("size", product.size)
                                    put("price", product.pricePerUnit)
                                    put("quantity", product.quantity)
                                })
                            }
                        })
                    }

                    // Log the JSON data being sent
                    println("Sending JSON: $json")

                    // Write the JSON data to the output stream
                    OutputStreamWriter(connection.outputStream).use { writer ->
                        writer.write(json.toString())
                        writer.flush()
                    }

                    // Get the response code and handle the response
                    val responseCode = connection.responseCode
                    println("Response Code: $responseCode")
                   // showCartDialog(this@ShopActivity)
                    // Handle response based on the response code
                    return@withContext if (responseCode == HttpURLConnection.HTTP_OK) {
                        BufferedReader(InputStreamReader(connection.inputStream)).use { reader ->
                            val response = reader.readText()
                            println("Response from server: $response")
                            "Success: $response"

                        }

                    } else {
                        BufferedReader(InputStreamReader(connection.errorStream)).use { reader ->
                            val errorResponse = reader.readText()
                            println("Error Response from server: $errorResponse")
                            "Error: $responseCode - $errorResponse"
                        }
                    }

                } catch (e: Exception) {
                    e.printStackTrace()
                    return@withContext "Error: ${e.message}"
                }
            }
        }







    }


    // Data class for Order
    data class Order(
        @SerializedName("order_id") val orderId: Int,
        @SerializedName("email") val email: String,
        @SerializedName("product_name") val productName: String,
        @SerializedName("product_image") val productImage: String,
        @SerializedName("product_size") val productSize: String,
        @SerializedName("price") val price: Double,
        @SerializedName("quantity") val quantity: Int,
        @SerializedName("total") val total: Double,
        @SerializedName("order_date") val orderDate: String
    )

    // Function to handle the About Us button click
    fun aboutUs(view: View) {
        // Save the user email in session
        val userSession = user_active(this)
        val userEmail = userSession.getUserEmail()?.toString()
       // Toast.makeText(this, userEmail, Toast.LENGTH_SHORT).show()
        if (!userEmail.isNullOrBlank()) {
            // Pass the email to the method
            fetchUserOrders(userEmail)
        } else {

            Toast.makeText(this, "Please login first", Toast.LENGTH_SHORT).show()
        }
    }

    // Function to fetch user orders from the server
    private fun fetchUserOrders(email: String) {
        CoroutineScope(Dispatchers.Main).launch {
            val orders = withContext(Dispatchers.IO) {
                try {
                    val url = URL("http://${ip.IP()}/basic/get_order_app.php")
                    val connection = url.openConnection() as HttpURLConnection
                    connection.requestMethod = "POST"
                    connection.doOutput = true
                    connection.setRequestProperty("Content-Type", "application/json; utf-8")

                    // Create JSON object to send
                    val jsonInputString = "{\"email\": \"$email\"}"
                    connection.outputStream.write(jsonInputString.toByteArray())

                    // Read the response
                    val reader = BufferedReader(InputStreamReader(connection.inputStream))
                    val jsonResponse = reader.readText()
                    reader.close()

                    // Print the JSON response for debugging
                    println(jsonResponse)

                    // Parse the JSON response into Order objects
                    Gson().fromJson(jsonResponse, Array<Order>::class.java).toList()
                } catch (e: Exception) {
                    e.printStackTrace()
                    emptyList<Order>()
                }
            }

            // Display the orders in an alert dialog
            displayOrdersDialog(orders)
        }
    }

    // Function to display the orders in an alert dialog
    fun displayOrdersDialog(orders: List<Order>) {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("User Orders")

        // Create a ScrollView programmatically
        val scrollView = ScrollView(this)
        val linearLayout = LinearLayout(this)
        linearLayout.orientation = LinearLayout.VERTICAL

        // Create a string to display all orders
        val ordersDisplay = StringBuilder()
        for (order in orders) {
            ordersDisplay.append("Order ID: ${order.orderId}\n")
            ordersDisplay.append("Product Name: ${order.productName ?: "N/A"}\n")
            ordersDisplay.append("Quantity: ${order.quantity}\n")
            ordersDisplay.append("Total: R${order.total}\n")
            ordersDisplay.append("Order Date: ${order.orderDate ?: "N/A"}\n\n")
        }

        // Create a TextView for displaying orders
        val textViewOrders = TextView(this)
        textViewOrders.text = ordersDisplay.toString().ifEmpty { "No orders found." }
        textViewOrders.textSize = 16f
        textViewOrders.setPadding(16, 16, 16, 16)

        // Add the TextView to the LinearLayout
        linearLayout.addView(textViewOrders)

        // Add the LinearLayout to the ScrollView
        scrollView.addView(linearLayout)

        // Set the ScrollView as the dialog's view
        builder.setView(scrollView)
            .setPositiveButton("OK") { dialog, _ -> dialog.dismiss() }

        builder.create().show()
    }

    //when the button shop is clicked
    fun shops(view: View) {
        //open the shop page
        val open_shop = Intent(this, ShopActivity::class.java);
        startActivity(open_shop)
    }
    //when the button shop is clicked
    fun home(view: View) {
        //open the shop page
        val open_shop = Intent(this, MainActivity::class.java);
        startActivity(open_shop)
    }



    fun contactUs(view: View) {

        //open the shop page
        val open_ContactUs = Intent(this, ContactUsActivity::class.java);
        startActivity(open_ContactUs)
    }

    // Function to handle the Profile button click
    fun profile(view: View) {
        // Get the user session
        val userSession = user_active(this)
        val userEmail = userSession.getUserEmail()?.toString()

        // Check if the user session is not empty
        if (!userEmail.isNullOrBlank()) {
            // Ask the user if they want to log out
            showLogoutConfirmationDialog()
        } else {
            // Open the profile page if session is empty
            val openProfileIntent = Intent(this, ProfileActivity::class.java)
            startActivity(openProfileIntent)
        }
    }

    // Function to show the logout confirmation dialog
    private fun showLogoutConfirmationDialog() {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Logout Confirmation")
        builder.setMessage("Are you sure you want to logout?")

        builder.setPositiveButton("Yes") { dialog, _ ->
            // Clear the session
            clearUserSession()
            dialog.dismiss()
        }

        builder.setNegativeButton("No") { dialog, _ ->
            dialog.dismiss()
        }

        val alertDialog = builder.create()
        alertDialog.show()
    }

    // Function to clear the user session (Implement this as needed)
    private fun clearUserSession() {
        // Code to clear the user session
        val userSession = user_active(this)
        userSession.clearSession()
        Toast.makeText(this, "you have successfully logged-out", Toast.LENGTH_SHORT).show()
        // Open the profile page if session is empty
        val openProfileIntent = Intent(this, MainActivity::class.java)
        startActivity(openProfileIntent)


    }

}
