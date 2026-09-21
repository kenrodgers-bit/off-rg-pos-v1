package com.example.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
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
import com.example.data.model.Product
import com.example.ui.components.FuturisticButton
import com.example.ui.theme.*
import com.example.ui.viewmodel.PosViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StockTakeScreen(
    viewModel: PosViewModel,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val allProducts by viewModel.allProducts.collectAsStateWithLifecycle()
    val currentUser by viewModel.currentUser.collectAsStateWithLifecycle()

    var selectedProduct by remember { mutableStateOf<Product?>(null) }
    var actualCountText by remember { mutableStateOf("") }
    var reasonText by remember { mutableStateOf("Physical count discrepancy") }
    var searchQuery by remember { mutableStateOf("") }

    val actualCount = actualCountText.toDoubleOrNull()
    val expectedCount = selectedProduct?.currentStockBase ?: 0.0
    val difference = if (actualCount != null) actualCount - expectedCount else 0.0

    Scaffold(
        containerColor = DarkBg,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Stock Take / Physical Count",
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
                .testTag("stock_take_screen"),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Select product to adjust physical count against system inventory.",
                color = TextMuted,
                fontSize = 13.sp
            )

            // Step 1: Choose Product
            Card(
                colors = CardDefaults.cardColors(containerColor = DarkSurfaceCard),
                border = androidx.compose.foundation.BorderStroke(1.dp, DarkBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("1. CHOOSE PRODUCT", color = TextMuted, fontSize = 11.sp, fontWeight = FontWeight.Bold)

                    if (selectedProduct == null) {
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            placeholder = { Text("Filter products...", fontSize = 12.sp) },
                            modifier = Modifier.fillMaxWidth().height(48.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = RgAccent,
                                unfocusedBorderColor = DarkBorder,
                                focusedTextColor = TextWhite,
                                unfocusedTextColor = TextWhite
                            ),
                            singleLine = true
                        )

                        val list = allProducts.filter {
                            searchQuery.isBlank() || it.name.contains(searchQuery, ignoreCase = true)
                        }.take(5)

                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            for (p in list) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(DarkSurfaceElevated)
                                        .clickable {
                                            selectedProduct = p
                                            actualCountText = p.currentStockBase.toInt().toString()
                                        }
                                        .padding(10.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(p.name, color = TextWhite, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                                    Text("Stock: ${p.currentStockBase.toInt()} ${p.baseUnit}s", color = RgAccent, fontSize = 12.sp)
                                }
                            }
                        }
                    } else {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(selectedProduct!!.name, color = TextWhite, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                Text("Recorded Base Stock: ${selectedProduct!!.currentStockBase.toInt()} ${selectedProduct!!.baseUnit}s", color = RgAccent, fontSize = 13.sp)
                            }
                            Button(
                                onClick = { selectedProduct = null },
                                colors = ButtonDefaults.buttonColors(containerColor = DarkSurfaceElevated, contentColor = TextWhite)
                            ) {
                                Text("Change")
                            }
                        }
                    }
                }
            }

            // Step 2: Physical Count Entry (Part 20)
            if (selectedProduct != null) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = DarkSurfaceCard),
                    border = androidx.compose.foundation.BorderStroke(1.dp, DarkBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Text("2. PHYSICAL COUNT", color = TextMuted, fontSize = 11.sp, fontWeight = FontWeight.Bold)

                        OutlinedTextField(
                            value = actualCountText,
                            onValueChange = { actualCountText = it },
                            label = { Text("Actual Counted (${selectedProduct!!.baseUnit}s)") },
                            modifier = Modifier.fillMaxWidth().testTag("actual_stock_count_input"),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = RgAccent,
                                unfocusedBorderColor = DarkBorder,
                                focusedTextColor = TextWhite,
                                unfocusedTextColor = TextWhite
                            ),
                            singleLine = true
                        )

                        // Live Difference calculation (Part 20)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("DIFFERENCE / VARIANCE:", color = TextMuted, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            val diffLabel = if (difference > 0) "+${difference.toInt()}" else "${difference.toInt()}"
                            val diffColor = if (difference < 0) AlertRed else if (difference > 0) SuccessGreen else TextWhite
                            Text(
                                text = "$diffLabel ${selectedProduct!!.baseUnit}s",
                                color = diffColor,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Black
                            )
                        }

                        OutlinedTextField(
                            value = reasonText,
                            onValueChange = { reasonText = it },
                            label = { Text("Reason for Adjustment") },
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

                        FuturisticButton(
                            text = "Apply Stock Adjustment",
                            icon = Icons.Default.Check,
                            enabled = actualCount != null,
                            onClick = {
                                coroutineScope.launch {
                                    viewModel.repository.applyStockTakeAdjustment(
                                        productId = selectedProduct!!.id,
                                        expectedStock = expectedCount,
                                        actualStock = actualCount!!,
                                        staffName = currentUser,
                                        reason = reasonText
                                    )
                                    Toast.makeText(context, "Stock adjusted successfully", Toast.LENGTH_SHORT).show()
                                    onNavigateBack()
                                }
                            },
                            modifier = Modifier.fillMaxWidth().testTag("apply_stock_adjustment_button")
                        )
                    }
                }
            }
        }
    }
}
