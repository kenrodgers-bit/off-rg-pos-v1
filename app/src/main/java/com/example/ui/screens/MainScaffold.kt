package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.*
import androidx.navigation.navArgument
import com.example.ui.navigation.MainTab
import com.example.ui.navigation.Screen
import com.example.ui.theme.*
import com.example.ui.viewmodel.PosViewModel

@Composable
fun MainScaffold(
    viewModel: PosViewModel,
    onLogout: () -> Unit
) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val cartItemCount by viewModel.cartItemCount.collectAsStateWithLifecycle()

    val tabRoutes = remember {
        listOf(
            Screen.Home.route,
            Screen.Pos.route,
            Screen.Inventory.route,
            Screen.Reports.route,
            Screen.More.route
        )
    }
    val isBottomBarVisible = currentRoute in tabRoutes

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBg)
    ) {
        val isTablet = maxWidth >= 720.dp

        if (isTablet) {
            Row(modifier = Modifier.fillMaxSize()) {
                if (isBottomBarVisible) {
                    NavigationRail(
                        containerColor = DarkSurfaceCard,
                        contentColor = TextWhite,
                        modifier = Modifier
                            .fillMaxHeight()
                            .border(1.dp, DarkBorder)
                    ) {
                        Spacer(modifier = Modifier.height(16.dp))
                        MainTab.values().forEach { tab ->
                            val isSelected = currentRoute == tab.route
                            NavigationRailItem(
                                selected = isSelected,
                                onClick = {
                                    if (currentRoute != tab.route) {
                                        navController.navigate(tab.route) {
                                            popUpTo(navController.graph.findStartDestination().id) {
                                                saveState = true
                                            }
                                            launchSingleTop = true
                                            restoreState = true
                                        }
                                    }
                                },
                                icon = {
                                    BadgedBox(
                                        badge = {
                                            if (tab == MainTab.POS && cartItemCount > 0) {
                                                Badge(
                                                    containerColor = RgAccent,
                                                    contentColor = DarkBg
                                                ) {
                                                    Text(
                                                        text = "$cartItemCount",
                                                        fontWeight = FontWeight.Bold,
                                                        fontSize = 10.sp
                                                    )
                                                }
                                            }
                                        }
                                    ) {
                                        Icon(
                                            imageVector = tab.icon,
                                            contentDescription = tab.title,
                                            tint = if (isSelected) RgAccent else TextMuted
                                        )
                                    }
                                },
                                label = {
                                    Text(
                                        text = tab.title,
                                        color = if (isSelected) RgAccent else TextMuted,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                        fontSize = 11.sp
                                    )
                                },
                                colors = NavigationRailItemDefaults.colors(
                                    selectedIconColor = RgAccent,
                                    unselectedIconColor = TextMuted,
                                    indicatorColor = RgAccent.copy(alpha = 0.12f)
                                ),
                                modifier = Modifier.testTag(tab.tag)
                            )
                        }
                    }
                }
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                ) {
                    MainAppNavHost(navController, viewModel, onLogout)
                }
            }
        } else {
            Scaffold(
                containerColor = DarkBg,
                bottomBar = {
                    if (isBottomBarVisible) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .navigationBarsPadding()
                                .padding(horizontal = 16.dp, vertical = 8.dp)
                        ) {
                            Surface(
                                color = DarkSurfaceCard,
                                shape = RoundedCornerShape(20.dp),
                                border = androidx.compose.foundation.BorderStroke(1.dp, DarkBorder),
                                shadowElevation = 8.dp,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                NavigationBar(
                                    containerColor = Color.Transparent,
                                    tonalElevation = 0.dp,
                                    modifier = Modifier.height(64.dp)
                                ) {
                                    MainTab.values().forEach { tab ->
                                        val isSelected = currentRoute == tab.route
                                        NavigationBarItem(
                                            selected = isSelected,
                                            onClick = {
                                                if (currentRoute != tab.route) {
                                                    navController.navigate(tab.route) {
                                                        popUpTo(navController.graph.findStartDestination().id) {
                                                            saveState = true
                                                        }
                                                        launchSingleTop = true
                                                        restoreState = true
                                                    }
                                                }
                                            },
                                            icon = {
                                                BadgedBox(
                                                    badge = {
                                                        if (tab == MainTab.POS && cartItemCount > 0) {
                                                            Badge(
                                                                containerColor = RgAccent,
                                                                contentColor = DarkBg
                                                            ) {
                                                                Text(
                                                                    text = "$cartItemCount",
                                                                    fontWeight = FontWeight.Bold,
                                                                    fontSize = 10.sp
                                                                )
                                                            }
                                                        }
                                                    }
                                                ) {
                                                    Icon(
                                                        imageVector = tab.icon,
                                                        contentDescription = tab.title,
                                                        tint = if (isSelected) RgAccent else TextMuted
                                                    )
                                                }
                                            },
                                            label = {
                                                Text(
                                                    text = tab.title,
                                                    color = if (isSelected) RgAccent else TextMuted,
                                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                                    fontSize = 11.sp
                                                )
                                            },
                                            colors = NavigationBarItemDefaults.colors(
                                                selectedIconColor = RgAccent,
                                                unselectedIconColor = TextMuted,
                                                indicatorColor = RgAccent.copy(alpha = 0.12f)
                                            ),
                                            modifier = Modifier.testTag(tab.tag)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            ) { paddingValues ->
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                ) {
                    MainAppNavHost(navController, viewModel, onLogout)
                }
            }
        }
    }
}

@Composable
private fun MainAppNavHost(
    navController: androidx.navigation.NavHostController,
    viewModel: PosViewModel,
    onLogout: () -> Unit
) {
    NavHost(
        navController = navController,
        startDestination = Screen.Home.route,
        modifier = Modifier.fillMaxSize()
    ) {
            // Tab 1: Dashboard / Home
            composable(Screen.Home.route) {
                HomeScreen(
                    viewModel = viewModel,
                    onNavigateToPos = {
                        navController.navigate(Screen.Pos.route) {
                            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                    onNavigateToAddProduct = {
                        navController.navigate(Screen.AddEditProduct.createRoute(0L))
                    },
                    onNavigateToReceiveStock = {
                        navController.navigate(Screen.Purchases.route)
                    },
                    onNavigateToAddExpense = {
                        navController.navigate(Screen.Expenses.route)
                    },
                    onNavigateToLowStock = {
                        navController.navigate(Screen.Inventory.route) {
                            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                    onNavigateToCustomers = {
                        navController.navigate(Screen.Customers.route)
                    },
                    onViewSaleReceipt = { sale ->
                        navController.navigate(Screen.SaleSuccess.createRoute(sale.id))
                    }
                )
            }

            // Tab 2: POS
            composable(Screen.Pos.route) {
                PosScreen(
                    viewModel = viewModel,
                    onNavigateToCart = {
                        navController.navigate(Screen.Cart.route)
                    },
                    onNavigateToHeldSales = {
                        navController.navigate(Screen.HeldSales.route)
                    }
                )
            }

            // Tab 3: Inventory
            composable(Screen.Inventory.route) {
                InventoryScreen(
                    viewModel = viewModel,
                    onNavigateToAddProduct = { productId ->
                        navController.navigate(Screen.AddEditProduct.createRoute(productId))
                    },
                    onNavigateToStockTake = {
                        navController.navigate(Screen.StockTake.route)
                    }
                )
            }

            // Tab 4: Reports
            composable(Screen.Reports.route) {
                ReportsScreen(
                    viewModel = viewModel
                )
            }

            // Tab 5: More (Operations Hub)
            composable(Screen.More.route) {
                MoreScreen(
                    onNavigate = { route ->
                        when (route) {
                            "customers" -> navController.navigate(Screen.Customers.route)
                            "purchases" -> navController.navigate(Screen.Purchases.route)
                            "suppliers" -> navController.navigate(Screen.Suppliers.route)
                            "expenses" -> navController.navigate(Screen.Expenses.route)
                            "cash_shift" -> navController.navigate(Screen.CashShift.route)
                            "mpesa_recon" -> navController.navigate(Screen.MpesaRecon.route)
                            "returns" -> navController.navigate(Screen.Returns.route)
                            "staff" -> navController.navigate(Screen.Staff.route)
                            "audit_log" -> navController.navigate(Screen.AuditLog.route)
                            "settings" -> navController.navigate(Screen.Settings.route)
                            "backup" -> navController.navigate(Screen.BackupRestore.route)
                            else -> {}
                        }
                    },
                    onLogout = onLogout
                )
            }

            // POS Sub-Flow: Cart
            composable(Screen.Cart.route) {
                CartScreen(
                    viewModel = viewModel,
                    onNavigateBack = { navController.popBackStack() },
                    onProceedToPayment = { navController.navigate(Screen.Payment.route) }
                )
            }

            // POS Sub-Flow: Payment
            composable(Screen.Payment.route) {
                PaymentScreen(
                    viewModel = viewModel,
                    onNavigateBack = { navController.popBackStack() },
                    onSaleCompleted = { saleId ->
                        navController.navigate(Screen.SaleSuccess.createRoute(saleId)) {
                            popUpTo(Screen.Pos.route) { inclusive = false }
                        }
                    }
                )
            }

            // POS Sub-Flow: Sale Receipt Success
            composable(
                route = Screen.SaleSuccess.route,
                arguments = listOf(navArgument("saleId") { type = NavType.LongType })
            ) { backStackEntry ->
                val saleId = backStackEntry.arguments?.getLong("saleId") ?: 0L
                SaleSuccessScreen(
                    saleId = saleId,
                    viewModel = viewModel,
                    onStartNewSale = {
                        navController.navigate(Screen.Pos.route) {
                            popUpTo(Screen.Pos.route) { inclusive = true }
                        }
                    }
                )
            }

            // POS Sub-Flow: Held Sales
            composable(Screen.HeldSales.route) {
                HeldSalesScreen(
                    viewModel = viewModel,
                    onNavigateBack = { navController.popBackStack() },
                    onResumeSale = {
                        navController.navigate(Screen.Cart.route) {
                            popUpTo(Screen.Pos.route) { inclusive = false }
                        }
                    }
                )
            }

            // Inventory Sub-Flow: Add/Edit Product
            composable(
                route = Screen.AddEditProduct.route,
                arguments = listOf(navArgument("productId") { type = NavType.LongType })
            ) { backStackEntry ->
                val productId = backStackEntry.arguments?.getLong("productId") ?: 0L
                AddEditProductScreen(
                    productId = productId,
                    viewModel = viewModel,
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            // Inventory Sub-Flow: Stock Take
            composable(Screen.StockTake.route) {
                StockTakeScreen(
                    viewModel = viewModel,
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            // More: Customers
            composable(Screen.Customers.route) {
                CustomersScreen(
                    viewModel = viewModel,
                    onNavigateBack = { navController.popBackStack() },
                    onSelectCustomerCredit = { custId ->
                        navController.navigate(Screen.CustomerCredit.createRoute(custId))
                    }
                )
            }

            // More: Customer Credit Ledger
            composable(
                route = Screen.CustomerCredit.route,
                arguments = listOf(navArgument("customerId") { type = NavType.LongType })
            ) { backStackEntry ->
                val customerId = backStackEntry.arguments?.getLong("customerId") ?: 0L
                CustomerCreditScreen(
                    customerId = customerId,
                    viewModel = viewModel,
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            // More: Purchases (Stock In)
            composable(Screen.Purchases.route) {
                PurchasesScreen(
                    viewModel = viewModel,
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            // More: Suppliers
            composable(Screen.Suppliers.route) {
                SuppliersScreen(
                    viewModel = viewModel,
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            // More: Operating Expenses
            composable(Screen.Expenses.route) {
                ExpensesScreen(
                    viewModel = viewModel,
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            // More: Cash Shift / Drawer
            composable(Screen.CashShift.route) {
                CashShiftScreen(
                    viewModel = viewModel,
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            // More: M-Pesa Recon
            composable(Screen.MpesaRecon.route) {
                MpesaReconScreen(
                    viewModel = viewModel,
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            // More: Returns & Refunds
            composable(Screen.Returns.route) {
                ReturnsScreen(
                    viewModel = viewModel,
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            // More: Staff Management
            composable(Screen.Staff.route) {
                StaffScreen(
                    viewModel = viewModel,
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            // More: Audit Log
            composable(Screen.AuditLog.route) {
                AuditLogScreen(
                    viewModel = viewModel,
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            // More: Business Settings
            composable(Screen.Settings.route) {
                SettingsScreen(
                    viewModel = viewModel,
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            // More: Local Backup & Restore
            composable(Screen.BackupRestore.route) {
                BackupRestoreScreen(
                    viewModel = viewModel,
                    onNavigateBack = { navController.popBackStack() }
                )
            }
        }
    }
