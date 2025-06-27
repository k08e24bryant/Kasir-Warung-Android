package com.example.kasirwarung.presentation.transaction

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.kasirwarung.data.Transaction
import com.example.kasirwarung.presentation.product.ProductViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionHistoryScreen(
    navController: NavController,
    productViewModel: ProductViewModel
) {
    // Mengambil data langsung dari state flow di ViewModel
    val transactions by productViewModel.transactions.collectAsState()
    var showDialog by remember { mutableStateOf(false) }
    var selectedTransaction by remember { mutableStateOf<Transaction?>(null) }
    val context = LocalContext.current

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/csv"),
        onResult = { uri ->
            uri?.let {
                productViewModel.exportTransactionsToCsv(context, it, transactions)
            }
        }
    )

    if (showDialog && selectedTransaction != null) {
        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = { Text("Konfirmasi Pembatalan") },
            text = { Text("Anda yakin ingin membatalkan transaksi ini? Stok barang akan dikembalikan.") },
            confirmButton = {
                Button(
                    onClick = {
                        selectedTransaction?.let {
                            productViewModel.deleteTransactionAndRollbackStock(it) { success ->
                                val message = if (success) "Transaksi berhasil dibatalkan" else "Gagal membatalkan transaksi"
                                Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
                            }
                        }
                        showDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Ya, Batalkan")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDialog = false }) {
                    Text("Tidak")
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Riwayat Transaksi") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Kembali")
                    }
                },
                actions = {
                    IconButton(
                        onClick = {
                            if (transactions.isNotEmpty()) {
                                val sdf = SimpleDateFormat("yyyyMMdd_HHmm", Locale.getDefault())
                                val fileName = "riwayat_transaksi_${sdf.format(Date())}.csv"
                                launcher.launch(fileName)
                            } else {
                                Toast.makeText(context, "Tidak ada data untuk diekspor", Toast.LENGTH_SHORT).show()
                            }
                        },
                        enabled = transactions.isNotEmpty() // Tombol hanya aktif jika ada data
                    ) {
                        Icon(Icons.Default.Share, contentDescription = "Ekspor ke CSV")
                    }
                }
            )
        }
    ) { paddingValues ->
        if (transactions.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize().padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Belum ada riwayat transaksi.")
                    Text("Lakukan checkout untuk melihat riwayat di sini.", style = MaterialTheme.typography.bodySmall)
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                items(transactions, key = { it.id }) { transaction ->
                    TransactionItem(
                        transaction = transaction,
                        onCancel = {
                            selectedTransaction = transaction
                            showDialog = true
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun TransactionItem(transaction: Transaction, onCancel: () -> Unit) {
    val dateFormat = remember { SimpleDateFormat("dd MMMM yyyy, HH:mm", Locale.getDefault()) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(2.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "ID: ...${transaction.id.takeLast(6)}",
                    style = MaterialTheme.typography.bodySmall
                )
                Text(
                    text = dateFormat.format(transaction.timestamp.toDate()),
                    style = MaterialTheme.typography.bodySmall
                )
            }
            Divider(modifier = Modifier.padding(vertical = 8.dp))
            transaction.items.forEach { item ->
                Row(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "${item["quantity"]}x ${item["productName"]}",
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        text = "Rp ${"%,.0f".format( (item["price"] as Double) * (item["quantity"] as Long) )}",
                        textAlign = TextAlign.End
                    )
                }
            }
            Divider(modifier = Modifier.padding(vertical = 8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Total",
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = "Rp ${"%,.0f".format(transaction.totalAmount)}",
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
            OutlinedButton(
                onClick = onCancel,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.error)
            ) {
                Text("Batalkan Transaksi")
            }
        }
    }
}