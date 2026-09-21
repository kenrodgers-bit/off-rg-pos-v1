package com.example.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PointOfSale
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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.components.FuturisticButton
import com.example.ui.components.NumericPinKeypad
import com.example.ui.components.OfflineIndicator
import com.example.ui.theme.*
import com.example.ui.viewmodel.PosViewModel
import kotlinx.coroutines.launch

@Composable
fun LoginScreen(
    viewModel: PosViewModel,
    onLoginSuccess: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val business by viewModel.business.collectAsStateWithLifecycle()
    val isOnline by viewModel.isOnline.collectAsStateWithLifecycle()

    var enteredPin by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBg)
            .padding(horizontal = 24.dp, vertical = 20.dp)
            .testTag("login_screen")
    ) {
        // Top row with offline indicator
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.TopCenter),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(
                    imageVector = Icons.Default.PointOfSale,
                    contentDescription = null,
                    tint = RgAccent,
                    modifier = Modifier.size(20.dp)
                )
                Text(
                    text = "RG POS",
                    color = TextWhite,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp
                )
            }
            OfflineIndicator(isOnline = isOnline)
        }

        // Center PIN unlock content
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.Center),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(CircleShape)
                    .background(DarkSurfaceElevated)
                    .border(1.5.dp, RgAccent, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = null,
                    tint = RgAccent,
                    modifier = Modifier.size(30.dp)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = business?.name ?: "RG POS Business",
                color = TextWhite,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "Enter PIN to Unlock",
                color = TextMuted,
                fontSize = 13.sp
            )

            Spacer(modifier = Modifier.height(24.dp))

            // PIN Dots
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                for (i in 0 until 4) {
                    val isFilled = i < enteredPin.length
                    Box(
                        modifier = Modifier
                            .size(18.dp)
                            .clip(CircleShape)
                            .background(if (isFilled) RgAccent else DarkSurfaceElevated)
                            .border(1.5.dp, if (isFilled) RgAccent else DarkBorder, CircleShape)
                    )
                }
            }

            if (errorMessage != null) {
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = errorMessage!!,
                    color = AlertRed,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )
            }

            Spacer(modifier = Modifier.height(28.dp))

            NumericPinKeypad(
                onDigitClick = { digit ->
                    if (enteredPin.length < 4) {
                        enteredPin += digit
                        errorMessage = null
                        if (enteredPin.length == 4) {
                            coroutineScope.launch {
                                val (authenticated, userName) = viewModel.repository.authenticatePin(enteredPin)
                                if (authenticated) {
                                    viewModel.setCurrentUser(userName)
                                    onLoginSuccess()
                                } else {
                                    errorMessage = "Incorrect PIN. Try again."
                                    enteredPin = ""
                                }
                            }
                        }
                    }
                },
                onDeleteClick = {
                    if (enteredPin.isNotEmpty()) {
                        enteredPin = enteredPin.dropLast(1)
                        errorMessage = null
                    }
                }
            )

            Spacer(modifier = Modifier.height(20.dp))

            FuturisticButton(
                text = "Unlock",
                enabled = enteredPin.length == 4,
                onClick = {
                    coroutineScope.launch {
                        val (authenticated, userName) = viewModel.repository.authenticatePin(enteredPin)
                        if (authenticated) {
                            viewModel.setCurrentUser(userName)
                            onLoginSuccess()
                        } else {
                            errorMessage = "Incorrect PIN. Try again."
                            enteredPin = ""
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth(0.8f),
                testTag = "unlock_button"
            )
        }
    }
}
