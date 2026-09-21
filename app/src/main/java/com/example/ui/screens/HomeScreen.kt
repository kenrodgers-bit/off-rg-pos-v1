package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.Sale
import com.example.ui.components.EmptyStateView
import com.example.ui.components.MetricCard
import com.example.ui.components.OfflineIndicator
import com.example.ui.theme.*
import com.example.ui.viewmodel.PosViewModel
import com.example.util.CurrencyFormatter
import java.util.Calendar

@Composable
fun HomeScreen(
    viewModel: PosViewModel,
    onNavigateToPos: () -> Unit,
    onNavigateToAddProduct: () -> Unit,
    onNavigateToReceiveStock: () -> Unit,
    onNavigateToAddExpense: () -> Unit,
    onNavigateToLowStock: () -> Unit,
    onNavigateToCustomers: () -> Unit,
    onViewSaleReceipt: (Sale) -> Unit
) {
    val business by viewModel.business.collectAsStateWithLifecycle()
    val isOnline by viewModel.isOnline.collectAsStateWithLifecycle()
    val todayStats by viewModel.todaySalesStats.collectAsStateWithLifecycle()
    val recentSales by viewModel.recentSales.collectAsStateWithLifecycle()
    val lowStockProducts by viewModel.lowStockProducts.collectAsStateWithLifecycle()
    val outstandingCredit by viewModel.totalOutstandingCredit.collectAsStateWithLifecycle()

    var cashSalesToday by remember { mutableDoubleStateOf(0.0) }
    var mpesaSalesToday by remember { mutableDoubleStateOf(0.0) }
    var creditSalesToday by remember { mutableDoubleStateOf(0.0) }

    LaunchedEffect(recentSales) {
        viewModel.getTodayPaymentSummary { cash, mpesa, credit ->
            cashSalesToday = cash
            mpesaSalesToday = mpesa
            creditSalesToday = credit
        }
    }

    val greeting = remember {
        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        when (hour) {
            in 0..11 -> "Good morning"
            in 12..16 -> "Good afternoon"
            else -> "Good evening"
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBg)
            .widthIn(max = 900.dp)
            .padding(horizontal = 16.dp)
            .testTag("home_screen"),
        contentPadding = PaddingValues(top = 16.dp, bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Header
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "$greeting,",
                        color = TextMuted,
                        fontSize = 13.sp
                    )
                    Text(
                        text = business?.name ?: "RG POS",
                        color = TextWhite,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                OfflineIndicator(isOnline = isOnline)
            }
        }

        // Primary Metrics Row
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                MetricCard(
                    title = "Today's Sales",
                    value = CurrencyFormatter.format(todayStats.first),
                    accentColor = RgAccent,
                    icon = Icons.Default.TrendingUp,
                    modifier = Modifier.weight(1f).testTag("today_sales_card")
                )
                MetricCard(
                    title = "Today's Profit",
                    value = CurrencyFormatter.format(todayStats.second),
                    accentColor = SuccessGreen,
                    icon = Icons.Default.AttachMoney,
                    modifier = Modifier.weight(1f).testTag("today_profit_card")
                )
                MetricCard(
                    title = "Transactions",
                    value = "${todayStats.third}",
                    accentColor = CreditBlue,
                    icon = Icons.Default.ReceiptLong,
                    modifier = Modifier.weight(0.85f).testTag("transactions_count_card")
                )
            }
        }

        // Payment Summary
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = DarkSurfaceCard),
                border = androidx.compose.foundation.BorderStroke(1.dp, DarkBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "PAYMENT SUMMARY",
                        color = TextMuted,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        PaymentSummaryItem(
                            label = "Cash",
                            amount = CurrencyFormatter.format(cashSalesToday),
                            color = CashAmber
                        )
                        PaymentSummaryItem(
                            label = "M-Pesa",
                            amount = CurrencyFormatter.format(mpesaSalesToday),
                            color = MpesaGreen
                        )
                        PaymentSummaryItem(
                            label = "Credit",
                            amount = CurrencyFormatter.format(creditSalesToday),
                            color = CreditBlue
                        )
                    }
                }
            }
        }

        // Alerts Section
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Low Stock Alert
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = if (lowStockProducts.isNotEmpty()) AlertRedBg else DarkSurfaceCard
                    ),
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        if (lowStockProducts.isNotEmpty()) AlertRed.copy(alpha = 0.5f) else DarkBorder
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .clickable { onNavigateToLowStock() }
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(if (lowStockProducts.isNotEmpty()) AlertRed.copy(alpha = 0.2f) else DarkSurfaceElevated),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = null,
                                tint = if (lowStockProducts.isNotEmpty()) AlertRed else TextMuted,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Column {
                            Text(
                                text = "Low Stock",
                                color = TextMuted,
                                fontSize = 11.sp
                            )
                            Text(
                                text = "${lowStockProducts.size} products",
                                color = if (lowStockProducts.isNotEmpty()) AlertRed else TextWhite,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                        }
                    }
                }

                // Outstanding Credit Alert
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = if (outstandingCredit > 0) WarningOrangeBg else DarkSurfaceCard
                    ),
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        if (outstandingCredit > 0) WarningOrange.copy(alpha = 0.5f) else DarkBorder
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .clickable { onNavigateToCustomers() }
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(if (outstandingCredit > 0) WarningOrange.copy(alpha = 0.2f) else DarkSurfaceElevated),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.AccountBalanceWallet,
                                contentDescription = null,
                                tint = if (outstandingCredit > 0) WarningOrange else TextMuted,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Column {
                            Text(
                                text = "Outstanding Credit",
                                color = TextMuted,
                                fontSize = 11.sp
                            )
                            Text(
                                text = CurrencyFormatter.format(outstandingCredit),
                                color = if (outstandingCredit > 0) WarningOrange else TextWhite,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        }

        // Quick Actions
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "QUICK ACTIONS",
                    color = TextMuted,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    QuickActionButton(
                        title = "New Sale",
                        icon = Icons.Default.ShoppingCart,
                        accent = RgAccent,
                        modifier = Modifier.weight(1f),
                        onClick = onNavigateToPos
                    )
                    QuickActionButton(
                        title = "Add Product",
                        icon = Icons.Default.AddBox,
                        accent = TextWhite,
                        modifier = Modifier.weight(1f),
                        onClick = onNavigateToAddProduct
                    )
                    QuickActionButton(
                        title = "Receive Stock",
                        icon = Icons.Default.Inventory,
                        accent = SuccessGreen,
                        modifier = Modifier.weight(1f),
                        onClick = onNavigateToReceiveStock
                    )
                    QuickActionButton(
                        title = "Add Expense",
                        icon = Icons.Default.Payments,
                        accent = CashAmber,
                        modifier = Modifier.weight(1f),
                        onClick = onNavigateToAddExpense
                    )
                }
            }
        }

        // Recent Sales Section
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "RECENT SALES",
                    color = TextMuted,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
                Text(
                    text = "${recentSales.size} sales",
                    color = TextSubtle,
                    fontSize = 12.sp
                )
            }
        }

        if (recentSales.isEmpty()) {
            item {
                EmptyStateView(
                    icon = Icons.Default.PointOfSale,
                    title = "No sales today",
                    description = "Your first sale will appear here.",
                    actionButtonText = "Start New Sale",
                    onActionClick = onNavigateToPos
                )
            }
        } else {
            items(recentSales.take(15)) { sale ->
                SaleRowItem(
                    sale = sale,
                    onClick = { onViewSaleReceipt(sale) }
                )
            }
        }
    }
}

@Composable
fun PaymentSummaryItem(label: String, amount: String, color: Color) {
    Column {
        Text(text = label, color = TextMuted, fontSize = 12.sp)
        Text(text = amount, color = color, fontSize = 14.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun QuickActionButton(
    title: String,
    icon: ImageVector,
    accent: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = DarkSurfaceCard),
        border = androidx.compose.foundation.BorderStroke(1.dp, DarkBorder),
        modifier = modifier.clickable { onClick() }
    ) {
        Column(
            modifier = Modifier.padding(vertical = 12.dp, horizontal = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(CircleShape)
                    .background(DarkSurfaceElevated),
                contentAlignment = Alignment.Center
            ) {
                Icon(imageVector = icon, contentDescription = null, tint = accent, modifier = Modifier.size(18.dp))
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = title,
                color = TextWhite,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun SaleRowItem(sale: Sale, onClick: () -> Unit) {
    Card(
        colors = CardDefaults.cardColors(containerColor = DarkSurfaceCard),
        border = androidx.compose.foundation.BorderStroke(1.dp, DarkBorder),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .testTag("sale_row_${sale.receiptNumber}")
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
                        .clip(RoundedCornerShape(8.dp))
                        .background(DarkSurfaceElevated),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Receipt,
                        contentDescription = null,
                        tint = RgAccent,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Column {
                    Text(
                        text = "#${sale.receiptNumber}",
                        color = TextWhite,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                    Text(
                        text = "${sale.customerName} • ${sale.saleType}",
                        color = TextMuted,
                        fontSize = 12.sp
                    )
                }
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = CurrencyFormatter.format(sale.total),
                    color = RgAccent,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp
                )
                Text(
                    text = CurrencyFormatter.formatTime(sale.dateEpoch),
                    color = TextSubtle,
                    fontSize = 11.sp
                )
            }
        }
    }
}
