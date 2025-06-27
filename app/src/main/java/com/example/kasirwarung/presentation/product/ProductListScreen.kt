package com.example.kasirwarung.presentation.product

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.kasirwarung.data.Product
import com.example.kasirwarung.navigation.Routes
import com.example.kasirwarung.presentation.auth.AuthViewModel
import com.example.kasirwarung.presentation.transaction.CartViewModel
import kotlinx.coroutines.flow.map

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProductListScreen(
    navController: NavController,
    productViewModel: ProductViewModel,
    authViewModel: AuthViewModel,
    cartViewModel: CartViewModel
) {
    val products by productViewModel.products.collectAsState()
    val searchQuery by productViewModel.searchQuery.collectAsState()
    val cartItemCount by cartViewModel.cartItems.map { it.size }.collectAsState(initial = 0)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Daftar Produk") },
                actions = {
                    IconButton(onClick = { navController.navigate("report") }) {
                        Icon(Icons.Filled.Assessment, contentDescription = "Laporan Penjualan")
                    }
                    IconButton(onClick = { navController.navigate("transactionHistory") }) {
                        Icon(Icons.Filled.History, contentDescription = "Riwayat Transaksi")
                    }
                    BadgedBox(
                        badge = {
                            if (cartItemCount > 0) {
                                Badge { Text("$cartItemCount") }
                            }
                        }
                    ) {
                        IconButton(onClick = { navController.navigate("cart") }) {
                            Icon(Icons.Default.ShoppingCart, contentDescription = "Keranjang")
                        }
                    }
                    IconButton(onClick = {
                        authViewModel.logout()
                        navController.navigate("login") { popUpTo(0) }
                    }) {
                        Icon(Icons.Default.ExitToApp, contentDescription = "Logout")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { navController.navigate(Routes.ADD_EDIT_PRODUCT_ROUTE) }) {
                Icon(Icons.Default.Add, contentDescription = "Tambah Produk")
            }
        }
    ) { paddingValues ->
        Column(modifier = Modifier.padding(paddingValues)) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { productViewModel.onSearchQueryChange(it) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                label = { Text("Cari Produk...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Cari") },
                singleLine = true
            )
            if (products.isEmpty() && searchQuery.isBlank()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text("Belum ada produk. Tekan tombol + untuk menambah.")
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 8.dp)
                ) {
                    items(products, key = { it.id }) { product ->
                        ProductItem(
                            product = product,
                            onAddToCart = { cartViewModel.addToCart(product) },
                            onEdit = {
                                navController.navigate("${Routes.ADD_EDIT_PRODUCT_ROUTE}?${Routes.PRODUCT_ID_ARG}=${product.id}")
                            }
                        )
                        Divider()
                    }
                }
            }
        }
    }
}

@Composable
fun ProductItem(product: Product, onAddToCart: () -> Unit, onEdit: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(product.name, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                Text("Harga: Rp ${"%,.0f".format(product.price)}")
                Text(
                    if (product.stock > 0) "Stok: ${product.stock}" else "Stok Habis",
                    color = if (product.stock > 0) LocalContentColor.current else MaterialTheme.colorScheme.error
                )
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onEdit) {
                    Icon(Icons.Filled.Edit, contentDescription = "Edit Produk")
                }
                Button(onClick = onAddToCart, enabled = product.stock > 0) {
                    Icon(Icons.Filled.AddShoppingCart, contentDescription = "Tambah ke Keranjang")
                }
            }
        }
    }
}