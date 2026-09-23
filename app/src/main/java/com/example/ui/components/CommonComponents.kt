package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*

@Composable
fun StatusBadge(
    text: String,
    color: Color,
    bgColor: Color = color.copy(alpha = 0.15f),
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(6.dp))
            .background(bgColor)
            .border(1.dp, color.copy(alpha = 0.3f), RoundedCornerShape(6.dp))
            .padding(horizontal = 8.dp, vertical = 3.dp)
    ) {
        Text(
            text = text,
            color = color,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
fun OfflineIndicator(isOnline: Boolean, modifier: Modifier = Modifier) {
    val (dotColor, label, bg) = if (isOnline) {
        Triple(SuccessGreen, "Online", SuccessGreen.copy(alpha = 0.1f))
    } else {
        Triple(WarningOrange, "Offline", WarningOrange.copy(alpha = 0.15f))
    }

    Row(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(bg)
            .border(1.dp, dotColor.copy(alpha = 0.25f), RoundedCornerShape(12.dp))
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        Box(
            modifier = Modifier
                .size(7.dp)
                .clip(CircleShape)
                .background(dotColor)
        )
        Text(
            text = label,
            color = dotColor,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

enum class PosButtonSize {
    PRIMARY,
    SECONDARY,
    COMPACT
}

@Composable
fun MetricCard(
    title: String,
    value: String,
    subtitle: String? = null,
    accentColor: Color = RgAccent,
    icon: ImageVector? = null,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null
) {
    Surface(
        modifier = modifier
            .clip(RoundedCornerShape(PosDesignTokens.RadiusCard))
            .then(if (onClick != null) Modifier.clickable { onClick() } else Modifier)
            .border(1.dp, DarkBorder, RoundedCornerShape(PosDesignTokens.RadiusCard)),
        color = DarkSurfaceCard,
        tonalElevation = PosDesignTokens.ElevationCard
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    color = TextMuted,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false)
                )
                if (icon != null) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = accentColor,
                        modifier = Modifier.size(PosDesignTokens.IconSizeXs)
                    )
                }
            }
            Text(
                text = value,
                color = TextWhite,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (!subtitle.isNullOrBlank()) {
                Text(
                    text = subtitle,
                    color = TextSubtle,
                    fontSize = 10.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
fun EmptyStateView(
    icon: ImageVector,
    title: String,
    description: String,
    actionButtonText: String? = null,
    onActionClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(56.dp)
                .clip(CircleShape)
                .background(DarkSurfaceElevated)
                .border(1.dp, DarkBorder, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = RgAccent,
                modifier = Modifier.size(28.dp)
            )
        }
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = title,
            color = TextWhite,
            fontSize = 16.sp,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = description,
            color = TextMuted,
            fontSize = 12.sp,
            textAlign = TextAlign.Center,
            lineHeight = 16.sp
        )
        if (actionButtonText != null && onActionClick != null) {
            Spacer(modifier = Modifier.height(14.dp))
            FuturisticButton(
                text = actionButtonText,
                onClick = onActionClick,
                size = PosButtonSize.SECONDARY,
                testTag = "empty_state_action_button"
            )
        }
    }
}

@Composable
fun FuturisticButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    isSecondary: Boolean = false,
    icon: ImageVector? = null,
    size: PosButtonSize = PosButtonSize.PRIMARY,
    testTag: String = "futuristic_button"
) {
    val height = when (size) {
        PosButtonSize.PRIMARY -> PosDesignTokens.ButtonHeightPrimary
        PosButtonSize.SECONDARY -> PosDesignTokens.ButtonHeightSecondary
        PosButtonSize.COMPACT -> PosDesignTokens.ButtonHeightCompact
    }
    val fontSize = when (size) {
        PosButtonSize.PRIMARY -> 13.sp
        PosButtonSize.SECONDARY -> 12.sp
        PosButtonSize.COMPACT -> 11.sp
    }
    val iconSize = when (size) {
        PosButtonSize.PRIMARY -> PosDesignTokens.IconSizeMd
        PosButtonSize.SECONDARY -> PosDesignTokens.IconSizeSm
        PosButtonSize.COMPACT -> PosDesignTokens.IconSizeXs
    }
    val hPadding = when (size) {
        PosButtonSize.PRIMARY -> PosDesignTokens.ButtonPaddingHorizontal
        PosButtonSize.SECONDARY -> PosDesignTokens.ButtonPaddingSecondaryHorizontal
        PosButtonSize.COMPACT -> PosDesignTokens.ButtonPaddingCompactHorizontal
    }

    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier
            .defaultMinSize(minWidth = 48.dp, minHeight = height)
            .heightIn(min = height, max = height + 4.dp)
            .testTag(testTag),
        shape = RoundedCornerShape(PosDesignTokens.RadiusButton),
        contentPadding = PaddingValues(horizontal = hPadding, vertical = 0.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = if (isSecondary) DarkSurfaceElevated else RgAccent,
            contentColor = if (isSecondary) TextWhite else DarkBg,
            disabledContainerColor = DarkBorder,
            disabledContentColor = TextSubtle
        ),
        border = if (isSecondary) androidx.compose.foundation.BorderStroke(1.dp, DarkBorder) else null
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier.wrapContentSize()
        ) {
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier.size(iconSize)
                )
            }
            Text(
                text = text,
                fontSize = fontSize,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun CompactButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    isSecondary: Boolean = false,
    icon: ImageVector? = null,
    testTag: String = "compact_button"
) {
    FuturisticButton(
        text = text,
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
        isSecondary = isSecondary,
        icon = icon,
        size = PosButtonSize.COMPACT,
        testTag = testTag
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AdaptiveButtonRow(
    modifier: Modifier = Modifier,
    horizontalArrangement: Arrangement.Horizontal = Arrangement.spacedBy(8.dp),
    verticalArrangement: Arrangement.Vertical = Arrangement.spacedBy(8.dp),
    content: @Composable FlowRowScope.() -> Unit
) {
    FlowRow(
        modifier = modifier,
        horizontalArrangement = horizontalArrangement,
        verticalArrangement = verticalArrangement,
        content = content
    )
}

@Composable
fun NumericPinKeypad(
    onDigitClick: (String) -> Unit,
    onDeleteClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val digits = listOf(
        listOf("1", "2", "3"),
        listOf("4", "5", "6"),
        listOf("7", "8", "9"),
        listOf("", "0", "DEL")
    )

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        for (row in digits) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterHorizontally)
            ) {
                for (digit in row) {
                    if (digit.isEmpty()) {
                        Spacer(modifier = Modifier.size(54.dp))
                    } else if (digit == "DEL") {
                        IconButton(
                            onClick = onDeleteClick,
                            modifier = Modifier
                                .size(54.dp)
                                .clip(CircleShape)
                                .background(DarkSurfaceCard)
                                .border(1.dp, DarkBorder, CircleShape)
                                .testTag("pin_delete_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Delete",
                                tint = TextMuted,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    } else {
                        Box(
                            modifier = Modifier
                                .size(54.dp)
                                .clip(CircleShape)
                                .background(DarkSurfaceCard)
                                .border(1.dp, DarkBorder, CircleShape)
                                .clickable { onDigitClick(digit) }
                                .testTag("pin_key_$digit"),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = digit,
                                color = TextWhite,
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}
