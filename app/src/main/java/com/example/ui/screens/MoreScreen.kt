package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.theme.*
import com.example.ui.viewmodel.PosViewModel

data class MoreHubItem(
    val title: String,
    val subtitle: String,
    val icon: ImageVector,
    val accentColor: Color,
    val route: String,
    val testTag: String,
    val isEnabled: Boolean = true
)

data class MoreCategorySection(
    val categoryName: String,
    val categoryIcon: ImageVector,
    val items: List<MoreHubItem>
)

@Composable
fun MoreScreen(
    viewModel: PosViewModel? = null,
    onNavigate: (String) -> Unit,
    onLogout: () -> Unit
) {
    val business by viewModel?.business?.collectAsStateWithLifecycle() ?: remember { mutableStateOf(null) }
    val currentUser by viewModel?.currentUser?.collectAsStateWithLifecycle() ?: remember { mutableStateOf("Staff") }
    val isOnline by viewModel?.isOnline?.collectAsStateWithLifecycle() ?: remember { mutableStateOf(true) }

    var searchQuery by remember { mutableStateOf("") }

    val categories = remember(business) {
        listOf(
            MoreCategorySection(
                categoryName = "Management",
                categoryIcon = Icons.Default.TrendingUp,
                items = listOf(
                    MoreHubItem(
                        title = "Reports & Analytics",
                        subtitle = "Sales, profit margins, tax, payment breakdown & PDF export",
                        icon = Icons.Default.BarChart,
                        accentColor = CreditBlue,
                        route = "reports",
                        testTag = "menu_reports"
                    ),
                    MoreHubItem(
                        title = "Purchases (Stock In)",
                        subtitle = "Receive supplier orders, purchase costs & vendor invoices",
                        icon = Icons.Default.LocalShipping,
                        accentColor = SuccessGreen,
                        route = "purchases",
                        testTag = "menu_purchases"
                    ),
                    MoreHubItem(
                        title = "Customers & Credit",
                        subtitle = if (business?.allowCredit == false) "Credit disabled in store settings" else "Customer profiles, debt ledger & credit limits",
                        icon = Icons.Default.People,
                        accentColor = WarningOrange,
                        route = "customers",
                        testTag = "menu_customers",
                        isEnabled = business?.allowCredit != false
                    ),
                    MoreHubItem(
                        title = "Suppliers Directory",
                        subtitle = "Vendor contacts, order records & payment tracking",
                        icon = Icons.Default.Storefront,
                        accentColor = CashAmber,
                        route = "suppliers",
                        testTag = "menu_suppliers"
                    ),
                    MoreHubItem(
                        title = "Operating Expenses",
                        subtitle = "Daily shop expenditure, utilities, wages & custom categories",
                        icon = Icons.Default.Payments,
                        accentColor = AlertRed,
                        route = "expenses",
                        testTag = "menu_expenses"
                    ),
                    MoreHubItem(
                        title = "Cash Shift Management",
                        subtitle = "Drawer opening float, cash drops & shift reconciliation",
                        icon = Icons.Default.PointOfSale,
                        accentColor = RgAccent,
                        route = "cash_shift",
                        testTag = "menu_cash_shift"
                    )
                )
            ),
            MoreCategorySection(
                categoryName = "Business",
                categoryIcon = Icons.Default.Store,
                items = listOf(
                    MoreHubItem(
                        title = "Staff & Access Control",
                        subtitle = "Cashier accounts, manager roles & security PIN controls",
                        icon = Icons.Default.Badge,
                        accentColor = RgAccent,
                        route = "staff",
                        testTag = "menu_staff"
                    ),
                    MoreHubItem(
                        title = "Store Profile & Receipts",
                        subtitle = "Business name, tax PIN, till number & receipt customize",
                        icon = Icons.Default.Store,
                        accentColor = CreditBlue,
                        route = "settings",
                        testTag = "menu_settings"
                    ),
                    MoreHubItem(
                        title = "M-Pesa Reconciliation",
                        subtitle = "Verify till codes, customer mobile numbers & transactions",
                        icon = Icons.Default.PhoneAndroid,
                        accentColor = MpesaGreen,
                        route = "mpesa_recon",
                        testTag = "menu_mpesa_recon"
                    ),
                    MoreHubItem(
                        title = "Returns & Restock",
                        subtitle = "Customer return orders, damaged goods & inventory restock",
                        icon = Icons.Default.AssignmentReturn,
                        accentColor = AlertRed,
                        route = "returns",
                        testTag = "menu_returns"
                    )
                )
            ),
            MoreCategorySection(
                categoryName = "Tools",
                categoryIcon = Icons.Default.Build,
                items = listOf(
                    MoreHubItem(
                        title = "Backup & Restore",
                        subtitle = "Encrypted local SQLite backup, auto-backup schedule & import",
                        icon = Icons.Default.CloudSync,
                        accentColor = CreditBlue,
                        route = "backup",
                        testTag = "menu_backup"
                    ),
                    MoreHubItem(
                        title = "Stock Take & Physical Count",
                        subtitle = "Physical inventory audit, discrepancy check & auto-adjustment",
                        icon = Icons.Default.FactCheck,
                        accentColor = WarningOrange,
                        route = "stock_take",
                        testTag = "menu_stock_take"
                    ),
                    MoreHubItem(
                        title = "System Audit Log",
                        subtitle = "Immutable timeline of all sales, edits, stock and staff actions",
                        icon = Icons.Default.History,
                        accentColor = TextMuted,
                        route = "audit_log",
                        testTag = "menu_audit_log"
                    )
                )
            ),
            MoreCategorySection(
                categoryName = "Settings & Security",
                categoryIcon = Icons.Default.Tune,
                items = listOf(
                    MoreHubItem(
                        title = "POS & System Settings",
                        subtitle = "Costing method (Weighted/LIFO), tax rates & defaults",
                        icon = Icons.Default.Settings,
                        accentColor = TextWhite,
                        route = "settings",
                        testTag = "menu_pos_settings"
                    ),
                    MoreHubItem(
                        title = "Lock Terminal / Switch Staff",
                        subtitle = "Lock screen & switch cashier account or PIN",
                        icon = Icons.Default.Lock,
                        accentColor = AlertRed,
                        route = "logout",
                        testTag = "menu_logout"
                    )
                )
            )
        )
    }

    val filteredCategories = remember(categories, searchQuery) {
        if (searchQuery.isBlank()) {
            categories
        } else {
            categories.mapNotNull { category ->
                val matchingItems = category.items.filter { item ->
                    item.title.contains(searchQuery, ignoreCase = true) ||
                            item.subtitle.contains(searchQuery, ignoreCase = true) ||
                            category.categoryName.contains(searchQuery, ignoreCase = true)
                }
                if (matchingItems.isNotEmpty()) {
                    category.copy(items = matchingItems)
                } else null
            }
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBg)
            .padding(horizontal = 16.dp)
            .testTag("more_screen"),
        contentPadding = PaddingValues(top = 16.dp, bottom = 100.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Hub Header & Active Profile Card
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = DarkSurfaceCard),
                border = androidx.compose.foundation.BorderStroke(1.dp, DarkBorder),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = business?.name ?: "RG POS Store",
                                color = TextWhite,
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(if (isOnline) SuccessGreen.copy(alpha = 0.15f) else WarningOrange.copy(alpha = 0.15f))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = if (isOnline) "ONLINE" else "OFFLINE",
                                    color = if (isOnline) SuccessGreen else WarningOrange,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Logged in: $currentUser • ${business?.currency ?: "KES"}",
                            color = TextMuted,
                            fontSize = 12.sp
                        )
                    }

                    OutlinedButton(
                        onClick = onLogout,
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = AlertRed),
                        border = androidx.compose.foundation.BorderStroke(1.dp, AlertRed.copy(alpha = 0.4f)),
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                        modifier = Modifier.testTag("more_quick_lock_btn")
                    ) {
                        Icon(Icons.Default.Lock, contentDescription = "Lock", modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Lock", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }

        // Quick Search Bar
        item {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Search modules, settings & tools...", color = TextMuted, fontSize = 13.sp) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = TextMuted) },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(Icons.Default.Close, contentDescription = "Clear", tint = TextMuted, modifier = Modifier.size(16.dp))
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = DarkSurfaceCard,
                    unfocusedContainerColor = DarkSurfaceCard,
                    focusedBorderColor = RgAccent,
                    unfocusedBorderColor = DarkBorder,
                    focusedTextColor = TextWhite,
                    unfocusedTextColor = TextWhite
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("more_search_field")
            )
        }

        // Categorized Sections
        itemsIndexed(filteredCategories) { _, category ->
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 4.dp)
                ) {
                    Icon(
                        imageVector = category.categoryIcon,
                        contentDescription = null,
                        tint = RgAccent,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = category.categoryName.uppercase(),
                        color = RgAccent,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        letterSpacing = 1.sp
                    )
                }

                Card(
                    colors = CardDefaults.cardColors(containerColor = DarkSurfaceCard),
                    border = androidx.compose.foundation.BorderStroke(1.dp, DarkBorder),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column {
                        category.items.forEachIndexed { index, item ->
                            val isClickable = item.isEnabled
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable(enabled = isClickable) {
                                        if (item.route == "logout") {
                                            onLogout()
                                        } else {
                                            onNavigate(item.route)
                                        }
                                    }
                                    .padding(horizontal = 14.dp, vertical = 12.dp)
                                    .testTag(item.testTag),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(38.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(item.accentColor.copy(alpha = if (isClickable) 0.14f else 0.06f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = item.icon,
                                        contentDescription = null,
                                        tint = if (isClickable) item.accentColor else TextMuted,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }

                                Spacer(modifier = Modifier.width(12.dp))

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = item.title,
                                        color = if (isClickable) TextWhite else TextMuted,
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 14.sp
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = item.subtitle,
                                        color = TextMuted,
                                        fontSize = 11.5.sp,
                                        lineHeight = 15.sp
                                    )
                                }

                                Spacer(modifier = Modifier.width(8.dp))

                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos,
                                    contentDescription = null,
                                    tint = if (isClickable) TextMuted.copy(alpha = 0.6f) else Color.Transparent,
                                    modifier = Modifier.size(13.dp)
                                )
                            }

                            if (index < category.items.lastIndex) {
                                HorizontalDivider(
                                    modifier = Modifier.padding(horizontal = 14.dp),
                                    color = DarkBorder.copy(alpha = 0.5f),
                                    thickness = 0.75.dp
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
