package com.example.ui.screens

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Business
import com.example.ui.components.FuturisticButton
import com.example.ui.components.NumericPinKeypad
import com.example.ui.theme.*
import com.example.ui.viewmodel.PosViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SetupScreen(
    viewModel: PosViewModel,
    onSetupComplete: () -> Unit,
    onNavigateToRestore: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var currentStep by remember { mutableIntStateOf(1) }

    // Step 2 fields
    var businessName by remember { mutableStateOf("Mama Sarah Wholesalers & Retail") }
    var businessPhone by remember { mutableStateOf("0722 123 456") }
    var businessLocation by remember { mutableStateOf("Nairobi, Kenya") }
    var ownerName by remember { mutableStateOf("Sarah Muthoni") }
    var currency by remember { mutableStateOf("KES — Kenyan Shilling") }

    // Step 3 fields
    var retailSelling by remember { mutableStateOf(true) }
    var wholesaleSelling by remember { mutableStateOf(true) }
    var allowCredit by remember { mutableStateOf(true) }
    var lowStockThreshold by remember { mutableStateOf("10") }
    var taxRate by remember { mutableStateOf("0") }
    var receiptHeader by remember { mutableStateOf("Thank you for shopping with us!") }
    var receiptFooter by remember { mutableStateOf("Goods once sold are only returnable with receipt within 7 days.") }

    // Step 5 fields
    var pin by remember { mutableStateOf("") }
    var confirmPin by remember { mutableStateOf("") }
    var isConfirmingPin by remember { mutableStateOf(false) }

    Scaffold(
        containerColor = DarkBg,
        topBar = {
            if (currentStep > 1) {
                TopAppBar(
                    title = {
                        Text(
                            text = "Step $currentStep of 5",
                            fontSize = 14.sp,
                            color = TextMuted
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = {
                            if (isConfirmingPin) {
                                isConfirmingPin = false
                                confirmPin = ""
                            } else {
                                currentStep--
                            }
                        }) {
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
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .testTag("setup_screen")
        ) {
            when (currentStep) {
                1 -> {
                    // STEP 1: Welcome Screen
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp)
                            .verticalScroll(rememberScrollState()),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(80.dp)
                                .clip(RoundedCornerShape(20.dp))
                                .background(DarkSurfaceElevated)
                                .border(1.5.dp, RgAccent, RoundedCornerShape(20.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Storefront,
                                contentDescription = null,
                                tint = RgAccent,
                                modifier = Modifier.size(44.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(28.dp))

                        Text(
                            text = "Welcome to RG POS",
                            color = TextWhite,
                            fontSize = 28.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.testTag("welcome_title")
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        Text(
                            text = "Your business. Your data. Your POS.\nEven offline.",
                            color = TextMuted,
                            fontSize = 16.sp,
                            lineHeight = 24.sp,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(48.dp))

                        FuturisticButton(
                            text = "Get Started",
                            onClick = { currentStep = 2 },
                            modifier = Modifier.fillMaxWidth(),
                            testTag = "get_started_button"
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        FuturisticButton(
                            text = "Restore Backup",
                            onClick = onNavigateToRestore,
                            isSecondary = true,
                            modifier = Modifier.fillMaxWidth(),
                            testTag = "restore_backup_button"
                        )
                    }
                }

                2 -> {
                    // STEP 2: Business Information
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp)
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Text(
                            text = "Business Information",
                            color = TextWhite,
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Configure your store details for receipts and management.",
                            color = TextMuted,
                            fontSize = 13.sp
                        )

                        OutlinedTextField(
                            value = businessName,
                            onValueChange = { businessName = it },
                            label = { Text("Business Name") },
                            modifier = Modifier.fillMaxWidth().testTag("business_name_input"),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = RgAccent,
                                unfocusedBorderColor = DarkBorder,
                                focusedTextColor = TextWhite,
                                unfocusedTextColor = TextWhite
                            ),
                            singleLine = true
                        )

                        OutlinedTextField(
                            value = businessPhone,
                            onValueChange = { businessPhone = it },
                            label = { Text("Business Phone") },
                            modifier = Modifier.fillMaxWidth().testTag("business_phone_input"),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = RgAccent,
                                unfocusedBorderColor = DarkBorder,
                                focusedTextColor = TextWhite,
                                unfocusedTextColor = TextWhite
                            ),
                            singleLine = true
                        )

                        OutlinedTextField(
                            value = businessLocation,
                            onValueChange = { businessLocation = it },
                            label = { Text("Business Location") },
                            modifier = Modifier.fillMaxWidth().testTag("business_location_input"),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = RgAccent,
                                unfocusedBorderColor = DarkBorder,
                                focusedTextColor = TextWhite,
                                unfocusedTextColor = TextWhite
                            ),
                            singleLine = true
                        )

                        OutlinedTextField(
                            value = ownerName,
                            onValueChange = { ownerName = it },
                            label = { Text("Owner Name") },
                            modifier = Modifier.fillMaxWidth().testTag("owner_name_input"),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = RgAccent,
                                unfocusedBorderColor = DarkBorder,
                                focusedTextColor = TextWhite,
                                unfocusedTextColor = TextWhite
                            ),
                            singleLine = true
                        )

                        OutlinedTextField(
                            value = currency,
                            onValueChange = { currency = it },
                            label = { Text("Currency") },
                            modifier = Modifier.fillMaxWidth(),
                            enabled = false,
                            colors = OutlinedTextFieldDefaults.colors(
                                disabledBorderColor = DarkBorder,
                                disabledTextColor = TextWhite,
                                disabledLabelColor = TextMuted
                            )
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        FuturisticButton(
                            text = "Continue",
                            onClick = {
                                if (businessName.isBlank()) {
                                    Toast.makeText(context, "Please enter your business name", Toast.LENGTH_SHORT).show()
                                } else {
                                    currentStep = 3
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            testTag = "step2_continue_button"
                        )
                    }
                }

                3 -> {
                    // STEP 3: Business Preferences
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp)
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Text(
                            text = "Business Preferences",
                            color = TextWhite,
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Customize sales channels, credit and receipt formats.",
                            color = TextMuted,
                            fontSize = 13.sp
                        )

                        Card(
                            colors = CardDefaults.cardColors(containerColor = DarkSurfaceCard),
                            border = androidx.compose.foundation.BorderStroke(1.dp, DarkBorder)
                        ) {
                            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text("Retail Selling", color = TextWhite, fontWeight = FontWeight.SemiBold)
                                        Text("Sell in pieces and packs to walk-ins", color = TextMuted, fontSize = 12.sp)
                                    }
                                    Switch(
                                        checked = retailSelling,
                                        onCheckedChange = { retailSelling = it },
                                        colors = SwitchDefaults.colors(checkedThumbColor = DarkBg, checkedTrackColor = RgAccent)
                                    )
                                }

                                HorizontalDivider(color = DarkDivider)

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text("Wholesale Selling", color = TextWhite, fontWeight = FontWeight.SemiBold)
                                        Text("Bulk price tier (cartons, dozens, bales)", color = TextMuted, fontSize = 12.sp)
                                    }
                                    Switch(
                                        checked = wholesaleSelling,
                                        onCheckedChange = { wholesaleSelling = it },
                                        colors = SwitchDefaults.colors(checkedThumbColor = DarkBg, checkedTrackColor = RgAccent)
                                    )
                                }

                                HorizontalDivider(color = DarkDivider)

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text("Allow Credit Sales", color = TextWhite, fontWeight = FontWeight.SemiBold)
                                        Text("Permit trusted customers to buy on credit", color = TextMuted, fontSize = 12.sp)
                                    }
                                    Switch(
                                        checked = allowCredit,
                                        onCheckedChange = { allowCredit = it },
                                        colors = SwitchDefaults.colors(checkedThumbColor = DarkBg, checkedTrackColor = RgAccent)
                                    )
                                }
                            }
                        }

                        OutlinedTextField(
                            value = lowStockThreshold,
                            onValueChange = { lowStockThreshold = it.filter { ch -> ch.isDigit() } },
                            label = { Text("Low-stock Alert Threshold") },
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = RgAccent,
                                unfocusedBorderColor = DarkBorder,
                                focusedTextColor = TextWhite,
                                unfocusedTextColor = TextWhite
                            ),
                            singleLine = true
                        )

                        OutlinedTextField(
                            value = taxRate,
                            onValueChange = { taxRate = it },
                            label = { Text("Tax Rate % (0 for none or 16 for VAT)") },
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = RgAccent,
                                unfocusedBorderColor = DarkBorder,
                                focusedTextColor = TextWhite,
                                unfocusedTextColor = TextWhite
                            ),
                            singleLine = true
                        )

                        OutlinedTextField(
                            value = receiptHeader,
                            onValueChange = { receiptHeader = it },
                            label = { Text("Receipt Header Note") },
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = RgAccent,
                                unfocusedBorderColor = DarkBorder,
                                focusedTextColor = TextWhite,
                                unfocusedTextColor = TextWhite
                            )
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        FuturisticButton(
                            text = "Continue",
                            onClick = { currentStep = 4 },
                            modifier = Modifier.fillMaxWidth(),
                            testTag = "step3_continue_button"
                        )
                    }
                }

                4 -> {
                    // STEP 4: Payment Methods
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp)
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Text(
                            text = "Payment Methods",
                            color = TextWhite,
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "RG POS supports local Kenyan payment methods built directly into checkout.",
                            color = TextMuted,
                            fontSize = 13.sp
                        )

                        Card(
                            colors = CardDefaults.cardColors(containerColor = DarkSurfaceCard),
                            border = androidx.compose.foundation.BorderStroke(1.dp, DarkBorder)
                        ) {
                            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Icon(imageVector = Icons.Default.CheckCircle, contentDescription = null, tint = RgAccent)
                                    Column {
                                        Text("Cash", color = TextWhite, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                        Text("Calculate change automatically with shift tracking", color = TextMuted, fontSize = 12.sp)
                                    }
                                }
                                HorizontalDivider(color = DarkDivider)
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Icon(imageVector = Icons.Default.CheckCircle, contentDescription = null, tint = RgAccent)
                                    Column {
                                        Text("M-Pesa", color = TextWhite, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                        Text("Direct reference code logging and end-of-day reconciliation", color = TextMuted, fontSize = 12.sp)
                                    }
                                }
                                HorizontalDivider(color = DarkDivider)
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Icon(imageVector = Icons.Default.CheckCircle, contentDescription = null, tint = RgAccent)
                                    Column {
                                        Text("Partial Payment (Split)", color = TextWhite, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                        Text("Split single checkout across Cash + M-Pesa + Credit balance", color = TextMuted, fontSize = 12.sp)
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(24.dp))

                        FuturisticButton(
                            text = "Continue",
                            onClick = { currentStep = 5 },
                            modifier = Modifier.fillMaxWidth(),
                            testTag = "step4_continue_button"
                        )
                    }
                }

                5 -> {
                    // STEP 5: Create Owner PIN
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = if (!isConfirmingPin) "Create Owner PIN" else "Confirm Owner PIN",
                                color = TextWhite,
                                fontSize = 22.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = if (!isConfirmingPin) "Enter a 4-digit PIN to secure your POS" else "Re-enter the 4-digit PIN",
                                color = TextMuted,
                                fontSize = 13.sp
                            )
                            Spacer(modifier = Modifier.height(24.dp))

                            // PIN dots display
                            val activePin = if (!isConfirmingPin) pin else confirmPin
                            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                                for (i in 0 until 4) {
                                    val isFilled = i < activePin.length
                                    Box(
                                        modifier = Modifier
                                            .size(20.dp)
                                            .clip(CircleShape)
                                            .background(if (isFilled) RgAccent else DarkSurfaceElevated)
                                            .border(1.5.dp, if (isFilled) RgAccent else DarkBorder, CircleShape)
                                    )
                                }
                            }
                        }

                        NumericPinKeypad(
                            onDigitClick = { digit ->
                                if (!isConfirmingPin) {
                                    if (pin.length < 4) pin += digit
                                    if (pin.length == 4) {
                                        isConfirmingPin = true
                                    }
                                } else {
                                    if (confirmPin.length < 4) confirmPin += digit
                                }
                            },
                            onDeleteClick = {
                                if (!isConfirmingPin) {
                                    if (pin.isNotEmpty()) pin = pin.dropLast(1)
                                } else {
                                    if (confirmPin.isNotEmpty()) {
                                        confirmPin = confirmPin.dropLast(1)
                                    } else {
                                        isConfirmingPin = false
                                    }
                                }
                            }
                        )

                        FuturisticButton(
                            text = "Finish Setup",
                            enabled = pin.length == 4 && confirmPin.length == 4,
                            onClick = {
                                if (pin != confirmPin) {
                                    Toast.makeText(context, "PINs do not match. Please try again.", Toast.LENGTH_SHORT).show()
                                    confirmPin = ""
                                    isConfirmingPin = false
                                } else {
                                    coroutineScope.launch {
                                        val newBiz = Business(
                                            name = businessName,
                                            phone = businessPhone,
                                            location = businessLocation,
                                            ownerName = ownerName,
                                            currency = "KES",
                                            retailEnabled = retailSelling,
                                            wholesaleEnabled = wholesaleSelling,
                                            allowCredit = allowCredit,
                                            lowStockThreshold = lowStockThreshold.toIntOrNull() ?: 10,
                                            taxRatePercent = taxRate.toDoubleOrNull() ?: 0.0,
                                            receiptHeader = receiptHeader,
                                            receiptFooter = receiptFooter,
                                            ownerPin = pin,
                                            isConfigured = true
                                        )
                                        viewModel.repository.saveBusiness(newBiz)
                                        viewModel.setCurrentUser(ownerName.ifBlank { "Owner" })
                                        Toast.makeText(context, "Setup Complete! Welcome to RG POS", Toast.LENGTH_SHORT).show()
                                        onSetupComplete()
                                    }
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            testTag = "finish_setup_button"
                        )
                    }
                }
            }
        }
    }
}
