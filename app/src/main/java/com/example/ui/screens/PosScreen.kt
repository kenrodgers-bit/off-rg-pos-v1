package com.example.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.Product
import com.example.data.model.UnitConversion
import com.example.ui.components.EmptyStateView
import com.example.ui.components.FuturisticButton
import com.example.ui.theme.*
import com.example.ui.viewmodel.PosViewModel
import com.example.util.CurrencyFormatter
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PosScreen(
    viewModel: PosViewModel,
    onNavigateToCart: () -> Unit,
    onNavigateToHeldSales: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    val allProducts by viewModel.allProducts.collectAsStateWithLifecycle()
    val cartItems by viewModel.cartItems.collectAsStateWithLifecycle()
    val cartTotal by viewModel.cartTotal.collectAsStateWithLifecycle()
    val saleType by viewModel.saleType.collectAsStateWithLifecycle()
    val heldSales by viewModel.heldSales.collectAsStateWithLifecycle()

    var searchQuery by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("All") }
    var selectedProductForUnit by remember { mutableStateOf<Product?>(null) }
    var barcodeSearchDialog by remember { mutableStateOf(false) }

    val categories = remember(allProducts) {
        listOf("All") + allProducts.map { it.categoryName }.distinct()
    }

    val filteredProducts = remember(allProducts, searchQuery, selectedCategory) {
        allProducts.filter { product ->
            val matchesQuery = searchQuery.isBlank() ||
                    product.name.contains(searchQuery, ignoreCase = true) ||
                    product.sku.contains(searchQuery, ignoreCase = true) ||
                    product.barcode.contains(searchQuery, ignoreCase = true)
            val matchesCat = selectedCategory == "All" || product.categoryName.equals(selectedCategory, ignoreCase = true)
            matchesQuery && matchesCat
        }
    }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBg)
            .testTag("pos_screen")
    ) {
        val isTablet = maxWidth >= 720.dp

        if (isTablet) {
            // Tablet / Landscape Split Pane Layout
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Left Column: Product Catalog & Search (Weight 1.15)
                Column(
                    modifier = Modifier
                        .weight(1.15f)
                        .fillMaxHeight(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Header Row: Title + Retail/Wholesale Toggle + Held Sales Icon
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "New Sale",
                                color = TextWhite,
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Select products & units to sell",
                                color = TextMuted,
                                fontSize = 12.sp
                            )
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            // Retail / Wholesale Pill Selector
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(DarkSurfaceCard)
                                    .border(1.dp, DarkBorder, RoundedCornerShape(8.dp))
                                    .padding(2.dp)
                            ) {
                                Row {
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(if (saleType == "RETAIL") RgAccent else Color.Transparent)
                                            .clickable { viewModel.setSaleType("RETAIL") }
                                            .padding(horizontal = 10.dp, vertical = 6.dp)
                                            .testTag("retail_mode_toggle_tablet")
                                    ) {
                                        Text(
                                            text = "Retail",
                                            color = if (saleType == "RETAIL") DarkBg else TextMuted,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(if (saleType == "WHOLESALE") RgAccent else Color.Transparent)
                                            .clickable { viewModel.setSaleType("WHOLESALE") }
                                            .padding(horizontal = 10.dp, vertical = 6.dp)
                                            .testTag("wholesale_mode_toggle_tablet")
                                    ) {
                                        Text(
                                            text = "Wholesale",
                                            color = if (saleType == "WHOLESALE") DarkBg else TextMuted,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }

                            // Held sales badge
                            IconButton(
                                onClick = onNavigateToHeldSales,
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(CircleShape)
                                    .background(DarkSurfaceCard)
                                    .border(1.dp, DarkBorder, CircleShape)
                                    .testTag("held_sales_button_tablet")
                            ) {
                                BadgedBox(
                                    badge = {
                                        if (heldSales.isNotEmpty()) {
                                            Badge(
                                                containerColor = WarningOrange,
                                                contentColor = DarkBg
                                            ) {
                                                Text("${heldSales.size}")
                                            }
                                        }
                                    }
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.PauseCircle,
                                        contentDescription = "Held Sales",
                                        tint = if (heldSales.isNotEmpty()) WarningOrange else TextMuted,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        }
                    }

                    // Search Bar & Barcode Scanner Button
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            placeholder = { Text("Search products by name, SKU or barcode...", fontSize = 13.sp, color = TextMuted) },
                            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = TextMuted) },
                            trailingIcon = {
                                if (searchQuery.isNotEmpty()) {
                                    IconButton(onClick = { searchQuery = "" }) {
                                        Icon(Icons.Default.Close, contentDescription = "Clear", tint = TextMuted)
                                    }
                                }
                            },
                            modifier = Modifier
                                .weight(1f)
                                .height(52.dp)
                                .testTag("pos_search_input_tablet"),
                            shape = RoundedCornerShape(10.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = RgAccent,
                                unfocusedBorderColor = DarkBorder,
                                focusedTextColor = TextWhite,
                                unfocusedTextColor = TextWhite,
                                focusedContainerColor = DarkSurfaceCard,
                                unfocusedContainerColor = DarkSurfaceCard
                            ),
                            singleLine = true
                        )

                        IconButton(
                            onClick = { barcodeSearchDialog = true },
                            modifier = Modifier
                                .size(52.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(DarkSurfaceCard)
                                .border(1.dp, DarkBorder, RoundedCornerShape(10.dp))
                                .testTag("barcode_scanner_button_tablet")
                        ) {
                            Icon(
                                imageVector = Icons.Default.QrCodeScanner,
                                contentDescription = "Scan Barcode",
                                tint = RgAccent,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }

                    // Category Filter Pills
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        contentPadding = PaddingValues(vertical = 2.dp)
                    ) {
                        items(categories) { category ->
                            val isSelected = category.equals(selectedCategory, ignoreCase = true)
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(20.dp))
                                    .background(if (isSelected) RgAccent else DarkSurfaceCard)
                                    .border(1.dp, if (isSelected) RgAccent else DarkBorder, RoundedCornerShape(20.dp))
                                    .clickable { selectedCategory = category }
                                    .padding(horizontal = 14.dp, vertical = 7.dp)
                            ) {
                                Text(
                                    text = category,
                                    color = if (isSelected) DarkBg else TextWhite,
                                    fontSize = 12.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                )
                            }
                        }
                    }

                    // Product List
                    if (filteredProducts.isEmpty()) {
                        EmptyStateView(
                            icon = Icons.Default.SearchOff,
                            title = "No products found",
                            description = if (searchQuery.isNotEmpty()) "No items matching \"$searchQuery\"" else "Add your first product to start selling."
                        )
                    } else {
                        LazyColumn(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            contentPadding = PaddingValues(bottom = 16.dp)
                        ) {
                            items(filteredProducts) { product ->
                                ProductPosRow(
                                    product = product,
                                    saleType = saleType,
                                    onClick = { selectedProductForUnit = product }
                                )
                            }
                        }
                    }
                }

                // Right Column: Live Sale Cart Pane (Weight 0.85)
                Card(
                    modifier = Modifier
                        .weight(0.85f)
                        .fillMaxHeight(),
                    colors = CardDefaults.cardColors(containerColor = DarkSurfaceCard),
                    border = androidx.compose.foundation.BorderStroke(1.dp, DarkBorder),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            // Header
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.ShoppingCart,
                                        contentDescription = null,
                                        tint = RgAccent
                                    )
                                    Text(
                                        text = "Current Sale (${cartItems.size})",
                                        color = TextWhite,
                                        fontSize = 18.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                if (cartItems.isNotEmpty()) {
                                    IconButton(onClick = { viewModel.clearCart() }) {
                                        Icon(
                                            imageVector = Icons.Default.DeleteOutline,
                                            contentDescription = "Clear",
                                            tint = AlertRed
                                        )
                                    }
                                }
                            }

                            HorizontalDivider(
                                color = DarkBorder,
                                modifier = Modifier.padding(vertical = 12.dp)
                            )

                            if (cartItems.isEmpty()) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .weight(1f),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.ShoppingBag,
                                            contentDescription = null,
                                            tint = TextMuted,
                                            modifier = Modifier.size(48.dp)
                                        )
                                        Text(
                                            text = "Cart is Empty",
                                            color = TextWhite,
                                            fontWeight = FontWeight.SemiBold,
                                            fontSize = 16.sp
                                        )
                                        Text(
                                            text = "Tap any product to add it to this sale",
                                            color = TextMuted,
                                            fontSize = 13.sp
                                        )
                                    }
                                }
                            } else {
                                LazyColumn(
                                    modifier = Modifier.weight(1f),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    items(cartItems) { item ->
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clip(RoundedCornerShape(10.dp))
                                                .background(DarkSurfaceElevated)
                                                .border(1.dp, DarkBorder, RoundedCornerShape(10.dp))
                                                .padding(10.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    text = item.product.name,
                                                    color = TextWhite,
                                                    fontWeight = FontWeight.SemiBold,
                                                    fontSize = 13.sp,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                                Text(
                                                    text = "${CurrencyFormatter.format(item.unitPrice)} / ${item.unitName}",
                                                    color = TextMuted,
                                                    fontSize = 11.sp
                                                )
                                            }

                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(2.dp)
                                            ) {
                                                IconButton(
                                                    onClick = {
                                                        viewModel.updateCartItemQuantity(item.id, item.quantity - 1)
                                                    },
                                                    modifier = Modifier.size(28.dp)
                                                ) {
                                                    Icon(
                                                        Icons.Default.Remove,
                                                        contentDescription = "Decrease",
                                                        tint = TextWhite,
                                                        modifier = Modifier.size(16.dp)
                                                    )
                                                }
                                                Text(
                                                    text = if (item.quantity % 1.0 == 0.0) "${item.quantity.toInt()}" else "${item.quantity}",
                                                    color = TextWhite,
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 13.sp
                                                )
                                                IconButton(
                                                    onClick = {
                                                        viewModel.updateCartItemQuantity(item.id, item.quantity + 1)
                                                    },
                                                    modifier = Modifier.size(28.dp)
                                                ) {
                                                    Icon(
                                                        Icons.Default.Add,
                                                        contentDescription = "Increase",
                                                        tint = RgAccent,
                                                        modifier = Modifier.size(16.dp)
                                                    )
                                                }
                                            }

                                            Spacer(modifier = Modifier.width(6.dp))

                                            Text(
                                                text = CurrencyFormatter.format(item.subtotal),
                                                color = RgAccent,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 13.sp
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        // Bottom checkout summary
                        if (cartItems.isNotEmpty()) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 8.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                HorizontalDivider(color = DarkBorder)
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("Total Payable:", color = TextWhite, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                    Text(
                                        CurrencyFormatter.format(cartTotal),
                                        color = RgAccent,
                                        fontWeight = FontWeight.Black,
                                        fontSize = 18.sp
                                    )
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    OutlinedButton(
                                        onClick = {
                                            viewModel.holdCurrentSale {
                                                Toast.makeText(context, "Sale held", Toast.LENGTH_SHORT).show()
                                            }
                                        },
                                        colors = ButtonDefaults.outlinedButtonColors(contentColor = WarningOrange),
                                        border = androidx.compose.foundation.BorderStroke(1.dp, WarningOrange.copy(alpha = 0.5f)),
                                        shape = RoundedCornerShape(10.dp),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Text("Hold Sale", fontSize = 12.sp)
                                    }

                                    FuturisticButton(
                                        text = "Pay",
                                        icon = Icons.Default.Payment,
                                        onClick = onNavigateToCart,
                                        modifier = Modifier.weight(1.3f)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        } else {
            // Standard Phone Single Column Layout
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Spacer(modifier = Modifier.height(6.dp))

                // Header Row: Title + Retail/Wholesale Toggle + Held Sales Icon
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "New Sale",
                            color = TextWhite,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Select products & units to sell",
                            color = TextMuted,
                            fontSize = 12.sp
                        )
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Retail / Wholesale Pill Selector
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(DarkSurfaceCard)
                                .border(1.dp, DarkBorder, RoundedCornerShape(8.dp))
                                .padding(2.dp)
                        ) {
                            Row {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(if (saleType == "RETAIL") RgAccent else Color.Transparent)
                                        .clickable { viewModel.setSaleType("RETAIL") }
                                        .padding(horizontal = 10.dp, vertical = 6.dp)
                                        .testTag("retail_mode_toggle")
                                ) {
                                    Text(
                                        text = "Retail",
                                        color = if (saleType == "RETAIL") DarkBg else TextMuted,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(if (saleType == "WHOLESALE") RgAccent else Color.Transparent)
                                        .clickable { viewModel.setSaleType("WHOLESALE") }
                                        .padding(horizontal = 10.dp, vertical = 6.dp)
                                        .testTag("wholesale_mode_toggle")
                                ) {
                                    Text(
                                        text = "Wholesale",
                                        color = if (saleType == "WHOLESALE") DarkBg else TextMuted,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }

                        // Held sales badge
                        IconButton(
                            onClick = onNavigateToHeldSales,
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(DarkSurfaceCard)
                                .border(1.dp, DarkBorder, CircleShape)
                                .testTag("held_sales_button")
                        ) {
                            BadgedBox(
                                badge = {
                                    if (heldSales.isNotEmpty()) {
                                        Badge(
                                            containerColor = WarningOrange,
                                            contentColor = DarkBg
                                        ) {
                                            Text("${heldSales.size}")
                                        }
                                    }
                                }
                            ) {
                                Icon(
                                    imageVector = Icons.Default.PauseCircle,
                                    contentDescription = "Held Sales",
                                    tint = if (heldSales.isNotEmpty()) WarningOrange else TextMuted,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }

                // Search Bar & Barcode Scanner Button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = { Text("Search products by name, SKU or barcode...", fontSize = 13.sp, color = TextMuted) },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = TextMuted) },
                        trailingIcon = {
                            if (searchQuery.isNotEmpty()) {
                                IconButton(onClick = { searchQuery = "" }) {
                                    Icon(Icons.Default.Close, contentDescription = "Clear", tint = TextMuted)
                                }
                            }
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(52.dp)
                            .testTag("pos_search_input"),
                        shape = RoundedCornerShape(10.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = RgAccent,
                            unfocusedBorderColor = DarkBorder,
                            focusedTextColor = TextWhite,
                            unfocusedTextColor = TextWhite,
                            focusedContainerColor = DarkSurfaceCard,
                            unfocusedContainerColor = DarkSurfaceCard
                        ),
                        singleLine = true
                    )

                    IconButton(
                        onClick = { barcodeSearchDialog = true },
                        modifier = Modifier
                            .size(52.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(DarkSurfaceCard)
                            .border(1.dp, DarkBorder, RoundedCornerShape(10.dp))
                            .testTag("barcode_scanner_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.QrCodeScanner,
                            contentDescription = "Scan Barcode",
                            tint = RgAccent,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }

                // Category Filter Pills
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(vertical = 2.dp)
                ) {
                    items(categories) { category ->
                        val isSelected = category.equals(selectedCategory, ignoreCase = true)
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(20.dp))
                                .background(if (isSelected) RgAccent else DarkSurfaceCard)
                                .border(1.dp, if (isSelected) RgAccent else DarkBorder, RoundedCornerShape(20.dp))
                            .clickable { selectedCategory = category }
                            .padding(horizontal = 14.dp, vertical = 7.dp)
                            .testTag("category_pill_$category")
                        ) {
                            Text(
                                text = category,
                                color = if (isSelected) DarkBg else TextWhite,
                                fontSize = 12.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                            )
                        }
                    }
                }

                // Product List
                if (filteredProducts.isEmpty()) {
                    EmptyStateView(
                        icon = Icons.Default.SearchOff,
                        title = "No products found",
                        description = if (searchQuery.isNotEmpty()) "No items matching \"$searchQuery\"" else "Add your first product to start selling."
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        contentPadding = PaddingValues(bottom = 110.dp)
                    ) {
                        items(filteredProducts) { product ->
                            ProductPosRow(
                                product = product,
                                saleType = saleType,
                                onClick = { selectedProductForUnit = product }
                            )
                        }
                    }
                }
            }

            // Floating Cart Summary Bar (Only on phone)
            if (cartItems.isNotEmpty()) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.BottomCenter)
                        .padding(horizontal = 16.dp, vertical = 24.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .border(1.dp, RgAccent.copy(alpha = 0.5f), RoundedCornerShape(14.dp))
                        .clickable { onNavigateToCart() }
                        .testTag("floating_cart_bar"),
                    color = DarkSurfaceElevated,
                    tonalElevation = 8.dp
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(RgAccent),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "${cartItems.sumOf { it.quantity }.toInt()}",
                                    color = DarkBg,
                                    fontWeight = FontWeight.Black,
                                    fontSize = 14.sp
                                )
                            }
                            Column {
                                Text(
                                    text = "${cartItems.size} items in cart",
                                    color = TextMuted,
                                    fontSize = 12.sp
                                )
                                Text(
                                    text = CurrencyFormatter.format(cartTotal),
                                    color = TextWhite,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp
                                )
                            }
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = "View Cart",
                                color = RgAccent,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                            Icon(
                                imageVector = Icons.Default.ArrowForward,
                                contentDescription = null,
                                tint = RgAccent,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }
        }

        // Product Selection / Unit Dialog (Part 8)
        if (selectedProductForUnit != null) {
            ProductUnitSelectionDialog(
                product = selectedProductForUnit!!,
                saleType = saleType,
                viewModel = viewModel,
                onDismiss = { selectedProductForUnit = null },
                onAddToCart = { unitName, conversionFactor, quantity, unitPrice, costPrice ->
                    viewModel.addToCart(
                        product = selectedProductForUnit!!,
                        unitName = unitName,
                        conversionFactor = conversionFactor,
                        quantity = quantity,
                        unitPrice = unitPrice,
                        costPrice = costPrice
                    )
                    Toast.makeText(context, "Added to cart", Toast.LENGTH_SHORT).show()
                    selectedProductForUnit = null
                }
            )
        }

        // Barcode Quick Search Dialog
        if (barcodeSearchDialog) {
            BarcodeSearchDialog(
                products = allProducts,
                onSelectProduct = { prod ->
                    barcodeSearchDialog = false
                    selectedProductForUnit = prod
                },
                onDismiss = { barcodeSearchDialog = false }
            )
        }
    }
}

@Composable
fun ProductPosRow(
    product: Product,
    saleType: String,
    onClick: () -> Unit
) {
    val activePrice = if (saleType == "WHOLESALE") product.wholesalePrice else product.retailPrice

    Card(
        colors = CardDefaults.cardColors(containerColor = DarkSurfaceCard),
        border = androidx.compose.foundation.BorderStroke(1.dp, DarkBorder),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .testTag("pos_product_row_${product.id}")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(DarkSurfaceElevated),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.ShoppingBag,
                        contentDescription = null,
                        tint = RgAccent,
                        modifier = Modifier.size(24.dp)
                    )
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = product.name,
                        color = TextWhite,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Stock: ${product.currentStockBase.toInt()} ${product.baseUnit}s",
                            color = if (product.currentStockBase <= product.minStock) AlertRed else TextMuted,
                            fontSize = 12.sp,
                            fontWeight = if (product.currentStockBase <= product.minStock) FontWeight.Bold else FontWeight.Normal
                        )
                        if (product.brand.isNotBlank()) {
                            Text(
                                text = "• ${product.brand}",
                                color = TextSubtle,
                                fontSize = 11.sp
                            )
                        }
                    }
                }
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "${CurrencyFormatter.format(activePrice)} / ${product.baseUnit}",
                    color = RgAccent,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
                if (saleType == "RETAIL" && product.wholesalePrice > 0) {
                    Text(
                        text = "Wholesale: ${CurrencyFormatter.format(product.wholesalePrice)}",
                        color = TextSubtle,
                        fontSize = 11.sp
                    )
                }
            }
        }
    }
}

@Composable
fun ProductUnitSelectionDialog(
    product: Product,
    saleType: String,
    viewModel: PosViewModel,
    onDismiss: () -> Unit,
    onAddToCart: (unitName: String, factor: Double, qty: Double, price: Double, cost: Double) -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    var conversions by remember { mutableStateOf<List<UnitConversion>>(emptyList()) }

    LaunchedEffect(product.id) {
        conversions = viewModel.repository.getUnitConversionsSync(product.id)
    }

    // Available unit options: Base Unit + conversions
    data class UnitOption(val name: String, val factor: Double, val price: Double, val cost: Double)

    val basePrice = if (saleType == "WHOLESALE") product.wholesalePrice else product.retailPrice
    val baseCost = product.buyingCost

    val allUnits = remember(conversions, saleType) {
        val list = mutableListOf(
            UnitOption(
                name = product.baseUnit,
                factor = 1.0,
                price = basePrice,
                cost = baseCost
            )
        )
        for (c in conversions) {
            val price = if (saleType == "WHOLESALE") {
                c.customWholesalePrice ?: (product.wholesalePrice * c.conversionFactor)
            } else {
                c.customRetailPrice ?: (product.retailPrice * c.conversionFactor)
            }
            val cost = product.buyingCost * c.conversionFactor
            list.add(UnitOption(name = c.unitName, factor = c.conversionFactor, price = price, cost = cost))
        }
        list
    }

    var selectedUnitOption by remember { mutableStateOf<UnitOption?>(null) }
    var quantity by remember { mutableDoubleStateOf(1.0) }

    LaunchedEffect(allUnits) {
        if (allUnits.isNotEmpty() && selectedUnitOption == null) {
            selectedUnitOption = allUnits.first()
        }
    }

    val currentOption = selectedUnitOption ?: UnitOption(product.baseUnit, 1.0, basePrice, baseCost)
    val subtotal = currentOption.price * quantity

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
                .testTag("unit_selection_dialog"),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = DarkSurfaceCard),
            border = androidx.compose.foundation.BorderStroke(1.dp, DarkBorder)
        ) {
            Column(
                modifier = Modifier.padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = product.name,
                            color = TextWhite,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Available stock: ${product.currentStockBase.toInt()} ${product.baseUnit}s",
                            color = RgAccent,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = TextMuted)
                    }
                }

                HorizontalDivider(color = DarkDivider)

                Text(
                    text = "Sell as:",
                    color = TextMuted,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold
                )

                // Units List Options (Part 8)
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    for (option in allUnits) {
                        val isSelected = option.name == currentOption.name
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isSelected) DarkSurfaceElevated else DarkBg)
                                .border(1.dp, if (isSelected) RgAccent else DarkBorder, RoundedCornerShape(8.dp))
                                .clickable { selectedUnitOption = option }
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(
                                    selected = isSelected,
                                    onClick = { selectedUnitOption = option },
                                    colors = RadioButtonDefaults.colors(selectedColor = RgAccent, unselectedColor = TextMuted)
                                )
                                Column {
                                    Text(
                                        text = option.name,
                                        color = TextWhite,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    if (option.factor > 1.0) {
                                        Text(
                                            text = "1 ${option.name} = ${option.factor.toInt()} ${product.baseUnit}s",
                                            color = TextMuted,
                                            fontSize = 11.sp
                                        )
                                    }
                                }
                            }

                            Text(
                                text = CurrencyFormatter.format(option.price),
                                color = if (isSelected) RgAccent else TextWhite,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                        }
                    }
                }

                HorizontalDivider(color = DarkDivider)

                // Quantity Stepper (Part 8: [-] 1 [+])
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Quantity:",
                        color = TextWhite,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 14.sp
                    )

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        IconButton(
                            onClick = { if (quantity > 1) quantity -= 1 },
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(DarkSurfaceElevated)
                                .border(1.dp, DarkBorder, CircleShape)
                        ) {
                            Icon(Icons.Default.Remove, contentDescription = "Decrease", tint = TextWhite)
                        }

                        Text(
                            text = if (quantity % 1.0 == 0.0) quantity.toInt().toString() else "%.1f".format(quantity),
                            color = TextWhite,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )

                        IconButton(
                            onClick = { quantity += 1 },
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(DarkSurfaceElevated)
                                .border(1.dp, DarkBorder, CircleShape)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = "Increase", tint = TextWhite)
                        }
                    }
                }

                // Price & Subtotal Live Calculation (Part 8)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Price per ${currentOption.name}:",
                        color = TextMuted,
                        fontSize = 13.sp
                    )
                    Text(
                        text = CurrencyFormatter.format(currentOption.price),
                        color = TextWhite,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 14.sp
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Subtotal:",
                        color = TextWhite,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = CurrencyFormatter.format(subtotal),
                        color = RgAccent,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Black
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))

                FuturisticButton(
                    text = "Add to Cart",
                    icon = Icons.Default.AddShoppingCart,
                    onClick = {
                        onAddToCart(
                            currentOption.name,
                            currentOption.factor,
                            quantity,
                            currentOption.price,
                            currentOption.cost
                        )
                    },
                    modifier = Modifier.fillMaxWidth(),
                    testTag = "add_to_cart_confirm_button"
                )
            }
        }
    }
}

@Composable
fun BarcodeSearchDialog(
    products: List<Product>,
    onSelectProduct: (Product) -> Unit,
    onDismiss: () -> Unit
) {
    var scannedCode by remember { mutableStateOf("") }
    val matching = products.filter { it.barcode.isNotBlank() }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier.fillMaxWidth().padding(14.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = DarkSurfaceCard),
            border = androidx.compose.foundation.BorderStroke(1.dp, DarkBorder)
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Barcode Scanner", color = TextWhite, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = TextMuted)
                    }
                }

                OutlinedTextField(
                    value = scannedCode,
                    onValueChange = { scannedCode = it },
                    label = { Text("Scan or Enter Barcode") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = RgAccent,
                        unfocusedBorderColor = DarkBorder,
                        focusedTextColor = TextWhite,
                        unfocusedTextColor = TextWhite
                    ),
                    singleLine = true
                )

                Text("Quick Sample Barcodes:", color = TextMuted, fontSize = 12.sp)
                LazyColumn(modifier = Modifier.heightIn(max = 200.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(matching) { p ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(DarkSurfaceElevated)
                                .clickable { onSelectProduct(p) }
                                .padding(10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(p.name, color = TextWhite, fontSize = 13.sp)
                            Text(p.barcode, color = RgAccent, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}
