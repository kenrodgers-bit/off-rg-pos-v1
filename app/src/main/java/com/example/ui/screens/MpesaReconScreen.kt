package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.Payment
import com.example.ui.components.EmptyStateView
import com.example.ui.components.MetricCard
import com.example.ui.components.StatusBadge
import com.example.ui.theme.*
import com.example.ui.viewmodel.PosViewModel
import com.example.util.CurrencyFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MpesaReconScreen(
    viewModel: PosViewModel,
    onNavigateBack: () -> Unit
) {
    val allPayments by viewModel.allMpesaPayments.collectAsStateWithLifecycle()
    var searchQuery by remember { mutableStateOf("") }

    val filtered = remember(allPayments, searchQuery) {
        if (searchQuery.isBlank()) allPayments else {
            allPayments.filter {
                it.mpesaRef.contains(searchQuery, ignoreCase = true)
            }
        }
    }

    val totalMpesa = remember(filtered) { filtered.sumOf { it.amount } }

    Scaffold(
        containerColor = DarkBg,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "M-Pesa Reconciliation",
                        color = TextWhite,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = TextWhite
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = DarkBg)
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp)
                .testTag("mpesa_recon_screen"),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Metrics Header
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                MetricCard(
                    title = "TOTAL M-PESA LOGGED",
                    value = CurrencyFormatter.format(totalMpesa),
                    accentColor = MpesaGreen,
                    icon = Icons.Default.PhoneAndroid,
                    modifier = Modifier.weight(1.3f)
                )
                MetricCard(
                    title = "TX COUNT",
                    value = "${filtered.size}",
                    accentColor = TextWhite,
                    modifier = Modifier.weight(0.7f)
                )
            }

            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Search by M-Pesa code (e.g. QKJ456...)", fontSize = 13.sp) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = TextMuted) },
                modifier = Modifier.fillMaxWidth().height(50.dp),
                shape = RoundedCornerShape(8.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = RgAccent,
                    unfocusedBorderColor = DarkBorder,
                    focusedTextColor = TextWhite,
                    unfocusedTextColor = TextWhite
                ),
                singleLine = true
            )

            if (filtered.isEmpty()) {
                EmptyStateView(
                    icon = Icons.Default.PhoneIphone,
                    title = "No M-Pesa payments recorded",
                    description = "M-Pesa transactions entered during checkout will appear here for end-of-day reconciliation."
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(bottom = 24.dp)
                ) {
                    items(filtered) { payment ->
                        Card(
                            colors = CardDefaults.cardColors(containerColor = DarkSurfaceCard),
                            border = androidx.compose.foundation.BorderStroke(1.dp, DarkBorder),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Text(
                                            text = if (payment.mpesaRef.isNotBlank()) payment.mpesaRef else "NO-REF",
                                            color = TextWhite,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 15.sp
                                        )
                                        StatusBadge(text = "VERIFIED", color = MpesaGreen)
                                    }
                                    Text(
                                        text = "Sale ID #${payment.saleId} • ${CurrencyFormatter.formatDate(payment.dateEpoch)}",
                                        color = TextMuted,
                                        fontSize = 12.sp
                                    )
                                }

                                Text(
                                    text = CurrencyFormatter.format(payment.amount),
                                    color = MpesaGreen,
                                    fontWeight = FontWeight.Black,
                                    fontSize = 16.sp
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
