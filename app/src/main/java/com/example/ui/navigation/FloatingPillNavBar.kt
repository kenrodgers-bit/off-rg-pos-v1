package com.example.ui.navigation

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.DarkBg
import com.example.ui.theme.RgAccent
import com.example.ui.theme.TextMuted

/**
 * Floating Pill-Shaped Navigation Bar for RG POS.
 *
 * Implements a modern, futuristic capsule design that floats cleanly above the
 * bottom edge of the screen, respects safe-area insets, and adapts responsively
 * across phones and tablets without reverting to a generic rectangular bar.
 */
@Composable
fun FloatingPillNavBar(
    activeTab: MainTab?,
    cartBadgeCount: Int,
    onTabSelected: (MainTab) -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 20.dp, vertical = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            shape = CircleShape,
            color = Color(0xF2121722),
            border = BorderStroke(1.dp, Color(0xFF283446)),
            shadowElevation = 16.dp,
            modifier = Modifier
                .widthIn(min = 280.dp, max = 460.dp)
                .fillMaxWidth()
                .height(60.dp)
                .testTag("floating_pill_nav_bar")
        ) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 6.dp, vertical = 5.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                MainTab.values().forEach { tab ->
                    val isSelected = activeTab == tab
                    FloatingPillNavItem(
                        tab = tab,
                        isSelected = isSelected,
                        badgeCount = if (tab == MainTab.POS) cartBadgeCount else 0,
                        onClick = { onTabSelected(tab) },
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                    )
                }
            }
        }
    }
}

@Composable
private fun FloatingPillNavItem(
    tab: MainTab,
    isSelected: Boolean,
    badgeCount: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val iconTint by animateColorAsState(
        targetValue = if (isSelected) RgAccent else TextMuted,
        animationSpec = tween(durationMillis = 200),
        label = "pill_icon_tint"
    )
    val labelColor by animateColorAsState(
        targetValue = if (isSelected) RgAccent else TextMuted,
        animationSpec = tween(durationMillis = 200),
        label = "pill_label_color"
    )
    val pillBgColor by animateColorAsState(
        targetValue = if (isSelected) RgAccent.copy(alpha = 0.16f) else Color.Transparent,
        animationSpec = tween(durationMillis = 200),
        label = "pill_bg_color"
    )
    val pillBorderColor by animateColorAsState(
        targetValue = if (isSelected) RgAccent.copy(alpha = 0.38f) else Color.Transparent,
        animationSpec = tween(durationMillis = 200),
        label = "pill_border_color"
    )

    Box(
        modifier = modifier
            .clip(CircleShape)
            .clickable(onClick = onClick)
            .testTag(tab.tag),
        contentAlignment = Alignment.Center
    ) {
        Row(
            modifier = Modifier
                .fillMaxHeight()
                .fillMaxWidth()
                .padding(horizontal = 4.dp, vertical = 4.dp)
                .background(pillBgColor, CircleShape)
                .border(1.dp, pillBorderColor, CircleShape),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            BadgedBox(
                badge = {
                    if (badgeCount > 0) {
                        Badge(
                            containerColor = RgAccent,
                            contentColor = DarkBg,
                            modifier = Modifier.offset(x = 4.dp, y = (-2).dp)
                        ) {
                            Text(
                                text = if (badgeCount > 99) "99+" else "$badgeCount",
                                fontWeight = FontWeight.Bold,
                                fontSize = 9.sp
                            )
                        }
                    }
                }
            ) {
                Icon(
                    imageVector = tab.icon,
                    contentDescription = tab.title,
                    tint = iconTint,
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.width(5.dp))

            Text(
                text = tab.title,
                color = labelColor,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                fontSize = 11.5.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
