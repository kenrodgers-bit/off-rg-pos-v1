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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.Supplier
import com.example.ui.components.EmptyStateView
import com.example.ui.components.FuturisticButton
import com.example.ui.theme.*
import com.example.ui.viewmodel.PosViewModel
import com.example.util.CurrencyFormatter
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SuppliersScreen(
    viewModel: PosViewModel,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val suppliers by viewModel.allSuppliers.collectAsStateWithLifecycle()
    var showAddSupplierDialog by remember { mutableStateOf(false) }

    Scaffold(
        containerColor = DarkBg,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Suppliers (${suppliers.size})",
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
                    IconButton(onClick = { showAddSupplierDialog = true }) {
                        Icon(Icons.Default.Add, contentDescription = "Add Supplier", tint = RgAccent)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = DarkBg)
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp)
                .testTag("suppliers_screen")
        ) {
            if (suppliers.isEmpty()) {
                EmptyStateView(
                    icon = Icons.Default.Factory,
                    title = "No suppliers yet",
                    description = "Add manufacturers and distributors you order stock from.",
                    actionButtonText = "+ Add Supplier",
                    onActionClick = { showAddSupplierDialog = true }
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(vertical = 12.dp)
                ) {
                    items(suppliers) { supplier ->
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
                                    Text(supplier.name, color = TextWhite, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                    Text("Contact: ${supplier.contactPerson} • ${supplier.phone}", color = TextMuted, fontSize = 12.sp)
                                    Text(supplier.address, color = TextSubtle, fontSize = 11.sp)
                                }

                                if (supplier.outstandingBalance > 0) {
                                    Column(horizontalAlignment = Alignment.End) {
                                        Text("Balance Owed", color = TextMuted, fontSize = 10.sp)
                                        Text(
                                            CurrencyFormatter.format(supplier.outstandingBalance),
                                            color = AlertRed,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.sp
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        if (showAddSupplierDialog) {
            AddSupplierDialog(
                onAdd = { name, phone, address, contact, balance ->
                    coroutineScope.launch {
                        viewModel.repository.saveSupplier(
                            Supplier(name = name, phone = phone, address = address, contactPerson = contact, outstandingBalance = balance)
                        )
                        showAddSupplierDialog = false
                        Toast.makeText(context, "Supplier saved", Toast.LENGTH_SHORT).show()
                    }
                },
                onDismiss = { showAddSupplierDialog = false }
            )
        }
    }
}

@Composable
fun AddSupplierDialog(
    onAdd: (name: String, phone: String, address: String, contact: String, balance: Double) -> Unit,
    onDismiss: () -> Unit
) {
    var name by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var address by remember { mutableStateOf("") }
    var contact by remember { mutableStateOf("") }
    var balanceText by remember { mutableStateOf("0") }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = DarkSurfaceCard),
            border = androidx.compose.foundation.BorderStroke(1.dp, DarkBorder)
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Add Supplier", color = TextWhite, fontWeight = FontWeight.Bold, fontSize = 16.sp)

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Company / Supplier Name") },
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
                    value = contact,
                    onValueChange = { contact = it },
                    label = { Text("Contact Person") },
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

                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    FuturisticButton(text = "Cancel", onClick = onDismiss, isSecondary = true, modifier = Modifier.weight(1f))
                    FuturisticButton(
                        text = "Save Supplier",
                        enabled = name.isNotBlank(),
                        onClick = { onAdd(name, phone, address, contact, balanceText.toDoubleOrNull() ?: 0.0) },
                        modifier = Modifier.weight(1.3f)
                    )
                }
            }
        }
    }
}
