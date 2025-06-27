package com.example.kasirwarung.presentation.product

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.kasirwarung.data.Product

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditProductScreen(
    navController: NavController,
    productViewModel: ProductViewModel,
    productId: String?
) {
    val context = LocalContext.current
    val isEditMode = productId != null

    var productState by remember { mutableStateOf<Product?>(null) }
    var name by remember { mutableStateOf("") }
    var price by remember { mutableStateOf("") }
    var stock by remember { mutableStateOf("") }

    LaunchedEffect(key1 = productId) {
        if (isEditMode) {
            productState = productViewModel.getProductById(productId!!)
            productState?.let {
                name = it.name
                price = it.price.toString()
                stock = it.stock.toString()
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (isEditMode) "Edit Produk" else "Tambah Produk Baru") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Kembali")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Nama Produk") },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(16.dp))
            OutlinedTextField(
                value = price,
                onValueChange = { price = it },
                label = { Text("Harga Produk") },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
            )
            Spacer(modifier = Modifier.height(16.dp))
            OutlinedTextField(
                value = stock,
                onValueChange = { stock = it },
                label = { Text("Stok Produk") },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
            )
            Spacer(modifier = Modifier.height(32.dp))
            Button(
                onClick = {
                    val priceDouble = price.toDoubleOrNull()
                    val stockInt = stock.toIntOrNull()

                    if (name.isBlank() || priceDouble == null || stockInt == null) {
                        Toast.makeText(context, "Semua kolom harus diisi dengan benar", Toast.LENGTH_SHORT).show()
                        return@Button
                    }

                    if (isEditMode) {
                        val updatedProduct = productState!!.copy(
                            name = name,
                            price = priceDouble,
                            stock = stockInt
                        )
                        productViewModel.updateProduct(updatedProduct)
                        Toast.makeText(context, "Produk berhasil diperbarui", Toast.LENGTH_SHORT).show()
                    } else {
                        productViewModel.addProduct(name, priceDouble, stockInt)
                        Toast.makeText(context, "Produk berhasil ditambahkan", Toast.LENGTH_SHORT).show()
                    }
                    navController.popBackStack()
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Simpan Perubahan")
            }

            if (isEditMode) {
                Spacer(modifier = Modifier.height(16.dp))
                OutlinedButton(
                    onClick = {
                        productViewModel.deleteProduct(productId!!)
                        Toast.makeText(context, "$name telah dihapus", Toast.LENGTH_SHORT).show()
                        navController.popBackStack()
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.error)
                ) {
                    Text("Hapus Produk Ini")
                }
            }
        }
    }
}