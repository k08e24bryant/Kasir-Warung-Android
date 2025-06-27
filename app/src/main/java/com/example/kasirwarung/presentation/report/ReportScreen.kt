package com.example.kasirwarung.presentation.report

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.kasirwarung.data.ReportData
import com.example.kasirwarung.presentation.product.ProductViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportScreen(
    navController: NavController,
    productViewModel: ProductViewModel
) {
    val reportData by productViewModel.reportData.collectAsState()
    val isGenerating by productViewModel.isGeneratingReport.collectAsState()

    var startDate by remember { mutableStateOf<Date?>(null) }
    var endDate by remember { mutableStateOf<Date?>(null) }
    var showDatePicker by remember { mutableStateOf(false) }
    var editingStartDate by remember { mutableStateOf(true) }

    val datePickerState = rememberDatePickerState()

    if (showDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDatePicker = false
                        val selectedDate = datePickerState.selectedDateMillis?.let { Date(it) }
                        if (editingStartDate) {
                            startDate = selectedDate
                        } else {
                            endDate = selectedDate
                        }
                    },
                    enabled = datePickerState.selectedDateMillis != null
                ) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("Batal") }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Laporan Penjualan") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Kembali")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier.fillMaxSize().padding(paddingValues).padding(16.dp)
        ) {
            Text("Pilih Rentang Tanggal", style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                DateSelector(label = "Dari Tanggal", selectedDate = startDate, modifier = Modifier.weight(1f)) {
                    editingStartDate = true
                    showDatePicker = true
                }
                DateSelector(label = "Sampai Tanggal", selectedDate = endDate, modifier = Modifier.weight(1f)) {
                    editingStartDate = false
                    showDatePicker = true
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = {
                    // Pastikan endDate mencakup hingga akhir hari
                    val finalEndDate = endDate?.let {
                        Calendar.getInstance().apply {
                            time = it
                            set(Calendar.HOUR_OF_DAY, 23)
                            set(Calendar.MINUTE, 59)
                            set(Calendar.SECOND, 59)
                        }.time
                    }
                    if (startDate != null && finalEndDate != null) {
                        productViewModel.generateReport(startDate!!, finalEndDate)
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = startDate != null && endDate != null && !isGenerating
            ) {
                Text("Buat Laporan")
            }
            Divider(modifier = Modifier.padding(vertical = 16.dp))

            if (isGenerating) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else {
                reportData?.let { data ->
                    ReportResultView(data)
                } ?: Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text("Pilih tanggal dan buat laporan untuk melihat hasilnya.")
                }
            }
        }
    }
}

@Composable
fun DateSelector(label: String, selectedDate: Date?, modifier: Modifier = Modifier, onClick: () -> Unit) {
    val dateFormat = remember { SimpleDateFormat("dd MMM yyyy", Locale.getDefault()) }
    OutlinedButton(onClick = onClick, modifier = modifier, shape = MaterialTheme.shapes.medium) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.DateRange, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text(selectedDate?.let { dateFormat.format(it) } ?: label)
        }
    }
}

@Composable
fun ReportResultView(data: ReportData) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        ReportCard("Total Omset", "Rp ${"%,.0f".format(data.totalRevenue)}")
        ReportCard("Jumlah Transaksi", "${data.transactionCount} Transaksi")

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("5 Produk Terlaris", style = MaterialTheme.typography.titleLarge)
                Spacer(modifier = Modifier.height(8.dp))
                if (data.bestSellingProducts.isEmpty()) {
                    Text("Tidak ada produk yang terjual pada periode ini.")
                } else {
                    data.bestSellingProducts.forEach { (name, count) ->
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(name, fontSize = 16.sp, modifier = Modifier.weight(1f))
                            Text("$count pcs", fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ReportCard(title: String, value: String) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(value, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        }
    }
}