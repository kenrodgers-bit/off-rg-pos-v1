package com.example.ui.screens

import android.widget.Toast
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.Customer
import com.example.data.model.CustomerTransaction
import com.example.ui.components.EmptyStateView
import com.example.ui.components.FuturisticButton
import com.example.ui.components.MetricCard
import com.example.ui.theme.*
import com.example.ui.viewmodel.PosViewModel
import com.example.util.CurrencyFormatter
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomerCreditScreen(
    customerId: Long,
    viewModel: PosViewModel,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val currentUser by viewModel.currentUser.collectAsStateWithLifecycle()

    var customer by remember { mutableStateOf<Customer?>(null) }
    var transactions by remember { mutableStateOf<List<CustomerTransaction>>(emptyList()) }
    var showRepaymentDialog by remember { mutableStateOf(false) }

    fun refreshData() {
        coroutineScope.launch {
            customer = viewModel.repository.db.customerDao().getCustomerById(customerId)
            viewModel.repository.db.customerDao().getCustomerTransactions(customerId).collect {
                transactions = it
            }
        }
    }

    LaunchedEffect(customerId) {
        refreshData()
    }

    Scaffold(
        containerColor = DarkBg,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = customer?.name ?: "Customer Ledger",
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
                .testTag("customer_credit_screen"),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Customer Header Card
            Card(
                colors = CardDefaults.cardColors(containerColor = DarkSurfaceCard),
                border = androidx.compose.foundation.BorderStroke(1.dp, DarkBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(customer?.name ?: "", color = TextWhite, fontWeight = FontWeight.Bold, fontSize = 20.sp)
                    Text("Phone: ${customer?.phone ?: ""} • ${customer?.address ?: ""}", color = TextMuted, fontSize = 13.sp)

                    HorizontalDivider(color = DarkDivider)

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        MetricCard(
                            title = "Outstanding Debt",
                            value = CurrencyFormatter.format(customer?.outstandingCredit ?: 0.0),
                            accentColor = if ((customer?.outstandingCredit ?: 0.0) > 0) WarningOrange else SuccessGreen,
                            modifier = Modifier.weight(1f)
                        )
                        MetricCard(
                            title = "Credit Limit",
                            value = CurrencyFormatter.format(customer?.creditLimit ?: 0.0),
                            accentColor = CreditBlue,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    FuturisticButton(
                        text = "Record Repayment",
                        icon = Icons.Default.Payment,
                        onClick = { showRepaymentDialog = true },
                        modifier = Modifier.fillMaxWidth().testTag("record_repayment_button")
                    )
                }
            }

            Text("TRANSACTION HISTORY / LEDGER", color = TextMuted, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)

            if (transactions.isEmpty()) {
                EmptyStateView(
                    icon = Icons.Default.ReceiptLong,
                    title = "No credit transactions yet",
                    description = "Sales on credit or repayments made by this customer will appear here."
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(bottom = 24.dp)
                ) {
                    items(transactions) { tx ->
                        val isRepayment = tx.type == "REPAYMENT"
                        Card(
                            colors = CardDefaults.cardColors(containerColor = DarkSurfaceCard),
                            border = androidx.compose.foundation.BorderStroke(1.dp, DarkBorder),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = if (isRepayment) "Repayment (${tx.referenceId})" else "Credit Sale (${tx.referenceId})",
                                        color = TextWhite,
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 14.sp
                                    )
                                    Text(
                                        text = "${CurrencyFormatter.formatDate(tx.dateEpoch)} • ${tx.notes}",
                                        color = TextMuted,
                                        fontSize = 11.sp
                                    )
                                    Text(
                                        text = "Balance after: ${CurrencyFormatter.format(tx.balanceAfter)}",
                                        color = TextSubtle,
                                        fontSize = 11.sp
                                    )
                                }

                                Text(
                                    text = if (isRepayment) "-${CurrencyFormatter.format(tx.amount)}" else "+${CurrencyFormatter.format(tx.amount)}",
                                    color = if (isRepayment) SuccessGreen else WarningOrange,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp
                                )
                            }
                        }
                    }
                }
            }
        }

        // Repayment Dialog
        if (showRepaymentDialog) {
            RecordRepaymentModal(
                customer = customer!!,
                onConfirm = { amount, notes ->
                    coroutineScope.launch {
                        viewModel.repository.recordCustomerRepayment(
                            customerId = customerId,
                            amount = amount,
                            notes = notes,
                            staffName = currentUser
                        )
                        showRepaymentDialog = false
                        Toast.makeText(context, "Repayment of ${CurrencyFormatter.format(amount)} recorded", Toast.LENGTH_SHORT).show()
                        customer = viewModel.repository.db.customerDao().getCustomerById(customerId)
                    }
                },
                onDismiss = { showRepaymentDialog = false }
            )
        }
    }
}

@Composable
fun RecordRepaymentModal(
    customer: Customer,
    onConfirm: (amount: Double, notes: String) -> Unit,
    onDismiss: () -> Unit
) {
    var amountText by remember { mutableStateOf(customer.outstandingCredit.toInt().toString()) }
    var notes by remember { mutableStateOf("Cash repayment at counter") }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = DarkSurfaceCard),
            border = androidx.compose.foundation.BorderStroke(1.dp, DarkBorder)
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Record Customer Repayment", color = TextWhite, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Text("Current Debt: ${CurrencyFormatter.format(customer.outstandingCredit)}", color = WarningOrange, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)

                OutlinedTextField(
                    value = amountText,
                    onValueChange = { amountText = it },
                    label = { Text("Repayment Amount (KSh)") },
                    modifier = Modifier.fillMaxWidth().testTag("repayment_amount_input"),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = RgAccent,
                        unfocusedBorderColor = DarkBorder,
                        focusedTextColor = TextWhite,
                        unfocusedTextColor = TextWhite
                    ),
                    singleLine = true
                )

                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Notes / Receipt Ref") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = RgAccent,
                        unfocusedBorderColor = DarkBorder,
                        focusedTextColor = TextWhite,
                        unfocusedTextColor = TextWhite
                    ),
                    singleLine = true
                )

                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    FuturisticButton(
                        text = "Cancel",
                        onClick = onDismiss,
                        isSecondary = true,
                        modifier = Modifier.weight(1f)
                    )
                    FuturisticButton(
                        text = "Confirm Payment",
                        enabled = (amountText.toDoubleOrNull() ?: 0.0) > 0,
                        onClick = {
                            onConfirm(amountText.toDoubleOrNull() ?: 0.0, notes)
                        },
                        modifier = Modifier.weight(1.3f),
                        testTag = "confirm_repayment_button"
                    )
                }
            }
        }
    }
}
