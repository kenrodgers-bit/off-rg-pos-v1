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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.Customer
import com.example.ui.components.EmptyStateView
import com.example.ui.components.FuturisticButton
import com.example.ui.theme.*
import com.example.ui.viewmodel.CartItem
import com.example.ui.viewmodel.PosViewModel
import com.example.util.CurrencyFormatter
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CartScreen(
    viewModel: PosViewModel,
    onNavigateBack: () -> Unit,
    onProceedToPayment: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    val cartItems by viewModel.cartItems.collectAsStateWithLifecycle()
    val subtotal by viewModel.cartSubtotal.collectAsStateWithLifecycle()
    val total by viewModel.cartTotal.collectAsStateWithLifecycle()
    val orderDiscount by viewModel.orderDiscount.collectAsStateWithLifecycle()
    val selectedCustomer by viewModel.selectedCustomer.collectAsStateWithLifecycle()
    val allCustomers by viewModel.allCustomers.collectAsStateWithLifecycle()
    val business by viewModel.business.collectAsStateWithLifecycle()

    var showCustomerDialog by remember { mutableStateOf(false) }
    var showDiscountDialog by remember { mutableStateOf(false) }
    var editingItemForPrice by remember { mutableStateOf<CartItem?>(null) }

    Scaffold(
        containerColor = DarkBg,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Current Sale (${cartItems.size})",
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
                    if (cartItems.isNotEmpty()) {
                        IconButton(onClick = { viewModel.clearCart() }) {
                            Icon(
                                imageVector = Icons.Default.DeleteOutline,
                                contentDescription = "Clear Cart",
                                tint = AlertRed
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = DarkBg)
            )
        }
    ) { innerPadding ->
        if (cartItems.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                EmptyStateView(
                    icon = Icons.Default.RemoveShoppingCart,
                    title = "Cart is empty",
                    description = "Add products from the POS screen to checkout.",
                    actionButtonText = "Back to Products",
                    onActionClick = onNavigateBack
                )
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(horizontal = 16.dp)
                    .testTag("cart_screen"),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = PaddingValues(vertical = 10.dp)
                ) {
                    // Customer Selector Card (Part 10)
                    item {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = DarkSurfaceCard),
                            border = androidx.compose.foundation.BorderStroke(1.dp, DarkBorder),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { showCustomerDialog = true }
                                .testTag("select_customer_trigger")
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(38.dp)
                                            .clip(CircleShape)
                                            .background(DarkSurfaceElevated),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Person,
                                            contentDescription = null,
                                            tint = RgAccent,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                    Column {
                                        Text(
                                            text = selectedCustomer?.name ?: "Walk-in Customer",
                                            color = TextWhite,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 15.sp
                                        )
                                        Text(
                                            text = if (selectedCustomer != null) {
                                                "Credit Limit: ${CurrencyFormatter.format(selectedCustomer!!.creditLimit)} • Bal: ${CurrencyFormatter.format(selectedCustomer!!.outstandingCredit)}"
                                            } else {
                                                "Tap to select registered customer"
                                            },
                                            color = if (selectedCustomer?.outstandingCredit ?: 0.0 > 0) WarningOrange else TextMuted,
                                            fontSize = 12.sp
                                        )
                                    }
                                }

                                Text(
                                    text = "Change",
                                    color = RgAccent,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }

                    // Cart Items List
                    items(cartItems) { item ->
                        CartItemRow(
                            item = item,
                            onQuantityChange = { newQty -> viewModel.updateCartItemQuantity(item.id, newQty) },
                            onDelete = { viewModel.removeCartItem(item.id) },
                            onEditPrice = { editingItemForPrice = item }
                        )
                    }
                }

                // Summary & Actions Footer
                Card(
                    colors = CardDefaults.cardColors(containerColor = DarkSurfaceCard),
                    border = androidx.compose.foundation.BorderStroke(1.dp, DarkBorder),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Subtotal
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Subtotal", color = TextMuted, fontSize = 13.sp)
                            Text(CurrencyFormatter.format(subtotal), color = TextWhite, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                        }

                        // Order Discount
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { showDiscountDialog = true },
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text("Discount", color = TextMuted, fontSize = 13.sp)
                                Icon(Icons.Default.Edit, contentDescription = "Edit Discount", tint = RgAccent, modifier = Modifier.size(14.dp))
                            }
                            Text(
                                text = if (orderDiscount > 0) "-${CurrencyFormatter.format(orderDiscount)}" else "KSh 0 (Tap to add)",
                                color = if (orderDiscount > 0) SuccessGreen else TextSubtle,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }

                        // Tax (if enabled)
                        if (business?.taxRatePercent ?: 0.0 > 0.0) {
                            val taxAmount = (subtotal - orderDiscount).coerceAtLeast(0.0) * ((business?.taxRatePercent ?: 0.0) / 100.0)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Tax (${business?.taxRatePercent}%)", color = TextMuted, fontSize = 13.sp)
                                Text(CurrencyFormatter.format(taxAmount), color = TextWhite, fontSize = 13.sp)
                            }
                        }

                        HorizontalDivider(color = DarkDivider)

                        // Grand Total
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("TOTAL", color = TextWhite, fontWeight = FontWeight.Black, fontSize = 16.sp)
                            Text(CurrencyFormatter.format(total), color = RgAccent, fontWeight = FontWeight.Black, fontSize = 22.sp)
                        }

                        Spacer(modifier = Modifier.height(4.dp))

                        // Bottom Actions: Hold Sale and Checkout
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            FuturisticButton(
                                text = "Hold Sale",
                                onClick = {
                                    viewModel.holdCurrentSale {
                                        Toast.makeText(context, "Sale held successfully", Toast.LENGTH_SHORT).show()
                                        onNavigateBack()
                                    }
                                },
                                isSecondary = true,
                                icon = Icons.Default.Pause,
                                modifier = Modifier.weight(1f),
                                testTag = "hold_sale_button"
                            )

                            FuturisticButton(
                                text = "Checkout",
                                onClick = onProceedToPayment,
                                icon = Icons.Default.Payment,
                                modifier = Modifier.weight(1.4f),
                                testTag = "proceed_to_payment_button"
                            )
                        }
                    }
                }
            }
        }

        // Customer Selection Dialog (Part 10)
        if (showCustomerDialog) {
            CustomerSelectionModal(
                customers = allCustomers,
                selected = selectedCustomer,
                onSelectCustomer = { customer ->
                    viewModel.selectCustomer(customer)
                    showCustomerDialog = false
                },
                onQuickAddCustomer = { newName, newPhone, limit ->
                    coroutineScope.launch {
                        val newCust = Customer(
                            name = newName,
                            phone = newPhone,
                            creditLimit = limit
                        )
                        viewModel.repository.saveCustomer(newCust)
                        viewModel.selectCustomer(newCust)
                        showCustomerDialog = false
                    }
                },
                onDismiss = { showCustomerDialog = false }
            )
        }

        // Order Discount Dialog
        if (showDiscountDialog) {
            DiscountInputDialog(
                currentDiscount = orderDiscount,
                onApply = { disc ->
                    viewModel.setOrderDiscount(disc)
                    showDiscountDialog = false
                },
                onDismiss = { showDiscountDialog = false }
            )
        }

        // Custom Item Price Dialog
        if (editingItemForPrice != null) {
            ItemPriceOverrideDialog(
                item = editingItemForPrice!!,
                onApply = { newPrice ->
                    viewModel.updateCartItemPrice(editingItemForPrice!!.id, newPrice)
                    editingItemForPrice = null
                },
                onDismiss = { editingItemForPrice = null }
            )
        }
    }
}

@Composable
fun CartItemRow(
    item: CartItem,
    onQuantityChange: (Double) -> Unit,
    onDelete: () -> Unit,
    onEditPrice: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = DarkSurfaceCard),
        border = androidx.compose.foundation.BorderStroke(1.dp, DarkBorder),
        modifier = Modifier.fillMaxWidth().testTag("cart_item_row_${item.id}")
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = item.product.name,
                        color = TextWhite,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 15.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Unit: ${item.unitName}",
                            color = RgAccent,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                        if (item.conversionFactor > 1.0) {
                            Text(
                                text = "(${item.totalBaseUnits.toInt()} ${item.product.baseUnit}s)",
                                color = TextSubtle,
                                fontSize = 11.sp
                            )
                        }
                    }
                }

                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Remove",
                        tint = TextMuted,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            HorizontalDivider(color = DarkDivider)

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Quantity Stepper
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    IconButton(
                        onClick = { onQuantityChange(item.quantity - 1) },
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(DarkSurfaceElevated)
                            .border(1.dp, DarkBorder, CircleShape)
                    ) {
                        Icon(Icons.Default.Remove, contentDescription = "Decrease", tint = TextWhite, modifier = Modifier.size(14.dp))
                    }

                    Text(
                        text = if (item.quantity % 1.0 == 0.0) item.quantity.toInt().toString() else "%.1f".format(item.quantity),
                        color = TextWhite,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )

                    IconButton(
                        onClick = { onQuantityChange(item.quantity + 1) },
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(DarkSurfaceElevated)
                            .border(1.dp, DarkBorder, CircleShape)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "Increase", tint = TextWhite, modifier = Modifier.size(14.dp))
                    }
                }

                // Price with override option
                Column(horizontalAlignment = Alignment.End) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier.clickable { onEditPrice() }
                    ) {
                        Text(
                            text = CurrencyFormatter.format(item.subtotal),
                            color = RgAccent,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                        Icon(Icons.Default.Edit, contentDescription = "Edit Price", tint = TextSubtle, modifier = Modifier.size(12.dp))
                    }
                    Text(
                        text = "@ ${CurrencyFormatter.format(item.unitPrice)} / ${item.unitName}",
                        color = TextSubtle,
                        fontSize = 11.sp
                    )
                }
            }
        }
    }
}

@Composable
fun CustomerSelectionModal(
    customers: List<Customer>,
    selected: Customer?,
    onSelectCustomer: (Customer?) -> Unit,
    onQuickAddCustomer: (name: String, phone: String, limit: Double) -> Unit,
    onDismiss: () -> Unit
) {
    var search by remember { mutableStateOf("") }
    var isAddingNew by remember { mutableStateOf(false) }
    var newName by remember { mutableStateOf("") }
    var newPhone by remember { mutableStateOf("") }
    var newLimit by remember { mutableStateOf("20000") }

    val filtered = customers.filter {
        search.isBlank() || it.name.contains(search, ignoreCase = true) || it.phone.contains(search)
    }

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
                    Text(
                        text = if (!isAddingNew) "Select Customer" else "Add New Customer",
                        color = TextWhite,
                        fontWeight = FontWeight.Bold,
                        fontSize = 17.sp
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = TextMuted)
                    }
                }

                if (!isAddingNew) {
                    OutlinedTextField(
                        value = search,
                        onValueChange = { search = it },
                        placeholder = { Text("Search customer by name or phone...", fontSize = 12.sp) },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = RgAccent,
                            unfocusedBorderColor = DarkBorder,
                            focusedTextColor = TextWhite,
                            unfocusedTextColor = TextWhite
                        ),
                        singleLine = true
                    )

                    // Walk-in option
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (selected == null) DarkSurfaceElevated else DarkBg)
                            .border(1.dp, if (selected == null) RgAccent else DarkBorder, RoundedCornerShape(8.dp))
                            .clickable { onSelectCustomer(null) }
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Walk-in Customer", color = TextWhite, fontWeight = FontWeight.SemiBold)
                        if (selected == null) {
                            Icon(Icons.Default.Check, contentDescription = null, tint = RgAccent, modifier = Modifier.size(16.dp))
                        }
                    }

                    LazyColumn(
                        modifier = Modifier.heightIn(max = 240.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        items(filtered) { c ->
                            val isSel = selected?.id == c.id
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isSel) DarkSurfaceElevated else DarkBg)
                                    .border(1.dp, if (isSel) RgAccent else DarkBorder, RoundedCornerShape(8.dp))
                                    .clickable { onSelectCustomer(c) }
                                    .padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(c.name, color = TextWhite, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                    Text("${c.phone} • Credit Bal: ${CurrencyFormatter.format(c.outstandingCredit)}", color = if (c.outstandingCredit > 0) WarningOrange else TextMuted, fontSize = 11.sp)
                                }
                                if (isSel) {
                                    Icon(Icons.Default.Check, contentDescription = null, tint = RgAccent, modifier = Modifier.size(16.dp))
                                }
                            }
                        }
                    }

                    FuturisticButton(
                        text = "+ New Customer",
                        onClick = { isAddingNew = true },
                        isSecondary = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                } else {
                    OutlinedTextField(
                        value = newName,
                        onValueChange = { newName = it },
                        label = { Text("Customer Name") },
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
                        value = newPhone,
                        onValueChange = { newPhone = it },
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
                        value = newLimit,
                        onValueChange = { newLimit = it },
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

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        FuturisticButton(
                            text = "Cancel",
                            onClick = { isAddingNew = false },
                            isSecondary = true,
                            modifier = Modifier.weight(1f)
                        )
                        FuturisticButton(
                            text = "Save & Select",
                            enabled = newName.isNotBlank(),
                            onClick = {
                                onQuickAddCustomer(newName, newPhone, newLimit.toDoubleOrNull() ?: 0.0)
                            },
                            modifier = Modifier.weight(1.2f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun DiscountInputDialog(
    currentDiscount: Double,
    onApply: (Double) -> Unit,
    onDismiss: () -> Unit
) {
    var discountText by remember { mutableStateOf(if (currentDiscount > 0) currentDiscount.toString() else "") }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier.fillMaxWidth().padding(14.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = DarkSurfaceCard),
            border = androidx.compose.foundation.BorderStroke(1.dp, DarkBorder)
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Text("Order Discount", color = TextWhite, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                OutlinedTextField(
                    value = discountText,
                    onValueChange = { discountText = it },
                    label = { Text("Discount Amount (KSh)") },
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
                        text = "Apply",
                        onClick = { onApply(discountText.toDoubleOrNull() ?: 0.0) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@Composable
fun ItemPriceOverrideDialog(
    item: CartItem,
    onApply: (Double) -> Unit,
    onDismiss: () -> Unit
) {
    var priceText by remember { mutableStateOf(item.unitPrice.toString()) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier.fillMaxWidth().padding(14.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = DarkSurfaceCard),
            border = androidx.compose.foundation.BorderStroke(1.dp, DarkBorder)
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Text("Override Price: ${item.product.name}", color = TextWhite, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                Text("Current Unit: ${item.unitName}", color = TextMuted, fontSize = 12.sp)
                OutlinedTextField(
                    value = priceText,
                    onValueChange = { priceText = it },
                    label = { Text("Unit Price (KSh)") },
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
                        text = "Update",
                        onClick = { onApply(priceText.toDoubleOrNull() ?: item.unitPrice) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}
