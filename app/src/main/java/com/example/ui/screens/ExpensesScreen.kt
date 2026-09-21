package com.example.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.Expense
import com.example.ui.components.EmptyStateView
import com.example.ui.components.FuturisticButton
import com.example.ui.components.MetricCard
import com.example.ui.theme.*
import com.example.ui.viewmodel.PosViewModel
import com.example.util.CurrencyFormatter
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExpensesScreen(
    viewModel: PosViewModel,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val expenses by viewModel.allExpenses.collectAsStateWithLifecycle()
    val currentUser by viewModel.currentUser.collectAsStateWithLifecycle()

    var showAddExpenseDialog by remember { mutableStateOf(false) }
    val totalExpenseAmount = remember(expenses) { expenses.sumOf { it.amount } }

    Scaffold(
        containerColor = DarkBg,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Business Expenses",
                        color = TextWhite,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = TextWhite
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { showAddExpenseDialog = true }) {
                        Icon(Icons.Default.Add, contentDescription = "Add Expense", tint = RgAccent)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = DarkBg)
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddExpenseDialog = true },
                containerColor = RgAccent,
                contentColor = DarkBg,
                shape = CircleShape,
                modifier = Modifier.testTag("add_expense_fab")
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Expense")
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp)
                .testTag("expenses_screen"),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            MetricCard(
                title = "TOTAL EXPENSES RECORDED",
                value = CurrencyFormatter.format(totalExpenseAmount),
                accentColor = CashAmber,
                icon = Icons.Default.Payments,
                modifier = Modifier.fillMaxWidth()
            )

            if (expenses.isEmpty()) {
                EmptyStateView(
                    icon = Icons.Default.Receipt,
                    title = "No expenses recorded",
                    description = "Track store operational expenses like rent, delivery, bags, and meals.",
                    actionButtonText = "+ Add Expense",
                    onActionClick = { showAddExpenseDialog = true }
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(bottom = 96.dp)
                ) {
                    items(expenses) { expense ->
                        Card(
                            colors = CardDefaults.cardColors(containerColor = DarkSurfaceCard),
                            border = androidx.compose.foundation.BorderStroke(1.dp, DarkBorder),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(expense.category, color = TextWhite, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                    Text(expense.description, color = TextMuted, fontSize = 12.sp)
                                    Text(
                                        "${CurrencyFormatter.formatDate(expense.dateEpoch)} • By ${expense.staffMember}",
                                        color = TextSubtle,
                                        fontSize = 11.sp
                                    )
                                }

                                Text(
                                    CurrencyFormatter.format(expense.amount),
                                    color = CashAmber,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp
                                )
                            }
                        }
                    }
                }
            }
        }

        if (showAddExpenseDialog) {
            AddExpenseDialog(
                staffName = currentUser,
                onAdd = { category, amount, description ->
                    coroutineScope.launch {
                        viewModel.repository.addExpense(
                            Expense(category = category, amount = amount, description = description, staffMember = currentUser)
                        )
                        showAddExpenseDialog = false
                        Toast.makeText(context, "Expense recorded", Toast.LENGTH_SHORT).show()
                    }
                },
                onDismiss = { showAddExpenseDialog = false }
            )
        }
    }
}

@Composable
fun AddExpenseDialog(
    staffName: String,
    onAdd: (category: String, amount: Double, description: String) -> Unit,
    onDismiss: () -> Unit
) {
    val categories = listOf("Rent", "Electricity / Water", "Transport & Delivery", "Staff Meals", "Packaging & Bags", "Maintenance", "Other")
    var selectedCategory by remember { mutableStateOf(categories.first()) }
    var amountText by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = DarkSurfaceCard),
            border = androidx.compose.foundation.BorderStroke(1.dp, DarkBorder)
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Record Business Expense", color = TextWhite, fontWeight = FontWeight.Bold, fontSize = 16.sp)

                Text("Category:", color = TextMuted, fontSize = 12.sp)
                LazyColumn(modifier = Modifier.heightIn(max = 120.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    items(categories) { cat ->
                        val isSel = cat == selectedCategory
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(6.dp))
                                .background(if (isSel) DarkSurfaceElevated else DarkBg)
                                .border(1.dp, if (isSel) RgAccent else DarkBorder, RoundedCornerShape(6.dp))
                                .clickable { selectedCategory = cat }
                                .padding(8.dp)
                        ) {
                            Text(cat, color = if (isSel) RgAccent else TextWhite, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }

                OutlinedTextField(
                    value = amountText,
                    onValueChange = { amountText = it },
                    label = { Text("Expense Amount (KSh)") },
                    modifier = Modifier.fillMaxWidth().testTag("expense_amount_input"),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = RgAccent,
                        unfocusedBorderColor = DarkBorder,
                        focusedTextColor = TextWhite,
                        unfocusedTextColor = TextWhite
                    ),
                    singleLine = true
                )

                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Description / Reason") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = RgAccent,
                        unfocusedBorderColor = DarkBorder,
                        focusedTextColor = TextWhite,
                        unfocusedTextColor = TextWhite
                    ),
                    singleLine = true
                )

                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    FuturisticButton(text = "Cancel", onClick = onDismiss, isSecondary = true, modifier = Modifier.weight(1f))
                    FuturisticButton(
                        text = "Save Expense",
                        enabled = (amountText.toDoubleOrNull() ?: 0.0) > 0,
                        onClick = { onAdd(selectedCategory, amountText.toDoubleOrNull() ?: 0.0, description) },
                        modifier = Modifier.weight(1.3f),
                        testTag = "save_expense_button"
                    )
                }
            }
        }
    }
}
