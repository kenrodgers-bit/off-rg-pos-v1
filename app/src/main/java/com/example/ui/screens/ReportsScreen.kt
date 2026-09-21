package com.example.ui.screens

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.*
import com.example.ui.components.FuturisticButton
import com.example.ui.components.MetricCard
import com.example.ui.components.StatusBadge
import com.example.ui.theme.*
import com.example.ui.viewmodel.PosViewModel
import com.example.util.CurrencyFormatter
import com.example.util.FilterCriteria
import com.example.util.ReportExporter
import com.example.util.ReportSummaryData
import java.text.SimpleDateFormat
import java.util.*

enum class DateFilterPreset(val label: String) {
    TODAY("Today"),
    YESTERDAY("Yesterday"),
    THIS_WEEK("This Week"),
    LAST_WEEK("Last Week"),
    THIS_MONTH("This Month"),
    LAST_MONTH("Last Month"),
    THIS_YEAR("This Year"),
    CUSTOM("Custom Range")
}

// One row per product + selling form (e.g. "Sweets" sold as "Carton" vs "Pack" vs "Piece"),
// scoped to whatever report filters are currently active.
data class SellingFormBreakdownRow(
    val productName: String,
    val unitName: String,
    val quantitySold: Double,
    val baseUnitsSold: Double,
    val revenue: Double,
    val cost: Double,
    val profit: Double
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportsScreen(
    viewModel: PosViewModel
) {
    val context = LocalContext.current

    val business by viewModel.business.collectAsStateWithLifecycle()
    val allCompletedSales by viewModel.allCompletedSales.collectAsStateWithLifecycle()
    val allSaleItems by viewModel.allSaleItems.collectAsStateWithLifecycle()
    val allExpenses by viewModel.allExpenses.collectAsStateWithLifecycle()
    val allPayments by viewModel.allPayments.collectAsStateWithLifecycle()
    val allReturns by viewModel.allReturns.collectAsStateWithLifecycle()
    val allStaff by viewModel.allStaff.collectAsStateWithLifecycle()
    val allCustomers by viewModel.allCustomers.collectAsStateWithLifecycle()

    // Filter State
    var selectedDatePreset by remember { mutableStateOf(DateFilterPreset.THIS_MONTH) }
    var customStartDateEpoch by remember { mutableStateOf<Long?>(null) }
    var customEndDateEpoch by remember { mutableStateOf<Long?>(null) }
    var showCustomDateDialog by remember { mutableStateOf(false) }

    var selectedStaff by remember { mutableStateOf("ALL") } // "ALL" or staff full name
    var selectedPaymentMethod by remember { mutableStateOf("ALL") } // "ALL", "CASH", "MPESA", "CREDIT", "PARTIAL"
    var selectedCustomerSegment by remember { mutableStateOf("ALL") } // "ALL", "RETAIL", "WHOLESALE", "CREDIT", "WALK_IN", or specific customer name

    // Compute Date Range Milliseconds
    val (rangeStartEpoch, rangeEndEpoch, rangeDisplayText) = remember(
        selectedDatePreset,
        customStartDateEpoch,
        customEndDateEpoch
    ) {
        val cal = Calendar.getInstance()
        val now = cal.timeInMillis

        when (selectedDatePreset) {
            DateFilterPreset.TODAY -> {
                cal.set(Calendar.HOUR_OF_DAY, 0)
                cal.set(Calendar.MINUTE, 0)
                cal.set(Calendar.SECOND, 0)
                cal.set(Calendar.MILLISECOND, 0)
                val start = cal.timeInMillis
                val end = now
                val fmt = SimpleDateFormat("dd MMM yyyy", Locale.US).format(Date(start))
                Triple(start, end, "Today ($fmt)")
            }
            DateFilterPreset.YESTERDAY -> {
                cal.add(Calendar.DAY_OF_YEAR, -1)
                cal.set(Calendar.HOUR_OF_DAY, 0)
                cal.set(Calendar.MINUTE, 0)
                cal.set(Calendar.SECOND, 0)
                cal.set(Calendar.MILLISECOND, 0)
                val start = cal.timeInMillis
                cal.set(Calendar.HOUR_OF_DAY, 23)
                cal.set(Calendar.MINUTE, 59)
                cal.set(Calendar.SECOND, 59)
                val end = cal.timeInMillis
                val fmt = SimpleDateFormat("dd MMM yyyy", Locale.US).format(Date(start))
                Triple(start, end, "Yesterday ($fmt)")
            }
            DateFilterPreset.THIS_WEEK -> {
                cal.set(Calendar.DAY_OF_WEEK, cal.firstDayOfWeek)
                cal.set(Calendar.HOUR_OF_DAY, 0)
                cal.set(Calendar.MINUTE, 0)
                cal.set(Calendar.SECOND, 0)
                val start = cal.timeInMillis
                val end = now
                val fmtStart = SimpleDateFormat("dd MMM", Locale.US).format(Date(start))
                val fmtEnd = SimpleDateFormat("dd MMM yyyy", Locale.US).format(Date(end))
                Triple(start, end, "$fmtStart – $fmtEnd")
            }
            DateFilterPreset.LAST_WEEK -> {
                cal.add(Calendar.WEEK_OF_YEAR, -1)
                cal.set(Calendar.DAY_OF_WEEK, cal.firstDayOfWeek)
                cal.set(Calendar.HOUR_OF_DAY, 0)
                cal.set(Calendar.MINUTE, 0)
                cal.set(Calendar.SECOND, 0)
                val start = cal.timeInMillis
                cal.add(Calendar.DAY_OF_WEEK, 6)
                cal.set(Calendar.HOUR_OF_DAY, 23)
                cal.set(Calendar.MINUTE, 59)
                val end = cal.timeInMillis
                val fmtStart = SimpleDateFormat("dd MMM", Locale.US).format(Date(start))
                val fmtEnd = SimpleDateFormat("dd MMM yyyy", Locale.US).format(Date(end))
                Triple(start, end, "Last Week ($fmtStart – $fmtEnd)")
            }
            DateFilterPreset.THIS_MONTH -> {
                cal.set(Calendar.DAY_OF_MONTH, 1)
                cal.set(Calendar.HOUR_OF_DAY, 0)
                cal.set(Calendar.MINUTE, 0)
                cal.set(Calendar.SECOND, 0)
                val start = cal.timeInMillis
                val end = now
                val fmt = SimpleDateFormat("MMMM yyyy", Locale.US).format(Date(start))
                Triple(start, end, "This Month ($fmt)")
            }
            DateFilterPreset.LAST_MONTH -> {
                cal.add(Calendar.MONTH, -1)
                cal.set(Calendar.DAY_OF_MONTH, 1)
                cal.set(Calendar.HOUR_OF_DAY, 0)
                cal.set(Calendar.MINUTE, 0)
                val start = cal.timeInMillis
                cal.set(Calendar.DAY_OF_MONTH, cal.getActualMaximum(Calendar.DAY_OF_MONTH))
                cal.set(Calendar.HOUR_OF_DAY, 23)
                cal.set(Calendar.MINUTE, 59)
                val end = cal.timeInMillis
                val fmt = SimpleDateFormat("MMMM yyyy", Locale.US).format(Date(start))
                Triple(start, end, "Last Month ($fmt)")
            }
            DateFilterPreset.THIS_YEAR -> {
                cal.set(Calendar.DAY_OF_YEAR, 1)
                cal.set(Calendar.HOUR_OF_DAY, 0)
                cal.set(Calendar.MINUTE, 0)
                val start = cal.timeInMillis
                val end = now
                val fmt = SimpleDateFormat("yyyy", Locale.US).format(Date(start))
                Triple(start, end, "Year $fmt")
            }
            DateFilterPreset.CUSTOM -> {
                val start = customStartDateEpoch ?: (now - 30L * 86400000L)
                val end = customEndDateEpoch ?: now
                val fmtStart = SimpleDateFormat("dd MMM yyyy", Locale.US).format(Date(start))
                val fmtEnd = SimpleDateFormat("dd MMM yyyy", Locale.US).format(Date(end))
                Triple(start, end, "$fmtStart – $fmtEnd")
            }
        }
    }

    // Step 1: Filter Sales by Date Range
    val dateFilteredSales = remember(allCompletedSales, rangeStartEpoch, rangeEndEpoch) {
        allCompletedSales.filter { it.dateEpoch in rangeStartEpoch..rangeEndEpoch }
    }

    // Step 2: Filter by Staff
    val staffFilteredSales = remember(dateFilteredSales, selectedStaff) {
        if (selectedStaff == "ALL") dateFilteredSales
        else dateFilteredSales.filter { it.cashierName.equals(selectedStaff, ignoreCase = true) }
    }

    // Map payments by saleId for multi-payment precision
    val paymentsBySaleId = remember(allPayments) {
        allPayments.groupBy { it.saleId }
    }

    // Step 3: Filter by Payment Method (handles split / partial payments)
    val paymentFilteredSales = remember(staffFilteredSales, selectedPaymentMethod, paymentsBySaleId) {
        when (selectedPaymentMethod) {
            "ALL" -> staffFilteredSales
            "CASH" -> staffFilteredSales.filter { sale ->
                val salePayments = paymentsBySaleId[sale.id] ?: emptyList()
                salePayments.any { it.paymentMethod == "CASH" }
            }
            "MPESA" -> staffFilteredSales.filter { sale ->
                val salePayments = paymentsBySaleId[sale.id] ?: emptyList()
                salePayments.any { it.paymentMethod == "MPESA" }
            }
            "CREDIT" -> staffFilteredSales.filter { sale ->
                val salePayments = paymentsBySaleId[sale.id] ?: emptyList()
                salePayments.any { it.paymentMethod == "CREDIT" }
            }
            "PARTIAL" -> staffFilteredSales.filter { sale ->
                val salePayments = paymentsBySaleId[sale.id] ?: emptyList()
                salePayments.size > 1
            }
            else -> staffFilteredSales
        }
    }

    // Step 4: Filter by Customer Segment
    val finalFilteredSales = remember(paymentFilteredSales, selectedCustomerSegment) {
        when (selectedCustomerSegment) {
            "ALL" -> paymentFilteredSales
            "RETAIL" -> paymentFilteredSales.filter { it.saleType == "RETAIL" }
            "WHOLESALE" -> paymentFilteredSales.filter { it.saleType == "WHOLESALE" }
            "WALK_IN" -> paymentFilteredSales.filter { it.customerId == null || it.customerName.contains("Walk-in", ignoreCase = true) }
            "CREDIT" -> paymentFilteredSales.filter { sale ->
                val pays = paymentsBySaleId[sale.id] ?: emptyList()
                pays.any { it.paymentMethod == "CREDIT" }
            }
            else -> paymentFilteredSales.filter { it.customerName.equals(selectedCustomerSegment, ignoreCase = true) }
        }
    }

    // Sales broken down by product + selling form (packaging unit), respecting all active filters.
    // Answers: how many cartons vs packs vs pieces were sold, with base-unit, revenue, cost and profit.
    val sellingFormBreakdown = remember(finalFilteredSales, allSaleItems) {
        val saleIds = finalFilteredSales.map { it.id }.toSet()
        allSaleItems
            .filter { it.saleId in saleIds }
            .groupBy { it.productName to it.unitName }
            .map { (key, items) ->
                val (productName, unitName) = key
                val revenue = items.sumOf { it.subtotal }
                val cost = items.sumOf { it.costPrice * it.quantity }
                SellingFormBreakdownRow(
                    productName = productName,
                    unitName = unitName,
                    quantitySold = items.sumOf { it.quantity },
                    baseUnitsSold = items.sumOf { it.baseQuantityDeducted },
                    revenue = revenue,
                    cost = cost,
                    profit = revenue - cost
                )
            }
            .sortedWith(compareBy({ it.productName }, { it.unitName }))
    }

    // Filter Expenses by date
    val filteredExpenses = remember(allExpenses, rangeStartEpoch, rangeEndEpoch) {
        allExpenses.filter { it.dateEpoch in rangeStartEpoch..rangeEndEpoch }
    }

    // Filter Returns by date and staff
    val filteredReturns = remember(allReturns, rangeStartEpoch, rangeEndEpoch, selectedStaff) {
        allReturns.filter { ret ->
            val matchesDate = ret.dateEpoch in rangeStartEpoch..rangeEndEpoch
            val matchesStaff = selectedStaff == "ALL" || ret.authorizedBy.equals(selectedStaff, ignoreCase = true)
            matchesDate && matchesStaff
        }
    }

    // Calculation Totals
    val grossSales: Double = remember(finalFilteredSales) { finalFilteredSales.sumOf { it.subtotal } }
    val totalDiscounts: Double = remember(finalFilteredSales) { finalFilteredSales.sumOf { it.discount } }
    val totalReturns: Double = remember(filteredReturns) { filteredReturns.sumOf { it.totalRefundAmount } }
    val netSales: Double = remember(finalFilteredSales, totalReturns) { finalFilteredSales.sumOf { it.total } - totalReturns }
    val totalCogs: Double = remember(finalFilteredSales) { finalFilteredSales.sumOf { it.costTotal } }
    val grossProfit: Double = remember(netSales, totalCogs) { netSales - totalCogs }
    val totalExpenseAmount: Double = remember(filteredExpenses) { filteredExpenses.sumOf { it.amount } }
    val netOperatingProfit: Double = remember(grossProfit, totalExpenseAmount) { grossProfit - totalExpenseAmount }
    val profitMargin: Double = if (netSales > 0.0) (netOperatingProfit / netSales) * 100.0 else 0.0

    val transactionCount: Int = finalFilteredSales.size
    val avgTransactionValue: Double = if (transactionCount > 0) (netSales / transactionCount.toDouble()) else 0.0

    // Component-level payment breakdown for filtered sales (no double counting)
    val paymentBreakdown = remember(finalFilteredSales, paymentsBySaleId) {
        val saleIds = finalFilteredSales.map { it.id }.toSet()
        val relevantPayments = allPayments.filter { it.saleId in saleIds }

        var cash = 0.0
        var mpesa = 0.0
        var credit = 0.0
        var partial = 0.0

        // Calculate each payment component independently
        finalFilteredSales.forEach { sale ->
            val pays = paymentsBySaleId[sale.id] ?: emptyList()
            if (pays.size > 1) {
                partial += sale.total
            }
            pays.forEach { p ->
                when (p.paymentMethod) {
                    "CASH" -> cash += p.amount
                    "MPESA" -> mpesa += p.amount
                    "CREDIT" -> credit += p.amount
                    else -> cash += p.amount
                }
            }
        }
        val collected = cash + mpesa

        val outstandingCredit = allCustomers.sumOf { it.outstandingCredit }

        ReportSummaryData(
            grossSales = grossSales,
            discounts = totalDiscounts,
            returns = totalReturns,
            netSales = netSales,
            costOfGoods = totalCogs,
            grossProfit = grossProfit,
            profitMargin = profitMargin,
            transactionCount = transactionCount,
            averageTransactionValue = avgTransactionValue,
            cashTotal = cash,
            mpesaTotal = mpesa,
            creditTotal = credit,
            partialTotal = partial,
            totalCollected = collected,
            outstandingCredit = outstandingCredit
        )
    }

    // Cashier performance calculation
    val cashierStats = remember(dateFilteredSales, allStaff, business) {
        val names = mutableSetOf<String>()
        business?.let { if (it.ownerName.isNotBlank()) names.add(it.ownerName) }
        allStaff.forEach { if (it.fullName.isNotBlank()) names.add(it.fullName) }
        dateFilteredSales.forEach { if (it.cashierName.isNotBlank()) names.add(it.cashierName) }

        names.map { name ->
            val staffSales = dateFilteredSales.filter { it.cashierName.equals(name, ignoreCase = true) }
            val count = staffSales.size
            val rev = staffSales.sumOf { it.total }
            val cogs = staffSales.sumOf { it.costTotal }
            val disc = staffSales.sumOf { it.discount }
            val profit = rev - cogs
            val avg = if (count > 0) rev / count else 0.0
            Triple(name, Triple(count, rev, profit), Pair(disc, avg))
        }.filter { it.second.first > 0 }
    }

    // Active filters tracking
    val hasActiveFilters = selectedDatePreset != DateFilterPreset.THIS_MONTH ||
            selectedStaff != "ALL" ||
            selectedPaymentMethod != "ALL" ||
            selectedCustomerSegment != "ALL"

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBg)
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
            .testTag("reports_screen"),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Screen Header & Export Buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Reports & Analytics",
                    color = TextWhite,
                    fontWeight = FontWeight.Bold,
                    fontSize = 22.sp
                )
                Text(
                    text = rangeDisplayText,
                    color = RgAccent,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                IconButton(
                    onClick = {
                        val filters = FilterCriteria(
                            dateLabel = rangeDisplayText,
                            staffFilter = selectedStaff,
                            paymentFilter = selectedPaymentMethod,
                            segmentFilter = selectedCustomerSegment
                        )
                        val file = ReportExporter.exportToCsv(
                            context = context,
                            businessName = business?.name ?: "RG POS",
                            reportTitle = "Business Performance Report",
                            filters = filters,
                            summary = paymentBreakdown,
                            sales = finalFilteredSales,
                            payments = allPayments,
                            sellingFormRows = sellingFormBreakdown.map { row ->
                                com.example.util.SellingFormExportRow(
                                    productName = row.productName,
                                    unitName = row.unitName,
                                    quantitySold = row.quantitySold,
                                    baseUnitsSold = row.baseUnitsSold,
                                    revenue = row.revenue,
                                    cost = row.cost,
                                    profit = row.profit
                                )
                            }
                        )
                        ReportExporter.shareReport(context, file)
                    },
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(DarkSurfaceCard)
                        .border(1.dp, DarkBorder, CircleShape)
                        .testTag("export_csv_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Share,
                        contentDescription = "Share Report",
                        tint = RgAccent
                    )
                }
            }
        }

        // Universal Date Preset Chips (Horizontal Scroll)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            DateFilterPreset.values().forEach { preset ->
                val isSelected = selectedDatePreset == preset
                FilterChip(
                    selected = isSelected,
                    onClick = {
                        if (preset == DateFilterPreset.CUSTOM) {
                            showCustomDateDialog = true
                        } else {
                            selectedDatePreset = preset
                        }
                    },
                    label = {
                        Text(
                            text = preset.label,
                            fontSize = 12.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                        )
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = RgAccent,
                        selectedLabelColor = DarkBg,
                        containerColor = DarkSurfaceCard,
                        labelColor = TextWhite
                    ),
                    border = FilterChipDefaults.filterChipBorder(
                        borderColor = if (isSelected) RgAccent else DarkBorder,
                        enabled = true,
                        selected = isSelected
                    ),
                    modifier = Modifier.testTag("date_preset_${preset.name.lowercase(Locale.US)}")
                )
            }
        }

        // Secondary Filters: Staff, Payment Method, Customer Segment
        Card(
            colors = CardDefaults.cardColors(containerColor = DarkSurfaceCard),
            border = androidx.compose.foundation.BorderStroke(1.dp, DarkBorder),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "ADVANCED FILTERS",
                    color = TextMuted,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )

                // Row 1: Staff Filter Selection
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Cashier / Staff:", color = TextWhite, fontSize = 13.sp)
                    var staffExpanded by remember { mutableStateOf(false) }
                    Box {
                        TextButton(onClick = { staffExpanded = true }) {
                            Text(
                                text = if (selectedStaff == "ALL") "All Staff ▼" else "$selectedStaff ▼",
                                color = RgAccent,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                        }
                        DropdownMenu(
                            expanded = staffExpanded,
                            onDismissRequest = { staffExpanded = false },
                            modifier = Modifier.background(DarkSurfaceCard)
                        ) {
                            DropdownMenuItem(
                                text = { Text("All Staff", color = TextWhite) },
                                onClick = {
                                    selectedStaff = "ALL"
                                    staffExpanded = false
                                }
                            )
                            business?.let {
                                if (it.ownerName.isNotBlank()) {
                                    DropdownMenuItem(
                                        text = { Text("${it.ownerName} (Owner)", color = TextWhite) },
                                        onClick = {
                                            selectedStaff = it.ownerName
                                            staffExpanded = false
                                        }
                                    )
                                }
                            }
                            allStaff.forEach { user ->
                                DropdownMenuItem(
                                    text = { Text("${user.fullName} (${user.role})", color = TextWhite) },
                                    onClick = {
                                        selectedStaff = user.fullName
                                        staffExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }

                // Row 2: Payment Method Filter Selection
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Payment Method:", color = TextWhite, fontSize = 13.sp)
                    var payExpanded by remember { mutableStateOf(false) }
                    Box {
                        TextButton(onClick = { payExpanded = true }) {
                            Text(
                                text = when (selectedPaymentMethod) {
                                    "CASH" -> "Cash Only ▼"
                                    "MPESA" -> "M-Pesa Only ▼"
                                    "CREDIT" -> "Credit (Debt) ▼"
                                    "PARTIAL" -> "Partial / Split ▼"
                                    else -> "All Payments ▼"
                                },
                                color = RgAccent,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                        }
                        DropdownMenu(
                            expanded = payExpanded,
                            onDismissRequest = { payExpanded = false },
                            modifier = Modifier.background(DarkSurfaceCard)
                        ) {
                            listOf(
                                "ALL" to "All Payments",
                                "CASH" to "Cash Only",
                                "MPESA" to "M-Pesa Only",
                                "CREDIT" to "Credit (Debts)",
                                "PARTIAL" to "Partial / Split Payments"
                            ).forEach { (code, label) ->
                                DropdownMenuItem(
                                    text = { Text(label, color = TextWhite) },
                                    onClick = {
                                        selectedPaymentMethod = code
                                        payExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }

                // Row 3: Customer Segment Filter Selection
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Customer Segment:", color = TextWhite, fontSize = 13.sp)
                    var segmentExpanded by remember { mutableStateOf(false) }
                    Box {
                        TextButton(onClick = { segmentExpanded = true }) {
                            Text(
                                text = when (selectedCustomerSegment) {
                                    "RETAIL" -> "Retail Customers ▼"
                                    "WHOLESALE" -> "Wholesale Customers ▼"
                                    "WALK_IN" -> "Walk-in Only ▼"
                                    "CREDIT" -> "Credit Buyers ▼"
                                    "ALL" -> "All Customers ▼"
                                    else -> "$selectedCustomerSegment ▼"
                                },
                                color = RgAccent,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                        }
                        DropdownMenu(
                            expanded = segmentExpanded,
                            onDismissRequest = { segmentExpanded = false },
                            modifier = Modifier.background(DarkSurfaceCard)
                        ) {
                            listOf(
                                "ALL" to "All Customers",
                                "RETAIL" to "Retail Only",
                                "WHOLESALE" to "Wholesale Only",
                                "WALK_IN" to "Walk-in Only",
                                "CREDIT" to "Credit Customers"
                            ).forEach { (code, label) ->
                                DropdownMenuItem(
                                    text = { Text(label, color = TextWhite) },
                                    onClick = {
                                        selectedCustomerSegment = code
                                        segmentExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }

                // Active Filter Chips & Clear Filters Action
                if (hasActiveFilters) {
                    HorizontalDivider(color = DarkBorder)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Active Filters:",
                            color = TextMuted,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                        TextButton(
                            onClick = {
                                selectedDatePreset = DateFilterPreset.THIS_MONTH
                                selectedStaff = "ALL"
                                selectedPaymentMethod = "ALL"
                                selectedCustomerSegment = "ALL"
                            },
                            modifier = Modifier.testTag("clear_filters_button")
                        ) {
                            Text("Clear All", color = AlertRed, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        if (selectedDatePreset != DateFilterPreset.THIS_MONTH) {
                            InputChip(
                                selected = true,
                                onClick = { selectedDatePreset = DateFilterPreset.THIS_MONTH },
                                label = { Text(selectedDatePreset.label, fontSize = 11.sp) },
                                trailingIcon = { Icon(Icons.Default.Close, contentDescription = null, Modifier.size(14.dp)) }
                            )
                        }
                        if (selectedStaff != "ALL") {
                            InputChip(
                                selected = true,
                                onClick = { selectedStaff = "ALL" },
                                label = { Text("Staff: $selectedStaff", fontSize = 11.sp) },
                                trailingIcon = { Icon(Icons.Default.Close, contentDescription = null, Modifier.size(14.dp)) }
                            )
                        }
                        if (selectedPaymentMethod != "ALL") {
                            InputChip(
                                selected = true,
                                onClick = { selectedPaymentMethod = "ALL" },
                                label = { Text("Payment: $selectedPaymentMethod", fontSize = 11.sp) },
                                trailingIcon = { Icon(Icons.Default.Close, contentDescription = null, Modifier.size(14.dp)) }
                            )
                        }
                        if (selectedCustomerSegment != "ALL") {
                            InputChip(
                                selected = true,
                                onClick = { selectedCustomerSegment = "ALL" },
                                label = { Text("Segment: $selectedCustomerSegment", fontSize = 11.sp) },
                                trailingIcon = { Icon(Icons.Default.Close, contentDescription = null, Modifier.size(14.dp)) }
                            )
                        }
                    }
                }
            }
        }

        // Primary P&L Summary Card
        Card(
            colors = CardDefaults.cardColors(containerColor = DarkSurfaceCard),
            border = androidx.compose.foundation.BorderStroke(1.dp, RgAccent.copy(alpha = 0.5f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "NET OPERATING PROFIT",
                    color = TextMuted,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
                Text(
                    text = CurrencyFormatter.format(netOperatingProfit),
                    color = if (netOperatingProfit >= 0.0) SuccessGreen else AlertRed,
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Black
                )
                Text(
                    text = "Profit Margin: ${String.format(Locale.US, "%.1f", profitMargin)}% • Transactions: $transactionCount",
                    color = TextWhite,
                    fontSize = 13.sp
                )
            }
        }

        // Core Financial Metrics Grid
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            MetricCard(
                title = "Gross Sales",
                value = CurrencyFormatter.format(grossSales),
                accentColor = RgAccent,
                modifier = Modifier.weight(1f)
            )
            MetricCard(
                title = "Discounts Given",
                value = CurrencyFormatter.format(totalDiscounts),
                accentColor = WarningOrange,
                modifier = Modifier.weight(1f)
            )
        }

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            MetricCard(
                title = "Net Sales Revenue",
                value = CurrencyFormatter.format(netSales),
                accentColor = SuccessGreen,
                modifier = Modifier.weight(1f)
            )
            MetricCard(
                title = "Cost of Goods (COGS)",
                value = CurrencyFormatter.format(totalCogs),
                accentColor = TextMuted,
                modifier = Modifier.weight(1f)
            )
        }

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            MetricCard(
                title = "Gross Profit",
                value = CurrencyFormatter.format(grossProfit),
                accentColor = SuccessGreen,
                modifier = Modifier.weight(1f)
            )
            MetricCard(
                title = "Operating Expenses",
                value = CurrencyFormatter.format(totalExpenseAmount),
                accentColor = CashAmber,
                modifier = Modifier.weight(1f)
            )
        }

        // Payment Method Attribution Breakdown Card
        Card(
            colors = CardDefaults.cardColors(containerColor = DarkSurfaceCard),
            border = androidx.compose.foundation.BorderStroke(1.dp, DarkBorder),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "PAYMENT METHOD ATTRIBUTION",
                    color = TextMuted,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Cash Payments:", color = TextWhite, fontSize = 13.sp)
                    Text(CurrencyFormatter.format(paymentBreakdown.cashTotal), color = CashAmber, fontWeight = FontWeight.Bold)
                }

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("M-Pesa Payments:", color = TextWhite, fontSize = 13.sp)
                    Text(CurrencyFormatter.format(paymentBreakdown.mpesaTotal), color = MpesaGreen, fontWeight = FontWeight.Bold)
                }

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Credit (Debt) Allowed:", color = TextWhite, fontSize = 13.sp)
                    Text(CurrencyFormatter.format(paymentBreakdown.creditTotal), color = CreditBlue, fontWeight = FontWeight.Bold)
                }

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Split / Partial Sales Volume:", color = TextWhite, fontSize = 13.sp)
                    Text(CurrencyFormatter.format(paymentBreakdown.partialTotal), color = WarningOrange, fontWeight = FontWeight.Bold)
                }

                HorizontalDivider(color = DarkBorder)

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Total Collected Cash & M-Pesa:", color = TextWhite, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    Text(CurrencyFormatter.format(paymentBreakdown.totalCollected), color = SuccessGreen, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                }
            }
        }

        // Cashier Performance Table / Cards
        if (cashierStats.isNotEmpty()) {
            Card(
                colors = CardDefaults.cardColors(containerColor = DarkSurfaceCard),
                border = androidx.compose.foundation.BorderStroke(1.dp, DarkBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "STAFF & CASHIER PERFORMANCE",
                        color = TextMuted,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )

                    cashierStats.forEach { (name, stats, extra) ->
                        val (count, rev, profit) = stats
                        val (disc, avg) = extra

                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(DarkSurfaceElevated)
                                .padding(10.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(name, color = TextWhite, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                Text("$count sales", color = RgAccent, fontSize = 12.sp)
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Revenue: ${CurrencyFormatter.format(rev)}", color = TextMuted, fontSize = 12.sp)
                                Text("Profit: ${CurrencyFormatter.format(profit)}", color = SuccessGreen, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Avg Ticket: ${CurrencyFormatter.format(avg)}", color = TextMuted, fontSize = 11.sp)
                                Text("Discounts: ${CurrencyFormatter.format(disc)}", color = WarningOrange, fontSize = 11.sp)
                            }
                        }
                    }
                }
            }
        }

        // Sales by Selling Form (e.g. Carton vs Pack vs Piece), respecting active filters
        if (sellingFormBreakdown.isNotEmpty()) {
            Card(
                colors = CardDefaults.cardColors(containerColor = DarkSurfaceCard),
                border = androidx.compose.foundation.BorderStroke(1.dp, DarkBorder),
                modifier = Modifier.fillMaxWidth().testTag("selling_form_breakdown_card")
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "SALES BY SELLING FORM",
                        color = TextMuted,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                    Text(
                        text = "Quantity and revenue per packaging form sold (e.g. carton vs pack vs piece)",
                        color = TextMuted,
                        fontSize = 11.sp
                    )

                    sellingFormBreakdown.forEach { row ->
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(DarkSurfaceElevated)
                                .padding(10.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(row.productName, color = TextWhite, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                Text(row.unitName, color = RgAccent, fontSize = 12.sp)
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                val qtyText = if (row.quantitySold % 1.0 == 0.0) row.quantitySold.toInt().toString() else "%.1f".format(row.quantitySold)
                                val baseText = if (row.baseUnitsSold % 1.0 == 0.0) row.baseUnitsSold.toInt().toString() else "%.1f".format(row.baseUnitsSold)
                                Text(
                                    "Sold: $qtyText ${row.unitName} (= $baseText base units)",
                                    color = TextMuted,
                                    fontSize = 11.sp
                                )
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Revenue: ${CurrencyFormatter.format(row.revenue)}", color = TextMuted, fontSize = 12.sp)
                                Text("Profit: ${CurrencyFormatter.format(row.profit)}", color = SuccessGreen, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(100.dp))
    }

    // Custom Date Range Dialog
    if (showCustomDateDialog) {
        var startYear by remember { mutableIntStateOf(2026) }
        var startMonth by remember { mutableIntStateOf(9) }
        var startDay by remember { mutableIntStateOf(1) }
        var endYear by remember { mutableIntStateOf(2026) }
        var endMonth by remember { mutableIntStateOf(9) }
        var endDay by remember { mutableIntStateOf(21) }

        Dialog(onDismissRequest = { showCustomDateDialog = false }) {
            Card(
                colors = CardDefaults.cardColors(containerColor = DarkSurfaceCard),
                shape = RoundedCornerShape(16.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, RgAccent),
                modifier = Modifier.fillMaxWidth().padding(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    Text("Select Custom Date Range", color = TextWhite, fontWeight = FontWeight.Bold, fontSize = 18.sp)

                    Text("Start Date (DD/MM/YYYY):", color = TextMuted, fontSize = 12.sp)
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = "$startDay",
                            onValueChange = { startDay = it.toIntOrNull() ?: startDay },
                            label = { Text("Day") },
                            modifier = Modifier.weight(1f),
                            colors = OutlinedTextFieldDefaults.colors(focusedTextColor = TextWhite, unfocusedTextColor = TextWhite)
                        )
                        OutlinedTextField(
                            value = "$startMonth",
                            onValueChange = { startMonth = it.toIntOrNull() ?: startMonth },
                            label = { Text("Month") },
                            modifier = Modifier.weight(1f),
                            colors = OutlinedTextFieldDefaults.colors(focusedTextColor = TextWhite, unfocusedTextColor = TextWhite)
                        )
                        OutlinedTextField(
                            value = "$startYear",
                            onValueChange = { startYear = it.toIntOrNull() ?: startYear },
                            label = { Text("Year") },
                            modifier = Modifier.weight(1.5f),
                            colors = OutlinedTextFieldDefaults.colors(focusedTextColor = TextWhite, unfocusedTextColor = TextWhite)
                        )
                    }

                    Text("End Date (DD/MM/YYYY):", color = TextMuted, fontSize = 12.sp)
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = "$endDay",
                            onValueChange = { endDay = it.toIntOrNull() ?: endDay },
                            label = { Text("Day") },
                            modifier = Modifier.weight(1f),
                            colors = OutlinedTextFieldDefaults.colors(focusedTextColor = TextWhite, unfocusedTextColor = TextWhite)
                        )
                        OutlinedTextField(
                            value = "$endMonth",
                            onValueChange = { endMonth = it.toIntOrNull() ?: endMonth },
                            label = { Text("Month") },
                            modifier = Modifier.weight(1f),
                            colors = OutlinedTextFieldDefaults.colors(focusedTextColor = TextWhite, unfocusedTextColor = TextWhite)
                        )
                        OutlinedTextField(
                            value = "$endYear",
                            onValueChange = { endYear = it.toIntOrNull() ?: endYear },
                            label = { Text("Year") },
                            modifier = Modifier.weight(1.5f),
                            colors = OutlinedTextFieldDefaults.colors(focusedTextColor = TextWhite, unfocusedTextColor = TextWhite)
                        )
                    }

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                        TextButton(onClick = { showCustomDateDialog = false }) {
                            Text("Cancel", color = TextMuted)
                        }
                        FuturisticButton(
                            text = "Apply Range",
                            onClick = {
                                val calStart = Calendar.getInstance().apply {
                                    set(startYear, startMonth - 1, startDay, 0, 0, 0)
                                }
                                val calEnd = Calendar.getInstance().apply {
                                    set(endYear, endMonth - 1, endDay, 23, 59, 59)
                                }
                                customStartDateEpoch = calStart.timeInMillis
                                customEndDateEpoch = calEnd.timeInMillis
                                selectedDatePreset = DateFilterPreset.CUSTOM
                                showCustomDateDialog = false
                            }
                        )
                    }
                }
            }
        }
    }
}
