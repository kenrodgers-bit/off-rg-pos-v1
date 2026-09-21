package com.example.util

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import com.example.data.model.Payment
import com.example.data.model.Sale
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class FilterCriteria(
    val dateLabel: String,
    val staffFilter: String,
    val paymentFilter: String,
    val segmentFilter: String
)

// One row per product + selling form (packaging unit) sold, for the "Sales by Selling
// Form" report section. Mirrors ui.screens.SellingFormBreakdownRow but kept dependency-free
// here so ReportExporter doesn't need to import the UI layer.
data class SellingFormExportRow(
    val productName: String,
    val unitName: String,
    val quantitySold: Double,
    val baseUnitsSold: Double,
    val revenue: Double,
    val cost: Double,
    val profit: Double
)

data class ReportSummaryData(
    val grossSales: Double,
    val discounts: Double,
    val returns: Double,
    val netSales: Double,
    val costOfGoods: Double,
    val grossProfit: Double,
    val profitMargin: Double,
    val transactionCount: Int,
    val averageTransactionValue: Double,
    val cashTotal: Double,
    val mpesaTotal: Double,
    val creditTotal: Double,
    val partialTotal: Double,
    val totalCollected: Double,
    val outstandingCredit: Double
)

object ReportExporter {

    fun exportToCsv(
        context: Context,
        businessName: String,
        reportTitle: String,
        filters: FilterCriteria,
        summary: ReportSummaryData,
        sales: List<Sale>,
        payments: List<Payment>,
        sellingFormRows: List<SellingFormExportRow> = emptyList()
    ): File {
        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        val filename = "RG_POS_Report_${timestamp}.csv"
        val file = File(context.cacheDir, filename)

        val sb = StringBuilder()
        sb.append("=== ${businessName.uppercase(Locale.US)} - $reportTitle ===\n")
        sb.append("Generated On,${SimpleDateFormat("dd MMM yyyy HH:mm:ss", Locale.US).format(Date())}\n")
        sb.append("Date Range,${filters.dateLabel}\n")
        sb.append("Staff Filter,${filters.staffFilter}\n")
        sb.append("Payment Method Filter,${filters.paymentFilter}\n")
        sb.append("Customer Segment Filter,${filters.segmentFilter}\n\n")

        sb.append("--- EXECUTIVE SUMMARY ---\n")
        sb.append("Metric,Amount (KSh)\n")
        sb.append("Gross Sales,${summary.grossSales}\n")
        sb.append("Discounts,${summary.discounts}\n")
        sb.append("Returns / Refunds,${summary.returns}\n")
        sb.append("Net Sales,${summary.netSales}\n")
        sb.append("Cost of Goods Sold (COGS),${summary.costOfGoods}\n")
        sb.append("Gross Profit,${summary.grossProfit}\n")
        sb.append("Profit Margin %,${String.format(Locale.US, "%.2f", summary.profitMargin)}%\n")
        sb.append("Transaction Count,${summary.transactionCount}\n")
        sb.append("Average Transaction Value,${summary.averageTransactionValue}\n\n")

        sb.append("--- PAYMENT METHOD BREAKDOWN ---\n")
        sb.append("Payment Method,Total (KSh)\n")
        sb.append("Cash,${summary.cashTotal}\n")
        sb.append("M-Pesa,${summary.mpesaTotal}\n")
        sb.append("Credit,${summary.creditTotal}\n")
        sb.append("Partial Transactions,${summary.partialTotal}\n")
        sb.append("Total Collected,${summary.totalCollected}\n\n")

        sb.append("--- TRANSACTION DETAILS ---\n")
        sb.append("Receipt #,Date,Cashier,Customer,Sale Type,Subtotal,Discount,Total,Status\n")
        val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US)
        sales.forEach { s ->
            sb.append("\"${s.receiptNumber}\",")
            sb.append("\"${dateFormat.format(Date(s.dateEpoch))}\",")
            sb.append("\"${s.cashierName}\",")
            sb.append("\"${s.customerName}\",")
            sb.append("\"${s.saleType}\",")
            sb.append("${s.subtotal},")
            sb.append("${s.discount},")
            sb.append("${s.total},")
            sb.append("\"${s.status}\"\n")
        }

        if (sellingFormRows.isNotEmpty()) {
            sb.append("\n--- SALES BY SELLING FORM ---\n")
            sb.append("Product,Selling Form,Quantity Sold,Base Units Sold,Revenue (KSh),Cost (KSh),Profit (KSh)\n")
            sellingFormRows.forEach { row ->
                sb.append("\"${row.productName}\",")
                sb.append("\"${row.unitName}\",")
                sb.append("${row.quantitySold},")
                sb.append("${row.baseUnitsSold},")
                sb.append("${row.revenue},")
                sb.append("${row.cost},")
                sb.append("${row.profit}\n")
            }
        }

        file.writeText(sb.toString(), Charsets.UTF_8)
        return file
    }

    fun shareReport(context: Context, file: File, mimeType: String = "text/csv") {
        try {
            val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = mimeType
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_SUBJECT, "RG POS Report: ${file.name}")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(shareIntent, "Export & Share Report"))
        } catch (e: Exception) {
            // Fallback to text intent if FileProvider is not yet declared
            val textIntent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, file.readText())
                putExtra(Intent.EXTRA_SUBJECT, "RG POS Report: ${file.name}")
            }
            context.startActivity(Intent.createChooser(textIntent, "Export & Share Report"))
        }
    }
}
