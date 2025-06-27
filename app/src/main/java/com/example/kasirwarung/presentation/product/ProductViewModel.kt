package com.example.kasirwarung.presentation.product

import android.content.Context
import android.net.Uri
import android.widget.Toast
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.kasirwarung.data.CartItem
import com.example.kasirwarung.data.Product
import com.example.kasirwarung.data.ReportData
import com.example.kasirwarung.data.Transaction
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.toObjects
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*

class ProductViewModel : ViewModel() {
    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private val productsCollection = firestore.collection("products")
    private val transactionsCollection = firestore.collection("transactions")

    private val _allProducts = MutableStateFlow<List<Product>>(emptyList())
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    val products: StateFlow<List<Product>> =
        combine(_allProducts, _searchQuery) { products, query ->
            if (query.isBlank()) {
                products
            } else {
                products.filter { it.name.contains(query, ignoreCase = true) }
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _transactions = MutableStateFlow<List<Transaction>>(emptyList())
    val transactions: StateFlow<List<Transaction>> = _transactions.asStateFlow()

    private val _reportData = MutableStateFlow<ReportData?>(null)
    val reportData: StateFlow<ReportData?> = _reportData.asStateFlow()

    private val _isGeneratingReport = MutableStateFlow(false)
    val isGeneratingReport: StateFlow<Boolean> = _isGeneratingReport.asStateFlow()

    init {
        // Panggil listener saat ViewModel dibuat
        auth.addAuthStateListener { firebaseAuth ->
            if (firebaseAuth.currentUser != null) {
                getProducts()
                getTransactions()
            } else {
                // Bersihkan data jika pengguna logout
                _allProducts.value = emptyList()
                _transactions.value = emptyList()
            }
        }
    }

    fun onSearchQueryChange(query: String) {
        _searchQuery.value = query
    }

    private fun getProducts() {
        val userId = auth.currentUser?.uid ?: return
        productsCollection.whereEqualTo("userId", userId)
            .addSnapshotListener { snapshot, _ ->
                snapshot?.let {
                    _allProducts.value = it.toObjects(Product::class.java)
                }
            }
    }

    private fun getTransactions() {
        val userId = auth.currentUser?.uid ?: return
        transactionsCollection.whereEqualTo("userId", userId)
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, _ ->
                snapshot?.let {
                    _transactions.value = it.toObjects(Transaction::class.java)
                }
            }
    }

    // ... (fungsi add, update, delete product, checkout, dan deleteTransaction tetap sama) ...
    fun addProduct(name: String, price: Double, stock: Int) {
        viewModelScope.launch {
            val userId = auth.currentUser?.uid ?: return@launch
            val product = Product(name = name, price = price, stock = stock, userId = userId)
            productsCollection.add(product).await()
        }
    }

    fun updateProduct(product: Product) {
        viewModelScope.launch {
            productsCollection.document(product.id).set(product).await()
        }
    }

    fun deleteProduct(productId: String) {
        viewModelScope.launch {
            productsCollection.document(productId).delete().await()
        }
    }

    fun getProductById(productId: String): Product? {
        return _allProducts.value.find { it.id == productId }
    }

    fun checkout(cartItems: List<CartItem>, totalAmount: Double, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            val userId = auth.currentUser?.uid ?: run {
                onResult(false)
                return@launch
            }
            val batch = firestore.batch()
            try {
                for (item in cartItems) {
                    val productRef = productsCollection.document(item.product.id)
                    batch.update(productRef, "stock", FieldValue.increment(-item.quantity.toLong()))
                }
                val transactionRef = transactionsCollection.document()
                val transactionItems = cartItems.map {
                    mapOf(
                        "productId" to it.product.id,
                        "productName" to it.product.name,
                        "price" to it.product.price,
                        "quantity" to it.quantity
                    )
                }
                val newTransaction = Transaction(
                    id = transactionRef.id,
                    items = transactionItems,
                    totalAmount = totalAmount,
                    timestamp = Timestamp.now(),
                    userId = userId
                )
                batch.set(transactionRef, newTransaction)
                batch.commit().await()
                onResult(true)
            } catch (e: Exception) {
                onResult(false)
            }
        }
    }

    fun deleteTransactionAndRollbackStock(transaction: Transaction, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            val batch = firestore.batch()
            try {
                for (item in transaction.items) {
                    val productId = item["productId"] as String
                    val quantity = item["quantity"] as Long
                    val productRef = productsCollection.document(productId)
                    batch.update(productRef, "stock", FieldValue.increment(quantity))
                }
                val transactionRef = transactionsCollection.document(transaction.id)
                batch.delete(transactionRef)
                batch.commit().await()
                onResult(true)
            } catch (e: Exception) {
                onResult(false)
            }
        }
    }
    // PERBAIKAN UTAMA DI SINI
    fun generateReport(startDate: Date, endDate: Date) {
        viewModelScope.launch {
            _isGeneratingReport.value = true
            _reportData.value = null // Reset data lama
            val userId = auth.currentUser?.uid ?: run {
                _isGeneratingReport.value = false
                return@launch
            }

            try {
                // Ambil data transaksi dari StateFlow yang sudah ada, lalu filter berdasarkan tanggal
                val filteredTransactions = _transactions.value.filter { transaction ->
                    val transactionDate = transaction.timestamp.toDate()
                    !transactionDate.before(startDate) && !transactionDate.after(endDate)
                }

                if (filteredTransactions.isEmpty()) {
                    _reportData.value = ReportData() // Kirim data kosong jika tidak ada transaksi
                    return@launch
                }

                val totalRevenue = filteredTransactions.sumOf { it.totalAmount }
                val transactionCount = filteredTransactions.size
                val productSales = mutableMapOf<String, Long>()

                filteredTransactions.flatMap { it.items }.forEach { item ->
                    val name = item["productName"] as String
                    val quantity = item["quantity"] as Long
                    productSales[name] = productSales.getOrDefault(name, 0) + quantity
                }

                val bestSellingProducts = productSales.toList()
                    .sortedByDescending { (_, value) -> value }
                    .take(5)

                _reportData.value = ReportData(
                    totalRevenue = totalRevenue,
                    transactionCount = transactionCount,
                    bestSellingProducts = bestSellingProducts
                )
            } catch (e: Exception) {
                _reportData.value = ReportData() // Handle jika ada error lain
            } finally {
                _isGeneratingReport.value = false
            }
        }
    }

    fun exportTransactionsToCsv(context: Context, uri: Uri, transactions: List<Transaction>) {
        viewModelScope.launch {
            try {
                context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                    outputStream.bufferedWriter().use { writer ->
                        writer.append("ID Transaksi,Tanggal,Waktu,Nama Produk,Jumlah,Harga Satuan,Subtotal\n")
                        val dateFormat = SimpleDateFormat("dd-MM-yyyy", Locale.getDefault())
                        val timeFormat = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
                        transactions.forEach { transaction ->
                            val date = dateFormat.format(transaction.timestamp.toDate())
                            val time = timeFormat.format(transaction.timestamp.toDate())
                            transaction.items.forEach { item ->
                                val productName = item["productName"] as String
                                val quantity = item["quantity"] as Long
                                val price = item["price"] as Double
                                val subtotal = price * quantity
                                writer.append("\"${transaction.id}\",\"$date\",\"$time\",\"$productName\",\"$quantity\",\"$price\",\"$subtotal\"\n")
                            }
                        }
                    }
                }
                Toast.makeText(context, "Ekspor berhasil!", Toast.LENGTH_SHORT).show()
            } catch (e: IOException) {
                Toast.makeText(context, "Ekspor gagal: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }
}