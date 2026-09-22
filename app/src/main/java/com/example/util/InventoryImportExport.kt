package com.example.util

import androidx.room.withTransaction
import com.example.data.database.AppDatabase
import com.example.data.model.AuditLog
import com.example.data.model.ImportBatch
import com.example.data.model.Product
import com.example.data.model.StockMovement
import com.example.data.model.UnitConversion
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * CSV-based spreadsheet import/export for inventory, opens fine in Excel/Sheets/Numbers.
 * CSV rather than real .xlsx deliberately: XLSX parsing on Android (Apache POI) needs
 * java.awt classes Android doesn't have and is a common source of build/runtime pain, and
 * this project has no spreadsheet-parsing dependency to build on. CSV covers the same
 * "download template, edit in a spreadsheet app, re-import" workflow without that risk.
 *
 * One row = one packaging form. A row whose Packaging Name matches the product's Base
 * Unit (or is left blank) defines/updates the PRODUCT itself; any other row with the
 * same product identity adds/updates one additional selling form (UnitConversion) for
 * that product, matching the spreadsheet-model examples in the spec ("Sweets | Pack | 30
 * | Piece", "Sweets | Carton | 600 | Piece" -- Packaging Quantity is the absolute
 * base-unit conversion factor, not a multiplier relative to a parent row).
 */
object InventoryImportExport {

    val CSV_HEADERS = listOf(
        "Product ID", "Product Name", "SKU", "Barcode", "Category", "Brand", "Base Unit",
        "Packaging Name", "Packaging Quantity", "Parent Packaging",
        "Purchase Enabled", "Selling Enabled", "Can Open",
        "Purchase Cost", "Retail Price", "Wholesale Price",
        "Stock Quantity", "Stock Operation", "Reorder Level", "Supplier", "Active"
    )

    private val INSTRUCTION_LINES = listOf(
        "# RG POS Inventory Import Template",
        "# Lines starting with # are instructions and are ignored on import -- do not remove the header row below them.",
        "#",
        "# Product ID: leave blank for new products. If filled, must match an existing RG POS product exactly.",
        "# Product Name / SKU / Barcode: used (in that order, after Product ID) to match this row to an existing product.",
        "# One row per packaging form. The row whose 'Packaging Name' equals the product's 'Base Unit' (or is left blank) is the product's main row.",
        "#   Extra rows with the same Product Name/SKU/Barcode add more selling forms (e.g. Pack, Carton) for that same product -- see examples below.",
        "# Packaging Quantity: number of Base Units in one of this packaging form (e.g. Pack=30 pieces, Carton=600 pieces -- NOT relative to the parent row).",
        "# Parent Packaging: optional, for display only (e.g. Carton's parent is Pack) -- does not affect the math, which always uses Packaging Quantity directly.",
        "# Stock Operation: one of ADD, ADJUST, SET, REMOVE, NO_CHANGE. Stock Quantity is in the row's own Packaging Name's units, converted to base units automatically.",
        "#   ADD adds to current stock. ADJUST applies +/- to current stock. SET sets stock to an exact amount. REMOVE subtracts (never below zero). NO_CHANGE leaves stock untouched.",
        "# Purchase Enabled / Selling Enabled / Can Open / Active: TRUE or FALSE.",
        "# Leave Purchase Cost, Retail Price, Wholesale Price blank to leave that price unchanged on an existing product/packaging form.",
        "#",
        "# Examples:",
        "# Sugar:      Base Unit=KG, one row \"50 KG Sack\" with Packaging Quantity=50 as an additional purchase/selling form.",
        "# Milk 500ml: Base Unit=Bottle, plus \"Carton 18\" (Packaging Quantity=18) and \"Carton 22\" (Packaging Quantity=22) as separate rows -- each can have its own price.",
        "# Sweets:     Base Unit=Piece, plus \"Pack\" (Packaging Quantity=30) and \"Carton\" (Packaging Quantity=600, Parent Packaging=Pack) as separate rows."
    )

    // ---------- CSV read/write ----------

    fun writeCsvField(value: String): String {
        return if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
            "\"" + value.replace("\"", "\"\"") + "\""
        } else {
            value
        }
    }

    fun writeCsvRow(fields: List<String>): String = fields.joinToString(",") { writeCsvField(it) }

    /** Parses the full CSV text into rows of fields, correctly handling quoted fields
     * (including embedded commas and embedded newlines) and doubled-quote escaping. */
    fun parseCsv(text: String): List<List<String>> {
        val rows = mutableListOf<List<String>>()
        var row = mutableListOf<String>()
        val field = StringBuilder()
        var inQuotes = false
        var i = 0
        val n = text.length

        fun endField() {
            row.add(field.toString())
            field.clear()
        }

        fun endRow() {
            endField()
            rows.add(row)
            row = mutableListOf()
        }

        while (i < n) {
            val c = text[i]
            if (inQuotes) {
                if (c == '"') {
                    if (i + 1 < n && text[i + 1] == '"') {
                        field.append('"')
                        i += 2
                        continue
                    } else {
                        inQuotes = false
                        i += 1
                        continue
                    }
                } else {
                    field.append(c)
                    i += 1
                    continue
                }
            } else {
                when (c) {
                    '"' -> {
                        inQuotes = true
                        i += 1
                    }
                    ',' -> {
                        endField()
                        i += 1
                    }
                    '\r' -> {
                        i += 1
                    }
                    '\n' -> {
                        endRow()
                        i += 1
                    }
                    else -> {
                        field.append(c)
                        i += 1
                    }
                }
            }
        }
        // Final field/row if the text doesn't end with a newline
        if (field.isNotEmpty() || row.isNotEmpty()) {
            endRow()
        }
        return rows.filter { r -> r.isNotEmpty() && !(r.size == 1 && r[0].isBlank()) }
    }

    // ---------- Template & export ----------

    fun generateTemplate(includeSampleData: Boolean): String {
        val sb = StringBuilder()
        INSTRUCTION_LINES.forEach { sb.append(it).append('\n') }
        sb.append(writeCsvRow(CSV_HEADERS)).append('\n')
        if (includeSampleData) {
            sb.append(
                writeCsvRow(
                    listOf(
                        "", "Sugar 2KG", "SUG002", "6161234500001", "Groceries", "Mumias", "KG",
                        "KG", "1", "",
                        "TRUE", "TRUE", "FALSE",
                        "150", "180", "165",
                        "50", "ADD", "10", "", "TRUE"
                    )
                )
            ).append('\n')
            sb.append(
                writeCsvRow(
                    listOf(
                        "", "Sugar 2KG", "SUG002", "6161234500001", "Groceries", "Mumias", "KG",
                        "50 KG Sack", "50", "",
                        "TRUE", "TRUE", "TRUE",
                        "6000", "", "",
                        "1", "ADD", "", "", "TRUE"
                    )
                )
            ).append('\n')
            sb.append(
                writeCsvRow(
                    listOf(
                        "", "Sweets", "SWE001", "", "Confectionery", "", "Piece",
                        "Piece", "1", "",
                        "TRUE", "TRUE", "FALSE",
                        "6", "10", "8",
                        "600", "SET", "50", "", "TRUE"
                    )
                )
            ).append('\n')
            sb.append(
                writeCsvRow(
                    listOf(
                        "", "Sweets", "SWE001", "", "Confectionery", "", "Piece",
                        "Pack", "30", "",
                        "TRUE", "TRUE", "FALSE",
                        "", "320", "290",
                        "0", "NO_CHANGE", "", "", "TRUE"
                    )
                )
            ).append('\n')
            sb.append(
                writeCsvRow(
                    listOf(
                        "", "Sweets", "SWE001", "", "Confectionery", "", "Piece",
                        "Carton", "600", "Pack",
                        "TRUE", "TRUE", "FALSE",
                        "", "2200", "2050",
                        "0", "NO_CHANGE", "", "", "TRUE"
                    )
                )
            ).append('\n')
        }
        return sb.toString()
    }

    fun generateExport(products: List<Product>, conversions: List<UnitConversion>): String {
        val sb = StringBuilder()
        sb.append(writeCsvRow(CSV_HEADERS)).append('\n')
        val byProduct = conversions.filter { it.isActive }.groupBy { it.productId }
        products.filter { !it.isArchived }.sortedBy { it.name }.forEach { p ->
            sb.append(
                writeCsvRow(
                    listOf(
                        p.id.toString(), p.name, p.sku, p.barcode, p.categoryName, p.brand, p.baseUnit,
                        p.baseUnit, "1", "",
                        "TRUE", "TRUE", "FALSE",
                        formatNum(p.buyingCost), formatNum(p.retailPrice), formatNum(p.wholesalePrice),
                        formatNum(p.currentStockBase), "SET", formatNum(p.minStock), "", (!p.isArchived).toString().uppercase(Locale.US)
                    )
                )
            ).append('\n')
            byProduct[p.id]?.forEach { uc ->
                sb.append(
                    writeCsvRow(
                        listOf(
                            p.id.toString(), p.name, uc.sku, uc.barcode, p.categoryName, p.brand, p.baseUnit,
                            uc.unitName, formatNum(uc.conversionFactor), uc.parentUnitName,
                            uc.purchaseEnabled.toString().uppercase(Locale.US),
                            uc.sellingEnabled.toString().uppercase(Locale.US),
                            uc.canOpen.toString().uppercase(Locale.US),
                            uc.purchaseCost?.let { formatNum(it) } ?: "",
                            uc.customRetailPrice?.let { formatNum(it) } ?: "",
                            uc.customWholesalePrice?.let { formatNum(it) } ?: "",
                            "0", "NO_CHANGE", "", "", "TRUE"
                        )
                    )
                ).append('\n')
            }
        }
        return sb.toString()
    }

    private fun formatNum(value: Double): String {
        return if (value == value.toLong().toDouble()) value.toLong().toString() else value.toString()
    }

    // ---------- Validation / preview ----------

    enum class Severity { WARNING, ERROR }
    data class Issue(val message: String, val severity: Severity)

    data class RowPlan(
        val rowNumber: Int,
        val productKey: String,
        val productName: String,
        val packagingName: String,
        val isProductRow: Boolean,
        val isNewProduct: Boolean,
        val isNewPackaging: Boolean,
        val matchedProductId: Long?,
        val stockOperation: String,
        val stockDeltaBase: Double,
        val priceChanged: Boolean,
        val willDeactivate: Boolean,
        val issues: List<Issue>,
        val resolvedProduct: Product?,
        val resolvedConversion: UnitConversion?
    ) {
        val hasErrors: Boolean get() = issues.any { it.severity == Severity.ERROR }
    }

    data class Preview(
        val batchCode: String,
        val filename: String,
        val rows: List<RowPlan>,
        val newProducts: Int,
        val updatedProducts: Int,
        val stockAdditions: Double,
        val stockReductions: Double,
        val priceChanges: Int,
        val packagingChanges: Int,
        val deactivations: Int,
        val warningsCount: Int,
        val errorsCount: Int
    ) {
        val canImport: Boolean get() = errorsCount == 0 && rows.isNotEmpty()
    }

    private fun newBatchCode(): String =
        "IMP-${SimpleDateFormat("yyyyMMdd-HHmmss", Locale.US).format(Date())}"

    fun parseAndValidate(
        csvText: String,
        filename: String,
        existingProducts: List<Product>,
        existingConversions: List<UnitConversion>
    ): Preview {
        val allRows = parseCsv(csvText).filter { it.isNotEmpty() && !it[0].trimStart().startsWith("#") }
        if (allRows.isEmpty()) {
            return Preview(newBatchCode(), filename, emptyList(), 0, 0, 0.0, 0.0, 0, 0, 0, 0, 0)
        }
        val header = allRows.first().map { it.trim() }
        val dataRows = allRows.drop(1)

        fun colIndex(name: String): Int = header.indexOf(name)
        val idxId = colIndex("Product ID")
        val idxName = colIndex("Product Name")
        val idxSku = colIndex("SKU")
        val idxBarcode = colIndex("Barcode")
        val idxCategory = colIndex("Category")
        val idxBrand = colIndex("Brand")
        val idxBaseUnit = colIndex("Base Unit")
        val idxPackName = colIndex("Packaging Name")
        val idxPackQty = colIndex("Packaging Quantity")
        val idxParentPack = colIndex("Parent Packaging")
        val idxPurchaseEnabled = colIndex("Purchase Enabled")
        val idxSellingEnabled = colIndex("Selling Enabled")
        val idxCanOpen = colIndex("Can Open")
        val idxPurchaseCost = colIndex("Purchase Cost")
        val idxRetailPrice = colIndex("Retail Price")
        val idxWholesalePrice = colIndex("Wholesale Price")
        val idxStockQty = colIndex("Stock Quantity")
        val idxStockOp = colIndex("Stock Operation")
        val idxReorderLevel = colIndex("Reorder Level")
        val idxActive = colIndex("Active")

        if (idxName < 0 || idxBaseUnit < 0 || idxPackName < 0 || idxPackQty < 0 || idxStockOp < 0) {
            return Preview(
                newBatchCode(), filename,
                listOf(
                    RowPlan(
                        1, "", "", "", true, false, false, null, "NO_CHANGE", 0.0, false, false,
                        listOf(Issue("Missing required column(s). Expected at least: Product Name, Base Unit, Packaging Name, Packaging Quantity, Stock Operation.", Severity.ERROR)),
                        null, null
                    )
                ),
                0, 0, 0.0, 0.0, 0, 0, 0, 0, 1
            )
        }

        fun cell(r: List<String>, idx: Int): String = if (idx in r.indices) r[idx].trim() else ""
        fun boolCell(r: List<String>, idx: Int, default: Boolean): Boolean {
            val v = cell(r, idx)
            return when (v.uppercase(Locale.US)) {
                "TRUE", "YES", "1" -> true
                "FALSE", "NO", "0" -> false
                else -> default
            }
        }

        val productsBySku = existingProducts.filter { it.sku.isNotBlank() }.associateBy { it.sku.trim().lowercase(Locale.US) }
        val productsByBarcode = existingProducts.filter { it.barcode.isNotBlank() }.associateBy { it.barcode.trim() }
        val productsById = existingProducts.associateBy { it.id }
        val conversionsByProduct = existingConversions.groupBy { it.productId }
        val seenKeysInFile = mutableSetOf<String>() // dedupe key = productKey|packagingName

        // Track products created/updated within this file so subsequent packaging rows for
        // the same new product resolve against it, and so we don't double-count.
        val fileProductByKey = mutableMapOf<String, Product>()
        val fileIsNewByKey = mutableMapOf<String, Boolean>()
        val plans = mutableListOf<RowPlan>()

        for ((offset, r) in dataRows.withIndex()) {
            val rowNumber = offset + 2 // account for header row, 1-indexed for the person
            if (r.all { it.isBlank() }) continue

            val issues = mutableListOf<Issue>()
            val productIdText = cell(r, idxId)
            val name = cell(r, idxName)
            val sku = cell(r, idxSku)
            val barcode = cell(r, idxBarcode)
            val category = cell(r, idxCategory)
            val brand = cell(r, idxBrand)
            val baseUnit = cell(r, idxBaseUnit)
            val packagingName = cell(r, idxPackName).ifBlank { baseUnit }
            val packagingQtyText = cell(r, idxPackQty)
            val parentPackaging = cell(r, idxParentPack)
            val purchaseEnabled = boolCell(r, idxPurchaseEnabled, true)
            val sellingEnabled = boolCell(r, idxSellingEnabled, true)
            val canOpen = boolCell(r, idxCanOpen, false)
            val purchaseCostText = cell(r, idxPurchaseCost)
            val retailPriceText = cell(r, idxRetailPrice)
            val wholesalePriceText = cell(r, idxWholesalePrice)
            val stockQtyText = cell(r, idxStockQty)
            val stockOp = cell(r, idxStockOp).ifBlank { "NO_CHANGE" }.uppercase(Locale.US)
            val reorderLevelText = cell(r, idxReorderLevel)
            val active = boolCell(r, idxActive, true)

            if (name.isBlank()) {
                plans.add(
                    RowPlan(rowNumber, "", "", packagingName, true, false, false, null, stockOp, 0.0, false, false,
                        listOf(Issue("Product Name is required.", Severity.ERROR)), null, null)
                )
                continue
            }
            if (stockOp !in setOf("ADD", "ADJUST", "SET", "REMOVE", "NO_CHANGE")) {
                issues.add(Issue("Unknown Stock Operation '$stockOp' -- expected ADD, ADJUST, SET, REMOVE or NO_CHANGE.", Severity.ERROR))
            }
            val stockQty = stockQtyText.toDoubleOrNull()
            if (stockQtyText.isNotBlank() && stockQty == null) {
                issues.add(Issue("Stock Quantity '$stockQtyText' is not a valid number.", Severity.ERROR))
            }
            val packagingQty = packagingQtyText.toDoubleOrNull()
            if (packagingQty == null || packagingQty <= 0) {
                issues.add(Issue("Packaging Quantity must be a positive number.", Severity.ERROR))
            }

            // ---- Match to an existing product: Product ID > SKU > Barcode > Name (within-file) ----
            var matchedProduct: Product? = null
            var matchSource = ""
            if (productIdText.isNotBlank()) {
                val id = productIdText.toLongOrNull()
                if (id == null) {
                    issues.add(Issue("Product ID '$productIdText' is not a valid number.", Severity.ERROR))
                } else {
                    matchedProduct = productsById[id]
                    if (matchedProduct == null) {
                        issues.add(Issue("Product ID $id does not match any existing product.", Severity.ERROR))
                    } else {
                        matchSource = "id"
                    }
                }
            }
            if (matchedProduct == null && sku.isNotBlank()) {
                matchedProduct = productsBySku[sku.lowercase(Locale.US)]
                if (matchedProduct != null) matchSource = "sku"
            }
            if (matchedProduct == null && barcode.isNotBlank()) {
                matchedProduct = productsByBarcode[barcode]
                if (matchedProduct != null) matchSource = "barcode"
            }

            // Conflict check: SKU matches one product but Barcode on this row belongs to a DIFFERENT product.
            if (matchSource == "sku" && barcode.isNotBlank()) {
                val byBarcode = productsByBarcode[barcode]
                if (byBarcode != null && matchedProduct != null && byBarcode.id != matchedProduct.id) {
                    issues.add(
                        Issue(
                            "SKU '$sku' matches ${matchedProduct.name} (#${matchedProduct.id}) but Barcode '$barcode' " +
                                "belongs to a different product (${byBarcode.name}, #${byBarcode.id}). Correct one of them and re-import.",
                            Severity.ERROR
                        )
                    )
                }
            }

            // Product key for grouping rows within this file: prefer the matched DB product's id,
            // else fall back to SKU, else Name -- so multiple rows for one new product (no SKU yet)
            // still group together as long as the Product Name matches exactly.
            val productKey = when {
                matchedProduct != null -> "id:${matchedProduct.id}"
                sku.isNotBlank() -> "sku:${sku.lowercase(Locale.US)}"
                else -> "name:${name.trim().lowercase(Locale.US)}"
            }

            val isProductRow = packagingName.equals(baseUnit, ignoreCase = true) ||
                (baseUnit.isBlank() && fileProductByKey[productKey] == null)

            val dedupeKey = "$productKey|${packagingName.lowercase(Locale.US)}"
            if (!seenKeysInFile.add(dedupeKey)) {
                issues.add(Issue("Duplicate row: '$name' / '$packagingName' already appears earlier in this file.", Severity.ERROR))
            }

            var resolvedProduct: Product? = null
            var isNewProduct = false
            var stockDeltaBase = 0.0
            var priceChanged = false
            var willDeactivate = false

            if (isProductRow) {
                val existing = matchedProduct ?: fileProductByKey[productKey]
                isNewProduct = existing == null
                val base = existing ?: Product(name = name, baseUnit = baseUnit.ifBlank { "Piece" })
                val newBuyingCost = purchaseCostText.toDoubleOrNull() ?: base.buyingCost
                val newRetail = retailPriceText.toDoubleOrNull() ?: base.retailPrice
                val newWholesale = wholesalePriceText.toDoubleOrNull() ?: base.wholesalePrice
                if (purchaseCostText.isNotBlank() && newBuyingCost != base.buyingCost) priceChanged = true
                if (retailPriceText.isNotBlank() && newRetail != base.retailPrice) priceChanged = true
                if (wholesalePriceText.isNotBlank() && newWholesale != base.wholesalePrice) priceChanged = true

                val currentStock = base.currentStockBase
                var newStock = currentStock
                if (stockQty != null) {
                    when (stockOp) {
                        "ADD" -> newStock = currentStock + stockQty
                        "ADJUST" -> newStock = currentStock + stockQty
                        "SET" -> newStock = stockQty
                        "REMOVE" -> {
                            newStock = currentStock - stockQty
                            if (newStock < 0) {
                                issues.add(Issue("REMOVE would take stock below zero (current $currentStock, removing $stockQty). Negative inventory isn't enabled.", Severity.ERROR))
                                newStock = currentStock
                            }
                        }
                        "NO_CHANGE" -> newStock = currentStock
                    }
                }
                stockDeltaBase = newStock - currentStock

                if (!active && (existing?.isArchived == false)) willDeactivate = true

                resolvedProduct = base.copy(
                    name = name,
                    sku = if (sku.isNotBlank()) sku else base.sku,
                    barcode = if (barcode.isNotBlank()) barcode else base.barcode,
                    categoryName = if (category.isNotBlank()) category else base.categoryName,
                    brand = if (brand.isNotBlank()) brand else base.brand,
                    baseUnit = if (baseUnit.isNotBlank()) baseUnit else base.baseUnit,
                    buyingCost = newBuyingCost,
                    retailPrice = newRetail,
                    wholesalePrice = newWholesale,
                    currentStockBase = newStock,
                    minStock = reorderLevelText.toDoubleOrNull() ?: base.minStock,
                    isArchived = !active
                )
                if (issues.none { it.severity == Severity.ERROR }) {
                    fileProductByKey[productKey] = resolvedProduct
                    fileIsNewByKey[productKey] = isNewProduct
                }
            } else {
                if (issues.none { it.severity == Severity.ERROR }) {
                    val ownerProduct = matchedProduct ?: fileProductByKey[productKey]
                    if (ownerProduct == null) {
                        issues.add(Issue("No product row found for '$name' before this packaging row -- add a row where Packaging Name = Base Unit ('$baseUnit') first.", Severity.ERROR))
                    } else {
                        val existingConv = conversionsByProduct[ownerProduct.id]?.firstOrNull { it.unitName.equals(packagingName, ignoreCase = true) }
                        val isNewPackaging = existingConv == null
                        val newRetail = retailPriceText.toDoubleOrNull() ?: existingConv?.customRetailPrice
                        val newWholesale = wholesalePriceText.toDoubleOrNull() ?: existingConv?.customWholesalePrice
                        val newPurchaseCost = purchaseCostText.toDoubleOrNull() ?: existingConv?.purchaseCost
                        if (retailPriceText.isNotBlank() && newRetail != existingConv?.customRetailPrice) priceChanged = true
                        if (wholesalePriceText.isNotBlank() && newWholesale != existingConv?.customWholesalePrice) priceChanged = true

                        val conv = (existingConv ?: UnitConversion(
                            productId = ownerProduct.id,
                            unitName = packagingName,
                            conversionFactor = packagingQty ?: 1.0
                        )).copy(
                            unitName = packagingName,
                            conversionFactor = packagingQty ?: (existingConv?.conversionFactor ?: 1.0),
                            customRetailPrice = newRetail,
                            customWholesalePrice = newWholesale,
                            purchaseCost = newPurchaseCost,
                            barcode = if (barcode.isNotBlank()) barcode else existingConv?.barcode ?: "",
                            sku = if (sku.isNotBlank()) sku else existingConv?.sku ?: "",
                            purchaseEnabled = purchaseEnabled,
                            sellingEnabled = sellingEnabled,
                            canOpen = canOpen,
                            parentUnitName = parentPackaging,
                            isActive = active
                        )

                        // Stock for a packaging row is expressed in that packaging's own units,
                        // converted to base units via its conversion factor.
                        if (stockQty != null && stockOp != "NO_CHANGE") {
                            val factor = conv.conversionFactor
                            val deltaInThisUnit = when (stockOp) {
                                "ADD", "ADJUST" -> stockQty
                                "REMOVE" -> -stockQty
                                "SET" -> null // handled specially: SET on a packaging row sets that form's own count, not directly comparable here
                                else -> 0.0
                            }
                            if (stockOp == "SET") {
                                issues.add(Issue("Stock Operation SET on a packaging row (not the product's main row) isn't supported -- use ADD/ADJUST/REMOVE here, or SET on the main product row.", Severity.WARNING))
                            } else if (deltaInThisUnit != null) {
                                val deltaBase = deltaInThisUnit * factor
                                if (deltaBase < 0 && (ownerProduct.currentStockBase + deltaBase) < 0) {
                                    issues.add(Issue("REMOVE would take stock below zero for '$packagingName'.", Severity.ERROR))
                                } else {
                                    stockDeltaBase = deltaBase
                                }
                            }
                        }

                        resolvedProduct = ownerProduct
                        plans.add(
                            RowPlan(
                                rowNumber, productKey, name, packagingName, false, false, isNewPackaging,
                                ownerProduct.id, stockOp, stockDeltaBase, priceChanged, false, issues,
                                null, conv
                            )
                        )
                        continue
                    }
                }
            }

            if (reorderLevelText.isNotBlank() && reorderLevelText.toDoubleOrNull() == null) {
                issues.add(Issue("Reorder Level '$reorderLevelText' is not a valid number.", Severity.WARNING))
            }
            if (isProductRow && matchedProduct == null && fileIsNewByKey[productKey] == true && sku.isBlank() && barcode.isBlank()) {
                issues.add(Issue("New product has no SKU or Barcode -- future imports won't be able to match it reliably by anything but exact name.", Severity.WARNING))
            }

            plans.add(
                RowPlan(
                    rowNumber, productKey, name, packagingName, isProductRow, isNewProduct, false,
                    matchedProduct?.id, stockOp, stockDeltaBase, priceChanged, willDeactivate, issues,
                    resolvedProduct, null
                )
            )
        }

        val newProducts = plans.count { it.isProductRow && it.isNewProduct && !it.hasErrors }
        val updatedProducts = plans.count { it.isProductRow && !it.isNewProduct && it.matchedProductId != null && !it.hasErrors }
        val stockAdditions = plans.filter { !it.hasErrors && it.stockDeltaBase > 0 }.sumOf { it.stockDeltaBase }
        val stockReductions = plans.filter { !it.hasErrors && it.stockDeltaBase < 0 }.sumOf { -it.stockDeltaBase }
        val priceChanges = plans.count { it.priceChanged && !it.hasErrors }
        val packagingChanges = plans.count { !it.isProductRow && !it.hasErrors }
        val deactivations = plans.count { it.willDeactivate && !it.hasErrors }
        val warningsCount = plans.sumOf { p -> p.issues.count { it.severity == Severity.WARNING } }
        val errorsCount = plans.sumOf { p -> p.issues.count { it.severity == Severity.ERROR } }

        return Preview(
            newBatchCode(), filename, plans,
            newProducts, updatedProducts, stockAdditions, stockReductions,
            priceChanges, packagingChanges, deactivations, warningsCount, errorsCount
        )
    }

    // ---------- Apply ----------

    /**
     * Applies a validated (error-free) preview as a single database transaction -- if
     * anything throws partway through, Room's withTransaction rolls the whole import back,
     * so a mid-import failure never leaves the database partially updated. Every stock
     * change goes through the normal stock_movements ledger (IMPORT_ADD / IMPORT_REMOVE /
     * IMPORT_ADJUST / IMPORT_SET), tagged with this batch's code, so it stays auditable
     * exactly like a POS sale or manual purchase would be.
     */
    suspend fun applyImport(
        db: AppDatabase,
        preview: Preview,
        userName: String
    ): ImportBatch {
        require(preview.canImport) { "Cannot apply an import with blocking errors." }

        var result = ImportBatch(
            batchCode = preview.batchCode,
            filename = preview.filename,
            importedByUserName = userName,
            mode = "FULL_IMPORT",
            totalRows = preview.rows.size,
            status = "FAILED"
        )

        db.withTransaction {
            val idByProductKey = mutableMapOf<String, Long>()

            // Pass 1: product rows first, so packaging rows can resolve the product id.
            for (plan in preview.rows) {
                if (!plan.isProductRow) continue
                val product = plan.resolvedProduct ?: continue
                val productId: Long = if (plan.matchedProductId != null) {
                    db.productDao().updateProduct(product.copy(id = plan.matchedProductId))
                    plan.matchedProductId
                } else {
                    db.productDao().insertProduct(product)
                }
                idByProductKey[plan.productKey] = productId

                if (plan.stockDeltaBase != 0.0) {
                    val movementType = "IMPORT_" + plan.stockOperation
                    val after = product.currentStockBase
                    db.stockMovementDao().insertMovement(
                        StockMovement(
                            productId = productId,
                            productName = product.name,
                            movementType = movementType,
                            quantityChangeBase = plan.stockDeltaBase,
                            unitUsed = product.baseUnit,
                            quantityInUnit = plan.stockDeltaBase,
                            stockAfterBase = after,
                            referenceId = preview.batchCode,
                            reason = "Spreadsheet Import: ${preview.filename}",
                            staffName = userName
                        )
                    )
                }
            }

            // Pass 2: packaging rows.
            for (plan in preview.rows) {
                if (plan.isProductRow) continue
                val conv = plan.resolvedConversion ?: continue
                val productId = plan.matchedProductId ?: idByProductKey[plan.productKey] ?: continue
                val resolvedConv = conv.copy(productId = productId)
                if (resolvedConv.id != 0L) {
                    db.productDao().updateUnitConversion(resolvedConv)
                } else {
                    db.productDao().insertUnitConversion(resolvedConv)
                }

                if (plan.stockDeltaBase != 0.0) {
                    val product = db.productDao().getProductById(productId)
                    val newBase = (product?.currentStockBase ?: 0.0) + plan.stockDeltaBase
                    if (product != null) {
                        db.productDao().updateProduct(product.copy(currentStockBase = newBase))
                    }
                    db.stockMovementDao().insertMovement(
                        StockMovement(
                            productId = productId,
                            productName = plan.productName,
                            movementType = "IMPORT_" + plan.stockOperation,
                            quantityChangeBase = plan.stockDeltaBase,
                            unitUsed = plan.packagingName,
                            quantityInUnit = plan.stockDeltaBase / (if (conv.conversionFactor != 0.0) conv.conversionFactor else 1.0),
                            stockAfterBase = newBase,
                            referenceId = preview.batchCode,
                            reason = "Spreadsheet Import: ${preview.filename}",
                            staffName = userName
                        )
                    )
                }
            }

            db.auditLogDao().insertLog(
                AuditLog(
                    staffName = userName,
                    action = "Spreadsheet Import (${preview.batchCode})",
                    recordInfo = "${preview.filename}: ${preview.newProducts} new, ${preview.updatedProducts} updated, " +
                        "${preview.rows.size} rows"
                )
            )

            result = result.copy(
                productsCreated = preview.newProducts,
                productsUpdated = preview.updatedProducts,
                packagingChanges = preview.packagingChanges,
                stockAdded = preview.stockAdditions,
                stockRemoved = preview.stockReductions,
                pricesUpdated = preview.priceChanges,
                deactivations = preview.deactivations,
                warningsCount = preview.warningsCount,
                errorsCount = preview.errorsCount,
                status = "COMPLETED"
            )
            val batchId = db.importBatchDao().insertBatch(result)
            result = result.copy(id = batchId)
        }

        return result
    }
}
