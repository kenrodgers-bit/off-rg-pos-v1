package com.example.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.BoxWithConstraintsScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Shared screen-size breakpoints, matching the threshold already used by
 * MainScaffold and PosScreen (maxWidth >= 720.dp = tablet). Kept in one
 * place so every screen uses the same definition of "tablet".
 */
object ResponsiveBreakpoints {
    val Tablet = 720.dp
    val LargeTablet = 1000.dp
}

fun isTabletWidth(maxWidth: Dp): Boolean = maxWidth >= ResponsiveBreakpoints.Tablet

fun isLargeTabletWidth(maxWidth: Dp): Boolean = maxWidth >= ResponsiveBreakpoints.LargeTablet

/**
 * Wraps a screen's content in BoxWithConstraints and exposes whether the
 * available width counts as "tablet", so screens can branch their layout
 * (e.g. two-column forms, wider cards, master-detail) without each screen
 * re-deriving its own breakpoint.
 *
 * Usage: replace `Scaffold(...) { padding -> MyColumn(...) }` with
 * `Scaffold(...) { padding -> ResponsiveScreen(Modifier.padding(padding)) { isTablet -> MyColumn(isTablet, ...) } }`
 */
@Composable
fun ResponsiveScreen(
    modifier: Modifier = Modifier,
    content: @Composable BoxWithConstraintsScope.(isTablet: Boolean) -> Unit
) {
    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        content(isTabletWidth(maxWidth))
    }
}

/**
 * Constrains and centers content on wide screens so phone-oriented layouts
 * (forms, single-column lists) don't stretch edge-to-edge on a tablet, while
 * remaining fillMaxWidth on phones. Wrap a screen's outer Column/LazyColumn
 * with this instead of changing every internal width modifier by hand.
 */
@Composable
fun ResponsiveContentWidth(
    isTablet: Boolean,
    modifier: Modifier = Modifier,
    maxContentWidth: Dp = 900.dp,
    content: @Composable () -> Unit
) {
    if (isTablet) {
        Box(modifier = modifier.fillMaxWidth(), contentAlignment = Alignment.TopCenter) {
            Box(modifier = Modifier.widthIn(max = maxContentWidth).fillMaxWidth()) {
                content()
            }
        }
    } else {
        Box(modifier = modifier.fillMaxWidth()) {
            content()
        }
    }
}
