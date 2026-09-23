package com.example.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import com.example.ui.components.*
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
    var initialUnitNameForSelection by remember { mutableStateOf<String?>(null) }
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
                            CategoryCard(
                                title = category,
                                isSelected = isSelected,
                                onClick = { selectedCategory = category },
                                testTag = "category_pill_tablet_$category"
                            )
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
                        CategoryCard(
                            title = category,
                            isSelected = isSelected,
                            onClick = { selectedCategory = category },
                            testTag = "category_pill_$category"
                        )
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
                initialUnitName = initialUnitNameForSelection,
                viewModel = viewModel,
                onDismiss = { 
                    selectedProductForUnit = null
                    initialUnitNameForSelection = null
                },
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
                    initialUnitNameForSelection = null
                }
            )
        }

        // Barcode Quick Search Dialog
        if (barcodeSearchDialog) {
            BarcodeSearchDialog(
                products = allProducts,
                viewModel = viewModel,
                onSelectProduct = { prod, packagingUnitName ->
                    barcodeSearchDialog = false
                    selectedProductForUnit = prod
                    initialUnitNameForSelection = packagingUnitName
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
    val wholesaleText = if (saleType == "RETAIL" && product.wholesalePrice > 0) {
        "Wholesale: ${CurrencyFormatter.format(product.wholesalePrice)}"
    } else null

    ProductCard(
        name = product.name,
        priceText = "${CurrencyFormatter.format(activePrice)} / ${product.baseUnit}",
        stockText = "Stock: ${product.currentStockBase.toInt()} ${product.baseUnit}s",
        isLowStock = product.currentStockBase <= product.minStock,
        subtitle = if (product.brand.isNotBlank()) product.brand else null,
        wholesalePriceText = wholesaleText,
        onClick = onClick,
        testTag = "pos_product_row_${product.id}"
    )
}

@Composable
fun ProductUnitSelectionDialog(
    product: Product,
    saleType: String,
    initialUnitName: String? = null,
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
    data class UnitOption(val name: String, val factor: Double, val price: Double, val cost: Double, val intactCount: Int = 0)

    val basePrice = if (saleType == "WHOLESALE") product.wholesalePrice else product.retailPrice
    val baseCost = product.buyingCost

    val allUnits = remember(conversions, saleType) {
        val list = mutableListOf(
            UnitOption(
                name = product.baseUnit,
                factor = 1.0,
                price = basePrice,
                cost = baseCost,
                intactCount = 0
            )
        )
        for (c in conversions) {
            val price = if (saleType == "WHOLESALE") {
                c.customWholesalePrice ?: (product.wholesalePrice * c.conversionFactor)
            } else {
                c.customRetailPrice ?: (product.retailPrice * c.conversionFactor)
            }
            val cost = c.purchaseCost ?: (product.buyingCost * c.conversionFactor)
            list.add(UnitOption(name = c.unitName, factor = c.conversionFactor, price = price, cost = cost, intactCount = c.intactCount))
        }
        list
    }

    var selectedUnitOption by remember { mutableStateOf<UnitOption?>(null) }
    var quantity by remember { mutableDoubleStateOf(1.0) }

    LaunchedEffect(allUnits, initialUnitName) {
        if (allUnits.isNotEmpty()) {
            if (!initialUnitName.isNullOrBlank()) {
                selectedUnitOption = allUnits.firstOrNull { it.name.equals(initialUnitName, ignoreCase = true) } ?: allUnits.first()
            } else if (selectedUnitOption == null) {
                selectedUnitOption = allUnits.first()
            }
        }
    }

    val currentOption = selectedUnitOption ?: UnitOption(product.baseUnit, 1.0, basePrice, baseCost)
    val subtotal = currentOption.price * quantity

    Dialog(
        onDismissRequest = onDismiss,
        properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false)
    ) {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 24.dp),
            contentAlignment = Alignment.Center
        ) {
            val isWide = maxWidth >= 600.dp
            val maxDialogWidth = if (isWide) 480.dp else 420.dp

            Card(
                modifier = Modifier
                    .widthIn(max = maxDialogWidth)
                    .fillMaxWidth()
                    .testTag("unit_selection_dialog"),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = DarkSurfaceCard),
                border = androidx.compose.foundation.BorderStroke(1.dp, DarkBorder)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Header: Product Name + Available Stock + Close
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = product.name,
                                color = TextWhite,
                                fontSize = 17.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "${product.categoryName} • Stock: ${product.currentStockBase.toInt()} ${product.baseUnit}s",
                                color = RgAccent,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        IconButton(
                            onClick = onDismiss,
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(DarkSurfaceElevated)
                                .testTag("sale_dialog_close_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Close",
                                tint = TextMuted,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }

                    HorizontalDivider(color = DarkDivider)

                    Text(
                        text = "Choose selling form",
                        color = TextMuted,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )

                    // Selling Form Options List (Consistent Card Sizing!)
                    val needsScroll = allUnits.size > 3
                    val optionsModifier = if (needsScroll) {
                        Modifier
                            .fillMaxWidth()
                            .heightIn(max = 200.dp)
                            .verticalScroll(rememberScrollState())
                    } else {
                        Modifier.fillMaxWidth()
                    }

                    Column(
                        modifier = optionsModifier,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        for (option in allUnits) {
                            val isSelected = option.name == currentOption.name
                            val conversionInfo = if (option.factor > 1.0) {
                                "1 ${option.name} = ${option.factor.toInt()} ${product.baseUnit}s"
                            } else null
                            val stockInfo = if (product.trackIntactPackages && option.factor > 1.0) {
                                "${option.intactCount} pkgs intact"
                            } else null

                            SellingFormCard(
                                unitName = option.name,
                                priceText = CurrencyFormatter.format(option.price),
                                conversionText = conversionInfo,
                                stockAvailabilityText = stockInfo,
                                isSelected = isSelected,
                                onClick = { selectedUnitOption = option },
                                testTag = "selling_form_${option.name}"
                            )
                        }
                    }

                    HorizontalDivider(color = DarkDivider)

                    // Quantity Stepper Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Quantity",
                                color = TextWhite,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 14.sp
                            )
                            Text(
                                text = "@ ${CurrencyFormatter.format(currentOption.price)} / ${currentOption.name}",
                                color = TextMuted,
                                fontSize = 11.sp
                            )
                        }

                        QuantityStepper(
                            quantity = quantity,
                            onQuantityChange = { quantity = it }
                        )
                    }

                    // Subtotal Preview Banner
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        color = DarkBg,
                        shape = RoundedCornerShape(10.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, DarkBorder)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Subtotal",
                                color = TextMuted,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = CurrencyFormatter.format(subtotal),
                                color = RgAccent,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Black
                            )
                        }
                    }

                    // Add to Cart Action Button
                    DialogActionButton(
                        text = "Add to Cart • ${CurrencyFormatter.format(subtotal)}",
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
}

@Composable
fun BarcodeSearchDialog(
    products: List<Product>,
    viewModel: PosViewModel,
    onSelectProduct: (Product, String?) -> Unit,
    onDismiss: () -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    var scannedCode by remember { mutableStateOf("") }
    var packagingProfilesWithBarcodes by remember { mutableStateOf<List<UnitConversion>>(emptyList()) }
    var lookupError by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        packagingProfilesWithBarcodes = viewModel.repository.getAllPackagingProfilesWithBarcode()
    }

    ResponsiveDialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(PosDesignTokens.RadiusCard),
            colors = CardDefaults.cardColors(containerColor = DarkSurfaceCard),
            border = androidx.compose.foundation.BorderStroke(1.dp, DarkBorder)
        ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.QrCodeScanner, contentDescription = null, tint = RgAccent)
                        Text("Barcode Scanner & Lookup", color = TextWhite, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = TextMuted)
                    }
                }

                Text(
                    text = "Scan or type any barcode. Supports loose items and package barcodes (Cartons, Sacks, Packs).",
                    color = TextMuted,
                    fontSize = 12.sp
                )

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = scannedCode,
                        onValueChange = {
                            scannedCode = it
                            lookupError = null
                        },
                        label = { Text("Enter / Scan Barcode") },
                        modifier = Modifier.weight(1f).testTag("barcode_dialog_input"),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = RgAccent,
                            unfocusedBorderColor = DarkBorder,
                            focusedTextColor = TextWhite,
                            unfocusedTextColor = TextWhite
                        ),
                        singleLine = true
                    )
                    Button(
                        onClick = {
                            if (scannedCode.isNotBlank()) {
                                coroutineScope.launch {
                                    val match = viewModel.repository.findProductAndPackagingByBarcode(scannedCode.trim())
                                    if (match != null) {
                                        onSelectProduct(match.first, match.second?.unitName)
                                    } else {
                                        lookupError = "No item found for barcode \"$scannedCode\""
                                    }
                                }
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = RgAccent, contentColor = DarkBg),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.align(Alignment.CenterVertically).testTag("barcode_lookup_button")
                    ) {
                        Text("Find", fontWeight = FontWeight.Bold)
                    }
                }

                if (lookupError != null) {
                    Text(lookupError!!, color = AlertRed, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                }

                Text("Quick Barcode Catalog (Tap to Sell):", color = TextMuted, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                LazyColumn(modifier = Modifier.heightIn(max = 240.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    // Packaging Profile Barcodes
                    items(packagingProfilesWithBarcodes) { profile ->
                        val parentProd = products.firstOrNull { it.id == profile.productId }
                        if (parentProd != null) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(DarkSurfaceElevated)
                                    .clickable { onSelectProduct(parentProd, profile.unitName) }
                                    .padding(10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                                        Text(parentProd.name, color = TextWhite, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(4.dp))
                                                .background(RgAccent.copy(alpha = 0.2f))
                                                .padding(horizontal = 4.dp, vertical = 1.dp)
                                        ) {
                                            Text(profile.unitName, color = RgAccent, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                    Text("Barcode: ${profile.barcode}", color = TextSubtle, fontSize = 11.sp)
                                }
                                Text("📦 ${profile.conversionFactor.toInt()} ${parentProd.baseUnit}s", color = CashAmber, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    // Base Product Barcodes
                    items(products.filter { it.barcode.isNotBlank() }) { p ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(DarkSurfaceElevated)
                                .clickable { onSelectProduct(p, p.baseUnit) }
                                .padding(10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Text(p.name, color = TextWhite, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                                    Text("(${p.baseUnit})", color = TextMuted, fontSize = 11.sp)
                                }
                                Text("Barcode: ${p.barcode}", color = TextSubtle, fontSize = 11.sp)
                            }
                            Text(CurrencyFormatter.format(p.retailPrice), color = RgAccent, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}
