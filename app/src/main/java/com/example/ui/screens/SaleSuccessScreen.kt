package com.example.ui.screens

import android.widget.Toast
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.Payment
import com.example.data.model.Sale
import com.example.data.model.SaleItem
import com.example.ui.components.FuturisticButton
import com.example.ui.theme.*
import com.example.ui.viewmodel.PosViewModel
import com.example.util.CurrencyFormatter
import com.example.util.ReceiptHelper
import kotlinx.coroutines.launch

@Composable
fun SaleSuccessScreen(
    saleId: Long,
    viewModel: PosViewModel,
    onStartNewSale: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val business by viewModel.business.collectAsStateWithLifecycle()

    var sale by remember { mutableStateOf<Sale?>(null) }
    var saleItems by remember { mutableStateOf<List<SaleItem>>(emptyList()) }
    var payments by remember { mutableStateOf<List<Payment>>(emptyList()) }

    LaunchedEffect(saleId) {
        val (s, items) = viewModel.repository.getSaleDetails(saleId)
        sale = s
        saleItems = items
        payments = viewModel.repository.getPaymentsForSale(saleId)
    }

    val scaleAnim = remember { Animatable(0.5f) }
    LaunchedEffect(Unit) {
        scaleAnim.animateTo(
            targetValue = 1f,
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessLow
            )
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBg)
            .padding(24.dp)
            .testTag("sale_success_screen"),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            // Success Circle
            Box(
                modifier = Modifier
                    .scale(scaleAnim.value)
                    .size(80.dp)
                    .clip(CircleShape)
                    .background(SuccessGreen)
                    .border(2.dp, SuccessGreen.copy(alpha = 0.5f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null,
                    tint = DarkBg,
                    modifier = Modifier.size(44.dp)
                )
            }

            Text(
                text = "Sale Completed",
                color = TextWhite,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold
            )

            Text(
                text = "Receipt #${sale?.receiptNumber ?: "..."}",
                color = RgAccent,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold
            )

            // Card Breakdown
            Card(
                colors = CardDefaults.cardColors(containerColor = DarkSurfaceCard),
                border = androidx.compose.foundation.BorderStroke(1.dp, DarkBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Total Paid:", color = TextMuted, fontSize = 14.sp)
                        Text(
                            CurrencyFormatter.format(sale?.total ?: 0.0),
                            color = TextWhite,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Black
                        )
                    }

                    HorizontalDivider(color = DarkDivider)

                    Text("PAYMENT BREAKDOWN", color = TextMuted, fontSize = 11.sp, fontWeight = FontWeight.Bold)

                    for (p in payments) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "${p.paymentMethod} ${if (p.mpesaRef.isNotBlank()) "(${p.mpesaRef})" else ""}",
                                color = TextWhite,
                                fontSize = 13.sp
                            )
                            Text(
                                text = CurrencyFormatter.format(p.amount),
                                color = RgAccent,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    if (sale?.customerName?.isNotBlank() == true) {
                        HorizontalDivider(color = DarkDivider)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Customer:", color = TextMuted, fontSize = 13.sp)
                            Text(sale?.customerName ?: "", color = TextWhite, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                FuturisticButton(
                    text = "Print",
                    icon = Icons.Default.Print,
                    onClick = {
                        Toast.makeText(context, "Receipt sent to printer", Toast.LENGTH_SHORT).show()
                    },
                    isSecondary = true,
                    modifier = Modifier.weight(1f),
                    testTag = "print_receipt_button"
                )

                FuturisticButton(
                    text = "Share",
                    icon = Icons.Default.Share,
                    onClick = {
                        if (sale != null) {
                            val text = ReceiptHelper.generateReceiptText(business, sale!!, saleItems, payments)
                            ReceiptHelper.shareReceipt(context, text)
                        }
                    },
                    isSecondary = true,
                    modifier = Modifier.weight(1f),
                    testTag = "share_receipt_button"
                )
            }

            FuturisticButton(
                text = "New Sale",
                icon = Icons.Default.AddShoppingCart,
                onClick = onStartNewSale,
                modifier = Modifier.fillMaxWidth(),
                testTag = "start_new_sale_button"
            )
        }
    }
}
