package com.example.ui.screens

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.window.Dialog
import com.example.data.model.Product
import com.example.data.model.UnitConversion
import com.example.ui.components.FuturisticButton
import com.example.ui.theme.*
import com.example.ui.viewmodel.PosViewModel
import com.example.util.CurrencyFormatter
import kotlinx.coroutines.launch
import kotlin.random.Random

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditProductScreen(
    productId: Long,
    viewModel: PosViewModel,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val isEditMode = productId > 0

    // Product Basic Fields
    var name by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("General") }
    var sku by remember { mutableStateOf("") }
    var barcode by remember { mutableStateOf("") }
    var brand by remember { mutableStateOf("") }

    // Configurable Base Stock Unit
    var baseUnit by remember { mutableStateOf("Piece") }
    val standardUnits = listOf("Piece", "Pack", "Bottle", "KG", "Litre", "Metre", "Tin", "Bag")

    // Pricing and Stock for Base Unit
    var buyingCostText by remember { mutableStateOf("") }
    var retailPriceText by remember { mutableStateOf("") }
    var wholesalePriceText by remember { mutableStateOf("") }
    var stockText by remember { mutableStateOf("0") }
    var minStockText by remember { mutableStateOf("10") }
    var trackIntactPackages by remember { mutableStateOf(true) }

    // Packaging Profiles list
    var packagingProfiles by remember { mutableStateOf<List<UnitConversion>>(emptyList()) }
    var editingProfile by remember { mutableStateOf<UnitConversion?>(null) }
    var showProfileDialog by remember { mutableStateOf(false) }

    var isSaving by remember { mutableStateOf(false) }

    LaunchedEffect(productId) {
        if (isEditMode) {
            val prod = viewModel.repository.getProductById(productId)
            if (prod != null) {
                name = prod.name
                category = prod.categoryName
                sku = prod.sku
                barcode = prod.barcode
                brand = prod.brand
                baseUnit = prod.baseUnit
                buyingCostText = prod.buyingCost.toString()
                retailPriceText = prod.retailPrice.toString()
                wholesalePriceText = prod.wholesalePrice.toString()
                stockText = prod.currentStockBase.toInt().toString()
                minStockText = prod.minStock.toInt().toString()
                trackIntactPackages = prod.trackIntactPackages

                // Load existing packaging profiles
                packagingProfiles = viewModel.repository.getUnitConversionsSync(productId)
            }
        } else {
            // Auto generate starter SKU and barcode
            sku = "SKU-${Random.nextInt(10000, 99999)}"
            barcode = "61611${Random.nextInt(1000000, 9999999)}"
        }
    }

    Scaffold(
        containerColor = DarkBg,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = if (isEditMode) "Edit Product" else "New Product Setup",
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
                .testTag("add_edit_product_screen"),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // SECTION 1: PRODUCT INFORMATION
            Text(
                text = "1. PRODUCT INFORMATION",
                color = RgAccent,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            )

            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Product Name (e.g. Sugar, Fresh Milk 500ml, Sweets)") },
                modifier = Modifier.fillMaxWidth().testTag("product_name_input"),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = RgAccent,
                    unfocusedBorderColor = DarkBorder,
                    focusedTextColor = TextWhite,
                    unfocusedTextColor = TextWhite
                ),
                singleLine = true
            )

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = category,
                    onValueChange = { category = it },
                    label = { Text("Category") },
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
                    value = brand,
                    onValueChange = { brand = it },
                    label = { Text("Brand / Maker") },
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

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = sku,
                    onValueChange = { sku = it },
                    label = { Text("Base SKU Code") },
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
                    value = barcode,
                    onValueChange = { barcode = it },
                    label = { Text("Base Barcode") },
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

            HorizontalDivider(color = DarkDivider)

            // SECTION 2: CONFIGURABLE BASE STOCK UNIT
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = "2. CONFIGURABLE BASE STOCK UNIT",
                    color = RgAccent,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
                Text(
                    text = "The base unit depends on the product (e.g. Sugar = KG, Milk = Bottle, Sweets = Piece). Never assume Piece.",
                    color = TextMuted,
                    fontSize = 12.sp
                )

                // Quick selector chips for standard base units
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(vertical = 4.dp)
                ) {
                    items(standardUnits) { u ->
                        val isSelected = u.equals(baseUnit, ignoreCase = true)
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(20.dp))
                                .background(if (isSelected) RgAccent else DarkSurfaceCard)
                                .border(1.dp, if (isSelected) RgAccent else DarkBorder, RoundedCornerShape(20.dp))
                                .clickable { baseUnit = u }
                                .padding(horizontal = 14.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = u,
                                color = if (isSelected) DarkBg else TextWhite,
                                fontSize = 12.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = baseUnit,
                    onValueChange = { baseUnit = it },
                    label = { Text("Base Unit Name (e.g. Piece, Bottle, KG, Pack, Metre)") },
                    modifier = Modifier.fillMaxWidth().testTag("base_unit_input"),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = RgAccent,
                        unfocusedBorderColor = DarkBorder,
                        focusedTextColor = TextWhite,
                        unfocusedTextColor = TextWhite
                    ),
                    singleLine = true
                )
            }

            HorizontalDivider(color = DarkDivider)

            // SECTION 3: BASE UNIT PRICING & INVENTORY
            Text(
                text = "3. BASE UNIT PRICING & INVENTORY (per 1 $baseUnit)",
                color = RgAccent,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            )

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = buyingCostText,
                    onValueChange = { buyingCostText = it },
                    label = { Text("Buying Cost / 1 $baseUnit") },
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
                    value = stockText,
                    onValueChange = { stockText = it },
                    label = { Text("Current Stock ($baseUnit)") },
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

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = retailPriceText,
                    onValueChange = { retailPriceText = it },
                    label = { Text("Retail Price / 1 $baseUnit") },
                    modifier = Modifier.weight(1f).testTag("retail_price_input"),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = RgAccent,
                        unfocusedBorderColor = DarkBorder,
                        focusedTextColor = TextWhite,
                        unfocusedTextColor = TextWhite
                    ),
                    singleLine = true
                )
                OutlinedTextField(
                    value = wholesalePriceText,
                    onValueChange = { wholesalePriceText = it },
                    label = { Text("Wholesale Price / 1 $baseUnit") },
                    modifier = Modifier.weight(1f).testTag("wholesale_price_input"),
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
                    .clickable { trackIntactPackages = !trackIntactPackages }
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Track Intact Packages vs Loose Stock", color = TextWhite, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                    Text("Maintains intact carton counts while inventory tracks total $baseUnit", color = TextMuted, fontSize = 11.sp)
                }
                Switch(
                    checked = trackIntactPackages,
                    onCheckedChange = { trackIntactPackages = it },
                    colors = SwitchDefaults.colors(checkedThumbColor = RgAccent, checkedTrackColor = DarkBg)
                )
            }

            HorizontalDivider(color = DarkDivider)

            // SECTION 4: PACKAGING PROFILES & MULTI-UNIT PRICING
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "4. PACKAGING PROFILES & MULTI-UNIT PRICING",
                        color = RgAccent,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                    Text(
                        text = "Cartons, Sacks, Packs, Dozens with independent pricing & barcodes",
                        color = TextMuted,
                        fontSize = 11.sp
                    )
                }
                Button(
                    onClick = {
                        editingProfile = null
                        showProfileDialog = true
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = RgAccent, contentColor = DarkBg),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                    modifier = Modifier.testTag("add_packaging_profile_button")
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Add Profile", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }

            if (packagingProfiles.isEmpty()) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = DarkSurfaceCard),
                    border = androidx.compose.foundation.BorderStroke(1.dp, DarkBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(Icons.Default.Inventory2, contentDescription = null, tint = TextMuted, modifier = Modifier.size(32.dp))
                        Text("No Packaging Profiles Configured", color = TextWhite, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                        Text(
                            "This product is currently only sold as single $baseUnit. Tap '+ Add Profile' to define Cartons, Packs, Sacks, etc.",
                            color = TextMuted,
                            fontSize = 12.sp,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                }
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    for (profile in packagingProfiles) {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = DarkSurfaceCard),
                            border = androidx.compose.foundation.BorderStroke(1.dp, DarkBorder),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                                        Text(profile.unitName, color = TextWhite, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                        if (profile.canOpen) {
                                            Box(
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(4.dp))
                                                    .background(SuccessGreen.copy(alpha = 0.2f))
                                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                                            ) {
                                                Text("Openable", color = SuccessGreen, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                            }
                                        }
                                    }

                                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                        IconButton(
                                            onClick = {
                                                editingProfile = profile
                                                showProfileDialog = true
                                            },
                                            modifier = Modifier.size(28.dp)
                                        ) {
                                            Icon(Icons.Default.Edit, contentDescription = "Edit", tint = RgAccent, modifier = Modifier.size(16.dp))
                                        }
                                        IconButton(
                                            onClick = {
                                                packagingProfiles = packagingProfiles.filter { it != profile }
                                            },
                                            modifier = Modifier.size(28.dp)
                                        ) {
                                            Icon(Icons.Default.Delete, contentDescription = "Delete", tint = AlertRed, modifier = Modifier.size(16.dp))
                                        }
                                    }
                                }

                                Text(
                                    text = if (profile.parentUnitName != null && profile.parentUnitMultiplier != null) {
                                        "Contains ${profile.parentUnitMultiplier.toInt()} × ${profile.parentUnitName} = ${profile.conversionFactor.toInt()} ${baseUnit}s"
                                    } else {
                                        "Contains ${profile.conversionFactor.toInt()} ${baseUnit}s"
                                    },
                                    color = TextMuted,
                                    fontSize = 12.sp
                                )

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text("Retail Price", color = TextSubtle, fontSize = 10.sp)
                                        Text(
                                            CurrencyFormatter.format(profile.customRetailPrice ?: 0.0),
                                            color = RgAccent,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.sp
                                        )
                                    }
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text("Wholesale", color = TextSubtle, fontSize = 10.sp)
                                        Text(
                                            CurrencyFormatter.format(profile.customWholesalePrice ?: 0.0),
                                            color = CreditBlue,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.sp
                                        )
                                    }
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text("Purchase Cost", color = TextSubtle, fontSize = 10.sp)
                                        Text(
                                            CurrencyFormatter.format(profile.purchaseCost ?: 0.0),
                                            color = TextWhite,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.sp
                                        )
                                    }
                                }

                                if (profile.barcode.isNotBlank() || profile.intactCount > 0) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        if (profile.barcode.isNotBlank()) {
                                            Text("Barcode: ${profile.barcode}", color = TextMuted, fontSize = 11.sp)
                                        }
                                        if (trackIntactPackages && profile.intactCount > 0) {
                                            Text("Intact: ${profile.intactCount} pkgs", color = CashAmber, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // SAVE PRODUCT BUTTON
            FuturisticButton(
                text = if (isSaving) "Saving Product & Packaging..." else "Save Product & Packaging",
                enabled = !isSaving && name.isNotBlank() && (retailPriceText.toDoubleOrNull() ?: 0.0) > 0,
                onClick = {
                    isSaving = true
                    coroutineScope.launch {
                        val product = Product(
                            id = if (isEditMode) productId else 0L,
                            name = name,
                            sku = sku,
                            barcode = barcode,
                            categoryName = category.ifBlank { "General" },
                            brand = brand,
                            baseUnit = baseUnit.ifBlank { "Piece" },
                            buyingCost = buyingCostText.toDoubleOrNull() ?: 0.0,
                            retailPrice = retailPriceText.toDoubleOrNull() ?: 0.0,
                            wholesalePrice = wholesalePriceText.toDoubleOrNull() ?: (retailPriceText.toDoubleOrNull() ?: 0.0),
                            currentStockBase = stockText.toDoubleOrNull() ?: 0.0,
                            minStock = minStockText.toDoubleOrNull() ?: 10.0,
                            trackIntactPackages = trackIntactPackages
                        )
                        val savedProductId = viewModel.repository.saveProduct(product)

                        // Save all packaging profiles
                        val existingFromDb = viewModel.repository.getUnitConversionsSync(savedProductId)
                        val currentIds = packagingProfiles.map { it.id }.toSet()

                        // Delete removed profiles
                        for (old in existingFromDb) {
                            if (old.id !in currentIds) {
                                viewModel.repository.deleteUnitConversion(old.id)
                            }
                        }

                        // Insert or update current profiles
                        for (p in packagingProfiles) {
                            val toSave = p.copy(productId = savedProductId)
                            if (toSave.id == 0L) {
                                viewModel.repository.addUnitConversion(toSave)
                            } else {
                                viewModel.repository.updateUnitConversion(toSave)
                            }
                        }

                        Toast.makeText(context, "Product & packaging saved successfully", Toast.LENGTH_SHORT).show()
                        onNavigateBack()
                    }
                },
                modifier = Modifier.fillMaxWidth().testTag("save_product_button")
            )

            Spacer(modifier = Modifier.height(24.dp))
        }
    }

    // Packaging Profile Dialog (Part 2, 4, 5, 11)
    if (showProfileDialog) {
        PackagingProfileDialog(
            baseUnit = baseUnit,
            existingProfile = editingProfile,
            otherProfiles = packagingProfiles.filter { it != editingProfile },
            onSave = { savedProfile ->
                if (editingProfile != null) {
                    packagingProfiles = packagingProfiles.map {
                        if (it == editingProfile) savedProfile else it
                    }
                } else {
                    packagingProfiles = packagingProfiles + savedProfile
                }
                showProfileDialog = false
                editingProfile = null
            },
            onDismiss = {
                showProfileDialog = false
                editingProfile = null
            }
        )
    }
}

@Composable
fun PackagingProfileDialog(
    baseUnit: String,
    existingProfile: UnitConversion?,
    otherProfiles: List<UnitConversion>,
    onSave: (UnitConversion) -> Unit,
    onDismiss: () -> Unit
) {
    var unitName by remember { mutableStateOf(existingProfile?.unitName ?: "") }
    var conversionMode by remember { mutableIntStateOf(if (existingProfile?.parentUnitName != null) 1 else 0) } // 0: Direct Base Units, 1: Hierarchical
    var factorText by remember { mutableStateOf(existingProfile?.conversionFactor?.toInt()?.toString() ?: "12") }

    // Hierarchical setup
    var selectedParentProfile by remember {
        mutableStateOf(otherProfiles.firstOrNull { it.unitName == existingProfile?.parentUnitName })
    }
    var parentMultiplierText by remember { mutableStateOf(existingProfile?.parentUnitMultiplier?.toInt()?.toString() ?: "10") }

    var purchaseCostText by remember { mutableStateOf(existingProfile?.purchaseCost?.toString() ?: "") }
    var retailPriceText by remember { mutableStateOf(existingProfile?.customRetailPrice?.toString() ?: "") }
    var wholesalePriceText by remember { mutableStateOf(existingProfile?.customWholesalePrice?.toString() ?: "") }
    var barcodeText by remember { mutableStateOf(existingProfile?.barcode ?: "") }
    var skuText by remember { mutableStateOf(existingProfile?.sku ?: "") }
    var canOpen by remember { mutableStateOf(existingProfile?.canOpen ?: true) }
    var intactCountText by remember { mutableStateOf(existingProfile?.intactCount?.toString() ?: "0") }

    // Compute effective conversion factor
    val effectiveFactor = remember(conversionMode, factorText, selectedParentProfile, parentMultiplierText) {
        if (conversionMode == 1 && selectedParentProfile != null) {
            val mult = parentMultiplierText.toDoubleOrNull() ?: 1.0
            mult * selectedParentProfile!!.conversionFactor
        } else {
            factorText.toDoubleOrNull() ?: 1.0
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier.fillMaxWidth().padding(8.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = DarkSurfaceCard),
            border = androidx.compose.foundation.BorderStroke(1.dp, DarkBorder)
        ) {
            Column(
                modifier = Modifier
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (existingProfile != null) "Edit Packaging Profile" else "Add Packaging Profile",
                        color = TextWhite,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                    IconButton(onClick = onDismiss, modifier = Modifier.size(24.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = TextMuted)
                    }
                }

                OutlinedTextField(
                    value = unitName,
                    onValueChange = { unitName = it },
                    label = { Text("Profile Name (e.g. Carton 18 × 500ml, 50 KG Sack, Pack)") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = RgAccent,
                        unfocusedBorderColor = DarkBorder,
                        focusedTextColor = TextWhite,
                        unfocusedTextColor = TextWhite
                    ),
                    singleLine = true
                )

                // Conversion Definition Mode
                Text("Conversion Setup:", color = TextMuted, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = conversionMode == 0,
                        onClick = { conversionMode = 0 },
                        label = { Text("Direct $baseUnit", fontSize = 11.sp) },
                        modifier = Modifier.weight(1f)
                    )
                    FilterChip(
                        selected = conversionMode == 1,
                        onClick = { conversionMode = 1 },
                        label = { Text("Hierarchical Unit", fontSize = 11.sp) },
                        enabled = otherProfiles.isNotEmpty(),
                        modifier = Modifier.weight(1f)
                    )
                }

                if (conversionMode == 0) {
                    OutlinedTextField(
                        value = factorText,
                        onValueChange = { factorText = it },
                        label = { Text("Contains how many $baseUnit?") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = RgAccent,
                            unfocusedBorderColor = DarkBorder,
                            focusedTextColor = TextWhite,
                            unfocusedTextColor = TextWhite
                        ),
                        singleLine = true
                    )
                } else {
                    // Hierarchical parent selection (e.g., 20 × Pack of 30 sweets = 600 pieces)
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("Select Parent Unit:", color = TextMuted, fontSize = 11.sp)
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            items(otherProfiles) { p ->
                                val isSel = p == selectedParentProfile
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(if (isSel) RgAccent else DarkSurfaceElevated)
                                        .border(1.dp, if (isSel) RgAccent else DarkBorder, RoundedCornerShape(6.dp))
                                        .clickable { selectedParentProfile = p }
                                        .padding(horizontal = 10.dp, vertical = 6.dp)
                                ) {
                                    Text(
                                        "${p.unitName} (${p.conversionFactor.toInt()} $baseUnit)",
                                        color = if (isSel) DarkBg else TextWhite,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }

                        OutlinedTextField(
                            value = parentMultiplierText,
                            onValueChange = { parentMultiplierText = it },
                            label = { Text("Contains how many of ${selectedParentProfile?.unitName ?: "Parent"}?") },
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = RgAccent,
                                unfocusedBorderColor = DarkBorder,
                                focusedTextColor = TextWhite,
                                unfocusedTextColor = TextWhite
                            ),
                            singleLine = true
                        )

                        Text(
                            "Total = ${effectiveFactor.toInt()} $baseUnit per $unitName",
                            color = SuccessGreen,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                HorizontalDivider(color = DarkDivider)

                Text("Independent Pricing & Costs (KES):", color = TextMuted, fontSize = 11.sp, fontWeight = FontWeight.Bold)

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = retailPriceText,
                        onValueChange = { retailPriceText = it },
                        label = { Text("Retail Selling (KSh)") },
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
                        value = wholesalePriceText,
                        onValueChange = { wholesalePriceText = it },
                        label = { Text("Wholesale (KSh)") },
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

                HorizontalDivider(color = DarkDivider)

                Text("Packaging Barcode & SKU:", color = TextMuted, fontSize = 11.sp, fontWeight = FontWeight.Bold)

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

                // Break-bulk & Intact count toggles
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
                        Text("Permits opening into loose $baseUnit in inventory", color = TextMuted, fontSize = 10.sp)
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
                    label = { Text("Initial Intact Packages Count") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = RgAccent,
                        unfocusedBorderColor = DarkBorder,
                        focusedTextColor = TextWhite,
                        unfocusedTextColor = TextWhite
                    ),
                    singleLine = true
                )

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    FuturisticButton(
                        text = "Cancel",
                        onClick = onDismiss,
                        isSecondary = true,
                        modifier = Modifier.weight(1f)
                    )
                    FuturisticButton(
                        text = "Save Profile",
                        enabled = unitName.isNotBlank() && effectiveFactor > 0,
                        onClick = {
                            val profile = UnitConversion(
                                id = existingProfile?.id ?: 0L,
                                productId = existingProfile?.productId ?: 0L,
                                unitName = unitName.trim(),
                                conversionFactor = effectiveFactor,
                                customRetailPrice = retailPriceText.toDoubleOrNull(),
                                customWholesalePrice = wholesalePriceText.toDoubleOrNull(),
                                barcode = barcodeText.trim(),
                                sku = skuText.trim(),
                                purchaseCost = purchaseCostText.toDoubleOrNull(),
                                canOpen = canOpen,
                                intactCount = intactCountText.toIntOrNull() ?: 0,
                                parentUnitName = if (conversionMode == 1) selectedParentProfile?.unitName else null,
                                parentUnitMultiplier = if (conversionMode == 1) parentMultiplierText.toDoubleOrNull() else null
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
