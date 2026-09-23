package com.example.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items as gridItems
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
import com.example.data.model.StockMovement
import com.example.data.model.UnitConversion
import com.example.ui.components.*
import com.example.ui.theme.*
import com.example.ui.viewmodel.PosViewModel
import com.example.util.CurrencyFormatter
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InventoryScreen(
    viewModel: PosViewModel,
    onNavigateToAddProduct: (Long) -> Unit,
    onNavigateToStockTake: () -> Unit,
    onNavigateToImport: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    val allProducts by viewModel.allProducts.collectAsStateWithLifecycle()
    val lowStockProducts by viewModel.lowStockProducts.collectAsStateWithLifecycle()
    val allMovements by viewModel.allMovements.collectAsStateWithLifecycle()

    var selectedTab by remember { mutableIntStateOf(0) } // 0: Products, 1: Stock Movements, 2: Low Stock
    var searchQuery by remember { mutableStateOf("") }
    var selectedProductForDetail by remember { mutableStateOf<Product?>(null) }

    // Summary calculations (Part 14)
    val totalStockValue = remember(allProducts) {
        allProducts.sumOf { it.currentStockBase * it.buyingCost }
    }
    val outOfStockCount = remember(allProducts) {
        allProducts.count { it.currentStockBase <= 0 }
    }

    val filteredProducts = remember(allProducts, searchQuery) {
        if (searchQuery.isBlank()) allProducts else {
            allProducts.filter {
                it.name.contains(searchQuery, ignoreCase = true) ||
                        it.sku.contains(searchQuery, ignoreCase = true) ||
                        it.categoryName.contains(searchQuery, ignoreCase = true)
            }
        }
    }

    Scaffold(
        containerColor = DarkBg,
        floatingActionButton = {
            FloatingActionButton(
                onClick = { onNavigateToAddProduct(0L) },
                containerColor = RgAccent,
                contentColor = DarkBg,
                shape = CircleShape,
                modifier = Modifier.padding(bottom = 70.dp).testTag("add_product_fab")
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Product")
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .widthIn(max = 900.dp)
                .padding(horizontal = 16.dp)
                .testTag("inventory_screen"),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Spacer(modifier = Modifier.height(4.dp))

            // Title & Stock Take Quick Button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Inventory",
                        color = TextWhite,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Base unit stock and conversion management",
                        color = TextMuted,
                        fontSize = 12.sp
                    )
                }

                Button(
                    onClick = onNavigateToStockTake,
                    colors = ButtonDefaults.buttonColors(containerColor = DarkSurfaceElevated, contentColor = TextWhite),
                    shape = RoundedCornerShape(8.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, DarkBorder),
                    modifier = Modifier.testTag("stock_take_button")
                ) {
                    Icon(Icons.Default.FactCheck, contentDescription = null, tint = RgAccent, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Stock Take", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                TextButton(
                    onClick = onNavigateToImport,
                    modifier = Modifier.testTag("import_export_button")
                ) {
                    Icon(Icons.Default.ImportExport, contentDescription = null, tint = RgAccent, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Import / Export Spreadsheet", fontSize = 12.sp, color = RgAccent, fontWeight = FontWeight.Bold)
                }
            }

            // Summary Header Metrics (Part 14)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                MetricCard(
                    title = "Total Items",
                    value = "${allProducts.size}",
                    accentColor = RgAccent,
                    modifier = Modifier.weight(1f)
                )
                MetricCard(
                    title = "Stock Value",
                    value = CurrencyFormatter.format(totalStockValue),
                    accentColor = SuccessGreen,
                    modifier = Modifier.weight(1.3f)
                )
                MetricCard(
                    title = "Low / Out",
                    value = "${lowStockProducts.size} / $outOfStockCount",
                    accentColor = if (lowStockProducts.isNotEmpty()) AlertRed else TextWhite,
                    modifier = Modifier.weight(1f)
                )
            }

            // Tabs Selector
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(DarkSurfaceCard)
                    .border(1.dp, DarkBorder, RoundedCornerShape(8.dp))
                    .padding(3.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                listOf("Products (${allProducts.size})", "Movements", "Low Stock (${lowStockProducts.size})").forEachIndexed { idx, label ->
                    val isSel = selectedTab == idx
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(6.dp))
                            .background(if (isSel) RgAccent else Color.Transparent)
                            .clickable { selectedTab = idx }
                            .padding(vertical = 8.dp)
                            .testTag("inventory_tab_$idx"),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = label,
                            color = if (isSel) DarkBg else TextMuted,
                            fontWeight = if (isSel) FontWeight.Bold else FontWeight.Medium,
                            fontSize = 11.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            when (selectedTab) {
                0 -> {
                    // Products List
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = { Text("Search products by name or SKU...", fontSize = 13.sp) },
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

                    if (filteredProducts.isEmpty()) {
                        EmptyStateView(
                            icon = Icons.Default.Inventory2,
                            title = "No products found",
                            description = "Add a product using the '+' button below.",
                            actionButtonText = "Add Product",
                            onActionClick = { onNavigateToAddProduct(0L) }
                        )
                    } else {
                        LazyVerticalGrid(
                            columns = GridCells.Adaptive(minSize = 340.dp),
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            contentPadding = PaddingValues(bottom = 110.dp)
                        ) {
                            gridItems(filteredProducts) { product ->
                                InventoryProductRow(
                                    product = product,
                                    onClick = { selectedProductForDetail = product }
                                )
                            }
                        }
                    }
                }

                1 -> {
                    // Stock Movements Log (Part 19)
                    if (allMovements.isEmpty()) {
                        EmptyStateView(
                            icon = Icons.Default.History,
                            title = "No stock movements recorded",
                            description = "Stock movements will appear when you sell, receive, or adjust products."
                        )
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            contentPadding = PaddingValues(bottom = 110.dp)
                        ) {
                            items(allMovements) { movement ->
                                StockMovementRow(movement = movement)
                            }
                        }
                    }
                }

                2 -> {
                    // Low Stock Tab (Part 17)
                    if (lowStockProducts.isEmpty()) {
                        EmptyStateView(
                            icon = Icons.Default.CheckCircle,
                            title = "Stock levels healthy",
                            description = "No products are currently at or below minimum threshold."
                        )
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            contentPadding = PaddingValues(bottom = 110.dp)
                        ) {
                            items(lowStockProducts) { product ->
                                InventoryProductRow(
                                    product = product,
                                    onClick = { selectedProductForDetail = product }
                                )
                            }
                        }
                    }
                }
            }
        }

        // Product Detail Modal (Part 18)
        if (selectedProductForDetail != null) {
            ProductDetailDialog(
                product = selectedProductForDetail!!,
                viewModel = viewModel,
                onDismiss = { selectedProductForDetail = null },
                onEdit = {
                    val pid = selectedProductForDetail!!.id
                    selectedProductForDetail = null
                    onNavigateToAddProduct(pid)
                }
            )
        }
    }
}

@Composable
fun InventoryProductRow(product: Product, onClick: () -> Unit) {
    val isLowStock = product.currentStockBase <= product.minStock

    Card(
        colors = CardDefaults.cardColors(containerColor = DarkSurfaceCard),
        border = androidx.compose.foundation.BorderStroke(1.dp, if (isLowStock) AlertRed.copy(alpha = 0.4f) else DarkBorder),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .testTag("inventory_item_${product.id}")
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
                    text = product.name,
                    color = TextWhite,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 15.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = product.categoryName,
                        color = TextMuted,
                        fontSize = 12.sp
                    )
                    Text("•", color = TextSubtle)
                    Text(
                        text = "Cost: ${CurrencyFormatter.format(product.buyingCost)}",
                        color = TextSubtle,
                        fontSize = 11.sp
                    )
                }
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "${product.currentStockBase.toInt()} ${product.baseUnit}s",
                    color = if (isLowStock) AlertRed else RgAccent,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp
                )
                Text(
                    text = "Retail: ${CurrencyFormatter.format(product.retailPrice)}",
                    color = TextMuted,
                    fontSize = 11.sp
                )
            }
        }
    }
}

@Composable
fun StockMovementRow(movement: StockMovement) {
    val isDeduction = movement.quantityChangeBase < 0

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
                    text = movement.productName,
                    color = TextWhite,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp
                )
                Text(
                    text = "${movement.movementType} • ${movement.reason}",
                    color = TextMuted,
                    fontSize = 11.sp
                )
                Text(
                    text = "${CurrencyFormatter.formatDate(movement.dateEpoch)} • By ${movement.staffName}",
                    color = TextSubtle,
                    fontSize = 10.sp
                )
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = if (isDeduction) "${movement.quantityChangeBase.toInt()} ${movement.unitUsed}s" else "+${movement.quantityChangeBase.toInt()} ${movement.unitUsed}s",
                    color = if (isDeduction) AlertRed else SuccessGreen,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
                Text(
                    text = "Stock After: ${movement.stockAfterBase.toInt()}",
                    color = TextMuted,
                    fontSize = 11.sp
                )
            }
        }
    }
}

@Composable
fun ProductDetailDialog(
    product: Product,
    viewModel: PosViewModel,
    onDismiss: () -> Unit,
    onEdit: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var conversions by remember { mutableStateOf<List<UnitConversion>>(emptyList()) }
    var showAddConversionDialog by remember { mutableStateOf(false) }
    var editingProfile by remember { mutableStateOf<UnitConversion?>(null) }
    var showOpenPackageDialog by remember { mutableStateOf(false) }
    var selectedProfileToOpen by remember { mutableStateOf<UnitConversion?>(null) }
    val currentUser by viewModel.currentUser.collectAsStateWithLifecycle()

    fun reloadConversions() {
        coroutineScope.launch {
            conversions = viewModel.repository.getUnitConversionsSync(product.id)
        }
    }

    LaunchedEffect(product.id) {
        reloadConversions()
    }

    // Compute intact packages total base units and loose stock
    val totalIntactBaseUnits = remember(conversions) {
        conversions.filter { it.intactCount > 0 }.sumOf { it.intactCount * it.conversionFactor }
    }
    val looseStock = remember(product.currentStockBase, totalIntactBaseUnits) {
        maxOf(0.0, product.currentStockBase - totalIntactBaseUnits)
    }
    val openableProfilesWithStock = remember(conversions) {
        conversions.filter { it.canOpen && it.intactCount > 0 }
    }

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
            val maxDialogWidth = if (isWide) 520.dp else 440.dp

            Card(
                modifier = Modifier
                    .widthIn(max = maxDialogWidth)
                    .fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = DarkSurfaceCard),
                border = androidx.compose.foundation.BorderStroke(1.dp, DarkBorder)
            ) {
                Column(
                    modifier = Modifier
                        .padding(16.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(product.name, color = TextWhite, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        Text("${product.categoryName} • ${product.sku}", color = TextMuted, fontSize = 12.sp)
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = TextMuted)
                    }
                }

                HorizontalDivider(color = DarkDivider)

                // Stock & Pricing Grid
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    MetricCard(
                        title = "Total Base Stock",
                        value = "${product.currentStockBase.toInt()} ${product.baseUnit}s",
                        accentColor = RgAccent,
                        modifier = Modifier.weight(1f)
                    )
                    MetricCard(
                        title = "Buying Cost",
                        value = CurrencyFormatter.format(product.buyingCost),
                        accentColor = TextWhite,
                        modifier = Modifier.weight(1f)
                    )
                }

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    MetricCard(
                        title = "Retail Price",
                        value = CurrencyFormatter.format(product.retailPrice),
                        accentColor = RgAccent,
                        modifier = Modifier.weight(1f)
                    )
                    MetricCard(
                        title = "Wholesale Price",
                        value = CurrencyFormatter.format(product.wholesalePrice),
                        accentColor = CreditBlue,
                        modifier = Modifier.weight(1f)
                    )
                }

                // INTACT VS LOOSE STOCK BREAKDOWN
                if (product.trackIntactPackages || conversions.any { it.intactCount > 0 }) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = DarkSurfaceElevated),
                        border = androidx.compose.foundation.BorderStroke(1.dp, DarkBorder),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("STOCK BREAKDOWN", color = TextMuted, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                if (openableProfilesWithStock.isNotEmpty()) {
                                    Button(
                                        onClick = {
                                            selectedProfileToOpen = openableProfilesWithStock.first()
                                            showOpenPackageDialog = true
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = SuccessGreen, contentColor = DarkBg),
                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                        shape = RoundedCornerShape(6.dp)
                                    ) {
                                        Icon(Icons.Default.Unarchive, contentDescription = null, modifier = Modifier.size(14.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Open Package", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Loose / Opened Stock:", color = TextWhite, fontSize = 12.sp)
                                Text("${looseStock.toInt()} ${product.baseUnit}s", color = RgAccent, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }

                            for (c in conversions.filter { it.intactCount > 0 }) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("📦 ${c.intactCount} × ${c.unitName}:", color = TextWhite, fontSize = 12.sp)
                                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                                        Text("= ${(c.intactCount * c.conversionFactor).toInt()} ${product.baseUnit}s", color = CashAmber, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                        if (c.canOpen) {
                                            Text(
                                                text = "Open",
                                                color = SuccessGreen,
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                modifier = Modifier
                                                    .clickable {
                                                        selectedProfileToOpen = c
                                                        showOpenPackageDialog = true
                                                    }
                                                    .padding(horizontal = 4.dp, vertical = 2.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // PACKAGING PROFILES & MULTI-UNIT PRICING SECTION
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("PACKAGING PROFILES", color = TextMuted, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    Text(
                        text = "+ Add Profile",
                        color = RgAccent,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.clickable {
                            editingProfile = null
                            showAddConversionDialog = true
                        }
                    )
                }

                // Base Unit representation
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(6.dp))
                        .background(DarkSurfaceElevated)
                        .padding(10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("1 ${product.baseUnit} (Base Stock Unit)", color = TextWhite, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                    Text("1 ${product.baseUnit}", color = TextMuted, fontSize = 12.sp)
                }

                for (c in conversions) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(6.dp))
                            .background(DarkSurfaceElevated)
                            .padding(10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                                Text(c.unitName, color = TextWhite, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                if (c.canOpen) {
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(4.dp))
                                            .background(SuccessGreen.copy(alpha = 0.2f))
                                            .padding(horizontal = 4.dp, vertical = 1.dp)
                                    ) {
                                        Text("Openable", color = SuccessGreen, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                            Text(
                                "1 ${c.unitName} = ${c.conversionFactor.toInt()} ${product.baseUnit}s",
                                color = TextMuted,
                                fontSize = 11.sp
                            )
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text(
                                    "Ret: ${CurrencyFormatter.format(c.customRetailPrice ?: (product.retailPrice * c.conversionFactor))}",
                                    color = RgAccent,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    "Wholesale: ${CurrencyFormatter.format(c.customWholesalePrice ?: (product.wholesalePrice * c.conversionFactor))}",
                                    color = CreditBlue,
                                    fontSize = 11.sp
                                )
                            }
                            if (c.barcode.isNotBlank()) {
                                Text("Barcode: ${c.barcode}", color = TextSubtle, fontSize = 10.sp)
                            }
                        }

                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            if (c.canOpen && c.intactCount > 0) {
                                IconButton(
                                    onClick = {
                                        selectedProfileToOpen = c
                                        showOpenPackageDialog = true
                                    },
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Icon(Icons.Default.Unarchive, contentDescription = "Open", tint = SuccessGreen, modifier = Modifier.size(18.dp))
                                }
                            }
                            IconButton(
                                onClick = {
                                    editingProfile = c
                                    showAddConversionDialog = true
                                },
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(Icons.Default.Edit, contentDescription = "Edit", tint = RgAccent, modifier = Modifier.size(16.dp))
                            }
                            IconButton(
                                onClick = {
                                    coroutineScope.launch {
                                        viewModel.repository.deleteUnitConversion(c.id)
                                        reloadConversions()
                                    }
                                },
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(Icons.Default.Delete, contentDescription = "Delete", tint = AlertRed, modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                    DialogActionButton(
                        text = "Edit Full Product Details",
                        icon = Icons.Default.Edit,
                        onClick = onEdit,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }

    // Open Package (Break Bulk) Dialog
    if (showOpenPackageDialog && selectedProfileToOpen != null) {
        OpenPackageBreakBulkDialog(
            product = product,
            profile = selectedProfileToOpen!!,
            onConfirm = { countToOpen ->
                coroutineScope.launch {
                    val staffName = currentUser.ifBlank { "Staff" }
                    val result = viewModel.repository.openPackage(
                        productId = product.id,
                        profileId = selectedProfileToOpen!!.id,
                        countToOpen = countToOpen,
                        staffName = staffName
                    )
                    if (result.isSuccess) {
                        Toast.makeText(
                            context,
                            "Successfully opened $countToOpen × ${selectedProfileToOpen!!.unitName}",
                            Toast.LENGTH_SHORT
                        ).show()
                        reloadConversions()
                    } else {
                        Toast.makeText(
                            context,
                            result.exceptionOrNull()?.message ?: "Cannot open packages",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                    showOpenPackageDialog = false
                    selectedProfileToOpen = null
                }
            },
            onDismiss = {
                showOpenPackageDialog = false
                selectedProfileToOpen = null
            }
        )
    }

    if (showAddConversionDialog) {
        PackagingProfileEditorDialog(
            product = product,
            existing = editingProfile,
            onSave = { updatedProfile ->
                coroutineScope.launch {
                    if (updatedProfile.id == 0L) {
                        viewModel.repository.addUnitConversion(updatedProfile)
                    } else {
                        viewModel.repository.updateUnitConversion(updatedProfile)
                    }
                    reloadConversions()
                    showAddConversionDialog = false
                    editingProfile = null
                }
            },
            onDismiss = {
                showAddConversionDialog = false
                editingProfile = null
            }
        )
    }
}

@Composable
fun OpenPackageBreakBulkDialog(
    product: Product,
    profile: UnitConversion,
    onConfirm: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    var countToOpen by remember { mutableIntStateOf(1) }
    val maxAvailable = profile.intactCount
    val looseUnitsAdded = countToOpen * profile.conversionFactor

    ResponsiveDialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(PosDesignTokens.RadiusCard),
            colors = CardDefaults.cardColors(containerColor = DarkSurfaceCard),
            border = androidx.compose.foundation.BorderStroke(1.dp, DarkBorder)
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Unarchive, contentDescription = null, tint = SuccessGreen)
                        Text("Open Package (Break Bulk)", color = TextWhite, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    }
                    IconButton(onClick = onDismiss, modifier = Modifier.size(24.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = TextMuted)
                    }
                }

                Text(
                    text = "Break open sealed packages to make loose ${product.baseUnit}s available for retail customers.",
                    color = TextMuted,
                    fontSize = 12.sp
                )

                Card(
                    colors = CardDefaults.cardColors(containerColor = DarkSurfaceElevated),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("Target Package: ${profile.unitName}", color = TextWhite, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Text("Intact Packages in Stock: $maxAvailable", color = CashAmber, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                        Text("Each package contains: ${profile.conversionFactor.toInt()} ${product.baseUnit}s", color = TextMuted, fontSize = 11.sp)
                    }
                }

                // Quantity to open stepper
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Packages to Open:", color = TextWhite, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        IconButton(
                            onClick = { if (countToOpen > 1) countToOpen -= 1 },
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(DarkSurfaceElevated)
                                .border(1.dp, DarkBorder, CircleShape)
                        ) {
                            Icon(Icons.Default.Remove, contentDescription = "Minus", tint = TextWhite)
                        }

                        Text(
                            text = "$countToOpen",
                            color = TextWhite,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )

                        IconButton(
                            onClick = { if (countToOpen < maxAvailable) countToOpen += 1 },
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(DarkSurfaceElevated)
                                .border(1.dp, DarkBorder, CircleShape)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = "Plus", tint = TextWhite)
                        }
                    }
                }

                // Live Impact Preview
                Card(
                    colors = CardDefaults.cardColors(containerColor = DarkBg),
                    border = androidx.compose.foundation.BorderStroke(1.dp, SuccessGreen.copy(alpha = 0.5f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("INVENTORY RESULT:", color = SuccessGreen, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        Text("• Will release +${looseUnitsAdded.toInt()} loose ${product.baseUnit}s into shelf stock.", color = TextWhite, fontSize = 12.sp)
                        Text("• Intact packages remaining: ${maxAvailable - countToOpen}.", color = TextWhite, fontSize = 12.sp)
                        Text("• Total stock (${product.currentStockBase.toInt()} ${product.baseUnit}s) remains identical.", color = TextMuted, fontSize = 11.sp)
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    FuturisticButton(
                        text = "Cancel",
                        onClick = onDismiss,
                        isSecondary = true,
                        modifier = Modifier.weight(1f)
                    )
                    FuturisticButton(
                        text = "Confirm Open",
                        enabled = countToOpen in 1..maxAvailable,
                        onClick = { onConfirm(countToOpen) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@Composable
fun PackagingProfileEditorDialog(
    product: Product,
    existing: UnitConversion?,
    onSave: (UnitConversion) -> Unit,
    onDismiss: () -> Unit
) {
    var unitName by remember { mutableStateOf(existing?.unitName ?: "Carton") }
    var factorText by remember { mutableStateOf(existing?.conversionFactor?.toInt()?.toString() ?: "12") }
    var customRetailText by remember { mutableStateOf(existing?.customRetailPrice?.toString() ?: "") }
    var customWholesaleText by remember { mutableStateOf(existing?.customWholesalePrice?.toString() ?: "") }
    var purchaseCostText by remember { mutableStateOf(existing?.purchaseCost?.toString() ?: "") }
    var barcodeText by remember { mutableStateOf(existing?.barcode ?: "") }
    var skuText by remember { mutableStateOf(existing?.sku ?: "") }
    var canOpen by remember { mutableStateOf(existing?.canOpen ?: true) }
    var intactCountText by remember { mutableStateOf(existing?.intactCount?.toString() ?: "0") }

    ResponsiveDialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(PosDesignTokens.RadiusCard),
            colors = CardDefaults.cardColors(containerColor = DarkSurfaceCard),
            border = androidx.compose.foundation.BorderStroke(1.dp, DarkBorder)
        ) {
            Column(
                modifier = Modifier
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = if (existing != null) "Edit Packaging Profile" else "Add Packaging Profile",
                    color = TextWhite,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
                Text(
                    text = "Define packaging name, conversion to ${product.baseUnit}, independent prices & barcode.",
                    color = TextMuted,
                    fontSize = 12.sp
                )

                OutlinedTextField(
                    value = unitName,
                    onValueChange = { unitName = it },
                    label = { Text("Packaging Name (e.g. Carton 18 × 500ml, 50 KG Sack)") },
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
                    value = factorText,
                    onValueChange = { factorText = it },
                    label = { Text("Contains how many ${product.baseUnit}s?") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = RgAccent,
                        unfocusedBorderColor = DarkBorder,
                        focusedTextColor = TextWhite,
                        unfocusedTextColor = TextWhite
                    ),
                    singleLine = true
                )

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = customRetailText,
                        onValueChange = { customRetailText = it },
                        label = { Text("Retail Price (KSh)") },
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
                        value = customWholesaleText,
                        onValueChange = { customWholesaleText = it },
                        label = { Text("Wholesale Price (KSh)") },
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

                OutlinedTextField(
                    value = purchaseCostText,
                    onValueChange = { purchaseCostText = it },
                    label = { Text("Purchase Cost (KSh)") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = RgAccent,
                        unfocusedBorderColor = DarkBorder,
                        focusedTextColor = TextWhite,
                        unfocusedTextColor = TextWhite
                    ),
                    singleLine = true
                )

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = barcodeText,
                        onValueChange = { barcodeText = it },
                        label = { Text("Packaging Barcode") },
                        modifier = Modifier.weight(1.3f),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = RgAccent,
                            unfocusedBorderColor = DarkBorder,
                            focusedTextColor = TextWhite,
                            unfocusedTextColor = TextWhite
                        ),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = skuText,
                        onValueChange = { skuText = it },
                        label = { Text("Packaging SKU") },
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

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(DarkSurfaceElevated)
                        .clickable { canOpen = !canOpen }
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Can be opened (Break Bulk)", color = TextWhite, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                        Text("Allows breaking open into loose ${product.baseUnit}s", color = TextMuted, fontSize = 10.sp)
                    }
                    Switch(
                        checked = canOpen,
                        onCheckedChange = { canOpen = it },
                        colors = SwitchDefaults.colors(checkedThumbColor = RgAccent, checkedTrackColor = DarkBg)
                    )
                }

                OutlinedTextField(
                    value = intactCountText,
                    onValueChange = { intactCountText = it },
                    label = { Text("Intact Packages in Stock") },
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
                        text = "Save Profile",
                        enabled = unitName.isNotBlank() && (factorText.toDoubleOrNull() ?: 0.0) > 0,
                        onClick = {
                            val profile = UnitConversion(
                                id = existing?.id ?: 0L,
                                productId = product.id,
                                unitName = unitName.trim(),
                                conversionFactor = factorText.toDoubleOrNull() ?: 1.0,
                                customRetailPrice = customRetailText.toDoubleOrNull(),
                                customWholesalePrice = customWholesaleText.toDoubleOrNull(),
                                barcode = barcodeText.trim(),
                                sku = skuText.trim(),
                                purchaseCost = purchaseCostText.toDoubleOrNull(),
                                canOpen = canOpen,
                                intactCount = intactCountText.toIntOrNull() ?: 0
                            )
                            onSave(profile)
                        },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}
