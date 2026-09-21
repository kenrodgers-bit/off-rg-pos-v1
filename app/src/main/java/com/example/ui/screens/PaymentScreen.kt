package com.example.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.components.FuturisticButton
import com.example.ui.components.StatusBadge
import com.example.ui.theme.*
import com.example.ui.viewmodel.PosViewModel
import com.example.util.CurrencyFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PaymentScreen(
    viewModel: PosViewModel,
    onNavigateBack: () -> Unit,
    onSaleCompleted: (Long) -> Unit
) {
    val context = LocalContext.current
    val total by viewModel.cartTotal.collectAsStateWithLifecycle()
    val selectedCustomer by viewModel.selectedCustomer.collectAsStateWithLifecycle()

    var paymentMode by remember { mutableStateOf("CASH") } // "CASH", "MPESA", "PARTIAL"

    // Cash Mode
    var cashReceivedText by remember(total) { mutableStateOf(total.toInt().toString()) }
    val cashReceived = cashReceivedText.toDoubleOrNull() ?: 0.0
    val cashChange = (cashReceived - total).coerceAtLeast(0.0)

    // M-Pesa Mode
    var mpesaAmountText by remember(total) { mutableStateOf(total.toInt().toString()) }
    var mpesaRef by remember { mutableStateOf("") }

    // Partial Split Mode
    var splitCashText by remember { mutableStateOf("") }
    var splitMpesaText by remember { mutableStateOf("") }
    var splitMpesaRef by remember { mutableStateOf("") }
    var splitCreditText by remember { mutableStateOf("") }

    val splitCash = splitCashText.toDoubleOrNull() ?: 0.0
    val splitMpesa = splitMpesaText.toDoubleOrNull() ?: 0.0
    val splitCredit = splitCreditText.toDoubleOrNull() ?: 0.0
    val splitPaid = splitCash + splitMpesa + splitCredit
    val splitRemaining = total - splitPaid

    var isProcessing by remember { mutableStateOf(false) }

    Scaffold(
        containerColor = DarkBg,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Payment",
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
                .verticalScroll(rememberScrollState())
                .testTag("payment_screen"),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Amount Due Header Card
            Card(
                colors = CardDefaults.cardColors(containerColor = DarkSurfaceCard),
                border = androidx.compose.foundation.BorderStroke(1.dp, RgAccent.copy(alpha = 0.5f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text("TOTAL AMOUNT DUE", color = TextMuted, fontSize = 12.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = CurrencyFormatter.format(total),
                        color = RgAccent,
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Black
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Customer: ${selectedCustomer?.name ?: "Walk-in Customer"}",
                        color = TextWhite,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            // Payment Mode Switcher Tabs
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(DarkSurfaceCard)
                    .border(1.dp, DarkBorder, RoundedCornerShape(10.dp))
                    .padding(4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                listOf("CASH" to "Cash", "MPESA" to "M-Pesa", "PARTIAL" to "Split / Partial").forEach { (mode, label) ->
                    val isSelected = paymentMode == mode
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isSelected) RgAccent else Color.Transparent)
                            .clickable { paymentMode = mode }
                            .padding(vertical = 10.dp)
                            .testTag("payment_tab_$mode"),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = label,
                            color = if (isSelected) DarkBg else TextWhite,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            fontSize = 13.sp
                        )
                    }
                }
            }

            // Payment Mode Body
            when (paymentMode) {
                "CASH" -> {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = DarkSurfaceCard),
                        border = androidx.compose.foundation.BorderStroke(1.dp, DarkBorder),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            Text("Cash Payment", color = TextWhite, fontWeight = FontWeight.Bold, fontSize = 16.sp)

                            OutlinedTextField(
                                value = cashReceivedText,
                                onValueChange = { cashReceivedText = it },
                                label = { Text("Cash Received (KSh)") },
                                modifier = Modifier.fillMaxWidth().testTag("cash_received_input"),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = RgAccent,
                                    unfocusedBorderColor = DarkBorder,
                                    focusedTextColor = TextWhite,
                                    unfocusedTextColor = TextWhite
                                ),
                                singleLine = true
                            )

                            // Quick Cash Suggestions (Part 11)
                            Text("Quick Cash Tender:", color = TextMuted, fontSize = 12.sp)
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                val exact = total.toInt()
                                val rounded1k = ((exact + 999) / 1000) * 1000
                                val rounded2k = ((exact + 1999) / 2000) * 2000
                                val suggestions = listOf(exact, rounded1k, rounded2k, 5000).distinct()

                                for (s in suggestions) {
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(DarkSurfaceElevated)
                                            .border(1.dp, DarkBorder, RoundedCornerShape(6.dp))
                                            .clickable { cashReceivedText = s.toString() }
                                            .padding(horizontal = 10.dp, vertical = 6.dp)
                                    ) {
                                        Text(text = "KSh $s", color = TextWhite, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                                    }
                                }
                            }

                            HorizontalDivider(color = DarkDivider)

                            // Live Change Indicator (Part 11)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("CHANGE TO RETURN:", color = TextMuted, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                Text(
                                    text = CurrencyFormatter.format(cashChange),
                                    color = if (cashChange > 0) CashAmber else TextWhite,
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.Black
                                )
                            }
                        }
                    }
                }

                "MPESA" -> {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = DarkSurfaceCard),
                        border = androidx.compose.foundation.BorderStroke(1.dp, DarkBorder),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            Text("M-Pesa Payment", color = TextWhite, fontWeight = FontWeight.Bold, fontSize = 16.sp)

                            OutlinedTextField(
                                value = mpesaAmountText,
                                onValueChange = { mpesaAmountText = it },
                                label = { Text("Amount Paid (KSh)") },
                                modifier = Modifier.fillMaxWidth().testTag("mpesa_amount_input"),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = RgAccent,
                                    unfocusedBorderColor = DarkBorder,
                                    focusedTextColor = TextWhite,
                                    unfocusedTextColor = TextWhite
                                ),
                                singleLine = true
                            )

                            OutlinedTextField(
                                value = mpesaRef,
                                onValueChange = { mpesaRef = it.uppercase() },
                                label = { Text("M-Pesa Reference Code (e.g. QKJ4567890)") },
                                placeholder = { Text("Enter M-Pesa Code", color = TextMuted) },
                                modifier = Modifier.fillMaxWidth().testTag("mpesa_ref_input"),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = RgAccent,
                                    unfocusedBorderColor = DarkBorder,
                                    focusedTextColor = TextWhite,
                                    unfocusedTextColor = TextWhite
                                ),
                                singleLine = true
                            )

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(Icons.Default.Verified, contentDescription = null, tint = MpesaGreen, modifier = Modifier.size(16.dp))
                                Text("Payment will be logged for end-of-day reconciliation.", color = TextMuted, fontSize = 12.sp)
                            }
                        }
                    }
                }

                "PARTIAL" -> {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = DarkSurfaceCard),
                        border = androidx.compose.foundation.BorderStroke(1.dp, DarkBorder),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text("Split Across Payment Methods", color = TextWhite, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                            Text("Enter split portions to match total KSh ${total.toInt()}.", color = TextMuted, fontSize = 12.sp)

                            OutlinedTextField(
                                value = splitCashText,
                                onValueChange = { splitCashText = it },
                                label = { Text("Cash Portion (KSh)") },
                                modifier = Modifier.fillMaxWidth().testTag("split_cash_input"),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = RgAccent,
                                    unfocusedBorderColor = DarkBorder,
                                    focusedTextColor = TextWhite,
                                    unfocusedTextColor = TextWhite
                                ),
                                singleLine = true
                            )

                            OutlinedTextField(
                                value = splitMpesaText,
                                onValueChange = { splitMpesaText = it },
                                label = { Text("M-Pesa Portion (KSh)") },
                                modifier = Modifier.fillMaxWidth().testTag("split_mpesa_input"),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = RgAccent,
                                    unfocusedBorderColor = DarkBorder,
                                    focusedTextColor = TextWhite,
                                    unfocusedTextColor = TextWhite
                                ),
                                singleLine = true
                            )

                            if (splitMpesa > 0) {
                                OutlinedTextField(
                                    value = splitMpesaRef,
                                    onValueChange = { splitMpesaRef = it.uppercase() },
                                    label = { Text("M-Pesa Reference Code") },
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = RgAccent,
                                        unfocusedBorderColor = DarkBorder,
                                        focusedTextColor = TextWhite,
                                        unfocusedTextColor = TextWhite
                                    ),
                                    singleLine = true
                                )
                            }

                            OutlinedTextField(
                                value = splitCreditText,
                                onValueChange = { splitCreditText = it },
                                label = { Text("Credit / Debt Portion (KSh)") },
                                modifier = Modifier.fillMaxWidth().testTag("split_credit_input"),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = RgAccent,
                                    unfocusedBorderColor = DarkBorder,
                                    focusedTextColor = TextWhite,
                                    unfocusedTextColor = TextWhite
                                ),
                                singleLine = true
                            )

                            if (splitCredit > 0 && selectedCustomer == null) {
                                Text(
                                    text = "⚠️ Customer selection is required to issue Credit!",
                                    color = AlertRed,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            HorizontalDivider(color = DarkDivider)

                            // Live Split Balance Tracker
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("Total Paid So Far:", color = TextMuted, fontSize = 13.sp)
                                    Text(CurrencyFormatter.format(splitPaid), color = TextWhite, fontWeight = FontWeight.Bold)
                                }
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("Remaining Balance:", color = if (splitRemaining != 0.0) AlertRed else SuccessGreen, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                    Text(
                                        CurrencyFormatter.format(splitRemaining),
                                        color = if (splitRemaining != 0.0) AlertRed else SuccessGreen,
                                        fontWeight = FontWeight.Black,
                                        fontSize = 15.sp
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Complete Sale Button
            FuturisticButton(
                text = if (isProcessing) "Completing Sale..." else "Complete Sale",
                enabled = !isProcessing,
                icon = Icons.Default.CheckCircle,
                onClick = {
                    isProcessing = true
                    when (paymentMode) {
                        "CASH" -> {
                            if (cashReceived < total) {
                                Toast.makeText(context, "Cash received is less than total due", Toast.LENGTH_SHORT).show()
                                isProcessing = false
                                return@FuturisticButton
                            }
                            viewModel.completeSale(
                                cashAmount = total,
                                mpesaAmount = 0.0,
                                mpesaRef = "",
                                creditAmount = 0.0,
                                onComplete = { saleId ->
                                    isProcessing = false
                                    onSaleCompleted(saleId)
                                },
                                onError = { err ->
                                    isProcessing = false
                                    Toast.makeText(context, err, Toast.LENGTH_LONG).show()
                                }
                            )
                        }

                        "MPESA" -> {
                            val amt = mpesaAmountText.toDoubleOrNull() ?: 0.0
                            if (amt != total) {
                                Toast.makeText(context, "M-Pesa amount must equal total amount", Toast.LENGTH_SHORT).show()
                                isProcessing = false
                                return@FuturisticButton
                            }
                            viewModel.completeSale(
                                cashAmount = 0.0,
                                mpesaAmount = total,
                                mpesaRef = mpesaRef,
                                creditAmount = 0.0,
                                onComplete = { saleId ->
                                    isProcessing = false
                                    onSaleCompleted(saleId)
                                },
                                onError = { err ->
                                    isProcessing = false
                                    Toast.makeText(context, err, Toast.LENGTH_LONG).show()
                                }
                            )
                        }

                        "PARTIAL" -> {
                            if (splitCredit > 0 && selectedCustomer == null) {
                                Toast.makeText(context, "Customer selection is required for Credit payment", Toast.LENGTH_LONG).show()
                                isProcessing = false
                                return@FuturisticButton
                            }
                            viewModel.completeSale(
                                cashAmount = splitCash,
                                mpesaAmount = splitMpesa,
                                mpesaRef = splitMpesaRef,
                                creditAmount = splitCredit,
                                onComplete = { saleId ->
                                    isProcessing = false
                                    onSaleCompleted(saleId)
                                },
                                onError = { err ->
                                    isProcessing = false
                                    Toast.makeText(context, err, Toast.LENGTH_LONG).show()
                                }
                            )
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth().testTag("complete_sale_button")
            )

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}
