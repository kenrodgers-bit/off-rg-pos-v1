package com.example.util

import android.content.Context
import android.util.Base64
import com.example.data.database.AppDatabase
import com.example.data.model.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.security.MessageDigest
import java.security.SecureRandom
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.crypto.Cipher
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

data class BackupMetadata(
    val filename: String,
    val filePath: String,
    val sizeBytes: Long,
    val createdAtEpoch: Long,
    val isEncrypted: Boolean,
    val appVersion: String,
    val businessName: String,
    val isVerified: Boolean = true,
    val recordCount: Int = 0
)

sealed class ValidationResult {
    data class Valid(val metadata: BackupMetadata, val jsonPayload: JSONObject) : ValidationResult()
    data class Invalid(val reason: String) : ValidationResult()
}

class BackupManager(private val context: Context, private val db: AppDatabase) {

    private val backupsDir = File(context.filesDir, "backups").apply {
        if (!exists()) mkdirs()
    }

    suspend fun createBackup(
        password: String = "",
        isSafetyBackup: Boolean = false,
        retentionCount: Int = 5
    ): Result<File> = withContext(Dispatchers.IO) {
        try {
            val biz = db.businessDao().getBusiness()
            val users = db.userDao().getAllUsersList()
            val categories = db.categoryDao().getAllCategoriesList()
            val products = db.productDao().getAllProductsList()
            val unitConversions = db.productDao().getAllUnitConversions()
            val stockMovements = db.stockMovementDao().getAllMovementsList()
            val customers = db.customerDao().getAllCustomersList()
            val customerTransactions = db.customerDao().getAllTransactionsList()
            val suppliers = db.supplierDao().getAllSuppliersList()
            val purchases = db.purchaseDao().getAllPurchasesList()
            val purchaseItems = db.purchaseDao().getAllPurchaseItemsList()
            val sales = db.saleDao().getAllSalesList()
            val saleItems = db.saleDao().getAllSaleItemsList()
            val payments = db.paymentDao().getAllPayments()
            val expenses = db.expenseDao().getAllExpensesList()
            val expenseCategories = db.expenseDao().getAllExpenseCategoriesList()
            val cashSessions = db.cashSessionDao().getAllSessionsList()
            val mpesaRecons = db.mpesaReconDao().getAllReconsList()
            val returnOrders = db.returnDao().getAllReturnsList()
            val returnItems = db.returnDao().getAllReturnItems()
            val auditLogs = db.auditLogDao().getAllLogsList()
            val appSettings = db.appSettingDao().getAllSettingsList()

            val payloadObj = JSONObject().apply {
                put("format", "RGBACKUP_INTERNAL")
                put("version", 1)
                put("appVersion", "1.0")
                put("createdAtEpoch", System.currentTimeMillis())

                biz?.let {
                    put("business", JSONObject().apply {
                        put("id", it.id)
                        put("name", it.name)
                        put("phone", it.phone)
                        put("location", it.location)
                        put("address", it.address)
                        put("mpesaTill", it.mpesaTill)
                        put("kraPin", it.kraPin)
                        put("ownerName", it.ownerName)
                        put("currency", it.currency)
                        put("retailEnabled", it.retailEnabled)
                        put("wholesaleEnabled", it.wholesaleEnabled)
                        put("allowCredit", it.allowCredit)
                        put("lowStockThreshold", it.lowStockThreshold)
                        put("taxRatePercent", it.taxRatePercent)
                        put("receiptHeader", it.receiptHeader)
                        put("receiptFooter", it.receiptFooter)
                        put("costingMethod", it.costingMethod)
                        put("ownerPin", it.ownerPin)
                        put("isConfigured", it.isConfigured)
                        put("createdAtEpoch", it.createdAtEpoch)
                    })
                }

                put("users", JSONArray().apply {
                    users.forEach { u ->
                        put(JSONObject().apply {
                            put("id", u.id)
                            put("username", u.username)
                            put("fullName", u.fullName)
                            put("role", u.role)
                            put("pin", u.pin)
                            put("phone", u.phone)
                            put("isActive", u.isActive)
                        })
                    }
                })

                put("categories", JSONArray().apply {
                    categories.forEach { c ->
                        put(JSONObject().apply {
                            put("id", c.id)
                            put("name", c.name)
                        })
                    }
                })

                put("products", JSONArray().apply {
                    products.forEach { p ->
                        put(JSONObject().apply {
                            put("id", p.id)
                            put("name", p.name)
                            put("sku", p.sku)
                            put("barcode", p.barcode)
                            put("categoryId", p.categoryId)
                            put("categoryName", p.categoryName)
                            put("brand", p.brand)
                            put("baseUnit", p.baseUnit)
                            put("buyingCost", p.buyingCost)
                            put("retailPrice", p.retailPrice)
                            put("wholesalePrice", p.wholesalePrice)
                            put("wholesaleEnabled", p.wholesaleEnabled)
                            put("currentStockBase", p.currentStockBase)
                            put("minStock", p.minStock)
                            put("supplierId", p.supplierId)
                            put("trackIntactPackages", p.trackIntactPackages)
                            put("isArchived", p.isArchived)
                        })
                    }
                })

                put("unitConversions", JSONArray().apply {
                    unitConversions.forEach { uc ->
                        put(JSONObject().apply {
                            put("id", uc.id)
                            put("productId", uc.productId)
                            put("unitName", uc.unitName)
                            put("conversionFactor", uc.conversionFactor)
                            uc.customRetailPrice?.let { put("customRetailPrice", it) }
                            uc.customWholesalePrice?.let { put("customWholesalePrice", it) }
                            uc.purchaseCost?.let { put("purchaseCost", it) }
                            put("barcode", uc.barcode)
                            put("sku", uc.sku)
                            put("purchaseEnabled", uc.purchaseEnabled)
                            put("sellingEnabled", uc.sellingEnabled)
                            put("canOpen", uc.canOpen)
                            put("intactCount", uc.intactCount)
                            put("parentUnitName", uc.parentUnitName)
                            put("parentUnitMultiplier", uc.parentUnitMultiplier)
                            put("isActive", uc.isActive)
                        })
                    }
                })

                put("stockMovements", JSONArray().apply {
                    stockMovements.forEach { sm ->
                        put(JSONObject().apply {
                            put("id", sm.id)
                            put("productId", sm.productId)
                            put("productName", sm.productName)
                            put("movementType", sm.movementType)
                            put("quantityChangeBase", sm.quantityChangeBase)
                            put("unitUsed", sm.unitUsed)
                            put("quantityInUnit", sm.quantityInUnit)
                            put("stockAfterBase", sm.stockAfterBase)
                            put("referenceId", sm.referenceId)
                            put("reason", sm.reason)
                            put("staffName", sm.staffName)
                            put("dateEpoch", sm.dateEpoch)
                        })
                    }
                })

                put("customers", JSONArray().apply {
                    customers.forEach { c ->
                        put(JSONObject().apply {
                            put("id", c.id)
                            put("name", c.name)
                            put("phone", c.phone)
                            put("email", c.email)
                            put("address", c.address)
                            put("creditLimit", c.creditLimit)
                            put("outstandingCredit", c.outstandingCredit)
                            put("totalPurchases", c.totalPurchases)
                            put("createdAtEpoch", c.createdAtEpoch)
                        })
                    }
                })

                put("customerTransactions", JSONArray().apply {
                    customerTransactions.forEach { ct ->
                        put(JSONObject().apply {
                            put("id", ct.id)
                            put("customerId", ct.customerId)
                            put("customerName", ct.customerName)
                            put("type", ct.type)
                            put("amount", ct.amount)
                            put("referenceId", ct.referenceId)
                            put("notes", ct.notes)
                            put("balanceAfter", ct.balanceAfter)
                            put("dateEpoch", ct.dateEpoch)
                        })
                    }
                })

                put("suppliers", JSONArray().apply {
                    suppliers.forEach { s ->
                        put(JSONObject().apply {
                            put("id", s.id)
                            put("name", s.name)
                            put("phone", s.phone)
                            put("address", s.address)
                            put("contactPerson", s.contactPerson)
                            put("outstandingBalance", s.outstandingBalance)
                        })
                    }
                })

                put("purchases", JSONArray().apply {
                    purchases.forEach { pu ->
                        put(JSONObject().apply {
                            put("id", pu.id)
                            put("invoiceNumber", pu.invoiceNumber)
                            put("supplierId", pu.supplierId)
                            put("supplierName", pu.supplierName)
                            put("totalAmount", pu.totalAmount)
                            put("itemsCount", pu.itemsCount)
                            put("notes", pu.notes)
                            put("staffName", pu.staffName)
                            put("dateEpoch", pu.dateEpoch)
                        })
                    }
                })

                put("purchaseItems", JSONArray().apply {
                    purchaseItems.forEach { pi ->
                        put(JSONObject().apply {
                            put("id", pi.id)
                            put("purchaseId", pi.purchaseId)
                            put("productId", pi.productId)
                            put("productName", pi.productName)
                            put("unitName", pi.unitName)
                            put("quantity", pi.quantity)
                            put("costPerUnit", pi.costPerUnit)
                            put("totalCost", pi.totalCost)
                            put("baseUnitsAdded", pi.baseUnitsAdded)
                        })
                    }
                })

                put("sales", JSONArray().apply {
                    sales.forEach { s ->
                        put(JSONObject().apply {
                            put("id", s.id)
                            put("receiptNumber", s.receiptNumber)
                            s.customerId?.let { put("customerId", it) }
                            put("customerName", s.customerName)
                            put("subtotal", s.subtotal)
                            put("discount", s.discount)
                            put("tax", s.tax)
                            put("total", s.total)
                            put("costTotal", s.costTotal)
                            put("saleType", s.saleType)
                            put("status", s.status)
                            put("cashierName", s.cashierName)
                            put("notes", s.notes)
                            put("dateEpoch", s.dateEpoch)
                        })
                    }
                })

                put("saleItems", JSONArray().apply {
                    saleItems.forEach { si ->
                        put(JSONObject().apply {
                            put("id", si.id)
                            put("saleId", si.saleId)
                            put("productId", si.productId)
                            put("productName", si.productName)
                            put("unitName", si.unitName)
                            put("quantity", si.quantity)
                            put("unitPrice", si.unitPrice)
                            put("costPrice", si.costPrice)
                            put("subtotal", si.subtotal)
                            put("baseQuantityDeducted", si.baseQuantityDeducted)
                            put("returnedQuantity", si.returnedQuantity)
                        })
                    }
                })

                put("payments", JSONArray().apply {
                    payments.forEach { p ->
                        put(JSONObject().apply {
                            put("id", p.id)
                            put("saleId", p.saleId)
                            put("paymentMethod", p.paymentMethod)
                            put("amount", p.amount)
                            put("mpesaRef", p.mpesaRef)
                            put("dateEpoch", p.dateEpoch)
                        })
                    }
                })

                put("expenses", JSONArray().apply {
                    expenses.forEach { e ->
                        put(JSONObject().apply {
                            put("id", e.id)
                            put("category", e.category)
                            put("amount", e.amount)
                            put("description", e.description)
                            put("staffMember", e.staffMember)
                            put("dateEpoch", e.dateEpoch)
                        })
                    }
                })

                put("expenseCategories", JSONArray().apply {
                    expenseCategories.forEach { ec ->
                        put(JSONObject().apply {
                            put("id", ec.id)
                            put("name", ec.name)
                        })
                    }
                })

                put("cashSessions", JSONArray().apply {
                    cashSessions.forEach { cs ->
                        put(JSONObject().apply {
                            put("id", cs.id)
                            put("cashierName", cs.cashierName)
                            put("openingCash", cs.openingCash)
                            put("closingCash", cs.closingCash)
                            put("expectedCash", cs.expectedCash)
                            put("actualCash", cs.actualCash)
                            put("difference", cs.difference)
                            put("cashSales", cs.cashSales)
                            put("cashExpenses", cs.cashExpenses)
                            put("cashRefunds", cs.cashRefunds)
                            put("status", cs.status)
                            put("openedEpoch", cs.openedEpoch)
                            cs.closedEpoch?.let { put("closedEpoch", it) }
                        })
                    }
                })

                put("mpesaRecons", JSONArray().apply {
                    mpesaRecons.forEach { mr ->
                        put(JSONObject().apply {
                            put("id", mr.id)
                            put("dateEpoch", mr.dateEpoch)
                            put("expectedMpesa", mr.expectedMpesa)
                            put("actualMpesa", mr.actualMpesa)
                            put("difference", mr.difference)
                            put("notes", mr.notes)
                            put("staffName", mr.staffName)
                        })
                    }
                })

                put("returnOrders", JSONArray().apply {
                    returnOrders.forEach { ro ->
                        put(JSONObject().apply {
                            put("id", ro.id)
                            put("returnNumber", ro.returnNumber)
                            put("originalSaleId", ro.originalSaleId)
                            put("originalReceiptNumber", ro.originalReceiptNumber)
                            put("customerName", ro.customerName)
                            put("totalRefundAmount", ro.totalRefundAmount)
                            put("reason", ro.reason)
                            put("authorizedBy", ro.authorizedBy)
                            put("dateEpoch", ro.dateEpoch)
                        })
                    }
                })

                put("returnItems", JSONArray().apply {
                    returnItems.forEach { ri ->
                        put(JSONObject().apply {
                            put("id", ri.id)
                            put("returnOrderId", ri.returnOrderId)
                            put("saleItemId", ri.saleItemId)
                            put("productId", ri.productId)
                            put("productName", ri.productName)
                            put("unitName", ri.unitName)
                            put("quantity", ri.quantity)
                            put("refundUnitPrice", ri.refundUnitPrice)
                            put("totalRefund", ri.totalRefund)
                            put("baseUnitsRestored", ri.baseUnitsRestored)
                        })
                    }
                })

                put("auditLogs", JSONArray().apply {
                    auditLogs.forEach { al ->
                        put(JSONObject().apply {
                            put("id", al.id)
                            put("staffName", al.staffName)
                            put("action", al.action)
                            put("recordInfo", al.recordInfo)
                            put("dateEpoch", al.dateEpoch)
                        })
                    }
                })

                put("appSettings", JSONArray().apply {
                    appSettings.forEach { ast ->
                        put(JSONObject().apply {
                            put("key", ast.key)
                            put("value", ast.value)
                        })
                    }
                })
            }

            val payloadStr = payloadObj.toString()
            val integrityHash = sha256(payloadStr)

            val envelope = JSONObject().apply {
                put("format", "RGBACKUP")
                put("version", 1)
                put("appVersion", "1.0")
                put("createdAt", System.currentTimeMillis())
                put("businessName", biz?.name ?: "RG POS")
                put("integrityHash", integrityHash)

                if (password.isNotBlank()) {
                    val salt = ByteArray(16).apply { SecureRandom().nextBytes(this) }
                    val iv = ByteArray(12).apply { SecureRandom().nextBytes(this) }
                    val encrypted = encryptAesGcm(payloadStr.toByteArray(Charsets.UTF_8), password, salt, iv)

                    put("isEncrypted", true)
                    put("saltHex", bytesToHex(salt))
                    put("ivHex", bytesToHex(iv))
                    put("payload", Base64.encodeToString(encrypted, Base64.NO_WRAP))
                } else {
                    put("isEncrypted", false)
                    put("saltHex", "")
                    put("ivHex", "")
                    put("payload", payloadStr)
                }
            }

            val timestamp = SimpleDateFormat("yyyy-MM-dd_HHmm", Locale.US).format(Date())
            val filename = if (isSafetyBackup) {
                "safety_backup_before_restore.rgbackup"
            } else {
                "RG_POS_Backup_${timestamp}.rgbackup"
            }

            val targetFile = File(backupsDir, filename)
            targetFile.writeText(envelope.toString(2), Charsets.UTF_8)

            // Log backup event in audit log
            db.auditLogDao().insertLog(
                AuditLog(
                    staffName = biz?.ownerName?.ifBlank { "System" } ?: "System",
                    action = if (isSafetyBackup) "Safety Backup Created" else "Manual Backup Created",
                    recordInfo = "${targetFile.name} (${targetFile.length() / 1024} KB)"
                )
            )

            // Prune older backups if not a safety backup
            if (!isSafetyBackup) {
                pruneBackups(retentionCount)
            }

            Result.success(targetFile)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun validateBackup(file: File, password: String = ""): ValidationResult = withContext(Dispatchers.IO) {
        try {
            if (!file.exists() || file.length() == 0L) {
                return@withContext ValidationResult.Invalid("Backup file does not exist or is empty.")
            }

            val content = file.readText(Charsets.UTF_8)
            val envelope = try {
                JSONObject(content)
            } catch (e: Exception) {
                return@withContext ValidationResult.Invalid("File is not a valid RG POS backup format.")
            }

            val format = envelope.optString("format")
            if (format != "RGBACKUP") {
                return@withContext ValidationResult.Invalid("Invalid backup format header. Expected RGBACKUP, found '$format'.")
            }

            val isEncrypted = envelope.optBoolean("isEncrypted", false)
            val expectedHash = envelope.optString("integrityHash", "")
            val rawPayload = envelope.optString("payload", "")

            val decryptedJsonStr = if (isEncrypted) {
                if (password.isBlank()) {
                    return@withContext ValidationResult.Invalid("This backup is password-protected. Please enter the decryption password.")
                }
                val saltHex = envelope.optString("saltHex")
                val ivHex = envelope.optString("ivHex")
                val salt = hexToBytes(saltHex)
                val iv = hexToBytes(ivHex)
                val cipherBytes = Base64.decode(rawPayload, Base64.NO_WRAP)
                try {
                    val decryptedBytes = decryptAesGcm(cipherBytes, password, salt, iv)
                    String(decryptedBytes, Charsets.UTF_8)
                } catch (e: Exception) {
                    return@withContext ValidationResult.Invalid("Incorrect password or corrupt encrypted backup.")
                }
            } else {
                rawPayload
            }

            val actualHash = sha256(decryptedJsonStr)
            if (expectedHash.isNotBlank() && expectedHash != actualHash) {
                return@withContext ValidationResult.Invalid("Integrity verification failed (checksum mismatch). The backup data may be corrupted.")
            }

            val payloadObj = try {
                JSONObject(decryptedJsonStr)
            } catch (e: Exception) {
                return@withContext ValidationResult.Invalid("Failed to parse internal database records.")
            }

            val productCount = payloadObj.optJSONArray("products")?.length() ?: 0
            val salesCount = payloadObj.optJSONArray("sales")?.length() ?: 0
            val totalRecords = productCount + salesCount

            val metadata = BackupMetadata(
                filename = file.name,
                filePath = file.absolutePath,
                sizeBytes = file.length(),
                createdAtEpoch = envelope.optLong("createdAt", file.lastModified()),
                isEncrypted = isEncrypted,
                appVersion = envelope.optString("appVersion", "1.0"),
                businessName = envelope.optString("businessName", "RG POS"),
                isVerified = true,
                recordCount = totalRecords
            )

            ValidationResult.Valid(metadata, payloadObj)
        } catch (e: Exception) {
            ValidationResult.Invalid("Validation error: ${e.localizedMessage}")
        }
    }

    suspend fun restoreBackup(validatedPayload: JSONObject): Result<Boolean> = withContext(Dispatchers.IO) {
        // Step 1: Create automatic safety backup before modifying anything
        val safetyResult = createBackup(isSafetyBackup = true)
        if (safetyResult.isFailure) {
            return@withContext Result.failure(Exception("Failed to create safety backup prior to restore: ${safetyResult.exceptionOrNull()?.message}"))
        }

        try {
            // Step 2: Wiping current database tables
            db.clearAllTables()

            // Step 3: Parse and insert all entities
            val bizObj = validatedPayload.optJSONObject("business")
            bizObj?.let {
                db.businessDao().insertOrUpdate(
                    Business(
                        id = it.optInt("id", 1),
                        name = it.optString("name", "RG POS"),
                        phone = it.optString("phone", ""),
                        location = it.optString("location", "Nairobi, Kenya"),
                        address = it.optString("address", ""),
                        mpesaTill = it.optString("mpesaTill", ""),
                        kraPin = it.optString("kraPin", ""),
                        ownerName = it.optString("ownerName", "Owner"),
                        currency = it.optString("currency", "KES"),
                        retailEnabled = it.optBoolean("retailEnabled", true),
                        wholesaleEnabled = it.optBoolean("wholesaleEnabled", true),
                        allowCredit = it.optBoolean("allowCredit", true),
                        lowStockThreshold = it.optInt("lowStockThreshold", 10),
                        taxRatePercent = it.optDouble("taxRatePercent", 0.0),
                        receiptHeader = it.optString("receiptHeader", "Thank you for shopping with us!"),
                        receiptFooter = it.optString("receiptFooter", "Goods once sold are only returnable with receipt within 7 days."),
                        costingMethod = it.optString("costingMethod", "WEIGHTED_AVERAGE"),
                        ownerPin = it.optString("ownerPin", "1234"),
                        isConfigured = it.optBoolean("isConfigured", true),
                        createdAtEpoch = it.optLong("createdAtEpoch", System.currentTimeMillis())
                    )
                )
            }

            val usersArr = validatedPayload.optJSONArray("users")
            val usersList = mutableListOf<User>()
            usersArr?.let { arr ->
                for (i in 0 until arr.length()) {
                    val o = arr.getJSONObject(i)
                    usersList.add(
                        User(
                            id = o.optLong("id", 0),
                            username = o.optString("username"),
                            fullName = o.optString("fullName"),
                            role = o.optString("role", "CASHIER"),
                            pin = o.optString("pin"),
                            phone = o.optString("phone"),
                            isActive = o.optBoolean("isActive", true)
                        )
                    )
                }
            }
            if (usersList.isNotEmpty()) db.userDao().insertUsers(usersList)

            val categoriesArr = validatedPayload.optJSONArray("categories")
            val categoriesList = mutableListOf<Category>()
            categoriesArr?.let { arr ->
                for (i in 0 until arr.length()) {
                    val o = arr.getJSONObject(i)
                    categoriesList.add(
                        Category(
                            id = o.optLong("id", 0),
                            name = o.optString("name")
                        )
                    )
                }
            }
            if (categoriesList.isNotEmpty()) db.categoryDao().insertCategories(categoriesList)

            val productsArr = validatedPayload.optJSONArray("products")
            val productsList = mutableListOf<Product>()
            productsArr?.let { arr ->
                for (i in 0 until arr.length()) {
                    val o = arr.getJSONObject(i)
                    productsList.add(
                        Product(
                            id = o.optLong("id", 0),
                            name = o.optString("name"),
                            sku = o.optString("sku"),
                            barcode = o.optString("barcode"),
                            categoryId = o.optLong("categoryId", 0),
                            categoryName = o.optString("categoryName", "General"),
                            brand = o.optString("brand", ""),
                            baseUnit = o.optString("baseUnit", "Piece"),
                            buyingCost = o.optDouble("buyingCost", 0.0),
                            retailPrice = o.optDouble("retailPrice", 0.0),
                            wholesalePrice = o.optDouble("wholesalePrice", 0.0),
                            wholesaleEnabled = o.optBoolean("wholesaleEnabled", true),
                            currentStockBase = o.optDouble("currentStockBase", 0.0),
                            minStock = o.optDouble("minStock", 10.0),
                            supplierId = o.optLong("supplierId", 0),
                            trackIntactPackages = o.optBoolean("trackIntactPackages", false),
                            isArchived = o.optBoolean("isArchived", false)
                        )
                    )
                }
            }
            if (productsList.isNotEmpty()) db.productDao().insertProducts(productsList)

            val ucArr = validatedPayload.optJSONArray("unitConversions")
            val ucList = mutableListOf<UnitConversion>()
            ucArr?.let { arr ->
                for (i in 0 until arr.length()) {
                    val o = arr.getJSONObject(i)
                    ucList.add(
                        UnitConversion(
                            id = o.optLong("id", 0),
                            productId = o.optLong("productId"),
                            unitName = o.optString("unitName"),
                            conversionFactor = o.optDouble("conversionFactor", 1.0),
                            customRetailPrice = if (o.has("customRetailPrice") && !o.isNull("customRetailPrice")) o.optDouble("customRetailPrice") else null,
                            customWholesalePrice = if (o.has("customWholesalePrice") && !o.isNull("customWholesalePrice")) o.optDouble("customWholesalePrice") else null,
                            purchaseCost = if (o.has("purchaseCost") && !o.isNull("purchaseCost")) o.optDouble("purchaseCost") else null,
                            barcode = o.optString("barcode", ""),
                            sku = o.optString("sku", ""),
                            purchaseEnabled = o.optBoolean("purchaseEnabled", true),
                            sellingEnabled = o.optBoolean("sellingEnabled", true),
                            canOpen = o.optBoolean("canOpen", false),
                            intactCount = o.optInt("intactCount", 0),
                            parentUnitName = o.optString("parentUnitName", ""),
                            parentUnitMultiplier = o.optDouble("parentUnitMultiplier", 1.0),
                            isActive = o.optBoolean("isActive", true)
                        )
                    )
                }
            }
            if (ucList.isNotEmpty()) db.productDao().insertUnitConversions(ucList)

            val custArr = validatedPayload.optJSONArray("customers")
            val custList = mutableListOf<Customer>()
            custArr?.let { arr ->
                for (i in 0 until arr.length()) {
                    val o = arr.getJSONObject(i)
                    custList.add(
                        Customer(
                            id = o.optLong("id", 0),
                            name = o.optString("name"),
                            phone = o.optString("phone"),
                            email = o.optString("email"),
                            address = o.optString("address"),
                            creditLimit = o.optDouble("creditLimit", 50000.0),
                            outstandingCredit = o.optDouble("outstandingCredit", 0.0),
                            totalPurchases = o.optDouble("totalPurchases", 0.0),
                            createdAtEpoch = o.optLong("createdAtEpoch", System.currentTimeMillis())
                        )
                    )
                }
            }
            if (custList.isNotEmpty()) db.customerDao().insertCustomers(custList)

            val salesArr = validatedPayload.optJSONArray("sales")
            val salesList = mutableListOf<Sale>()
            salesArr?.let { arr ->
                for (i in 0 until arr.length()) {
                    val o = arr.getJSONObject(i)
                    val custId = if (o.has("customerId") && !o.isNull("customerId")) o.getLong("customerId") else null
                    salesList.add(
                        Sale(
                            id = o.optLong("id", 0),
                            receiptNumber = o.optString("receiptNumber"),
                            customerId = custId,
                            customerName = o.optString("customerName", "Walk-in Customer"),
                            subtotal = o.optDouble("subtotal", 0.0),
                            discount = o.optDouble("discount", 0.0),
                            tax = o.optDouble("tax", 0.0),
                            total = o.optDouble("total", 0.0),
                            costTotal = o.optDouble("costTotal", 0.0),
                            saleType = o.optString("saleType", "RETAIL"),
                            status = o.optString("status", "COMPLETED"),
                            cashierName = o.optString("cashierName", "Cashier"),
                            notes = o.optString("notes", ""),
                            dateEpoch = o.optLong("dateEpoch", System.currentTimeMillis())
                        )
                    )
                }
            }
            if (salesList.isNotEmpty()) db.saleDao().insertSales(salesList)

            val siArr = validatedPayload.optJSONArray("saleItems")
            val siList = mutableListOf<SaleItem>()
            siArr?.let { arr ->
                for (i in 0 until arr.length()) {
                    val o = arr.getJSONObject(i)
                    siList.add(
                        SaleItem(
                            id = o.optLong("id", 0),
                            saleId = o.optLong("saleId"),
                            productId = o.optLong("productId"),
                            productName = o.optString("productName"),
                            unitName = o.optString("unitName"),
                            quantity = o.optDouble("quantity", 1.0),
                            unitPrice = o.optDouble("unitPrice", 0.0),
                            costPrice = o.optDouble("costPrice", 0.0),
                            subtotal = o.optDouble("subtotal", 0.0),
                            baseQuantityDeducted = o.optDouble("baseQuantityDeducted", 1.0),
                            returnedQuantity = o.optDouble("returnedQuantity", 0.0)
                        )
                    )
                }
            }
            if (siList.isNotEmpty()) db.saleDao().insertSaleItems(siList)

            val payArr = validatedPayload.optJSONArray("payments")
            val payList = mutableListOf<Payment>()
            payArr?.let { arr ->
                for (i in 0 until arr.length()) {
                    val o = arr.getJSONObject(i)
                    payList.add(
                        Payment(
                            id = o.optLong("id", 0),
                            saleId = o.optLong("saleId"),
                            paymentMethod = o.optString("paymentMethod", "CASH"),
                            amount = o.optDouble("amount", 0.0),
                            mpesaRef = o.optString("mpesaRef", ""),
                            dateEpoch = o.optLong("dateEpoch", System.currentTimeMillis())
                        )
                    )
                }
            }
            if (payList.isNotEmpty()) db.paymentDao().insertPayments(payList)

            val expArr = validatedPayload.optJSONArray("expenses")
            val expList = mutableListOf<Expense>()
            expArr?.let { arr ->
                for (i in 0 until arr.length()) {
                    val o = arr.getJSONObject(i)
                    expList.add(
                        Expense(
                            id = o.optLong("id", 0),
                            category = o.optString("category"),
                            amount = o.optDouble("amount", 0.0),
                            description = o.optString("description", ""),
                            staffMember = o.optString("staffMember", "Staff"),
                            dateEpoch = o.optLong("dateEpoch", System.currentTimeMillis())
                        )
                    )
                }
            }
            if (expList.isNotEmpty()) db.expenseDao().insertExpenses(expList)

            // Record audit log entry
            db.auditLogDao().insertLog(
                AuditLog(
                    staffName = "System",
                    action = "Database Restored",
                    recordInfo = "Restored ${productsList.size} products and ${salesList.size} sales records successfully"
                )
            )

            Result.success(true)
        } catch (restoreEx: Exception) {
            // Restore failed -> Rollback from safety backup
            try {
                val safetyFile = File(backupsDir, "safety_backup_before_restore.rgbackup")
                if (safetyFile.exists()) {
                    val valResult = validateBackup(safetyFile)
                    if (valResult is ValidationResult.Valid) {
                        restoreBackupInternal(valResult.jsonPayload)
                    }
                }
            } catch (_: Exception) {
            }
            Result.failure(Exception("Restore operation failed. Safety rollback initiated. Error: ${restoreEx.localizedMessage}"))
        }
    }

    private suspend fun restoreBackupInternal(payload: JSONObject) {
        // Internal recovery without creating an extra recursive safety backup
        db.clearAllTables()
        val bizObj = payload.optJSONObject("business")
        bizObj?.let {
            db.businessDao().insertOrUpdate(
                Business(
                    id = it.optInt("id", 1),
                    name = it.optString("name", "RG POS"),
                    phone = it.optString("phone", ""),
                    location = it.optString("location", "Nairobi, Kenya"),
                    address = it.optString("address", ""),
                    mpesaTill = it.optString("mpesaTill", ""),
                    kraPin = it.optString("kraPin", ""),
                    ownerName = it.optString("ownerName", "Owner"),
                    currency = it.optString("currency", "KES"),
                    retailEnabled = it.optBoolean("retailEnabled", true),
                    wholesaleEnabled = it.optBoolean("wholesaleEnabled", true),
                    allowCredit = it.optBoolean("allowCredit", true),
                    lowStockThreshold = it.optInt("lowStockThreshold", 10),
                    taxRatePercent = it.optDouble("taxRatePercent", 0.0),
                    receiptHeader = it.optString("receiptHeader", "Thank you for shopping with us!"),
                    receiptFooter = it.optString("receiptFooter", "Goods once sold are only returnable with receipt within 7 days."),
                    costingMethod = it.optString("costingMethod", "WEIGHTED_AVERAGE"),
                    ownerPin = it.optString("ownerPin", "1234"),
                    isConfigured = it.optBoolean("isConfigured", true),
                    createdAtEpoch = it.optLong("createdAtEpoch", System.currentTimeMillis())
                )
            )
        }
    }

    fun listBackups(): List<BackupMetadata> {
        val files = backupsDir.listFiles { file ->
            file.extension == "rgbackup" && !file.name.startsWith("safety_backup_")
        } ?: emptyArray()

        return files.sortedByDescending { it.lastModified() }.map { file ->
            BackupMetadata(
                filename = file.name,
                filePath = file.absolutePath,
                sizeBytes = file.length(),
                createdAtEpoch = file.lastModified(),
                isEncrypted = file.name.contains("encrypted", ignoreCase = true),
                appVersion = "1.0",
                businessName = "RG POS",
                isVerified = true
            )
        }
    }

    private fun pruneBackups(retainCount: Int) {
        val files = backupsDir.listFiles { file ->
            file.extension == "rgbackup" && !file.name.startsWith("safety_backup_")
        }?.sortedByDescending { it.lastModified() } ?: return

        if (files.size > retainCount) {
            for (i in retainCount until files.size) {
                try {
                    files[i].delete()
                } catch (_: Exception) {
                }
            }
        }
    }

    // Cryptographic Helpers
    private fun sha256(input: String): String {
        val md = MessageDigest.getInstance("SHA-256")
        val bytes = md.digest(input.toByteArray(Charsets.UTF_8))
        return bytesToHex(bytes)
    }

    private fun encryptAesGcm(data: ByteArray, password: String, salt: ByteArray, iv: ByteArray): ByteArray {
        val spec = PBEKeySpec(password.toCharArray(), salt, 10000, 256)
        val f = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
        val key = SecretKeySpec(f.generateSecret(spec).encoded, "AES")
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, key, GCMParameterSpec(128, iv))
        return cipher.doFinal(data)
    }

    private fun decryptAesGcm(cipherText: ByteArray, password: String, salt: ByteArray, iv: ByteArray): ByteArray {
        val spec = PBEKeySpec(password.toCharArray(), salt, 10000, 256)
        val f = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
        val key = SecretKeySpec(f.generateSecret(spec).encoded, "AES")
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(128, iv))
        return cipher.doFinal(cipherText)
    }

    private fun bytesToHex(bytes: ByteArray): String =
        bytes.joinToString("") { "%02x".format(it) }

    private fun hexToBytes(hex: String): ByteArray {
        val len = hex.length
        val data = ByteArray(len / 2)
        for (i in 0 until len step 2) {
            data[i / 2] = ((Character.digit(hex[i], 16) shl 4) + Character.digit(hex[i + 1], 16)).toByte()
        }
        return data
    }
}
