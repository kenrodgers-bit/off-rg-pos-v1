package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
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
import com.example.ui.navigation.FloatingPillNavBar
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

    // Map current route to active primary tab (or secondary screens under More)
    val activeTab = remember(currentRoute) {
        when (currentRoute) {
            Screen.Home.route -> MainTab.HOME
            Screen.Pos.route -> MainTab.POS
            Screen.Inventory.route -> MainTab.INVENTORY
            Screen.More.route,
            Screen.Reports.route,
            Screen.BackupRestore.route,
            Screen.Purchases.route,
            Screen.Customers.route,
            Screen.CustomerCredit.route,
            Screen.Suppliers.route,
            Screen.Expenses.route,
            Screen.CashShift.route,
            Screen.MpesaRecon.route,
            Screen.Returns.route,
            Screen.Staff.route,
            Screen.AuditLog.route,
            Screen.Settings.route,
            Screen.StockTake.route -> MainTab.MORE
            else -> null
        }
    }

    val isPillNavVisible = activeTab != null

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBg)
    ) {
        // Main Screen Content
        Box(modifier = Modifier.fillMaxSize()) {
            MainAppNavHost(navController, viewModel, onLogout)
        }

        // Floating Pill Navigation Bar (floats consistently above bottom edge on all devices)
        if (isPillNavVisible) {
            FloatingPillNavBar(
                activeTab = activeTab,
                cartBadgeCount = cartItemCount,
                onTabSelected = { tab ->
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
                modifier = Modifier.align(Alignment.BottomCenter)
            )
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
                    },
                    onNavigateToImport = {
                        navController.navigate(Screen.InventoryImport.route)
                    }
                )
            }

            composable(Screen.InventoryImport.route) {
                InventoryImportScreen(
                    viewModel = viewModel,
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            // Tab 4: Reports
            composable(Screen.Reports.route) {
                ReportsScreen(
                    viewModel = viewModel
                )
            }

            // More (Operations Hub)
            composable(Screen.More.route) {
                MoreScreen(
                    viewModel = viewModel,
                    onNavigate = { route ->
                        when (route) {
                            "reports" -> navController.navigate(Screen.Reports.route)
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
                            "stock_take" -> navController.navigate(Screen.StockTake.route)
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
