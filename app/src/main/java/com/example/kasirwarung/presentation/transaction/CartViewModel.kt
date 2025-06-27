package com.example.kasirwarung.presentation.transaction

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.kasirwarung.data.CartItem
import com.example.kasirwarung.data.Product
import kotlinx.coroutines.flow.*

class CartViewModel : ViewModel() {
    private val _cartItems = MutableStateFlow<List<CartItem>>(emptyList())
    val cartItems: StateFlow<List<CartItem>> = _cartItems.asStateFlow()

    // PERBAIKAN: Menggunakan stateIn untuk mengubah Flow menjadi StateFlow
    val totalAmount: StateFlow<Double> = _cartItems.map { items ->
        items.sumOf { it.product.price * it.quantity }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = 0.0
    )

    fun addToCart(product: Product) {
        // Logika ini diperbarui agar lebih aman dan reaktif
        _cartItems.update { currentItems ->
            val mutableItems = currentItems.toMutableList()
            val existingItem = mutableItems.find { it.product.id == product.id }

            if (existingItem != null) {
                if (existingItem.quantity < product.stock) {
                    existingItem.quantity++
                }
            } else {
                if (product.stock > 0) {
                    mutableItems.add(CartItem(product, 1))
                }
            }
            mutableItems
        }
    }

    fun removeFromCart(productId: String) {
        _cartItems.update { currentItems ->
            currentItems.filterNot { it.product.id == productId }
        }
    }

    fun increaseQuantity(productId: String) {
        _cartItems.update { currentItems ->
            currentItems.map {
                if (it.product.id == productId && it.quantity < it.product.stock) {
                    it.copy(quantity = it.quantity + 1)
                } else {
                    it
                }
            }
        }
    }

    fun decreaseQuantity(productId: String) {
        _cartItems.update { currentItems ->
            val item = currentItems.find { it.product.id == productId }
            if (item != null && item.quantity > 1) {
                currentItems.map {
                    if (it.product.id == productId) {
                        it.copy(quantity = it.quantity - 1)
                    } else {
                        it
                    }
                }
            } else {
                // Jika jumlahnya 1, langsung hapus
                currentItems.filterNot { it.product.id == productId }
            }
        }
    }

    fun clearCart() {
        _cartItems.value = emptyList()
    }
}