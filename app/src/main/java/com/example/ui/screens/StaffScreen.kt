package com.example.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import com.example.data.model.User
import com.example.data.model.name
import com.example.ui.components.EmptyStateView
import com.example.ui.components.FuturisticButton
import com.example.ui.components.StatusBadge
import com.example.ui.theme.*
import com.example.ui.viewmodel.PosViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StaffScreen(
    viewModel: PosViewModel,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val staffList by viewModel.allStaff.collectAsStateWithLifecycle()
    var showAddStaffDialog by remember { mutableStateOf(false) }

    Scaffold(
        containerColor = DarkBg,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Staff & Cashiers (${staffList.size})",
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
                    IconButton(onClick = { showAddStaffDialog = true }) {
                        Icon(Icons.Default.PersonAdd, contentDescription = "Add Staff", tint = RgAccent)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = DarkBg)
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddStaffDialog = true },
                containerColor = RgAccent,
                contentColor = DarkBg,
                shape = CircleShape,
                modifier = Modifier.testTag("add_staff_fab")
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Staff")
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp)
                .testTag("staff_screen")
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(vertical = 12.dp)
            ) {
                items(staffList) { staff ->
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
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Text(staff.name, color = TextWhite, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                    StatusBadge(
                                        text = staff.role,
                                        color = if (staff.role == "ADMIN") RgAccent else CreditBlue
                                    )
                                }
                                Text("Phone: ${staff.phone.ifBlank { "N/A" }}", color = TextMuted, fontSize = 12.sp)
                            }

                            Switch(
                                checked = staff.isActive,
                                onCheckedChange = { active ->
                                    coroutineScope.launch {
                                        viewModel.repository.saveStaff(staff.copy(isActive = active))
                                        Toast.makeText(context, "${staff.name} is now ${if (active) "Active" else "Inactive"}", Toast.LENGTH_SHORT).show()
                                    }
                                },
                                colors = SwitchDefaults.colors(checkedThumbColor = RgAccent, checkedTrackColor = RgAccent.copy(alpha = 0.5f))
                            )
                        }
                    }
                }
            }
        }

        if (showAddStaffDialog) {
            AddStaffDialog(
                onAdd = { name, role, pin, phone ->
                    coroutineScope.launch {
                        viewModel.repository.saveStaff(
                            User(fullName = name, role = role, pin = pin, phone = phone, isActive = true)
                        )
                        showAddStaffDialog = false
                        Toast.makeText(context, "Staff member created", Toast.LENGTH_SHORT).show()
                    }
                },
                onDismiss = { showAddStaffDialog = false }
            )
        }
    }
}

@Composable
fun AddStaffDialog(
    onAdd: (name: String, role: String, pin: String, phone: String) -> Unit,
    onDismiss: () -> Unit
) {
    var name by remember { mutableStateOf("") }
    var role by remember { mutableStateOf("CASHIER") }
    var pin by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = DarkSurfaceCard),
            border = androidx.compose.foundation.BorderStroke(1.dp, DarkBorder)
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Add New Staff Member", color = TextWhite, fontWeight = FontWeight.Bold, fontSize = 16.sp)

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Staff Full Name") },
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
                    value = pin,
                    onValueChange = { if (it.length <= 4 && it.all { c -> c.isDigit() }) pin = it },
                    label = { Text("4-Digit Access PIN") },
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
                    value = phone,
                    onValueChange = { phone = it },
                    label = { Text("Phone Number") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = RgAccent,
                        unfocusedBorderColor = DarkBorder,
                        focusedTextColor = TextWhite,
                        unfocusedTextColor = TextWhite
                    ),
                    singleLine = true
                )

                // Role selector
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("CASHIER", "MANAGER", "ADMIN").forEach { r ->
                        val isSel = role == r
                        Button(
                            onClick = { role = r },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isSel) RgAccent else DarkSurfaceElevated,
                                contentColor = if (isSel) DarkBg else TextWhite
                            ),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(r, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    FuturisticButton(text = "Cancel", onClick = onDismiss, isSecondary = true, modifier = Modifier.weight(1f))
                    FuturisticButton(
                        text = "Save Staff",
                        enabled = name.isNotBlank() && pin.length == 4,
                        onClick = { onAdd(name, role, pin, phone) },
                        modifier = Modifier.weight(1.3f)
                    )
                }
            }
        }
    }
}
