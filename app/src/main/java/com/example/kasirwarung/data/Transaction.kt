package com.example.kasirwarung.data

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId

data class Transaction(
    @DocumentId val id: String = "",
    val userId: String = "",
    val items: List<Map<String, Any>> = emptyList(),
    val totalAmount: Double = 0.0,
    val timestamp: Timestamp = Timestamp.now()
)