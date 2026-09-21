package com.example.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.PointOfSale
import androidx.compose.ui.graphics.vector.ImageVector

sealed class Screen(val route: String) {
    // Root Authentication Flow
    object Splash : Screen("splash")
    object Setup : Screen("setup")
    object Login : Screen("login")
    object MainHost : Screen("main_host")

    // Main Tabs (Bottom Navigation)
    object Home : Screen("home")
    object Pos : Screen("pos")
    object Inventory : Screen("inventory")
    object Reports : Screen("reports")
    object More : Screen("more")

    // Sub-screens
    object Cart : Screen("cart")
    object Payment : Screen("payment")
    object SaleSuccess : Screen("sale_success/{saleId}") {
        fun createRoute(saleId: Long) = "sale_success/$saleId"
    }
    object HeldSales : Screen("held_sales")
    object AddEditProduct : Screen("add_edit_product/{productId}") {
        fun createRoute(productId: Long = 0L) = "add_edit_product/$productId"
    }
    object StockTake : Screen("stock_take")
    object Purchases : Screen("purchases")
    object Customers : Screen("customers")
    object CustomerCredit : Screen("customer_credit/{customerId}") {
        fun createRoute(customerId: Long) = "customer_credit/$customerId"
    }
    object Suppliers : Screen("suppliers")
    object Expenses : Screen("expenses")
    object CashShift : Screen("cash_shift")
    object MpesaRecon : Screen("mpesa_recon")
    object Returns : Screen("returns")
    object Staff : Screen("staff")
    object AuditLog : Screen("audit_log")
    object Settings : Screen("settings")
    object BackupRestore : Screen("backup_restore")
}

enum class MainTab(val title: String, val icon: ImageVector, val route: String, val tag: String) {
    HOME("Dashboard", Icons.Default.Dashboard, Screen.Home.route, "nav_home"),
    POS("POS", Icons.Default.PointOfSale, Screen.Pos.route, "nav_pos"),
    INVENTORY("Inventory", Icons.Default.Inventory2, Screen.Inventory.route, "nav_inventory"),
    REPORTS("Reports", Icons.Default.BarChart, Screen.Reports.route, "nav_reports"),
    MORE("More", Icons.Default.Menu, Screen.More.route, "nav_more")
}
