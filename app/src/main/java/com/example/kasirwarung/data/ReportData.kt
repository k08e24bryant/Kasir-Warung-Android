package com.example.kasirwarung.data

data class ReportData(
    val totalRevenue: Double = 0.0,
    val transactionCount: Int = 0,
    val bestSellingProducts: List<Pair<String, Long>> = emptyList()
)