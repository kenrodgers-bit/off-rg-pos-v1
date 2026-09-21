package com.example.ui.screens

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.AuditLog
import com.example.ui.components.EmptyStateView
import com.example.ui.theme.*
import com.example.ui.viewmodel.PosViewModel
import com.example.util.CurrencyFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AuditLogScreen(
    viewModel: PosViewModel,
    onNavigateBack: () -> Unit
) {
    val auditLogs by viewModel.allAuditLogs.collectAsStateWithLifecycle()
    var searchQuery by remember { mutableStateOf("") }

    val filtered = remember(auditLogs, searchQuery) {
        if (searchQuery.isBlank()) auditLogs else {
            auditLogs.filter {
                it.action.contains(searchQuery, ignoreCase = true) ||
                        it.staffName.contains(searchQuery, ignoreCase = true) ||
                        it.recordInfo.contains(searchQuery, ignoreCase = true)
            }
        }
    }

    Scaffold(
        containerColor = DarkBg,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "System Audit Trail",
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
                .testTag("audit_log_screen"),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Search logs by staff, action or details...", fontSize = 13.sp) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = TextMuted) },
                modifier = Modifier.fillMaxWidth().height(50.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = RgAccent,
                    unfocusedBorderColor = DarkBorder,
                    focusedTextColor = TextWhite,
                    unfocusedTextColor = TextWhite
                ),
                singleLine = true
            )

            if (filtered.isEmpty()) {
                EmptyStateView(
                    icon = Icons.Default.Search,
                    title = "No audit records found",
                    description = "System actions like sales, refunds, stock adjustments, and logins are recorded here."
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(bottom = 24.dp)
                ) {
                    items(filtered) { log ->
                        Card(
                            colors = CardDefaults.cardColors(containerColor = DarkSurfaceCard),
                            border = androidx.compose.foundation.BorderStroke(1.dp, DarkBorder),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = log.action,
                                        color = RgAccent,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp
                                    )
                                    Text(
                                        text = CurrencyFormatter.formatDate(log.dateEpoch),
                                        color = TextSubtle,
                                        fontSize = 11.sp
                                    )
                                }
                                Text(
                                    text = log.recordInfo,
                                    color = TextWhite,
                                    fontSize = 12.sp
                                )
                                Text(
                                    text = "Operator: ${log.staffName}",
                                    color = TextMuted,
                                    fontSize = 11.sp
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
