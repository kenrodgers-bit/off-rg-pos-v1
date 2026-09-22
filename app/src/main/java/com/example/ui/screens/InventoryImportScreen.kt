package com.example.ui.screens

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.ImportBatch
import com.example.ui.components.FuturisticButton
import com.example.ui.theme.*
import com.example.ui.viewmodel.PosViewModel
import com.example.util.InventoryImportExport
import com.example.util.ReportExporter
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InventoryImportScreen(
    viewModel: PosViewModel,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var selectedFileName by remember { mutableStateOf<String?>(null) }
    var preview by remember { mutableStateOf<InventoryImportExport.Preview?>(null) }
    var isProcessing by remember { mutableStateOf(false) }
    var lastResult by remember { mutableStateOf<ImportBatch?>(null) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var expandedFilter by remember { mutableStateOf<InventoryImportExport.Severity?>(null) }

    val importHistory by viewModel.repository.importHistory.collectAsStateWithLifecycle(initialValue = emptyList())

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri == null) return@rememberLauncherForActivityResult
        isProcessing = true
        errorMessage = null
        lastResult = null
        coroutineScope.launch {
            try {
                val text = context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
                if (text.isNullOrBlank()) {
                    errorMessage = "Couldn't read that file, or it's empty."
                } else {
                    val name = uri.lastPathSegment?.substringAfterLast('/') ?: "import.csv"
                    selectedFileName = name
                    val products = viewModel.repository.getAllProductsSnapshot()
                    val conversions = viewModel.repository.getAllUnitConversionsSnapshot()
                    preview = InventoryImportExport.parseAndValidate(text, name, products, conversions)
                }
            } catch (e: Exception) {
                errorMessage = "Couldn't read that file: ${e.message}"
            } finally {
                isProcessing = false
            }
        }
    }

    Scaffold(
        containerColor = DarkBg,
        topBar = {
            TopAppBar(
                title = { Text("Spreadsheet Import / Export", color = TextWhite, fontWeight = FontWeight.Bold, fontSize = 16.sp) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = TextWhite)
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
                .testTag("inventory_import_screen"),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Spacer(modifier = Modifier.height(4.dp))

            // ---- Template & export ----
            Card(
                colors = CardDefaults.cardColors(containerColor = DarkSurfaceCard),
                border = androidx.compose.foundation.BorderStroke(1.dp, DarkBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("1. Get a spreadsheet", color = TextWhite, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    Text(
                        "Download a template to fill in, or export your current inventory to edit and re-import. Opens in Excel, Google Sheets, or Numbers.",
                        color = TextMuted,
                        fontSize = 12.sp
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                        FuturisticButton(
                            text = "Blank Template",
                            isSecondary = true,
                            icon = Icons.Default.Description,
                            modifier = Modifier.weight(1f),
                            testTag = "download_blank_template_button",
                            onClick = {
                                val file = File(context.cacheDir, "RG_POS_Import_Template.csv")
                                file.writeText(InventoryImportExport.generateTemplate(includeSampleData = false))
                                ReportExporter.shareReport(context, file)
                            }
                        )
                        FuturisticButton(
                            text = "With Examples",
                            isSecondary = true,
                            icon = Icons.Default.Lightbulb,
                            modifier = Modifier.weight(1f),
                            testTag = "download_sample_template_button",
                            onClick = {
                                val file = File(context.cacheDir, "RG_POS_Import_Template_Sample.csv")
                                file.writeText(InventoryImportExport.generateTemplate(includeSampleData = true))
                                ReportExporter.shareReport(context, file)
                            }
                        )
                    }
                    FuturisticButton(
                        text = "Export Current Stock",
                        icon = Icons.Default.CloudDownload,
                        testTag = "export_current_stock_button",
                        onClick = {
                            coroutineScope.launch {
                                val products = viewModel.repository.getAllProductsSnapshot()
                                val conversions = viewModel.repository.getAllUnitConversionsSnapshot()
                                val file = File(context.cacheDir, "RG_POS_Current_Stock.csv")
                                file.writeText(InventoryImportExport.generateExport(products, conversions))
                                ReportExporter.shareReport(context, file)
                            }
                        }
                    )
                }
            }

            // ---- Select & preview ----
            Card(
                colors = CardDefaults.cardColors(containerColor = DarkSurfaceCard),
                border = androidx.compose.foundation.BorderStroke(1.dp, DarkBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("2. Select a filled-in spreadsheet", color = TextWhite, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    Text(
                        "Nothing changes in your inventory until you review this file's preview and confirm.",
                        color = TextMuted,
                        fontSize = 12.sp
                    )
                    FuturisticButton(
                        text = if (isProcessing) "Reading..." else "Select CSV File",
                        icon = Icons.Default.UploadFile,
                        enabled = !isProcessing,
                        testTag = "select_import_file_button",
                        onClick = { filePickerLauncher.launch(arrayOf("*/*")) }
                    )
                    if (isProcessing) {
                        LinearProgressIndicator(modifier = Modifier.fillMaxWidth(), color = RgAccent)
                    }
                    errorMessage?.let {
                        Text(it, color = AlertRed, fontSize = 12.sp)
                    }
                }
            }

            // ---- Preview summary ----
            val p = preview
            if (p != null) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = DarkSurfaceCard),
                    border = androidx.compose.foundation.BorderStroke(1.dp, if (p.canImport) SuccessGreen else AlertRed),
                    modifier = Modifier.fillMaxWidth().testTag("import_preview_card")
                ) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("Import Preview: ${selectedFileName ?: ""}", color = TextWhite, fontWeight = FontWeight.Bold, fontSize = 15.sp)

                        val summaryLines = listOf(
                            "New Products" to p.newProducts.toString(),
                            "Products to Update" to p.updatedProducts.toString(),
                            "Packaging Changes" to p.packagingChanges.toString(),
                            "Stock Additions" to "+${formatQty(p.stockAdditions)}",
                            "Stock Reductions" to "-${formatQty(p.stockReductions)}",
                            "Price Changes" to p.priceChanges.toString(),
                            "Deactivations" to p.deactivations.toString(),
                            "Warnings" to p.warningsCount.toString(),
                            "Errors" to p.errorsCount.toString()
                        )
                        summaryLines.forEach { (label, value) ->
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text(label, color = TextMuted, fontSize = 12.sp)
                                Text(
                                    value,
                                    color = if (label == "Errors" && p.errorsCount > 0) AlertRed
                                        else if (label == "Warnings" && p.warningsCount > 0) WarningOrange
                                        else TextWhite,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        if (!p.canImport) {
                            Text(
                                "Import cannot proceed while errors remain. Fix the rows below in your spreadsheet and re-select the file.",
                                color = AlertRed,
                                fontSize = 12.sp
                            )
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            FilterChip(
                                selected = expandedFilter == InventoryImportExport.Severity.ERROR,
                                onClick = {
                                    expandedFilter = if (expandedFilter == InventoryImportExport.Severity.ERROR) null else InventoryImportExport.Severity.ERROR
                                },
                                label = { Text("View Errors (${p.errorsCount})", fontSize = 11.sp) }
                            )
                            FilterChip(
                                selected = expandedFilter == InventoryImportExport.Severity.WARNING,
                                onClick = {
                                    expandedFilter = if (expandedFilter == InventoryImportExport.Severity.WARNING) null else InventoryImportExport.Severity.WARNING
                                },
                                label = { Text("View Warnings (${p.warningsCount})", fontSize = 11.sp) }
                            )
                        }

                        if (expandedFilter != null) {
                            val rowsToShow = p.rows.filter { row -> row.issues.any { it.severity == expandedFilter } }.take(200)
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(max = 320.dp)
                                    .verticalScroll(rememberScrollState()),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                rowsToShow.forEach { row ->
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(DarkSurfaceElevated)
                                            .padding(8.dp)
                                    ) {
                                        Text("Row ${row.rowNumber}: ${row.productName} / ${row.packagingName}", color = TextWhite, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                        row.issues.filter { it.severity == expandedFilter }.forEach { issue ->
                                            Text(
                                                "\u2022 ${issue.message}",
                                                color = if (issue.severity == InventoryImportExport.Severity.ERROR) AlertRed else WarningOrange,
                                                fontSize = 11.sp
                                            )
                                        }
                                    }
                                }
                                if (p.rows.count { row -> row.issues.any { it.severity == expandedFilter } } > 200) {
                                    Text("Showing first 200 of ${p.rows.size} rows with this issue type.", color = TextMuted, fontSize = 11.sp)
                                }
                            }
                        }

                        FuturisticButton(
                            text = if (isProcessing) "Importing..." else "Confirm & Apply Import",
                            icon = Icons.Default.Check,
                            enabled = p.canImport && !isProcessing,
                            testTag = "confirm_import_button",
                            onClick = {
                                isProcessing = true
                                coroutineScope.launch {
                                    try {
                                        val userName = viewModel.currentUser.value.ifBlank { "Staff" }
                                        val batch = viewModel.repository.applyInventoryImport(p, userName)
                                        lastResult = batch
                                        preview = null
                                        selectedFileName = null
                                        Toast.makeText(context, "Import complete: ${batch.productsCreated} new, ${batch.productsUpdated} updated.", Toast.LENGTH_LONG).show()
                                    } catch (e: Exception) {
                                        errorMessage = "Import failed, no changes were applied: ${e.message}"
                                    } finally {
                                        isProcessing = false
                                    }
                                }
                            }
                        )
                    }
                }
            }

            lastResult?.let { result ->
                Card(
                    colors = CardDefaults.cardColors(containerColor = DarkSurfaceCard),
                    border = androidx.compose.foundation.BorderStroke(1.dp, SuccessGreen),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("Import Complete: ${result.batchCode}", color = SuccessGreen, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Text("Products created: ${result.productsCreated}", color = TextMuted, fontSize = 12.sp)
                        Text("Products updated: ${result.productsUpdated}", color = TextMuted, fontSize = 12.sp)
                        Text("Packaging changes: ${result.packagingChanges}", color = TextMuted, fontSize = 12.sp)
                        Text("Stock added: +${formatQty(result.stockAdded)}  Removed: -${formatQty(result.stockRemoved)}", color = TextMuted, fontSize = 12.sp)
                        Text("Prices updated: ${result.pricesUpdated}", color = TextMuted, fontSize = 12.sp)
                        Text("Warnings: ${result.warningsCount}  Errors: ${result.errorsCount}", color = TextMuted, fontSize = 12.sp)
                    }
                }
            }

            // ---- Import history ----
            Text("Import History", color = TextWhite, fontWeight = FontWeight.Bold, fontSize = 15.sp)
            if (importHistory.isEmpty()) {
                Text("No spreadsheet imports yet.", color = TextMuted, fontSize = 12.sp)
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    importHistory.take(30).forEach { batch ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(DarkSurfaceCard)
                                .border(1.dp, DarkBorder, RoundedCornerShape(8.dp))
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(batch.batchCode, color = TextWhite, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                Text(
                                    "${batch.filename} \u2022 ${SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.US).format(java.util.Date(batch.dateEpoch))}",
                                    color = TextMuted,
                                    fontSize = 11.sp
                                )
                                Text(
                                    "${batch.productsCreated} new, ${batch.productsUpdated} updated, ${batch.totalRows} rows",
                                    color = TextMuted,
                                    fontSize = 11.sp
                                )
                            }
                            Text(
                                batch.status,
                                color = if (batch.status == "COMPLETED") SuccessGreen else AlertRed,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(80.dp))
        }
    }
}

private fun formatQty(value: Double): String {
    return if (value == value.toLong().toDouble()) value.toLong().toString() else "%.2f".format(value)
}
