package com.example.util

import android.content.Context
import android.content.Intent
import com.example.data.model.Business
import com.example.data.model.Payment
import com.example.data.model.Sale
import com.example.data.model.SaleItem

object ReceiptHelper {

    fun generateReceiptText(
        business: Business?,
        sale: Sale,
        items: List<SaleItem>,
        payments: List<Payment>
    ): String {
        val bizName = business?.name?.ifBlank { "RG POS STORE" } ?: "RG POS STORE"
        val phone = business?.phone?.ifBlank { "" } ?: ""
        val location = business?.location?.ifBlank { "" } ?: ""

        val sb = StringBuilder()
        sb.appendLine("================================")
        sb.appendLine("           RG POS")
        sb.appendLine("          by RGDev")
        sb.appendLine("================================")
        sb.appendLine(bizName.uppercase())
        if (phone.isNotBlank()) sb.appendLine("Tel: $phone")
        if (location.isNotBlank()) sb.appendLine("Location: $location")
        sb.appendLine("--------------------------------")
        sb.appendLine("Receipt: #${sale.receiptNumber}")
        sb.appendLine("Date: ${CurrencyFormatter.formatDate(sale.dateEpoch)}")
        sb.appendLine("Cashier: ${sale.cashierName}")
        sb.appendLine("Customer: ${sale.customerName}")
        sb.appendLine("Sale Type: ${sale.saleType}")
        sb.appendLine("--------------------------------")
        sb.appendLine("ITEMS:")

        for (item in items) {
            sb.appendLine(item.productName)
            val qtyStr = if (item.quantity % 1.0 == 0.0) item.quantity.toInt().toString() else "%.2f".format(item.quantity)
            sb.appendLine("  $qtyStr ${item.unitName} × ${CurrencyFormatter.format(item.unitPrice)} = ${CurrencyFormatter.format(item.subtotal)}")
        }

        sb.appendLine("--------------------------------")
        if (sale.discount > 0) {
            sb.appendLine("Subtotal: ${CurrencyFormatter.format(sale.subtotal)}")
            sb.appendLine("Discount: -${CurrencyFormatter.format(sale.discount)}")
        }
        if (sale.tax > 0) {
            sb.appendLine("Tax: ${CurrencyFormatter.format(sale.tax)}")
        }
        sb.appendLine("TOTAL: ${CurrencyFormatter.format(sale.total)}")
        sb.appendLine("--------------------------------")
        sb.appendLine("PAYMENT BREAKDOWN:")
        for (p in payments) {
            val ref = if (p.mpesaRef.isNotBlank()) " (Ref: ${p.mpesaRef})" else ""
            sb.appendLine("  ${p.paymentMethod}: ${CurrencyFormatter.format(p.amount)}$ref")
        }
        sb.appendLine("--------------------------------")
        sb.appendLine(business?.receiptHeader ?: "Thank you for shopping with us.")
        if (business?.receiptFooter?.isNotBlank() == true) {
            sb.appendLine(business.receiptFooter)
        }
        sb.appendLine("================================")
        return sb.toString()
    }

    fun shareReceipt(context: Context, receiptText: String) {
        val sendIntent = Intent().apply {
            action = Intent.ACTION_SEND
            putExtra(Intent.EXTRA_TEXT, receiptText)
            type = "text/plain"
        }
        val shareIntent = Intent.createChooser(sendIntent, "Share Receipt via")
        context.startActivity(shareIntent)
    }
}
