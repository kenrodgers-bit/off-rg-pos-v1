package com.example.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddShoppingCart
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.model.Product
import com.example.ui.theme.*
import com.example.util.CurrencyFormatter

/**
 * Shared design tokens for POS components, ensuring predictable card sizing,
 * uniform heights, and consistent touch targets throughout the application.
 */
object PosDesignTokens {
    // Spacing
    val SpacingXxs = 2.dp
    val SpacingXs = 4.dp
    val SpacingSm = 8.dp
    val SpacingMd = 12.dp
    val SpacingLg = 16.dp
    val SpacingXl = 24.dp

    // Corner radii (moderate, clean, not giant oversized boards)
    val RadiusPill = 20.dp
    val RadiusCard = 10.dp
    val RadiusOption = 8.dp
    val RadiusButton = 8.dp
    val RadiusInput = 8.dp

    // Button Heights (Consistent adaptive system)
    val ButtonHeightPrimary = 42.dp
    val ButtonHeightSecondary = 38.dp
    val ButtonHeightCompact = 32.dp
    val TouchTargetMin = 48.dp

    // Button Paddings
    val ButtonPaddingHorizontal = 14.dp
    val ButtonPaddingSecondaryHorizontal = 12.dp
    val ButtonPaddingCompactHorizontal = 10.dp
    val ButtonPaddingVertical = 6.dp

    // Icon Sizes
    val IconSizeXs = 14.dp
    val IconSizeSm = 16.dp
    val IconSizeMd = 18.dp
    val IconSizeLg = 22.dp

    // Component Heights
    val CategoryPillHeight = 34.dp
    val SellingFormCardMinHeight = 52.dp
    val SellingFormGridCardHeight = 68.dp
    val ProductCardMinHeight = 60.dp
    val ActionButtonHeight = 44.dp
    val MetricCardMinHeight = 72.dp

    // Responsive Max Widths (avoids oversized board layout on tablets & landscape)
    val DialogMaxWidthCompact = 420.dp
    val DialogMaxWidthExpanded = 520.dp
    val FormContentMaxWidth = 720.dp
    val ScreenContentMaxWidth = 840.dp

    // Paddings
    val DialogPadding = 14.dp
    val OptionPaddingHorizontal = 12.dp
    val OptionPaddingVertical = 8.dp

    // Elevation
    val ElevationNone = 0.dp
    val ElevationLow = 1.dp
    val ElevationCard = 2.dp
    val ElevationPill = 4.dp
    val ElevationDialog = 6.dp
}

/**
 * Universal responsive dialog wrapper preventing dialogs from expanding
 * into giant rectangular boards on tablets, foldables, or landscape viewports.
 */
@Composable
fun ResponsiveDialog(
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    maxWidth: Dp = PosDesignTokens.DialogMaxWidthCompact,
    content: @Composable () -> Unit
) {
    Dialog(
        onDismissRequest = onDismissRequest,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = modifier
                    .widthIn(max = maxWidth)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                content()
            }
        }
    }
}

/**
 * Data model for a selling form / unit option in the sale dialog.
 */
data class PosSellingOption(
    val name: String,
    val factor: Double,
    val price: Double,
    val cost: Double,
    val availableStockUnits: Int,
    val intactPackages: Int = 0,
    val isBaseUnit: Boolean = false
)

/**
 * 1. CategoryCard (CategoryPill)
 * Compact, uniform card style for product category selectors.
 * Avoids oversized cards that consume vertical space while keeping comfortable touch targets.
 */
@Composable
fun CategoryCard(
    title: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    testTag: String? = null
) {
    val bg by animateColorAsState(
        targetValue = if (isSelected) RgAccent else DarkSurfaceCard,
        animationSpec = tween(150),
        label = "cat_bg"
    )
    val borderCol by animateColorAsState(
        targetValue = if (isSelected) RgAccent else DarkBorder,
        animationSpec = tween(150),
        label = "cat_border"
    )
    val textColor by animateColorAsState(
        targetValue = if (isSelected) DarkBg else TextWhite,
        animationSpec = tween(150),
        label = "cat_text"
    )

    Box(
        modifier = modifier
            .defaultMinSize(minHeight = PosDesignTokens.TouchTargetMin)
            .wrapContentHeight(Alignment.CenterVertically)
            .clip(RoundedCornerShape(PosDesignTokens.RadiusPill))
            .background(bg)
            .border(1.dp, borderCol, RoundedCornerShape(PosDesignTokens.RadiusPill))
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 7.dp)
            .then(if (testTag != null) Modifier.testTag(testTag) else Modifier),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = title,
            color = textColor,
            fontSize = 12.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

/**
 * 2. ProductCard
 * Standardized, bounded product item card for the POS catalog.
 * Guarantees consistent vertical height, controlled text wrapping, and aligned pricing.
 */
@Composable
fun ProductCard(
    name: String,
    priceText: String,
    stockText: String,
    isLowStock: Boolean = false,
    subtitle: String? = null,
    wholesalePriceText: String? = null,
    icon: ImageVector = Icons.Default.ShoppingBag,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    testTag: String? = null
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = DarkSurfaceCard),
        border = BorderStroke(1.dp, DarkBorder),
        shape = RoundedCornerShape(PosDesignTokens.RadiusCard),
        modifier = modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = PosDesignTokens.ProductCardMinHeight)
            .clickable(onClick = onClick)
            .then(if (testTag != null) Modifier.testTag(testTag) else Modifier)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                // Uniform Icon Container (40.dp x 40.dp)
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(DarkSurfaceElevated),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = RgAccent,
                        modifier = Modifier.size(20.dp)
                    )
                }

                // Name & Metadata
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = name,
                        color = TextWhite,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = stockText,
                            color = if (isLowStock) AlertRed else TextMuted,
                            fontSize = 11.sp,
                            fontWeight = if (isLowStock) FontWeight.Bold else FontWeight.Normal,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (!subtitle.isNullOrBlank()) {
                            Text(
                                text = "• $subtitle",
                                color = TextSubtle,
                                fontSize = 11.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Pricing Column
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = priceText,
                    color = RgAccent,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1
                )
                if (!wholesalePriceText.isNullOrBlank()) {
                    Text(
                        text = wholesalePriceText,
                        color = TextSubtle,
                        fontSize = 10.sp,
                        maxLines = 1
                    )
                }
            }
        }
    }
}

/**
 * 3. SellingFormCard (UnitOptionCard)
 * Clean, consistent option card for selecting a packaging or selling form in a list.
 * Fixes inconsistent heights by guaranteeing bounded vertical sizing, controlled text wrapping,
 * aligned pricing, and comfortable touch targets.
 */
@Composable
fun SellingFormCard(
    unitName: String,
    priceText: String,
    stockAvailabilityText: String? = null,
    conversionText: String? = null,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    testTag: String? = null
) {
    val bg by animateColorAsState(
        targetValue = if (isSelected) DarkSurfaceElevated else DarkBg,
        animationSpec = tween(150),
        label = "form_bg"
    )
    val borderCol by animateColorAsState(
        targetValue = if (isSelected) RgAccent else DarkBorder,
        animationSpec = tween(150),
        label = "form_border"
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = PosDesignTokens.SellingFormCardMinHeight)
            .clip(RoundedCornerShape(PosDesignTokens.RadiusOption))
            .background(bg)
            .border(
                width = if (isSelected) 1.5.dp else 1.dp,
                color = borderCol,
                shape = RoundedCornerShape(PosDesignTokens.RadiusOption)
            )
            .clickable(onClick = onClick)
            .padding(horizontal = PosDesignTokens.OptionPaddingHorizontal, vertical = PosDesignTokens.OptionPaddingVertical)
            .then(if (testTag != null) Modifier.testTag(testTag) else Modifier),
        contentAlignment = Alignment.CenterStart
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier.weight(1f),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Radio/Check Indicator
                Box(
                    modifier = Modifier
                        .size(20.dp)
                        .clip(CircleShape)
                        .background(if (isSelected) RgAccent.copy(alpha = 0.2f) else DarkSurfaceCard)
                        .border(
                            width = if (isSelected) 2.dp else 1.dp,
                            color = if (isSelected) RgAccent else TextMuted.copy(alpha = 0.5f),
                            shape = CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    if (isSelected) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(RgAccent)
                        )
                    }
                }

                // Text Hierarchy: Name (medium emphasis), Availability & Conversion (secondary)
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = unitName,
                        color = TextWhite,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (!conversionText.isNullOrBlank() || !stockAvailabilityText.isNullOrBlank()) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (!conversionText.isNullOrBlank()) {
                                Text(
                                    text = conversionText,
                                    color = TextMuted,
                                    fontSize = 11.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            if (!conversionText.isNullOrBlank() && !stockAvailabilityText.isNullOrBlank()) {
                                Text(
                                    text = "•",
                                    color = TextSubtle,
                                    fontSize = 10.sp
                                )
                            }
                            if (!stockAvailabilityText.isNullOrBlank()) {
                                Text(
                                    text = stockAvailabilityText,
                                    color = TextSubtle,
                                    fontSize = 11.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Price (strong emphasis)
            Text(
                text = priceText,
                color = if (isSelected) RgAccent else TextWhite,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                maxLines = 1
            )
        }
    }
}

/**
 * 4. SellingFormGridCard (PriceOptionCard)
 * Standardized grid card for displaying selling forms / pricing options in a multi-column grid.
 * All items in the grid share strictly identical dimensions, avoiding randomly tall boxes.
 */
@Composable
fun SellingFormGridCard(
    unitName: String,
    priceText: String,
    stockText: String? = null,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    testTag: String? = null
) {
    val bg by animateColorAsState(
        targetValue = if (isSelected) DarkSurfaceElevated else DarkBg,
        animationSpec = tween(150),
        label = "grid_bg"
    )
    val borderCol by animateColorAsState(
        targetValue = if (isSelected) RgAccent else DarkBorder,
        animationSpec = tween(150),
        label = "grid_border"
    )

    Box(
        modifier = modifier
            .height(PosDesignTokens.SellingFormGridCardHeight)
            .clip(RoundedCornerShape(PosDesignTokens.RadiusOption))
            .background(bg)
            .border(
                width = if (isSelected) 1.5.dp else 1.dp,
                color = borderCol,
                shape = RoundedCornerShape(PosDesignTokens.RadiusOption)
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 8.dp)
            .then(if (testTag != null) Modifier.testTag(testTag) else Modifier),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = unitName,
                color = TextWhite,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center
            )
            Text(
                text = priceText,
                color = if (isSelected) RgAccent else TextWhite,
                fontSize = 14.sp,
                fontWeight = FontWeight.ExtraBold,
                maxLines = 1,
                textAlign = TextAlign.Center
            )
            if (!stockText.isNullOrBlank()) {
                Text(
                    text = stockText,
                    color = TextMuted,
                    fontSize = 10.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center
                )
            } else {
                Spacer(modifier = Modifier.height(10.dp))
            }
        }
    }
}

/**
 * 5. DialogActionButton
 * Reusable action button for dialogs and modal sheets with consistent adaptive height
 * and comfortable touch targets.
 */
@Composable
fun DialogActionButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    isPrimary: Boolean = true,
    enabled: Boolean = true,
    testTag: String? = null
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier
            .height(PosDesignTokens.ActionButtonHeight)
            .then(if (testTag != null) Modifier.testTag(testTag) else Modifier),
        shape = RoundedCornerShape(PosDesignTokens.RadiusButton),
        colors = ButtonDefaults.buttonColors(
            containerColor = if (isPrimary) RgAccent else DarkSurfaceElevated,
            contentColor = if (isPrimary) DarkBg else TextWhite,
            disabledContainerColor = DarkBorder,
            disabledContentColor = TextMuted
        ),
        border = if (!isPrimary) BorderStroke(1.dp, DarkBorder) else null,
        contentPadding = PaddingValues(horizontal = PosDesignTokens.ButtonPaddingHorizontal, vertical = PosDesignTokens.ButtonPaddingVertical)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier.size(PosDesignTokens.IconSizeSm)
                )
                Spacer(modifier = Modifier.width(6.dp))
            }
            Text(
                text = text,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

/**
 * 6. QuantityStepper
 * Compact stepper control for entering sale quantities, offering >=48dp touch targets.
 */
@Composable
fun QuantityStepper(
    quantity: Double,
    onQuantityChange: (Double) -> Unit,
    modifier: Modifier = Modifier,
    minQuantity: Double = 1.0,
    step: Double = 1.0
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        IconButton(
            onClick = {
                if (quantity > minQuantity) {
                    onQuantityChange(quantity - step)
                }
            },
            enabled = quantity > minQuantity,
            modifier = Modifier
                .size(PosDesignTokens.TouchTargetMin)
                .clip(CircleShape)
                .background(DarkSurfaceElevated)
                .border(1.dp, DarkBorder, CircleShape)
                .testTag("stepper_decrease_button")
        ) {
            Icon(
                imageVector = Icons.Default.Remove,
                contentDescription = "Decrease Quantity",
                tint = if (quantity > minQuantity) TextWhite else TextMuted,
                modifier = Modifier.size(18.dp)
            )
        }

        Box(
            modifier = Modifier
                .defaultMinSize(minWidth = 44.dp)
                .padding(horizontal = 4.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = if (quantity % 1.0 == 0.0) quantity.toInt().toString() else "%.1f".format(quantity),
                color = TextWhite,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
        }

        IconButton(
            onClick = { onQuantityChange(quantity + step) },
            modifier = Modifier
                .size(PosDesignTokens.TouchTargetMin)
                .clip(CircleShape)
                .background(DarkSurfaceElevated)
                .border(1.dp, DarkBorder, CircleShape)
                .testTag("stepper_increase_button")
        ) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = "Increase Quantity",
                tint = TextWhite,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

/**
 * 7. ProductSaleDialog
 * Compact, responsive, one-hand friendly product sale dialog for POS.
 * Eliminates randomly large cards, maintains a tight type hierarchy,
 * scales options cleanly, and guarantees easy access to Quantity and Add to Cart.
 */
@Composable
fun ProductSaleDialog(
    productName: String,
    categoryName: String,
    totalStockText: String,
    options: List<PosSellingOption>,
    selectedOption: PosSellingOption?,
    quantity: Double,
    onSelectOption: (PosSellingOption) -> Unit,
    onQuantityChange: (Double) -> Unit,
    onAddToCart: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val activeOption = selectedOption ?: options.firstOrNull()
    val subtotal = (activeOption?.price ?: 0.0) * quantity

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 24.dp),
            contentAlignment = Alignment.Center
        ) {
            val isWide = maxWidth >= 600.dp
            val maxDialogWidth = if (isWide) 480.dp else 400.dp

            Card(
                modifier = modifier
                    .widthIn(max = maxDialogWidth)
                    .fillMaxWidth()
                    .testTag("product_sale_dialog"),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = DarkSurfaceCard),
                border = BorderStroke(1.dp, DarkBorder)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Header: Product Name + Category/Stock + Close
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = productName,
                                color = TextWhite,
                                fontSize = 17.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "$categoryName • Stock: $totalStockText",
                                color = RgAccent,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        IconButton(
                            onClick = onDismiss,
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(DarkSurfaceElevated)
                                .testTag("sale_dialog_close_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Close",
                                tint = TextMuted,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }

                    HorizontalDivider(color = DarkDivider)

                    // Section: Selling Form Options
                    Text(
                        text = "Choose selling form",
                        color = TextMuted,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )

                    // Options Container (List if <= 3 or grid if wide, with bounded scroll for many options)
                    val needsScroll = options.size > 3
                    val optionsContentModifier = if (needsScroll) {
                        Modifier
                            .fillMaxWidth()
                            .heightIn(max = 190.dp)
                            .verticalScroll(rememberScrollState())
                    } else {
                        Modifier.fillMaxWidth()
                    }

                    Column(
                        modifier = optionsContentModifier,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        options.forEach { option ->
                            val isSelected = activeOption?.name == option.name
                            val conversionInfo = if (option.factor > 1.0) {
                                "1 ${option.name} = ${option.factor.toInt()} base units"
                            } else null
                            val stockInfo = if (option.availableStockUnits > 0) {
                                "${option.availableStockUnits} avail"
                            } else null

                            SellingFormCard(
                                unitName = option.name,
                                priceText = CurrencyFormatter.format(option.price),
                                conversionText = conversionInfo,
                                stockAvailabilityText = stockInfo,
                                isSelected = isSelected,
                                onClick = { onSelectOption(option) },
                                testTag = "selling_form_${option.name}"
                            )
                        }
                    }

                    HorizontalDivider(color = DarkDivider)

                    // Quantity Stepper Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Quantity",
                                color = TextWhite,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 14.sp
                            )
                            if (activeOption != null) {
                                Text(
                                    text = "@ ${CurrencyFormatter.format(activeOption.price)} / ${activeOption.name}",
                                    color = TextMuted,
                                    fontSize = 11.sp
                                )
                            }
                        }

                        QuantityStepper(
                            quantity = quantity,
                            onQuantityChange = onQuantityChange
                        )
                    }

                    // Subtotal Preview Banner
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        color = DarkBg,
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.dp, DarkBorder)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Subtotal",
                                color = TextMuted,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = CurrencyFormatter.format(subtotal),
                                color = RgAccent,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Black
                            )
                        }
                    }

                    // Action Button: Add to Cart
                    DialogActionButton(
                        text = "Add to Cart • ${CurrencyFormatter.format(subtotal)}",
                        icon = Icons.Default.AddShoppingCart,
                        onClick = onAddToCart,
                        modifier = Modifier.fillMaxWidth(),
                        testTag = "add_to_cart_confirm_button"
                    )
                }
            }
        }
    }
}
