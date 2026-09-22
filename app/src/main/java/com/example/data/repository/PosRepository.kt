package com.example.data.repository

import androidx.room.withTransaction
import com.example.data.database.AppDatabase
import com.example.data.model.*
import kotlinx.coroutines.flow.Flow
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class PosRepository(val db: AppDatabase) {

    val businessFlow: Flow<Business?> = db.businessDao().getBusinessFlow()
    val allProducts: Flow<List<Product>> = db.productDao().getAllProducts()
    val lowStockProducts: Flow<List<Product>> = db.productDao().getLowStockProducts()
    val allCustomers: Flow<List<Customer>> = db.customerDao().getAllCustomers()
    val allSuppliers: Flow<List<Supplier>> = db.supplierDao().getAllSuppliers()
    val recentSales: Flow<List<Sale>> = db.saleDao().getRecentCompletedSales()
    val allCompletedSales: Flow<List<Sale>> = db.saleDao().getAllCompletedSalesFlow()
    val allSaleItems: Flow<List<SaleItem>> = db.saleDao().getAllSaleItemsFlow()
    val heldSales: Flow<List<Sale>> = db.saleDao().getHeldSales()
    val allExpenses: Flow<List<Expense>> = db.expenseDao().getAllExpenses()
    val activeCashSession: Flow<CashSession?> = db.cashSessionDao().getActiveSessionFlow()
    val allCashSessions: Flow<List<CashSession>> = db.cashSessionDao().getAllSessions()
    val allMovements: Flow<List<StockMovement>> = db.stockMovementDao().getAllMovements()
    val allPurchases: Flow<List<Purchase>> = db.purchaseDao().getAllPurchases()
    val allReturns: Flow<List<ReturnOrder>> = db.returnDao().getAllReturns()
    val allUsers: Flow<List<User>> = db.userDao().getAllUsersFlow()
    val auditLogs: Flow<List<AuditLog>> = db.auditLogDao().getAllLogs()
    val mpesaRecons: Flow<List<MpesaRecon>> = db.mpesaReconDao().getAllReconciliations()
    val allMpesaPayments: Flow<List<Payment>> = db.paymentDao().getMpesaPaymentsFlow()
    val allPayments: Flow<List<Payment>> = db.paymentDao().getAllPaymentsFlow()

    suspend fun saveStaff(user: User): Long = db.userDao().insertUser(user)

    suspend fun getBusiness(): Business? = db.businessDao().getBusiness()

    suspend fun saveBusiness(business: Business) {
        db.businessDao().insertOrUpdate(business)
        db.auditLogDao().insertLog(
            AuditLog(
                staffName = business.ownerName.ifBlank { "Owner" },
                action = "Configured Business",
                recordInfo = "${business.name} (${business.location})"
            )
        )
    }

    suspend fun authenticatePin(pin: String): Pair<Boolean, String> {
        val biz = getBusiness()
        if (biz != null && biz.ownerPin == pin) {
            return Pair(true, biz.ownerName.ifBlank { "Owner" })
        }
        val user = db.userDao().getUserByPin(pin)
        if (user != null) {
            return Pair(true, "${user.fullName} (${user.role})")
        }
        return Pair(false, "")
    }

    // Product & Unit Conversions / Packaging Profiles
    suspend fun getProductById(id: Long): Product? = db.productDao().getProductById(id)
    suspend fun getProductByBarcode(barcode: String): Product? = db.productDao().getProductByBarcode(barcode)
    fun searchProducts(query: String): Flow<List<Product>> = db.productDao().searchProducts(query)
    fun getUnitConversions(productId: Long): Flow<List<UnitConversion>> = db.productDao().getUnitConversionsForProduct(productId)
    suspend fun getUnitConversionsSync(productId: Long): List<UnitConversion> = db.productDao().getUnitConversionsSync(productId)
    suspend fun getAllPackagingProfilesWithBarcode(): List<UnitConversion> = db.productDao().getAllPackagingProfilesWithBarcode()

    suspend fun findProductAndPackagingByBarcode(barcode: String): Pair<Product, UnitConversion?>? {
        val cleanBarcode = barcode.trim()
        if (cleanBarcode.isBlank()) return null

        // 1. Check if barcode matches a specific packaging profile
        val profile = db.productDao().getPackagingProfileByBarcode(cleanBarcode)
        if (profile != null) {
            val product = db.productDao().getProductById(profile.productId)
            if (product != null && !product.isArchived) {
                return Pair(product, profile)
            }
        }

        // 2. Check if barcode matches a product-level barcode
        val product = db.productDao().getProductByBarcode(cleanBarcode)
        if (product != null && !product.isArchived) {
            return Pair(product, null)
        }

        return null
    }

    suspend fun saveProduct(product: Product): Long {
        val isNew = product.id == 0L
        val productId = if (isNew) {
            val id = db.productDao().insertProduct(product)
            db.auditLogDao().insertLog(
                AuditLog(
                    staffName = "System",
                    action = "Added Product",
                    recordInfo = "${product.name} (Stock: ${product.currentStockBase} ${product.baseUnit})"
                )
            )
            // Initial stock movement if any
            if (product.currentStockBase > 0) {
                db.stockMovementDao().insertMovement(
                    StockMovement(
                        productId = id,
                        productName = product.name,
                        movementType = "INITIAL",
                        quantityChangeBase = product.currentStockBase,
                        unitUsed = product.baseUnit,
                        quantityInUnit = product.currentStockBase,
                        stockAfterBase = product.currentStockBase,
                        reason = "Initial Stock Setup",
                        staffName = "System"
                    )
                )
            }
            id
        } else {
            db.productDao().updateProduct(product)
            db.auditLogDao().insertLog(
                AuditLog(
                    staffName = "System",
                    action = "Updated Product",
                    recordInfo = product.name
                )
            )
            product.id
        }
        return productId
    }

    suspend fun addUnitConversion(conversion: UnitConversion) {
        db.productDao().insertUnitConversion(conversion)
    }

    suspend fun updateUnitConversion(conversion: UnitConversion) {
        db.productDao().updateUnitConversion(conversion)
    }

    suspend fun deleteUnitConversion(id: Long) {
        db.productDao().deleteUnitConversion(id)
    }

    // Auto-backup scheduling preferences (persisted so they survive app restarts;
    // the WorkManager schedule itself also persists independently, but this lets the
    // Backup & Restore screen show the user's last-saved choice on reopen).
    suspend fun getAutoBackupFrequency(): String =
        db.appSettingDao().getSetting("auto_backup_frequency") ?: "OFF"

    suspend fun getAutoBackupRetentionCount(): Int =
        db.appSettingDao().getSetting("auto_backup_retention_count")?.toIntOrNull() ?: 5

    suspend fun saveAutoBackupSettings(frequency: String, retentionCount: Int) {
        db.appSettingDao().insertSettings(
            listOf(
                AppSetting(key = "auto_backup_frequency", value = frequency),
                AppSetting(key = "auto_backup_retention_count", value = retentionCount.toString())
            )
        )
    }

    // Critical Package Opening / Break-Bulk Engine
    suspend fun openPackage(
        productId: Long,
        profileId: Long,
        countToOpen: Int,
        staffName: String
    ): Result<String> = db.withTransaction {
        val product = db.productDao().getProductById(productId)
            ?: return@withTransaction Result.failure(Exception("Product not found"))
        val profile = db.productDao().getUnitConversionsSync(productId).firstOrNull { it.id == profileId }
            ?: return@withTransaction Result.failure(Exception("Packaging profile not found"))

        if (profile.intactCount < countToOpen) {
            return@withTransaction Result.failure(
                Exception("Cannot open $countToOpen: only ${profile.intactCount} intact package(s) available")
            )
        }

        // Deduct intact packages count
        db.productDao().updateIntactCount(profileId, -countToOpen)

        val looseBaseUnitsReleased = countToOpen * profile.conversionFactor

        // Record stock movement as PACKAGE_OPENED with full audit trail
        db.stockMovementDao().insertMovement(
            StockMovement(
                productId = product.id,
                productName = product.name,
                movementType = "PACKAGE_OPENED",
                quantityChangeBase = 0.0, // Base inventory total is unchanged; form converted to loose
                unitUsed = profile.unitName,
                quantityInUnit = countToOpen.toDouble(),
                stockAfterBase = product.currentStockBase,
                referenceId = "OPEN-PKG-${System.currentTimeMillis() % 100000}",
                reason = "Opened $countToOpen × ${profile.unitName} into ${looseBaseUnitsReleased.toInt()} loose ${product.baseUnit}s",
                staffName = staffName
            )
        )

        db.auditLogDao().insertLog(
            AuditLog(
                staffName = staffName,
                action = "PACKAGE_OPENED",
                recordInfo = "${product.name}: Opened $countToOpen × ${profile.unitName} (Released ${looseBaseUnitsReleased.toInt()} ${product.baseUnit}s to loose stock)"
            )
        )

        return@withTransaction Result.success(
            "Opened $countToOpen × ${profile.unitName}. ${looseBaseUnitsReleased.toInt()} loose ${product.baseUnit}s are now available."
        )
    }

    // Critical Inventory Conversion Engine (Part 39)
    suspend fun convertToUnits(productId: Long, unitName: String, quantity: Double): Double {
        val product = db.productDao().getProductById(productId) ?: return quantity
        if (unitName.equals(product.baseUnit, ignoreCase = true)) {
            return quantity
        }
        val conversions = db.productDao().getUnitConversionsSync(productId)
        val match = conversions.firstOrNull { it.unitName.equals(unitName, ignoreCase = true) }
        return if (match != null && match.conversionFactor > 0) {
            quantity * match.conversionFactor
        } else {
            quantity
        }
    }

    // Atomic Sale Transaction Integrity (Part 40)
    suspend fun completeSale(
        sale: Sale,
        items: List<SaleItem>,
        payments: List<Payment>,
        cashier: String
    ): Long = db.withTransaction {
        // 1. Create Sale
        val saleId = db.saleDao().insertSale(sale)

        // 2. Insert Sale Items with reference
        val itemsWithSaleId = items.map { it.copy(saleId = saleId) }
        db.saleDao().insertSaleItems(itemsWithSaleId)

        // 3. Record Payments
        val paymentsWithSaleId = payments.map { it.copy(saleId = saleId) }
        db.paymentDao().insertPayments(paymentsWithSaleId)

        // 4. Update Inventory and 5. Create Stock Movement Records
        for (item in itemsWithSaleId) {
            val product = db.productDao().getProductById(item.productId)
            if (product != null) {
                val newStock = product.currentStockBase - item.baseQuantityDeducted
                db.productDao().updateStock(product.id, -item.baseQuantityDeducted)

                // If sold form matches a packaging profile with intact tracking, decrement intact count
                val conversions = db.productDao().getUnitConversionsSync(product.id)
                val matchingProfile = conversions.firstOrNull { it.unitName.equals(item.unitName, ignoreCase = true) }
                if (matchingProfile != null && matchingProfile.intactCount > 0) {
                    val toDeduct = minOf(matchingProfile.intactCount, item.quantity.toInt())
                    if (toDeduct > 0) {
                        db.productDao().updateIntactCount(matchingProfile.id, -toDeduct)
                    }
                }

                db.stockMovementDao().insertMovement(
                    StockMovement(
                        productId = product.id,
                        productName = product.name,
                        movementType = "SALE",
                        quantityChangeBase = -item.baseQuantityDeducted,
                        unitUsed = item.unitName,
                        quantityInUnit = item.quantity,
                        stockAfterBase = newStock,
                        referenceId = sale.receiptNumber,
                        reason = "Sale to ${sale.customerName} (${item.quantity.toInt()} × ${item.unitName})",
                        staffName = cashier
                    )
                )
            }
        }

        // 6. Update Customer Credit if credit payment was used
        val creditPayment = payments.firstOrNull { it.paymentMethod == "CREDIT" }
        if (creditPayment != null && creditPayment.amount > 0 && sale.customerId != null) {
            val customer = db.customerDao().getCustomerById(sale.customerId)
            if (customer != null) {
                val newCredit = customer.outstandingCredit + creditPayment.amount
                val newTotalPurchases = customer.totalPurchases + sale.total
                db.customerDao().updateCreditAndPurchases(customer.id, creditPayment.amount, sale.total)

                db.customerDao().insertTransaction(
                    CustomerTransaction(
                        customerId = customer.id,
                        customerName = customer.name,
                        type = "CREDIT_SALE",
                        amount = creditPayment.amount,
                        referenceId = sale.receiptNumber,
                        notes = "Sale credit balance",
                        balanceAfter = newCredit
                    )
                )
            }
        } else if (sale.customerId != null) {
            // Cash / M-Pesa sale with customer selected -> update total purchases
            db.customerDao().updateCreditAndPurchases(sale.customerId, 0.0, sale.total)
        }

        // Update Cash Session if active and Cash payment was made
        val cashPayment = payments.firstOrNull { it.paymentMethod == "CASH" }
        if (cashPayment != null && cashPayment.amount > 0) {
            val activeSession = db.cashSessionDao().getActiveSession()
            if (activeSession != null) {
                val updatedSession = activeSession.copy(
                    cashSales = activeSession.cashSales + cashPayment.amount,
                    expectedCash = activeSession.expectedCash + cashPayment.amount
                )
                db.cashSessionDao().updateSession(updatedSession)
            }
        }

        // 7. Audit Log
        db.auditLogDao().insertLog(
            AuditLog(
                staffName = cashier,
                action = "Completed Sale #${sale.receiptNumber}",
                recordInfo = "Amount: KES ${sale.total} (${sale.customerName})"
            )
        )

        saleId
    }

    // Held Sales Management (Part 13)
    suspend fun holdSale(sale: Sale, items: List<SaleItem>): Long = db.withTransaction {
        val heldSale = sale.copy(status = "HELD")
        val saleId = db.saleDao().insertSale(heldSale)
        val itemsWithId = items.map { it.copy(saleId = saleId) }
        db.saleDao().insertSaleItems(itemsWithId)
        db.auditLogDao().insertLog(
            AuditLog(
                staffName = sale.cashierName,
                action = "Held Sale #${sale.receiptNumber}",
                recordInfo = "${sale.customerName} (${items.size} items)"
            )
        )
        saleId
    }

    suspend fun deleteHeldSale(saleId: Long) = db.withTransaction {
        db.saleDao().deleteSale(saleId)
    }

    suspend fun getSaleDetails(saleId: Long): Pair<Sale?, List<SaleItem>> {
        val sale = db.saleDao().getSaleById(saleId)
        val items = db.saleDao().getSaleItems(saleId)
        return Pair(sale, items)
    }

    suspend fun getSaleByReceiptNumber(receiptNumber: String): Pair<Sale?, List<SaleItem>> {
        val sale = db.saleDao().getSaleByReceiptNumber(receiptNumber)
        val items = if (sale != null) db.saleDao().getSaleItems(sale.id) else emptyList()
        return Pair(sale, items)
    }

    suspend fun getPaymentsForSale(saleId: Long): List<Payment> = db.paymentDao().getPaymentsForSale(saleId)

    // Stock Take / Adjustment (Part 20)
    suspend fun applyStockTakeAdjustment(
        productId: Long,
        expectedStock: Double,
        actualStock: Double,
        staffName: String,
        reason: String
    ) = db.withTransaction {
        val product = db.productDao().getProductById(productId) ?: return@withTransaction
        val difference = actualStock - expectedStock
        db.productDao().updateStock(productId, difference)

        db.stockMovementDao().insertMovement(
            StockMovement(
                productId = productId,
                productName = product.name,
                movementType = if (difference < 0) "DAMAGE/SHRINKAGE" else "ADJUSTMENT",
                quantityChangeBase = difference,
                unitUsed = product.baseUnit,
                quantityInUnit = difference,
                stockAfterBase = actualStock,
                reason = reason.ifBlank { "Stock Take Physical Count (Diff: $difference)" },
                staffName = staffName
            )
        )

        db.auditLogDao().insertLog(
            AuditLog(
                staffName = staffName,
                action = "Stock Take Adjustment",
                recordInfo = "${product.name}: $expectedStock -> $actualStock (${if (difference >= 0) "+$difference" else "$difference"} ${product.baseUnit})"
            )
        )
    }

    // Purchases (Part 21 & 22)
    suspend fun receivePurchase(
        purchase: Purchase,
        items: List<PurchaseItem>,
        staffName: String
    ): Long = db.withTransaction {
        val purchaseId = db.purchaseDao().insertPurchase(purchase)
        val itemsWithId = items.map { it.copy(purchaseId = purchaseId) }
        db.purchaseDao().insertPurchaseItems(itemsWithId)

        val costingMethod = db.businessDao().getBusiness()?.costingMethod ?: "WEIGHTED_AVERAGE"

        for (item in itemsWithId) {
            val product = db.productDao().getProductById(item.productId)
            if (product != null) {
                val newStock = product.currentStockBase + item.baseUnitsAdded
                db.productDao().updateStock(product.id, item.baseUnitsAdded)

                // Recalculate cost-per-base-unit so profit/margin figures reflect actual
                // purchase cost rather than staying frozen at the product's original cost.
                if (item.baseUnitsAdded > 0) {
                    val newCostPerBase = item.totalCost / item.baseUnitsAdded
                    val updatedBuyingCost = when (costingMethod) {
                        "LAST_PURCHASE_COST" -> newCostPerBase
                        else -> { // WEIGHTED_AVERAGE (default)
                            if (product.currentStockBase <= 0.0) {
                                newCostPerBase
                            } else {
                                ((product.currentStockBase * product.buyingCost) + item.totalCost) / newStock
                            }
                        }
                    }
                    if (updatedBuyingCost != product.buyingCost) {
                        db.productDao().updateProduct(product.copy(buyingCost = updatedBuyingCost))
                    }
                }

                // Update intact count if receiving a packaging profile with intact tracking or canOpen
                val conversions = db.productDao().getUnitConversionsSync(product.id)
                val matchingProfile = conversions.firstOrNull { it.unitName.equals(item.unitName, ignoreCase = true) }
                if (matchingProfile != null && (matchingProfile.canOpen || matchingProfile.intactCount > 0)) {
                    db.productDao().updateIntactCount(matchingProfile.id, item.quantity.toInt())
                }

                db.stockMovementDao().insertMovement(
                    StockMovement(
                        productId = product.id,
                        productName = product.name,
                        movementType = "PURCHASE",
                        quantityChangeBase = item.baseUnitsAdded,
                        unitUsed = item.unitName,
                        quantityInUnit = item.quantity,
                        stockAfterBase = newStock,
                        referenceId = purchase.invoiceNumber,
                        reason = "Purchase from ${purchase.supplierName} (${item.quantity.toInt()} × ${item.unitName})",
                        staffName = staffName
                    )
                )
            }
        }

        db.auditLogDao().insertLog(
            AuditLog(
                staffName = staffName,
                action = "Received Stock (Purchase #${purchase.invoiceNumber})",
                recordInfo = "Total: KES ${purchase.totalAmount} from ${purchase.supplierName}"
            )
        )
        purchaseId
    }

    // Returns & Refunds (Part 32)
    suspend fun processReturn(
        originalSale: Sale,
        saleItem: SaleItem,
        returnQty: Double,
        refundAmount: Double,
        baseQuantityToRestore: Double,
        reason: String,
        authorizedBy: String
    ): Long = db.withTransaction {
        val returnNum = "RET-${System.currentTimeMillis().toString().takeLast(6)}"
        val returnOrder = ReturnOrder(
            returnNumber = returnNum,
            originalSaleId = originalSale.id,
            originalReceiptNumber = originalSale.receiptNumber,
            customerName = originalSale.customerName,
            totalRefundAmount = refundAmount,
            reason = reason,
            authorizedBy = authorizedBy
        )
        val returnOrderId = db.returnDao().insertReturnOrder(returnOrder)

        val returnItem = ReturnItem(
            returnOrderId = returnOrderId,
            saleItemId = saleItem.id,
            productId = saleItem.productId,
            productName = saleItem.productName,
            unitName = saleItem.unitName,
            quantity = returnQty,
            refundUnitPrice = saleItem.unitPrice,
            totalRefund = refundAmount,
            baseUnitsRestored = baseQuantityToRestore
        )
        db.returnDao().insertReturnItems(listOf(returnItem))

        // Update original sale item returned qty
        db.saleDao().updateSaleItem(saleItem.copy(returnedQuantity = saleItem.returnedQuantity + returnQty))

        // Restore stock in base units
        val product = db.productDao().getProductById(saleItem.productId)
        if (product != null) {
            val newStock = product.currentStockBase + baseQuantityToRestore
            db.productDao().updateStock(product.id, baseQuantityToRestore)

            db.stockMovementDao().insertMovement(
                StockMovement(
                    productId = product.id,
                    productName = product.name,
                    movementType = "RETURN",
                    quantityChangeBase = baseQuantityToRestore,
                    unitUsed = saleItem.unitName,
                    quantityInUnit = returnQty,
                    stockAfterBase = newStock,
                    referenceId = returnNum,
                    reason = "Return: $reason (Auth by: $authorizedBy)",
                    staffName = authorizedBy
                )
            )
        }

        // If active cash session, update cash refunds
        val activeSession = db.cashSessionDao().getActiveSession()
        if (activeSession != null) {
            val updated = activeSession.copy(
                cashRefunds = activeSession.cashRefunds + refundAmount,
                expectedCash = activeSession.expectedCash - refundAmount
            )
            db.cashSessionDao().updateSession(updated)
        }

        db.auditLogDao().insertLog(
            AuditLog(
                staffName = authorizedBy,
                action = "Approved Return $returnNum",
                recordInfo = "Item: ${saleItem.productName} ($returnQty ${saleItem.unitName}) - Refund: KES $refundAmount"
            )
        )
        returnOrderId
    }

    // Customer Credit Repayments (Part 25)
    suspend fun recordCustomerRepayment(
        customerId: Long,
        amount: Double,
        notes: String,
        staffName: String
    ) = db.withTransaction {
        val customer = db.customerDao().getCustomerById(customerId) ?: return@withTransaction
        val newBalance = (customer.outstandingCredit - amount).coerceAtLeast(0.0)
        db.customerDao().updateCreditAndPurchases(customerId, -amount, 0.0)

        db.customerDao().insertTransaction(
            CustomerTransaction(
                customerId = customerId,
                customerName = customer.name,
                type = "REPAYMENT",
                amount = amount,
                referenceId = "PAY-${System.currentTimeMillis().toString().takeLast(6)}",
                notes = notes.ifBlank { "Credit Repayment" },
                balanceAfter = newBalance
            )
        )

        db.auditLogDao().insertLog(
            AuditLog(
                staffName = staffName,
                action = "Recorded Credit Repayment",
                recordInfo = "${customer.name}: Repaid KES $amount (Balance: KES $newBalance)"
            )
        )
    }

    // Customers & Suppliers
    suspend fun saveCustomer(customer: Customer): Long = db.customerDao().insertCustomer(customer)
    suspend fun saveSupplier(supplier: Supplier): Long = db.supplierDao().insertSupplier(supplier)

    // Expenses (Part 26)
    suspend fun addExpense(expense: Expense): Long = db.withTransaction {
        val id = db.expenseDao().insertExpense(expense)
        // If cash session active, record cash expense
        val activeSession = db.cashSessionDao().getActiveSession()
        if (activeSession != null) {
            val updated = activeSession.copy(
                cashExpenses = activeSession.cashExpenses + expense.amount,
                expectedCash = activeSession.expectedCash - expense.amount
            )
            db.cashSessionDao().updateSession(updated)
        }
        db.auditLogDao().insertLog(
            AuditLog(
                staffName = expense.staffMember,
                action = "Added Expense",
                recordInfo = "${expense.category}: KES ${expense.amount} (${expense.description})"
            )
        )
        id
    }

    // Cash Shifts (Part 27)
    suspend fun openCashSession(cashierName: String, openingCash: Double): Long = db.withTransaction {
        val session = CashSession(
            cashierName = cashierName,
            openingCash = openingCash,
            expectedCash = openingCash,
            status = "OPEN"
        )
        val id = db.cashSessionDao().insertSession(session)
        db.auditLogDao().insertLog(
            AuditLog(
                staffName = cashierName,
                action = "Opened Cash Shift",
                recordInfo = "Opening Cash: KES $openingCash"
            )
        )
        id
    }

    suspend fun closeCashSession(sessionId: Long, actualCash: Double): Unit = db.withTransaction {
        val current = db.cashSessionDao().getActiveSession() ?: return@withTransaction
        val difference = actualCash - current.expectedCash
        val closed = current.copy(
            status = "CLOSED",
            actualCash = actualCash,
            difference = difference,
            closingCash = actualCash,
            closedEpoch = System.currentTimeMillis()
        )
        db.cashSessionDao().updateSession(closed)
        db.auditLogDao().insertLog(
            AuditLog(
                staffName = current.cashierName,
                action = "Closed Cash Shift",
                recordInfo = "Expected: KES ${current.expectedCash}, Actual: KES $actualCash (Diff: KES $difference)"
            )
        )
    }

    // M-Pesa Reconciliation (Part 28)
    suspend fun recordMpesaRecon(recon: MpesaRecon): Long = db.withTransaction {
        val id = db.mpesaReconDao().insertRecon(recon)
        db.auditLogDao().insertLog(
            AuditLog(
                staffName = recon.staffName,
                action = "M-Pesa Reconciliation",
                recordInfo = "Expected: KES ${recon.expectedMpesa}, Actual: KES ${recon.actualMpesa} (Diff: KES ${recon.difference})"
            )
        )
        id
    }

    // Staff Management (Part 33)
    suspend fun saveStaffUser(user: User): Long = db.userDao().insertUser(user)
    suspend fun deleteStaffUser(id: Long) = db.userDao().deleteUser(id)

    // Reports Queries (Part 29 & 30)
    suspend fun getSalesBetween(startTime: Long, endTime: Long): List<Sale> =
        db.saleDao().getSalesBetween(startTime, endTime)

    suspend fun getPaymentsBetween(startTime: Long, endTime: Long): List<Payment> =
        db.paymentDao().getPaymentsBetween(startTime, endTime)

    suspend fun getExpensesBetween(startTime: Long, endTime: Long): List<Expense> =
        db.expenseDao().getExpensesBetween(startTime, endTime)

    suspend fun getAllCompletedSales(): List<Sale> = db.saleDao().getAllCompletedSalesList()

    // Next receipt number generator
    suspend fun generateNextReceiptNumber(): String {
        val count = db.saleDao().getAllCompletedSalesList().size + 1
        return "RG-%06d".format(count)
    }

    // Seed Kenyan starter inventory so the app has real, immediate working data!
    suspend fun seedStarterProductsIfEmpty() = db.withTransaction {
        val existing = db.productDao().getProductById(1)
        if (existing == null) {
            // Seed 1: Fresh Milk 500ml (Matches Part 11: Base = Bottle, Carton 18 and Carton 22)
            val p1Id = db.productDao().insertProduct(
                Product(
                    name = "Fresh Milk 500ml",
                    sku = "MLK-500-01",
                    barcode = "616110003344",
                    categoryName = "Dairy",
                    brand = "Brookside",
                    baseUnit = "Bottle",
                    buyingCost = 50.0,
                    retailPrice = 70.0,
                    wholesalePrice = 62.0,
                    currentStockBase = 101.0, // 5 cartons of 18 (90) + 11 loose = 101 bottles
                    minStock = 20.0,
                    trackIntactPackages = true
                )
            )
            db.productDao().insertUnitConversion(
                UnitConversion(
                    productId = p1Id,
                    unitName = "Carton 18 × 500ml",
                    conversionFactor = 18.0,
                    purchaseCost = 900.0,
                    customRetailPrice = 1050.0,
                    customWholesalePrice = 980.0,
                    barcode = "616110003355",
                    canOpen = true,
                    intactCount = 5
                )
            )
            db.productDao().insertUnitConversion(
                UnitConversion(
                    productId = p1Id,
                    unitName = "Carton 22 × 500ml",
                    conversionFactor = 22.0,
                    purchaseCost = 1050.0,
                    customRetailPrice = 1220.0,
                    customWholesalePrice = 1140.0,
                    barcode = "616110003366",
                    canOpen = true,
                    intactCount = 0
                )
            )

            // Seed 2: White Sugar (Matches Part 9 & 11: Base = KG, 50KG Sack, 5KG & 10KG packs; 9 intact sacks + 37 loose KG = 487 KG)
            val p2Id = db.productDao().insertProduct(
                Product(
                    name = "White Sugar",
                    sku = "SUG-WHT-02",
                    barcode = "616110001122",
                    categoryName = "Sugar & Sweeteners",
                    brand = "Mumias",
                    baseUnit = "KG",
                    buyingCost = 120.0,
                    retailPrice = 150.0,
                    wholesalePrice = 138.0,
                    currentStockBase = 487.0, // 9 sacks x 50 KG (450) + 37 loose KG = 487 KG
                    minStock = 60.0,
                    trackIntactPackages = true
                )
            )
            db.productDao().insertUnitConversion(
                UnitConversion(
                    productId = p2Id,
                    unitName = "50 KG Sack",
                    conversionFactor = 50.0,
                    purchaseCost = 6000.0,
                    customRetailPrice = 6500.0,
                    customWholesalePrice = 6200.0,
                    barcode = "616110001199",
                    canOpen = true,
                    intactCount = 9
                )
            )
            db.productDao().insertUnitConversion(
                UnitConversion(
                    productId = p2Id,
                    unitName = "5 KG Pack",
                    conversionFactor = 5.0,
                    purchaseCost = 620.0,
                    customRetailPrice = 700.0,
                    customWholesalePrice = 680.0,
                    barcode = "616110001155"
                )
            )
            db.productDao().insertUnitConversion(
                UnitConversion(
                    productId = p2Id,
                    unitName = "10 KG Pack",
                    conversionFactor = 10.0,
                    purchaseCost = 1220.0,
                    customRetailPrice = 1350.0,
                    customWholesalePrice = 1300.0,
                    barcode = "616110001110"
                )
            )

            // Seed 3: Assorted Fruit Sweets (Matches Part 4, 5, 6, 17, 18: Base = Piece, Pack of 30, Carton of 20 packs = 600 pieces)
            val p3Id = db.productDao().insertProduct(
                Product(
                    name = "Assorted Fruit Sweets",
                    sku = "SWT-FRT-03",
                    barcode = "616110008800",
                    categoryName = "Confectionery",
                    brand = "Kenafric",
                    baseUnit = "Piece",
                    buyingCost = 7.0,
                    retailPrice = 10.0,
                    wholesalePrice = 8.0,
                    currentStockBase = 2652.0, // 4 cartons (2400) + 7 packs (210) + 42 loose = 2652 pieces
                    minStock = 200.0,
                    trackIntactPackages = true
                )
            )
            db.productDao().insertUnitConversion(
                UnitConversion(
                    productId = p3Id,
                    unitName = "Pack (30 Pieces)",
                    conversionFactor = 30.0,
                    purchaseCost = 240.0,
                    customRetailPrice = 320.0,
                    customWholesalePrice = 290.0,
                    barcode = "616110008811",
                    canOpen = true,
                    intactCount = 7
                )
            )
            db.productDao().insertUnitConversion(
                UnitConversion(
                    productId = p3Id,
                    unitName = "Carton (20 Packs)",
                    conversionFactor = 600.0,
                    parentUnitName = "Pack (30 Pieces)",
                    parentUnitMultiplier = 20.0,
                    purchaseCost = 1800.0,
                    customRetailPrice = 2200.0,
                    customWholesalePrice = 2050.0,
                    barcode = "616110008822",
                    canOpen = true,
                    intactCount = 4
                )
            )

            // Seed 4: Digestive Biscuits (Matches Part 1: Base = Pack, Carton = 72 Packs)
            val p4Id = db.productDao().insertProduct(
                Product(
                    name = "Digestive Biscuits",
                    sku = "BIS-DIG-04",
                    barcode = "616110001234",
                    categoryName = "Confectionery",
                    brand = "Britannia",
                    baseUnit = "Pack",
                    buyingCost = 15.0,
                    retailPrice = 25.0,
                    wholesalePrice = 20.0,
                    currentStockBase = 216.0, // 2 cartons x 72 (144) + 72 loose packs = 216 packs
                    minStock = 50.0,
                    trackIntactPackages = true
                )
            )
            db.productDao().insertUnitConversion(
                UnitConversion(
                    productId = p4Id,
                    unitName = "Carton (72 Packs)",
                    conversionFactor = 72.0,
                    purchaseCost = 1080.0,
                    customRetailPrice = 1400.0,
                    customWholesalePrice = 1280.0,
                    barcode = "616110001272",
                    canOpen = true,
                    intactCount = 2
                )
            )

            // Seed 5: Jogoo Maize Flour 2kg
            val p5Id = db.productDao().insertProduct(
                Product(
                    name = "Jogoo Maize Flour 2kg",
                    sku = "UNG-JOG-05",
                    barcode = "616110005678",
                    categoryName = "Grains & Flours",
                    brand = "Ungalimited",
                    baseUnit = "Packet",
                    buyingCost = 160.0,
                    retailPrice = 195.0,
                    wholesalePrice = 180.0,
                    currentStockBase = 120.0,
                    minStock = 24.0,
                    trackIntactPackages = true
                )
            )
            db.productDao().insertUnitConversion(
                UnitConversion(
                    productId = p5Id,
                    unitName = "Bale (12 Packets)",
                    conversionFactor = 12.0,
                    purchaseCost = 1920.0,
                    customRetailPrice = 2300.0,
                    customWholesalePrice = 2160.0,
                    barcode = "616110005612",
                    canOpen = true,
                    intactCount = 8
                )
            )

            // Seed 6: Rina Cooking Oil 1L
            val p6Id = db.productDao().insertProduct(
                Product(
                    name = "Rina Cooking Oil 1L",
                    sku = "OIL-RIN-06",
                    barcode = "616110009101",
                    categoryName = "Oils & Fats",
                    brand = "Kapa",
                    baseUnit = "Bottle",
                    buyingCost = 280.0,
                    retailPrice = 340.0,
                    wholesalePrice = 310.0,
                    currentStockBase = 60.0,
                    minStock = 15.0,
                    trackIntactPackages = true
                )
            )
            db.productDao().insertUnitConversion(
                UnitConversion(
                    productId = p6Id,
                    unitName = "Carton (12 Bottles)",
                    conversionFactor = 12.0,
                    purchaseCost = 3360.0,
                    customRetailPrice = 4000.0,
                    customWholesalePrice = 3720.0,
                    barcode = "616110009112",
                    canOpen = true,
                    intactCount = 4
                )
            )

            // Seed 7: Low Stock Alert Demo Product
            db.productDao().insertProduct(
                Product(
                    name = "Geisha Soap 225g",
                    sku = "SOP-GEI-07",
                    barcode = "616110005566",
                    categoryName = "Personal Care",
                    brand = "Unilever",
                    baseUnit = "Piece",
                    buyingCost = 90.0,
                    retailPrice = 120.0,
                    wholesalePrice = 105.0,
                    currentStockBase = 4.0, // <= min stock 10
                    minStock = 10.0
                )
            )

            // Seed Sample Customers
            val c1Id = db.customerDao().insertCustomer(
                Customer(
                    name = "John Mwangi",
                    phone = "0712 345 678",
                    address = "Nairobi West",
                    creditLimit = 30000.0,
                    outstandingCredit = 10000.0, // Matches Part 25 specification
                    totalPurchases = 45000.0
                )
            )
            db.customerDao().insertTransaction(
                CustomerTransaction(
                    customerId = c1Id,
                    customerName = "John Mwangi",
                    type = "CREDIT_SALE",
                    amount = 2000.0,
                    referenceId = "RG-000098",
                    notes = "18 Sept Sale",
                    balanceAfter = 10000.0,
                    dateEpoch = System.currentTimeMillis() - 86400000L * 2
                )
            )

            db.customerDao().insertCustomer(
                Customer(
                    name = "Amina Wanjiku",
                    phone = "0722 889 900",
                    address = "Eastleigh 2nd Ave",
                    creditLimit = 50000.0,
                    outstandingCredit = 0.0,
                    totalPurchases = 82000.0
                )
            )

            // Seed Sample Supplier
            db.supplierDao().insertSupplier(
                Supplier(
                    name = "Kapa Oil Refineries Ltd",
                    phone = "020 6970000",
                    address = "Commercial St, Industrial Area, Nairobi",
                    contactPerson = "Peter Kamau",
                    outstandingBalance = 24000.0
                )
            )

            db.supplierDao().insertSupplier(
                Supplier(
                    name = "Britannia Distributors Kenya",
                    phone = "0733 112 233",
                    address = "Mombasa Road, Nairobi",
                    contactPerson = "Grace Muthoni",
                    outstandingBalance = 0.0
                )
            )

            // Seed Categories
            db.categoryDao().insertCategory(Category(name = "Confectionery"))
            db.categoryDao().insertCategory(Category(name = "Grains & Flours"))
            db.categoryDao().insertCategory(Category(name = "Oils & Fats"))
            db.categoryDao().insertCategory(Category(name = "Dairy"))
            db.categoryDao().insertCategory(Category(name = "Personal Care"))
            db.categoryDao().insertCategory(Category(name = "Beverages"))

            // Seed Expense Categories
            db.expenseDao().insertExpenseCategory(ExpenseCategory(name = "Rent"))
            db.expenseDao().insertExpenseCategory(ExpenseCategory(name = "Electricity / Water"))
            db.expenseDao().insertExpenseCategory(ExpenseCategory(name = "Transport & Delivery"))
            db.expenseDao().insertExpenseCategory(ExpenseCategory(name = "Staff Meals"))
            db.expenseDao().insertExpenseCategory(ExpenseCategory(name = "Packaging & Bags"))
            db.expenseDao().insertExpenseCategory(ExpenseCategory(name = "Maintenance"))
        }
    }
}
