package com.example.util

import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object CurrencyFormatter {
    private val formatter = NumberFormat.getNumberInstance(Locale.US).apply {
        minimumFractionDigits = 0
        maximumFractionDigits = 2
    }

    fun format(amount: Double, currency: String = "KSh"): String {
        return "$currency ${formatter.format(amount)}"
    }

    fun formatDate(epoch: Long): String {
        val sdf = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault())
        return sdf.format(Date(epoch))
    }

    fun formatDateShort(epoch: Long): String {
        val sdf = SimpleDateFormat("dd MMM", Locale.getDefault())
        return sdf.format(Date(epoch))
    }

    fun formatTime(epoch: Long): String {
        val sdf = SimpleDateFormat("HH:mm", Locale.getDefault())
        return sdf.format(Date(epoch))
    }
}
