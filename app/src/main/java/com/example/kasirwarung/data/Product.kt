package com.example.kasirwarung.data

import com.google.firebase.firestore.DocumentId

data class Product(
    @DocumentId val id: String = "",
    val name: String = "",
    val price: Double = 0.0,
    val stock: Int = 0,
    val userId: String = ""
)