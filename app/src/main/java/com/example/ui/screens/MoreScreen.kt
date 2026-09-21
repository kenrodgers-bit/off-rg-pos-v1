package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*

data class MoreMenuItem(
    val title: String,
    val subtitle: String,
    val icon: ImageVector,
    val accentColor: Color,
    val route: String,
    val testTag: String
)

@Composable
fun MoreScreen(
    onNavigate: (String) -> Unit,
    onLogout: () -> Unit
) {
    val items = listOf(
        MoreMenuItem("Customers & Credit", "Ledger, debts & limits", Icons.Default.People, WarningOrange, "customers", "menu_customers"),
        MoreMenuItem("Purchases (Stock In)", "Receive goods & invoices", Icons.Default.LocalShipping, SuccessGreen, "purchases", "menu_purchases"),
        MoreMenuItem("Suppliers", "Vendors & orders", Icons.Default.Storefront, CreditBlue, "suppliers", "menu_suppliers"),
        MoreMenuItem("Expenses", "Daily shop expenditure", Icons.Default.Payments, CashAmber, "expenses", "menu_expenses"),
        MoreMenuItem("Cash Shift", "Drawer float & reconciliation", Icons.Default.PointOfSale, RgAccent, "cash_shift", "menu_cash_shift"),
        MoreMenuItem("M-Pesa Recon", "Verify till reference codes", Icons.Default.PhoneAndroid, MpesaGreen, "mpesa_recon", "menu_mpesa_recon"),
        MoreMenuItem("Returns & Refunds", "Customer returns & restock", Icons.Default.AssignmentReturn, AlertRed, "returns", "menu_returns"),
        MoreMenuItem("Staff & Access", "Cashiers & PIN control", Icons.Default.Badge, RgAccent, "staff", "menu_staff"),
        MoreMenuItem("System Audit Log", "Immutable actions history", Icons.Default.History, TextMuted, "audit_log", "menu_audit_log"),
        MoreMenuItem("Store Settings", "Business profile & receipts", Icons.Default.Settings, TextWhite, "settings", "menu_settings"),
        MoreMenuItem("Backup & Data", "Local SQLite backups", Icons.Default.CloudSync, CreditBlue, "backup", "menu_backup"),
        MoreMenuItem("Lock / Switch Staff", "Switch user PIN", Icons.Default.Lock, AlertRed, "logout", "menu_logout")
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBg)
            .widthIn(max = 900.dp)
            .padding(horizontal = 16.dp, vertical = 12.dp)
            .testTag("more_screen"),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Column {
            Text(
                text = "Operations & Management",
                color = TextWhite,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Full offline-first business suite",
                color = TextMuted,
                fontSize = 12.sp
            )
        }

        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            contentPadding = PaddingValues(bottom = 90.dp)
        ) {
            items(items) { item ->
                Card(
                    colors = CardDefaults.cardColors(containerColor = DarkSurfaceCard),
                    border = androidx.compose.foundation.BorderStroke(1.dp, DarkBorder),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .clickable {
                            if (item.route == "logout") {
                                onLogout()
                            } else {
                                onNavigate(item.route)
                            }
                        }
                        .testTag(item.testTag)
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(item.accentColor.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = item.icon,
                                contentDescription = null,
                                tint = item.accentColor,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        Text(
                            text = item.title,
                            color = TextWhite,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )

                        Text(
                            text = item.subtitle,
                            color = TextMuted,
                            fontSize = 11.sp,
                            lineHeight = 14.sp
                        )
                    }
                }
            }
        }
    }
}
