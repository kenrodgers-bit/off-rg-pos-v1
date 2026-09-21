package com.example.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import com.example.data.model.CashSession
import com.example.ui.components.FuturisticButton
import com.example.ui.components.MetricCard
import com.example.ui.components.StatusBadge
import com.example.ui.theme.*
import com.example.ui.viewmodel.PosViewModel
import com.example.util.CurrencyFormatter
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CashShiftScreen(
    viewModel: PosViewModel,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val activeSession by viewModel.activeCashSession.collectAsStateWithLifecycle()
    val allSessions by viewModel.allCashSessions.collectAsStateWithLifecycle()
    val currentUser by viewModel.currentUser.collectAsStateWithLifecycle()

    var openingCashText by remember { mutableStateOf("2000") }
    var showCloseShiftDialog by remember { mutableStateOf(false) }

    Scaffold(
        containerColor = DarkBg,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Cash Drawer & Shifts",
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
                colors = TopAppBarDefaults.topAppBarColors(containerColor = DarkBg)
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState())
                .testTag("cash_shift_screen"),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Active Shift Card or Open Shift Section
            if (activeSession == null) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = DarkSurfaceCard),
                    border = androidx.compose.foundation.BorderStroke(1.dp, DarkBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(Icons.Default.LockClock, contentDescription = null, tint = WarningOrange)
                            Text("No Open Shift", color = TextWhite, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        }

                        Text("Open a shift with your starting cash drawer float to track cash accurately.", color = TextMuted, fontSize = 13.sp)

                        OutlinedTextField(
                            value = openingCashText,
                            onValueChange = { openingCashText = it },
                            label = { Text("Opening Cash Float (KSh)") },
                            modifier = Modifier.fillMaxWidth().testTag("opening_float_input"),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = RgAccent,
                                unfocusedBorderColor = DarkBorder,
                                focusedTextColor = TextWhite,
                                unfocusedTextColor = TextWhite
                            ),
                            singleLine = true
                        )

                        FuturisticButton(
                            text = "Open Cash Shift",
                            icon = Icons.Default.LockOpen,
                            onClick = {
                                coroutineScope.launch {
                                    viewModel.repository.openCashSession(
                                        cashierName = currentUser,
                                        openingCash = openingCashText.toDoubleOrNull() ?: 0.0
                                    )
                                    Toast.makeText(context, "Shift opened successfully", Toast.LENGTH_SHORT).show()
                                }
                            },
                            modifier = Modifier.fillMaxWidth().testTag("open_shift_button")
                        )
                    }
                }
            } else {
                val session = activeSession!!
                Card(
                    colors = CardDefaults.cardColors(containerColor = DarkSurfaceCard),
                    border = androidx.compose.foundation.BorderStroke(1.dp, RgAccent.copy(alpha = 0.5f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text("ACTIVE SHIFT", color = RgAccent, fontWeight = FontWeight.Bold, fontSize = 12.sp, letterSpacing = 1.sp)
                                Text("Cashier: ${session.cashierName}", color = TextWhite, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                            }
                            StatusBadge(text = "SHIFT OPEN", color = SuccessGreen)
                        }

                        HorizontalDivider(color = DarkDivider)

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            MetricCard(title = "Opening Float", value = CurrencyFormatter.format(session.openingCash), modifier = Modifier.weight(1f))
                            MetricCard(title = "Cash Sales", value = CurrencyFormatter.format(session.cashSales), accentColor = SuccessGreen, modifier = Modifier.weight(1f))
                        }

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            MetricCard(title = "Cash Expenses", value = "-${CurrencyFormatter.format(session.cashExpenses)}", accentColor = CashAmber, modifier = Modifier.weight(1f))
                            MetricCard(title = "Expected in Till", value = CurrencyFormatter.format(session.expectedCash), accentColor = RgAccent, modifier = Modifier.weight(1f))
                        }

                        Spacer(modifier = Modifier.height(4.dp))

                        FuturisticButton(
                            text = "Close Cash Shift",
                            icon = Icons.Default.Lock,
                            onClick = { showCloseShiftDialog = true },
                            isSecondary = true,
                            modifier = Modifier.fillMaxWidth().testTag("close_shift_button")
                        )
                    }
                }
            }

            Text("PAST SHIFTS HISTORY", color = TextMuted, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)

            val closedSessions = allSessions.filter { it.status == "CLOSED" }
            if (closedSessions.isEmpty()) {
                Text("No past closed shifts.", color = TextSubtle, fontSize = 13.sp)
            } else {
                for (s in closedSessions.take(10)) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = DarkSurfaceCard),
                        border = androidx.compose.foundation.BorderStroke(1.dp, DarkBorder),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text("${s.cashierName} • Shift", color = TextWhite, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                                Text("Closed: ${CurrencyFormatter.formatDate(s.closedEpoch ?: s.openedEpoch)}", color = TextMuted, fontSize = 11.sp)
                                Text("Expected: ${CurrencyFormatter.format(s.expectedCash)} • Counted: ${CurrencyFormatter.format(s.actualCash)}", color = TextSubtle, fontSize = 11.sp)
                            }

                            Column(horizontalAlignment = Alignment.End) {
                                val diff = s.difference
                                val diffColor = if (diff < 0) AlertRed else if (diff > 0) SuccessGreen else TextWhite
                                val diffText = if (diff == 0.0) "Balanced (KSh 0)" else if (diff > 0) "+${CurrencyFormatter.format(diff)}" else CurrencyFormatter.format(diff)
                                Text(diffText, color = diffColor, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            }
                        }
                    }
                }
            }
        }

        // Close Shift Modal Dialog (Part 27)
        if (showCloseShiftDialog && activeSession != null) {
            CloseShiftModal(
                session = activeSession!!,
                onConfirm = { actualCash ->
                    coroutineScope.launch {
                        viewModel.repository.closeCashSession(activeSession!!.id, actualCash)
                        showCloseShiftDialog = false
                        Toast.makeText(context, "Shift closed and reconciled", Toast.LENGTH_SHORT).show()
                    }
                },
                onDismiss = { showCloseShiftDialog = false }
            )
        }
    }
}

@Composable
fun CloseShiftModal(
    session: CashSession,
    onConfirm: (actualCash: Double) -> Unit,
    onDismiss: () -> Unit
) {
    var actualCashText by remember { mutableStateOf(session.expectedCash.toInt().toString()) }
    val actual = actualCashText.toDoubleOrNull() ?: 0.0
    val variance = actual - session.expectedCash

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = DarkSurfaceCard),
            border = androidx.compose.foundation.BorderStroke(1.dp, DarkBorder)
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("End of Shift Cash Reconciliation", color = TextWhite, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Text("Count all the physical cash in the till drawer.", color = TextMuted, fontSize = 12.sp)

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Expected Cash in Till:", color = TextMuted, fontSize = 13.sp)
                    Text(CurrencyFormatter.format(session.expectedCash), color = TextWhite, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                }

                OutlinedTextField(
                    value = actualCashText,
                    onValueChange = { actualCashText = it },
                    label = { Text("Actual Cash Counted (KSh)") },
                    modifier = Modifier.fillMaxWidth().testTag("actual_cash_counted_input"),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = RgAccent,
                        unfocusedBorderColor = DarkBorder,
                        focusedTextColor = TextWhite,
                        unfocusedTextColor = TextWhite
                    ),
                    singleLine = true
                )

                // Live Variance indicator
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("VARIANCE / DIFFERENCE:", color = TextMuted, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    val color = if (variance < 0) AlertRed else if (variance > 0) SuccessGreen else TextWhite
                    val text = if (variance == 0.0) "Balanced (KSh 0)" else if (variance > 0) "+${CurrencyFormatter.format(variance)}" else CurrencyFormatter.format(variance)
                    Text(text, color = color, fontSize = 16.sp, fontWeight = FontWeight.Black)
                }

                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    FuturisticButton(text = "Cancel", onClick = onDismiss, isSecondary = true, modifier = Modifier.weight(1f))
                    FuturisticButton(
                        text = "Confirm & Close",
                        enabled = actualCashText.isNotBlank(),
                        onClick = { onConfirm(actual) },
                        modifier = Modifier.weight(1.3f),
                        testTag = "confirm_close_shift_button"
                    )
                }
            }
        }
    }
}
