package com.example.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
fun CustomersScreen(
    viewModel: PosViewModel,
    onNavigateBack: () -> Unit,
    onSelectCustomerCredit: (Long) -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val customers by viewModel.allCustomers.collectAsStateWithLifecycle()
    val totalCredit by viewModel.totalOutstandingCredit.collectAsStateWithLifecycle()

    var searchQuery by remember { mutableStateOf("") }
    var showAddCustomerDialog by remember { mutableStateOf(false) }

    val filtered = remember(customers, searchQuery) {
        if (searchQuery.isBlank()) customers else {
            customers.filter {
                it.name.contains(searchQuery, ignoreCase = true) || it.phone.contains(searchQuery)
            }
        }
    }

    Scaffold(
        containerColor = DarkBg,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Customers & Credit (${customers.size})",
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
                actions = {
                    IconButton(onClick = { showAddCustomerDialog = true }) {
                        Icon(Icons.Default.PersonAdd, contentDescription = "Add Customer", tint = RgAccent)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = DarkBg)
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddCustomerDialog = true },
                containerColor = RgAccent,
                contentColor = DarkBg,
                shape = CircleShape,
                modifier = Modifier.testTag("add_customer_fab")
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Customer")
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp)
                .testTag("customers_screen"),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Outstanding credit overview banner
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = if (totalCredit > 0) WarningOrangeBg else DarkSurfaceCard
                ),
                border = androidx.compose.foundation.BorderStroke(
                    1.dp,
                    if (totalCredit > 0) WarningOrange.copy(alpha = 0.5f) else DarkBorder
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("TOTAL OUTSTANDING CREDIT", color = TextMuted, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        Text(
                            text = CurrencyFormatter.format(totalCredit),
                            color = if (totalCredit > 0) WarningOrange else TextWhite,
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Black
                        )
                    }
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(DarkSurfaceElevated),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.AccountBalanceWallet, contentDescription = null, tint = WarningOrange)
                    }
                }
            }

            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Search by customer name or phone...", fontSize = 13.sp) },
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
                    icon = Icons.Default.PeopleOutline,
                    title = "No customers found",
                    description = "Register your credit and frequent customers here.",
                    actionButtonText = "+ Add Customer",
                    onActionClick = { showAddCustomerDialog = true }
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(bottom = 110.dp)
                ) {
                    items(filtered) { customer ->
                        Card(
                            colors = CardDefaults.cardColors(containerColor = DarkSurfaceCard),
                            border = androidx.compose.foundation.BorderStroke(1.dp, DarkBorder),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onSelectCustomerCredit(customer.id) }
                                .testTag("customer_card_${customer.id}")
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(customer.name, color = TextWhite, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                    Text("${customer.phone} • Limit: ${CurrencyFormatter.format(customer.creditLimit)}", color = TextMuted, fontSize = 12.sp)
                                }

                                Column(horizontalAlignment = Alignment.End) {
                                    Text(
                                        text = if (customer.outstandingCredit > 0) "Debt: ${CurrencyFormatter.format(customer.outstandingCredit)}" else "Paid Up",
                                        color = if (customer.outstandingCredit > 0) WarningOrange else SuccessGreen,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp
                                    )
                                    Text(
                                        text = "Total: ${CurrencyFormatter.format(customer.totalPurchases)}",
                                        color = TextSubtle,
                                        fontSize = 11.sp
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Add Customer Dialog
        if (showAddCustomerDialog) {
            AddCustomerDialog(
                onAdd = { name, phone, address, limit ->
                    coroutineScope.launch {
                        viewModel.repository.saveCustomer(
                            Customer(name = name, phone = phone, address = address, creditLimit = limit)
                        )
                        Toast.makeText(context, "Customer created", Toast.LENGTH_SHORT).show()
                        showAddCustomerDialog = false
                    }
                },
                onDismiss = { showAddCustomerDialog = false }
            )
        }
    }
}

@Composable
fun AddCustomerDialog(
    onAdd: (name: String, phone: String, address: String, limit: Double) -> Unit,
    onDismiss: () -> Unit
) {
    var name by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var address by remember { mutableStateOf("") }
    var creditLimitText by remember { mutableStateOf("30000") }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = DarkSurfaceCard),
            border = androidx.compose.foundation.BorderStroke(1.dp, DarkBorder)
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Add New Customer", color = TextWhite, fontWeight = FontWeight.Bold, fontSize = 16.sp)

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Full Name") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = RgAccent,
                        unfocusedBorderColor = DarkBorder,
                        focusedTextColor = TextWhite,
                        unfocusedTextColor = TextWhite
                    ),
                    singleLine = true
                )

                OutlinedTextField(
                    value = phone,
                    onValueChange = { phone = it },
                    label = { Text("Phone Number") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = RgAccent,
                        unfocusedBorderColor = DarkBorder,
                        focusedTextColor = TextWhite,
                        unfocusedTextColor = TextWhite
                    ),
                    singleLine = true
                )

                OutlinedTextField(
                    value = address,
                    onValueChange = { address = it },
                    label = { Text("Location / Address") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = RgAccent,
                        unfocusedBorderColor = DarkBorder,
                        focusedTextColor = TextWhite,
                        unfocusedTextColor = TextWhite
                    ),
                    singleLine = true
                )

                OutlinedTextField(
                    value = creditLimitText,
                    onValueChange = { creditLimitText = it },
                    label = { Text("Credit Limit (KSh)") },
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
                        text = "Save Customer",
                        enabled = name.isNotBlank(),
                        onClick = {
                            onAdd(name, phone, address, creditLimitText.toDoubleOrNull() ?: 0.0)
                        },
                        modifier = Modifier.weight(1.3f)
                    )
                }
            }
        }
    }
}
