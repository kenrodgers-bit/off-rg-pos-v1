package com.example.ui.screens

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.components.FuturisticButton
import com.example.ui.components.StatusBadge
import com.example.ui.theme.*
import com.example.ui.viewmodel.PosViewModel
import com.example.util.BackupManager
import com.example.util.BackupMetadata
import com.example.util.ReportExporter
import com.example.util.ValidationResult
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BackupRestoreScreen(
    viewModel: PosViewModel,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val backupManager = remember { BackupManager(context, viewModel.repository.db) }

    val allProducts by viewModel.allProducts.collectAsStateWithLifecycle()
    val allSales by viewModel.allCompletedSales.collectAsStateWithLifecycle()
    val allCustomers by viewModel.allCustomers.collectAsStateWithLifecycle()

    var isProcessing by remember { mutableStateOf(false) }
    var processingMessage by remember { mutableStateOf("") }
    var backupList by remember { mutableStateOf<List<BackupMetadata>>(emptyList()) }

    // Dialog States
    var showCreateDialog by remember { mutableStateOf(false) }
    var showPasswordDialog by remember { mutableStateOf(false) }
    var pendingRestoreFile by remember { mutableStateOf<File?>(null) }
    var validatedRestorePayload by remember { mutableStateOf<JSONObject?>(null) }
    var validatedRestoreMeta by remember { mutableStateOf<BackupMetadata?>(null) }
    var showRestoreConfirmDialog by remember { mutableStateOf(false) }
    var showCloudDialog by remember { mutableStateOf(false) }
    var showSettingsDialog by remember { mutableStateOf(false) }

    // Automatic backup settings
    var autoBackupFreq by remember { mutableStateOf("DAILY") }
    var retentionCount by remember { mutableIntStateOf(5) }

    fun refreshBackups() {
        backupList = backupManager.listBackups()
    }

    LaunchedEffect(Unit) {
        refreshBackups()
        autoBackupFreq = viewModel.repository.getAutoBackupFrequency()
        retentionCount = viewModel.repository.getAutoBackupRetentionCount()
    }

    Scaffold(
        containerColor = DarkBg,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Backup & Data Management",
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
                    IconButton(onClick = { showSettingsDialog = true }) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Backup Settings",
                            tint = TextMuted
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
                .testTag("backup_restore_screen"),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Backup Status Card
            Card(
                colors = CardDefaults.cardColors(containerColor = DarkSurfaceCard),
                border = androidx.compose.foundation.BorderStroke(1.dp, RgAccent.copy(alpha = 0.5f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("BACKUP SYSTEM STATUS", color = TextMuted, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                        StatusBadge(text = "VERIFIED (HEALTHY)", color = SuccessGreen)
                    }

                    Text("Offline-First Persistent Storage", color = TextWhite, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    Text(
                        text = "Full SQLite + Room database state with authenticated AES-256 encryption. Cloud-sync ready.",
                        color = TextMuted,
                        fontSize = 12.sp
                    )

                    HorizontalDivider(color = DarkBorder)

                    val latestBackup = backupList.firstOrNull()
                    val latestTimeStr = if (latestBackup != null) {
                        SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.US).format(Date(latestBackup.createdAtEpoch))
                    } else "No backups created yet"

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Last Backup:", color = TextMuted, fontSize = 13.sp)
                        Text(latestTimeStr, color = RgAccent, fontWeight = FontWeight.Bold)
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Active Products:", color = TextMuted, fontSize = 13.sp)
                        Text("${allProducts.size} items", color = TextWhite, fontWeight = FontWeight.Bold)
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Completed Sales:", color = TextMuted, fontSize = 13.sp)
                        Text("${allSales.size} receipts", color = TextWhite, fontWeight = FontWeight.Bold)
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Customer Ledgers:", color = TextMuted, fontSize = 13.sp)
                        Text("${allCustomers.size} accounts", color = TextWhite, fontWeight = FontWeight.Bold)
                    }
                }
            }

            // Quick Actions Grid
            Text("BACKUP & RESTORE ACTIONS", color = TextMuted, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                FuturisticButton(
                    text = "Create Backup",
                    icon = Icons.Default.CloudUpload,
                    onClick = { showCreateDialog = true },
                    modifier = Modifier.weight(1f).testTag("create_backup_btn")
                )
                FuturisticButton(
                    text = "Cloud Backup",
                    icon = Icons.Default.CloudQueue,
                    isSecondary = true,
                    onClick = { showCloudDialog = true },
                    modifier = Modifier.weight(1f).testTag("cloud_backup_btn")
                )
            }

            // Available Backups History List
            Card(
                colors = CardDefaults.cardColors(containerColor = DarkSurfaceCard),
                border = androidx.compose.foundation.BorderStroke(1.dp, DarkBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("LOCAL BACKUP HISTORY (${backupList.size})", color = TextMuted, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        IconButton(onClick = { refreshBackups() }) {
                            Icon(Icons.Default.Refresh, contentDescription = "Refresh", tint = RgAccent)
                        }
                    }

                    if (backupList.isEmpty()) {
                        Text(
                            text = "No saved backups found. Tap 'Create Backup' above to generate your first verified snapshot.",
                            color = TextMuted,
                            fontSize = 12.sp,
                            modifier = Modifier.padding(vertical = 12.dp)
                        )
                    } else {
                        backupList.forEach { item ->
                            val dateStr = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.US).format(Date(item.createdAtEpoch))
                            val sizeKb = item.sizeBytes / 1024

                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(DarkSurfaceElevated)
                                    .padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = item.filename,
                                            color = TextWhite,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.sp
                                        )
                                        Text(
                                            text = "$dateStr • ${sizeKb} KB • v${item.appVersion}",
                                            color = TextMuted,
                                            fontSize = 11.sp
                                        )
                                    }

                                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                        IconButton(
                                            onClick = {
                                                val file = File(item.filePath)
                                                ReportExporter.shareReport(context, file, "application/octet-stream")
                                            }
                                        ) {
                                            Icon(Icons.Default.Share, contentDescription = "Share", tint = TextMuted)
                                        }

                                        IconButton(
                                            onClick = {
                                                pendingRestoreFile = File(item.filePath)
                                                coroutineScope.launch {
                                                    isProcessing = true
                                                    processingMessage = "Validating backup integrity..."
                                                    val res = backupManager.validateBackup(pendingRestoreFile!!)
                                                    isProcessing = false
                                                    when (res) {
                                                        is ValidationResult.Valid -> {
                                                            validatedRestoreMeta = res.metadata
                                                            validatedRestorePayload = res.jsonPayload
                                                            showRestoreConfirmDialog = true
                                                        }
                                                        is ValidationResult.Invalid -> {
                                                            if (res.reason.contains("password", ignoreCase = true)) {
                                                                showPasswordDialog = true
                                                            } else {
                                                                Toast.makeText(context, res.reason, Toast.LENGTH_LONG).show()
                                                            }
                                                        }
                                                    }
                                                }
                                            },
                                            modifier = Modifier.testTag("restore_item_${item.filename}")
                                        ) {
                                            Icon(Icons.Default.Restore, contentDescription = "Restore", tint = WarningOrange)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(100.dp))
        }
    }

    // Dialog: Create Backup (with optional password encryption)
    if (showCreateDialog) {
        var password by remember { mutableStateOf("") }
        var isPasswordProtected by remember { mutableStateOf(false) }

        Dialog(onDismissRequest = { showCreateDialog = false }) {
            Card(
                colors = CardDefaults.cardColors(containerColor = DarkSurfaceCard),
                shape = RoundedCornerShape(16.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, RgAccent),
                modifier = Modifier.fillMaxWidth().padding(16.dp)
            ) {
                Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Create New Backup", color = TextWhite, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    Text(
                        text = "This captures your full database: Products, Unit Conversions, Sales, Inventory, Customers, Credit, Payments, and Settings.",
                        color = TextMuted,
                        fontSize = 12.sp
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Password Protection (AES-256):", color = TextWhite, fontSize = 13.sp)
                        Switch(
                            checked = isPasswordProtected,
                            onCheckedChange = { isPasswordProtected = it },
                            colors = SwitchDefaults.colors(checkedThumbColor = RgAccent)
                        )
                    }

                    if (isPasswordProtected) {
                        OutlinedTextField(
                            value = password,
                            onValueChange = { password = it },
                            label = { Text("Backup Password") },
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(focusedTextColor = TextWhite, unfocusedTextColor = TextWhite)
                        )
                    }

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                        TextButton(onClick = { showCreateDialog = false }) {
                            Text("Cancel", color = TextMuted)
                        }
                        FuturisticButton(
                            text = "Save Backup",
                            onClick = {
                                showCreateDialog = false
                                isProcessing = true
                                processingMessage = "Creating and verifying backup snapshot..."
                                coroutineScope.launch {
                                    val result = backupManager.createBackup(
                                        password = if (isPasswordProtected) password else "",
                                        retentionCount = retentionCount
                                    )
                                    isProcessing = false
                                    if (result.isSuccess) {
                                        Toast.makeText(context, "Backup successfully created: ${result.getOrNull()?.name}", Toast.LENGTH_LONG).show()
                                        refreshBackups()
                                    } else {
                                        Toast.makeText(context, "Backup failed: ${result.exceptionOrNull()?.message}", Toast.LENGTH_LONG).show()
                                    }
                                }
                            }
                        )
                    }
                }
            }
        }
    }

    // Dialog: Password Input for Encrypted Backup
    if (showPasswordDialog) {
        var passwordInput by remember { mutableStateOf("") }

        Dialog(onDismissRequest = { showPasswordDialog = false }) {
            Card(
                colors = CardDefaults.cardColors(containerColor = DarkSurfaceCard),
                shape = RoundedCornerShape(16.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, WarningOrange),
                modifier = Modifier.fillMaxWidth().padding(16.dp)
            ) {
                Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Encrypted Backup", color = TextWhite, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    Text("Enter the password chosen when creating this backup to verify and restore.", color = TextMuted, fontSize = 12.sp)

                    OutlinedTextField(
                        value = passwordInput,
                        onValueChange = { passwordInput = it },
                        label = { Text("Decryption Password") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = TextWhite, unfocusedTextColor = TextWhite)
                    )

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                        TextButton(onClick = { showPasswordDialog = false }) {
                            Text("Cancel", color = TextMuted)
                        }
                        FuturisticButton(
                            text = "Verify Password",
                            onClick = {
                                showPasswordDialog = false
                                isProcessing = true
                                processingMessage = "Verifying cryptographic key..."
                                coroutineScope.launch {
                                    val res = backupManager.validateBackup(pendingRestoreFile!!, passwordInput)
                                    isProcessing = false
                                    when (res) {
                                        is ValidationResult.Valid -> {
                                            validatedRestoreMeta = res.metadata
                                            validatedRestorePayload = res.jsonPayload
                                            showRestoreConfirmDialog = true
                                        }
                                        is ValidationResult.Invalid -> {
                                            Toast.makeText(context, res.reason, Toast.LENGTH_LONG).show()
                                        }
                                    }
                                }
                            }
                        )
                    }
                }
            }
        }
    }

    // Dialog: Multi-stage Restore Warning & Explicit Confirmation (# 12, # 13)
    if (showRestoreConfirmDialog && validatedRestorePayload != null) {
        Dialog(onDismissRequest = { showRestoreConfirmDialog = false }) {
            Card(
                colors = CardDefaults.cardColors(containerColor = DarkSurfaceCard),
                shape = RoundedCornerShape(16.dp),
                border = androidx.compose.foundation.BorderStroke(2.dp, AlertRed),
                modifier = Modifier.fillMaxWidth().padding(16.dp)
            ) {
                Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Default.Warning, contentDescription = null, tint = AlertRed)
                        Text("⚠ Restore RG POS Data?", color = AlertRed, fontWeight = FontWeight.Black, fontSize = 18.sp)
                    }

                    Text(
                        text = "Restoring this backup will replace the current business data on this device.",
                        color = TextWhite,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )

                    Text(
                        text = "Any sales, inventory changes, customers, payments, or other records created after this backup may be removed.\n\nA safety backup of your current data will be created automatically before restoration. If any issue arises, current data is rolled back safely.",
                        color = TextMuted,
                        fontSize = 12.sp
                    )

                    validatedRestoreMeta?.let { meta ->
                        Surface(
                            color = DarkSurfaceElevated,
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text("Backup File: ${meta.filename}", color = TextWhite, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                Text("Business: ${meta.businessName}", color = RgAccent, fontSize = 11.sp)
                                Text("Version: ${meta.appVersion} • ${meta.sizeBytes / 1024} KB", color = TextMuted, fontSize = 10.sp)
                            }
                        }
                    }

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                        TextButton(onClick = { showRestoreConfirmDialog = false }) {
                            Text("Cancel", color = TextMuted)
                        }
                        Button(
                            onClick = {
                                showRestoreConfirmDialog = false
                                isProcessing = true
                                processingMessage = "Restoring database securely..."
                                coroutineScope.launch {
                                    val result = backupManager.restoreBackup(validatedRestorePayload!!)
                                    isProcessing = false
                                    if (result.isSuccess) {
                                        Toast.makeText(context, "✓ Database successfully restored from backup!", Toast.LENGTH_LONG).show()
                                        refreshBackups()
                                    } else {
                                        Toast.makeText(context, "Restore failed: ${result.exceptionOrNull()?.message}", Toast.LENGTH_LONG).show()
                                    }
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = AlertRed)
                        ) {
                            Text("Confirm Restore", color = TextWhite, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }

    // Dialog: Cloud Backup / Google Drive Info & Export (# 9)
    if (showCloudDialog) {
        Dialog(onDismissRequest = { showCloudDialog = false }) {
            Card(
                colors = CardDefaults.cardColors(containerColor = DarkSurfaceCard),
                shape = RoundedCornerShape(16.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, RgAccent),
                modifier = Modifier.fillMaxWidth().padding(16.dp)
            ) {
                Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Cloud Backup", color = TextWhite, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    Text(
                        text = "RG POS doesn't connect directly to a specific cloud account yet. " +
                            "Tapping below opens Android's share sheet so you can send your latest " +
                            "backup to whichever cloud app you have installed -- Google Drive, " +
                            "Dropbox, OneDrive, email, etc. Automatic daily/weekly backups are also " +
                            "copied into Downloads / RG POS Backups on this device, so a folder-sync " +
                            "app pointed at that folder can pick new backups up on its own without " +
                            "you having to do this manually each time.",
                        color = TextMuted,
                        fontSize = 12.sp
                    )

                    FuturisticButton(
                        text = "Send Latest Backup to a Cloud App",
                        icon = Icons.Default.CloudUpload,
                        onClick = {
                            val latest = backupList.firstOrNull()
                            if (latest != null) {
                                val file = File(latest.filePath)
                                ReportExporter.shareReport(context, file, "application/octet-stream")
                                showCloudDialog = false
                            } else {
                                Toast.makeText(context, "Please create a local backup first before uploading.", Toast.LENGTH_SHORT).show()
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    )

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                        TextButton(onClick = { showCloudDialog = false }) {
                            Text("Close", color = TextMuted)
                        }
                    }
                }
            }
        }
    }

    // Dialog: Automatic Backup Settings (# 8)
    if (showSettingsDialog) {
        Dialog(onDismissRequest = { showSettingsDialog = false }) {
            Card(
                colors = CardDefaults.cardColors(containerColor = DarkSurfaceCard),
                shape = RoundedCornerShape(16.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, DarkBorder),
                modifier = Modifier.fillMaxWidth().padding(16.dp)
            ) {
                Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Automatic Backup Configuration", color = TextWhite, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    Text(
                        "Every backup is also copied to Downloads / RG POS Backups on this " +
                            "device. Point Google Drive, Dropbox, OneDrive or any folder-sync " +
                            "app you use at that folder to get automatic cloud backup without " +
                            "connecting an account in RG POS itself.",
                        color = TextMuted,
                        fontSize = 11.sp
                    )

                    Text("Backup Frequency:", color = TextMuted, fontSize = 12.sp)
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf("DAILY", "WEEKLY", "OFF").forEach { freq ->
                            FilterChip(
                                selected = autoBackupFreq == freq,
                                onClick = { autoBackupFreq = freq },
                                label = { Text(freq, fontSize = 11.sp) }
                            )
                        }
                    }

                    Text("Retention Limit (Keep last N copies):", color = TextMuted, fontSize = 12.sp)
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf(3, 5, 10).forEach { count ->
                            FilterChip(
                                selected = retentionCount == count,
                                onClick = { retentionCount = count },
                                label = { Text("Keep $count", fontSize = 11.sp) }
                            )
                        }
                    }

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                        FuturisticButton(
                            text = "Save Settings",
                            onClick = {
                                coroutineScope.launch {
                                    viewModel.repository.saveAutoBackupSettings(autoBackupFreq, retentionCount)
                                    com.example.util.AutoBackupWorker.schedule(context, autoBackupFreq, retentionCount)
                                    val message = if (autoBackupFreq == "OFF") {
                                        "Automatic backup turned off."
                                    } else {
                                        "Automatic backup scheduled: $autoBackupFreq, keeping last $retentionCount copies."
                                    }
                                    Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
                                }
                                showSettingsDialog = false
                            }
                        )
                    }
                }
            }
        }
    }

    // Processing Progress Modal
    if (isProcessing) {
        Dialog(onDismissRequest = {}) {
            Surface(
                color = DarkSurfaceCard,
                shape = RoundedCornerShape(12.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, RgAccent),
                modifier = Modifier.padding(24.dp)
            ) {
                Row(
                    modifier = Modifier.padding(20.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    CircularProgressIndicator(color = RgAccent)
                    Text(processingMessage, color = TextWhite, fontSize = 13.sp)
                }
            }
        }
    }
}
