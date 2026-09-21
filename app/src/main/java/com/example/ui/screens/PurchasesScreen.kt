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
import com.example.data.model.*
import com.example.ui.components.EmptyStateView
import com.example.ui.components.FuturisticButton
import com.example.ui.theme.*
import com.example.ui.viewmodel.PosViewModel
import com.example.util.CurrencyFormatter
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PurchasesScreen(
    viewModel: PosViewModel,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    val purchases by viewModel.allPurchases.collectAsStateWithLifecycle()
    val allSuppliers by viewModel.allSuppliers.collectAsStateWithLifecycle()
    val allProducts by viewModel.allProducts.collectAsStateWithLifecycle()
    val currentUser by viewModel.currentUser.collectAsStateWithLifecycle()

    var showNewPurchaseDialog by remember { mutableStateOf(false) }

    Scaffold(
        containerColor = DarkBg,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Stock Purchases (${purchases.size})",
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
                    IconButton(onClick = { showNewPurchaseDialog = true }) {
                        Icon(Icons.Default.Add, contentDescription = "New Purchase", tint = RgAccent)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = DarkBg)
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showNewPurchaseDialog = true },
                containerColor = RgAccent,
                contentColor = DarkBg,
                shape = CircleShape,
                modifier = Modifier.testTag("receive_stock_fab")
            ) {
                Icon(Icons.Default.Add, contentDescription = "Receive Stock")
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .widthIn(max = 900.dp)
                .padding(horizontal = 16.dp)
                .testTag("purchases_screen")
        ) {
            if (purchases.isEmpty()) {
                EmptyStateView(
                    icon = Icons.Default.LocalShipping,
                    title = "No purchases yet",
                    description = "Record inventory deliveries and supplier invoices.",
                    actionButtonText = "Receive Stock",
                    onActionClick = { showNewPurchaseDialog = true }
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = PaddingValues(vertical = 12.dp)
                ) {
                    items(purchases) { purchase ->
                        Card(
                            colors = CardDefaults.cardColors(containerColor = DarkSurfaceCard),
                            border = androidx.compose.foundation.BorderStroke(1.dp, DarkBorder),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(14.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Invoice #${purchase.invoiceNumber}",
                                        color = TextWhite,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 15.sp
                                    )
                                    Text(
                                        text = CurrencyFormatter.format(purchase.totalAmount),
                                        color = SuccessGreen,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 15.sp
                                    )
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = "Supplier: ${purchase.supplierName}",
                                        color = TextMuted,
                                        fontSize = 12.sp
                                    )
                                    Text(
                                        text = CurrencyFormatter.formatDateShort(purchase.dateEpoch),
                                        color = TextSubtle,
                                        fontSize = 12.sp
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // New Purchase Modal Dialog (Part 22)
        if (showNewPurchaseDialog) {
            NewPurchaseDialog(
                suppliers = allSuppliers,
                products = allProducts,
                viewModel = viewModel,
                onDismiss = { showNewPurchaseDialog = false },
                onComplete = {
                    showNewPurchaseDialog = false
                    Toast.makeText(context, "Stock received and added to inventory", Toast.LENGTH_SHORT).show()
                }
            )
        }
    }
}

@Composable
fun NewPurchaseDialog(
    suppliers: List<Supplier>,
    products: List<Product>,
    viewModel: PosViewModel,
    onDismiss: () -> Unit,
    onComplete: () -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    var invoiceNumber by remember { mutableStateOf("INV-${System.currentTimeMillis().toString().takeLast(5)}") }
    var selectedSupplier by remember { mutableStateOf(suppliers.firstOrNull()) }
    var selectedProduct by remember { mutableStateOf(products.firstOrNull()) }
    var quantityText by remember { mutableStateOf("10") }
    var unitPriceText by remember { mutableStateOf("") }
    var unitName by remember { mutableStateOf("Carton") }
    var factorText by remember { mutableStateOf("12") }

    LaunchedEffect(selectedProduct) {
        if (selectedProduct != null) {
            unitName = selectedProduct!!.baseUnit
            factorText = "1"
            unitPriceText = selectedProduct!!.buyingCost.toString()
        }
    }

    val quantity = quantityText.toDoubleOrNull() ?: 0.0
    val unitPrice = unitPriceText.toDoubleOrNull() ?: 0.0
    val factor = factorText.toDoubleOrNull() ?: 1.0
    val totalCost = quantity * unitPrice
    val baseUnitsToAdd = quantity * factor

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier.fillMaxWidth().padding(8.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = DarkSurfaceCard),
            border = androidx.compose.foundation.BorderStroke(1.dp, DarkBorder)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Receive Stock (Stock In)", color = TextWhite, fontWeight = FontWeight.Bold, fontSize = 17.sp)
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = TextMuted)
                    }
                }

                OutlinedTextField(
                    value = invoiceNumber,
                    onValueChange = { invoiceNumber = it },
                    label = { Text("Invoice / Delivery Ref #") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = RgAccent,
                        unfocusedBorderColor = DarkBorder,
                        focusedTextColor = TextWhite,
                        unfocusedTextColor = TextWhite
                    ),
                    singleLine = true
                )

                // Select Product
                Text("Select Product:", color = TextMuted, fontSize = 12.sp)
                LazyColumn(modifier = Modifier.heightIn(max = 120.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    items(products) { p ->
                        val isSel = p.id == selectedProduct?.id
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(6.dp))
                                .background(if (isSel) DarkSurfaceElevated else DarkBg)
                                .border(1.dp, if (isSel) RgAccent else DarkBorder, RoundedCornerShape(6.dp))
                                .clickable { selectedProduct = p }
                                .padding(8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(p.name, color = TextWhite, fontSize = 12.sp)
                            Text("Stock: ${p.currentStockBase.toInt()} ${p.baseUnit}s", color = RgAccent, fontSize = 11.sp)
                        }
                    }
                }

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = unitName,
                        onValueChange = { unitName = it },
                        label = { Text("Unit (e.g. Carton)") },
                        modifier = Modifier.weight(1f),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = RgAccent,
                            unfocusedBorderColor = DarkBorder,
                            focusedTextColor = TextWhite,
                            unfocusedTextColor = TextWhite
                        ),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = factorText,
                        onValueChange = { factorText = it },
                        label = { Text("Factor (pcs/unit)") },
                        modifier = Modifier.weight(1f),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = RgAccent,
                            unfocusedBorderColor = DarkBorder,
                            focusedTextColor = TextWhite,
                            unfocusedTextColor = TextWhite
                        ),
                        singleLine = true
                    )
                }

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = quantityText,
                        onValueChange = { quantityText = it },
                        label = { Text("Quantity") },
                        modifier = Modifier.weight(1f),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = RgAccent,
                            unfocusedBorderColor = DarkBorder,
                            focusedTextColor = TextWhite,
                            unfocusedTextColor = TextWhite
                        ),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = unitPriceText,
                        onValueChange = { unitPriceText = it },
                        label = { Text("Cost per Unit") },
                        modifier = Modifier.weight(1f),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = RgAccent,
                            unfocusedBorderColor = DarkBorder,
                            focusedTextColor = TextWhite,
                            unfocusedTextColor = TextWhite
                        ),
                        singleLine = true
                    )
                }

                // Live calculation preview
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(DarkSurfaceElevated)
                        .padding(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Base Units Added to Stock:", color = TextMuted, fontSize = 12.sp)
                        Text("+${baseUnitsToAdd.toInt()} ${selectedProduct?.baseUnit ?: "unit"}s", color = SuccessGreen, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Total Invoice Cost:", color = TextMuted, fontSize = 12.sp)
                        Text(CurrencyFormatter.format(totalCost), color = RgAccent, fontWeight = FontWeight.Black, fontSize = 15.sp)
                    }
                }

                FuturisticButton(
                    text = "Confirm & Receive Stock",
                    icon = Icons.Default.Check,
                    enabled = selectedProduct != null && quantity > 0 && unitPrice > 0,
                    onClick = {
                        coroutineScope.launch {
                            val purchase = Purchase(
                                invoiceNumber = invoiceNumber,
                                supplierId = selectedSupplier?.id ?: 0L,
                                supplierName = selectedSupplier?.name ?: "Direct Supplier",
                                totalAmount = totalCost,
                                itemsCount = 1,
                                staffName = "Staff"
                            )
                            val purchaseItem = PurchaseItem(
                                purchaseId = 0,
                                productId = selectedProduct!!.id,
                                productName = selectedProduct!!.name,
                                unitName = unitName,
                                quantity = quantity,
                                costPerUnit = unitPrice,
                                totalCost = totalCost,
                                baseUnitsAdded = baseUnitsToAdd
                            )
                            viewModel.repository.receivePurchase(purchase, listOf(purchaseItem), "Staff")
                            onComplete()
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}
