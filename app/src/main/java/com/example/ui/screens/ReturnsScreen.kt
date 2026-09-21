package com.example.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
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
import com.example.data.model.Sale
import com.example.data.model.SaleItem
import com.example.ui.components.FuturisticButton
import com.example.ui.theme.*
import com.example.ui.viewmodel.PosViewModel
import com.example.util.CurrencyFormatter
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReturnsScreen(
    viewModel: PosViewModel,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val currentUser by viewModel.currentUser.collectAsStateWithLifecycle()

    var receiptQuery by remember { mutableStateOf("") }
    var searchedSale by remember { mutableStateOf<Sale?>(null) }
    var saleItems by remember { mutableStateOf<List<SaleItem>>(emptyList()) }
    var selectedItem by remember { mutableStateOf<SaleItem?>(null) }
    var returnQtyText by remember { mutableStateOf("1") }
    var isDamaged by remember { mutableStateOf(false) }
    var refundMethod by remember { mutableStateOf("CASH") } // "CASH", "STORE_CREDIT"
    var reason by remember { mutableStateOf("Customer return / exchange") }
    var isProcessing by remember { mutableStateOf(false) }

    Scaffold(
        containerColor = DarkBg,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Returns & Refunds",
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
                .widthIn(max = 900.dp)
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState())
                .testTag("returns_screen"),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text(
                text = "Search original receipt to initiate return or refund.",
                color = TextMuted,
                fontSize = 13.sp
            )

            // Search Receipt
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = receiptQuery,
                    onValueChange = { receiptQuery = it },
                    placeholder = { Text("Receipt # (e.g. RG-000001)") },
                    modifier = Modifier.weight(1f).testTag("return_receipt_search_input"),
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
                        coroutineScope.launch {
                            val sale = viewModel.repository.db.saleDao().getSaleByReceiptNumber(receiptQuery.trim())
                            if (sale != null) {
                                searchedSale = sale
                                saleItems = viewModel.repository.db.saleDao().getSaleItems(sale.id)
                                selectedItem = saleItems.firstOrNull()
                                returnQtyText = "1"
                            } else {
                                Toast.makeText(context, "Receipt not found", Toast.LENGTH_SHORT).show()
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = RgAccent, contentColor = DarkBg),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.testTag("search_receipt_button")
                ) {
                    Text("Search", fontWeight = FontWeight.Bold)
                }
            }

            if (searchedSale != null) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = DarkSurfaceCard),
                    border = androidx.compose.foundation.BorderStroke(1.dp, DarkBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("RECEIPT #${searchedSale!!.receiptNumber}", color = RgAccent, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Text("Customer: ${searchedSale!!.customerName} • Total: ${CurrencyFormatter.format(searchedSale!!.total)}", color = TextWhite, fontSize = 13.sp)

                        HorizontalDivider(color = DarkDivider)

                        Text("Select Item to Return:", color = TextMuted, fontSize = 12.sp)
                        for (item in saleItems) {
                            val isSel = item.id == selectedItem?.id
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(if (isSel) DarkSurfaceElevated else DarkBg)
                                    .border(1.dp, if (isSel) RgAccent else DarkBorder, RoundedCornerShape(6.dp))
                                    .clickable { selectedItem = item }
                                    .padding(10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(item.productName, color = TextWhite, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                                    Text("Purchased: ${item.quantity.toInt()} ${item.unitName} @ ${CurrencyFormatter.format(item.unitPrice)}", color = TextMuted, fontSize = 11.sp)
                                }
                                Text(CurrencyFormatter.format(item.subtotal), color = RgAccent, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                if (selectedItem != null) {
                    val returnQty = returnQtyText.toDoubleOrNull() ?: 0.0
                    val refundAmount = returnQty * selectedItem!!.unitPrice

                    Card(
                        colors = CardDefaults.cardColors(containerColor = DarkSurfaceCard),
                        border = androidx.compose.foundation.BorderStroke(1.dp, DarkBorder),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Text("RETURN PARAMETERS", color = TextMuted, fontSize = 11.sp, fontWeight = FontWeight.Bold)

                            OutlinedTextField(
                                value = returnQtyText,
                                onValueChange = { returnQtyText = it },
                                label = { Text("Quantity to Return (${selectedItem!!.unitName})") },
                                modifier = Modifier.fillMaxWidth(),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = RgAccent,
                                    unfocusedBorderColor = DarkBorder,
                                    focusedTextColor = TextWhite,
                                    unfocusedTextColor = TextWhite
                                ),
                                singleLine = true
                            )

                            // Item condition toggle
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text("Damaged / Defective?", color = TextWhite, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                                    Text("If marked damaged, stock will NOT be restored to shelf", color = TextMuted, fontSize = 11.sp)
                                }
                                Switch(
                                    checked = isDamaged,
                                    onCheckedChange = { isDamaged = it },
                                    colors = SwitchDefaults.colors(checkedThumbColor = AlertRed, checkedTrackColor = AlertRed.copy(alpha = 0.5f))
                                )
                            }

                            OutlinedTextField(
                                value = reason,
                                onValueChange = { reason = it },
                                label = { Text("Return Reason") },
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

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("REFUND TOTAL DUE:", color = TextMuted, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                Text(CurrencyFormatter.format(refundAmount), color = CashAmber, fontWeight = FontWeight.Black, fontSize = 18.sp)
                            }

                            FuturisticButton(
                                text = if (isProcessing) "Processing Return..." else "Process Return & Refund",
                                enabled = !isProcessing && returnQty > 0 && returnQty <= selectedItem!!.quantity,
                                icon = Icons.Default.AssignmentReturn,
                                onClick = {
                                    isProcessing = true
                                    coroutineScope.launch {
                                        val factorPerUnit = if (selectedItem!!.quantity > 0) selectedItem!!.baseQuantityDeducted / selectedItem!!.quantity else 1.0
                                        viewModel.repository.processReturn(
                                            originalSale = searchedSale!!,
                                            saleItem = selectedItem!!,
                                            returnQty = returnQty,
                                            refundAmount = refundAmount,
                                            baseQuantityToRestore = if (!isDamaged) returnQty * factorPerUnit else 0.0,
                                            reason = reason,
                                            authorizedBy = currentUser
                                        )
                                        isProcessing = false
                                        Toast.makeText(context, "Return processed: ${CurrencyFormatter.format(refundAmount)} refunded", Toast.LENGTH_SHORT).show()
                                        onNavigateBack()
                                    }
                                },
                                modifier = Modifier.fillMaxWidth().testTag("confirm_return_button")
                            )
                        }
                    }
                }
            }
        }
    }
}
