package com.example.ui.screens

import android.widget.Toast
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.components.FuturisticButton
import com.example.ui.theme.*
import com.example.ui.viewmodel.PosViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: PosViewModel,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val business by viewModel.business.collectAsStateWithLifecycle()

    var name by remember(business) { mutableStateOf(business?.name ?: "") }
    var phone by remember(business) { mutableStateOf(business?.phone ?: "") }
    var address by remember(business) { mutableStateOf(business?.address ?: "") }
    var mpesaTill by remember(business) { mutableStateOf(business?.mpesaTill ?: "") }
    var kraPin by remember(business) { mutableStateOf(business?.kraPin ?: "") }
    var receiptFooter by remember(business) { mutableStateOf(business?.receiptFooter ?: "Asante kwa kununua nasi! Karibu tena.") }
    var costingMethod by remember(business) { mutableStateOf(business?.costingMethod ?: "WEIGHTED_AVERAGE") }
    var isSaving by remember { mutableStateOf(false) }

    Scaffold(
        containerColor = DarkBg,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Store Settings",
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
                .widthIn(max = 900.dp)
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState())
                .testTag("settings_screen"),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text(
                text = "Business Profile & Receipts",
                color = TextMuted,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            )

            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Business / Store Name") },
                modifier = Modifier.fillMaxWidth().testTag("business_name_input"),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = RgAccent,
                    unfocusedBorderColor = DarkBorder,
                    focusedTextColor = TextWhite,
                    unfocusedTextColor = TextWhite
                ),
                singleLine = true
            )

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = phone,
                    onValueChange = { phone = it },
                    label = { Text("Phone Number") },
                    modifier = Modifier.weight(1f),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = RgAccent,
                        unfocusedBorderColor = DarkBorder,
                        focusedTextColor = TextWhite,
                        unfocusedTextColor = TextWhite
                    ),
                    singleLine = true
                )
                OutlinedTextField(
                    value = mpesaTill,
                    onValueChange = { mpesaTill = it },
                    label = { Text("M-Pesa Till / Paybill") },
                    modifier = Modifier.weight(1f),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = RgAccent,
                        unfocusedBorderColor = DarkBorder,
                        focusedTextColor = TextWhite,
                        unfocusedTextColor = TextWhite
                    ),
                    singleLine = true
                )
            }

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = address,
                    onValueChange = { address = it },
                    label = { Text("Physical Location") },
                    modifier = Modifier.weight(1f),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = RgAccent,
                        unfocusedBorderColor = DarkBorder,
                        focusedTextColor = TextWhite,
                        unfocusedTextColor = TextWhite
                    ),
                    singleLine = true
                )
                OutlinedTextField(
                    value = kraPin,
                    onValueChange = { kraPin = it },
                    label = { Text("KRA PIN (Optional)") },
                    modifier = Modifier.weight(1f),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = RgAccent,
                        unfocusedBorderColor = DarkBorder,
                        focusedTextColor = TextWhite,
                        unfocusedTextColor = TextWhite
                    ),
                    singleLine = true
                )
            }

            OutlinedTextField(
                value = receiptFooter,
                onValueChange = { receiptFooter = it },
                label = { Text("Receipt Footer Note") },
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = RgAccent,
                    unfocusedBorderColor = DarkBorder,
                    focusedTextColor = TextWhite,
                    unfocusedTextColor = TextWhite
                ),
                singleLine = true
            )

            Text(
                text = "Inventory Costing",
                color = TextMuted,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            )
            Text(
                text = "How the cost per unit is recalculated when new stock is received. Used for profit and margin reports.",
                color = TextMuted,
                fontSize = 11.sp
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf(
                    "WEIGHTED_AVERAGE" to "Weighted Average",
                    "LAST_PURCHASE_COST" to "Last Purchase Cost"
                ).forEach { (value, label) ->
                    FilterChip(
                        selected = costingMethod == value,
                        onClick = { costingMethod = value },
                        label = { Text(label, fontSize = 12.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = RgAccent,
                            selectedLabelColor = DarkBg
                        ),
                        modifier = Modifier.testTag("costing_method_${value}")
                    )
                }
            }

            FuturisticButton(
                text = if (isSaving) "Saving..." else "Save Changes",
                icon = Icons.Default.Save,
                enabled = !isSaving && name.isNotBlank(),
                onClick = {
                    isSaving = true
                    coroutineScope.launch {
                        if (business != null) {
                            val updated = business!!.copy(
                                name = name,
                                phone = phone,
                                address = address,
                                mpesaTill = mpesaTill,
                                kraPin = kraPin,
                                receiptFooter = receiptFooter,
                                costingMethod = costingMethod
                            )
                            viewModel.repository.saveBusiness(updated)
                            isSaving = false
                            Toast.makeText(context, "Settings updated successfully", Toast.LENGTH_SHORT).show()
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth().testTag("save_settings_button")
            )

            HorizontalDivider(color = DarkDivider)

            // App details
            Card(
                colors = CardDefaults.cardColors(containerColor = DarkSurfaceCard),
                border = androidx.compose.foundation.BorderStroke(1.dp, DarkBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("RG POS", color = TextWhite, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    Text("Sell. Track. Grow. Offline. • by RGDev", color = RgAccent, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                    Text("Database: SQLite + Room • Full Offline Mode Active", color = TextMuted, fontSize = 11.sp)
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}
