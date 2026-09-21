package com.example.data.model

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "business")
data class Business(
    @PrimaryKey val id: Int = 1,
    val name: String,
    val phone: String = "",
    val location: String = "",
    val address: String = "",
    val mpesaTill: String = "",
    val kraPin: String = "",
    val ownerName: String = "",
    val currency: String = "KES",
    val retailEnabled: Boolean = true,
    val wholesaleEnabled: Boolean = true,
    val allowCredit: Boolean = true,
    val lowStockThreshold: Int = 10,
    val taxRatePercent: Double = 0.0,
    val receiptHeader: String = "Thank you for shopping with us!",
    val receiptFooter: String = "Goods once sold are only returnable with receipt within 7 days.",
    // Costing method used to recalculate Product.buyingCost when new stock is received.
    // One of: "WEIGHTED_AVERAGE", "LAST_PURCHASE_COST"
    val costingMethod: String = "WEIGHTED_AVERAGE",
    val ownerPin: String,
    val isConfigured: Boolean = false,
    val createdAtEpoch: Long = System.currentTimeMillis()
)

@Entity(tableName = "users")
data class User(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val username: String = "",
    val fullName: String = "",
    val role: String = "CASHIER", // OWNER, MANAGER, CASHIER, STOREKEEPER
    val pin: String = "",
    val phone: String = "",
    val isActive: Boolean = true
)

typealias Staff = User
val User.name: String get() = if (fullName.isNotBlank()) fullName else username

@Entity(tableName = "categories")
data class Category(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String
)

@Entity(
    tableName = "products",
    indices = [Index(value = ["barcode"]), Index(value = ["sku"]), Index(value = ["name"])]
)
data class Product(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val sku: String = "",
    val barcode: String = "",
    val categoryId: Long = 0,
    val categoryName: String = "General",
    val brand: String = "",
    val baseUnit: String = "Piece", // e.g. Piece, Kg, Packet, Metre
    val buyingCost: Double = 0.0, // Buying cost per base unit
    val retailPrice: Double = 0.0, // Retail selling price per base unit
    val wholesalePrice: Double = 0.0, // Wholesale selling price per base unit
    val wholesaleEnabled: Boolean = true,
    val currentStockBase: Double = 0.0, // ALWAYS stored in base unit
    val minStock: Double = 10.0,
    val supplierId: Long = 0,
    val trackIntactPackages: Boolean = false,
    val isArchived: Boolean = false
)

@Entity(
    tableName = "unit_conversions",
    indices = [Index(value = ["productId"]), Index(value = ["barcode"])]
)
data class UnitConversion(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val productId: Long,
    val unitName: String, // e.g. "Carton 18 × 500ml", "50 KG Sack", "Pack 30", "Dozen"
    val conversionFactor: Double, // Number of base units in 1 of this unit (e.g. 18 for Carton 18, 22 for Carton 22, 600 for Sweets Carton)
    val customRetailPrice: Double? = null, // Independent Retail Selling Price (NOT auto-divided/calculated)
    val customWholesalePrice: Double? = null, // Independent Wholesale Selling Price
    val purchaseCost: Double? = null, // Actual purchase cost for this packaging configuration
    val barcode: String = "", // Dedicated barcode for this packaging form
    val sku: String = "", // Packaging-specific SKU
    val purchaseEnabled: Boolean = true, // Whether this form can be purchased
    val sellingEnabled: Boolean = true, // Whether this form can be sold in POS
    val canOpen: Boolean = false, // Whether the package can be broken/opened into base units (Break-bulk)
    val intactCount: Int = 0, // Number of intact/unopened packages tracked
    val parentUnitName: String = "", // For hierarchy (e.g. "Pack" in 1 Carton = 20 Packs)
    val parentUnitMultiplier: Double = 1.0, // Multiplier to parent (e.g. 20)
    val isActive: Boolean = true
)

typealias PackagingProfile = UnitConversion

val UnitConversion.retailPrice: Double? get() = customRetailPrice
val UnitConversion.wholesalePrice: Double? get() = customWholesalePrice
val UnitConversion.baseQuantity: Double get() = conversionFactor

@Entity(
    tableName = "stock_movements",
    indices = [Index(value = ["productId"]), Index(value = ["dateEpoch"])]
)
data class StockMovement(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val productId: Long,
    val productName: String,
    val movementType: String, // PURCHASE, SALE, ADJUSTMENT, DAMAGE, RETURN
    val quantityChangeBase: Double, // Positive for in, negative for out
    val unitUsed: String,
    val quantityInUnit: Double,
    val stockAfterBase: Double,
    val referenceId: String = "",
    val reason: String = "",
    val staffName: String = "Staff",
    val dateEpoch: Long = System.currentTimeMillis()
)

@Entity(tableName = "customers")
data class Customer(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val phone: String = "",
    val email: String = "",
    val address: String = "",
    val creditLimit: Double = 50000.0,
    val outstandingCredit: Double = 0.0,
    val totalPurchases: Double = 0.0,
    val createdAtEpoch: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "customer_transactions",
    indices = [Index(value = ["customerId"]), Index(value = ["dateEpoch"])]
)
data class CustomerTransaction(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val customerId: Long,
    val customerName: String,
    val type: String, // CREDIT_SALE, REPAYMENT
    val amount: Double,
    val referenceId: String = "",
    val notes: String = "",
    val balanceAfter: Double = 0.0,
    val dateEpoch: Long = System.currentTimeMillis()
)

@Entity(tableName = "suppliers")
data class Supplier(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val phone: String = "",
    val address: String = "",
    val contactPerson: String = "",
    val outstandingBalance: Double = 0.0
)

@Entity(tableName = "purchases")
data class Purchase(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val invoiceNumber: String,
    val supplierId: Long,
    val supplierName: String,
    val totalAmount: Double,
    val itemsCount: Int = 1,
    val notes: String = "",
    val staffName: String = "Staff",
    val dateEpoch: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "purchase_items",
    indices = [Index(value = ["purchaseId"]), Index(value = ["productId"])]
)
data class PurchaseItem(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val purchaseId: Long,
    val productId: Long,
    val productName: String,
    val unitName: String,
    val quantity: Double,
    val costPerUnit: Double,
    val totalCost: Double,
    val baseUnitsAdded: Double
)

@Entity(
    tableName = "sales",
    indices = [Index(value = ["receiptNumber"], unique = true), Index(value = ["dateEpoch"])]
)
data class Sale(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val receiptNumber: String, // e.g. #RG-000142
    val customerId: Long? = null,
    val customerName: String = "Walk-in Customer",
    val subtotal: Double,
    val discount: Double = 0.0,
    val tax: Double = 0.0,
    val total: Double,
    val costTotal: Double = 0.0, // For gross profit calculations
    val saleType: String = "RETAIL", // RETAIL, WHOLESALE
    val status: String = "COMPLETED", // COMPLETED, HELD, RETURNED
    val cashierName: String = "Cashier",
    val notes: String = "",
    val dateEpoch: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "sale_items",
    indices = [Index(value = ["saleId"]), Index(value = ["productId"])]
)
data class SaleItem(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val saleId: Long,
    val productId: Long,
    val productName: String,
    val unitName: String,
    val quantity: Double,
    val unitPrice: Double,
    val costPrice: Double,
    val subtotal: Double,
    val baseQuantityDeducted: Double,
    val returnedQuantity: Double = 0.0
)

@Entity(
    tableName = "payments",
    indices = [Index(value = ["saleId"])]
)
data class Payment(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val saleId: Long,
    val paymentMethod: String, // CASH, MPESA, CREDIT
    val amount: Double,
    val mpesaRef: String = "",
    val dateEpoch: Long = System.currentTimeMillis()
)

@Entity(tableName = "expenses")
data class Expense(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val amount: Double,
    val category: String,
    val description: String,
    val staffMember: String = "Staff",
    val dateEpoch: Long = System.currentTimeMillis()
)

@Entity(tableName = "expense_categories")
data class ExpenseCategory(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String
)

@Entity(tableName = "cash_sessions")
data class CashSession(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val cashierName: String,
    val openingCash: Double,
    val closingCash: Double = 0.0,
    val expectedCash: Double = 0.0,
    val actualCash: Double = 0.0,
    val difference: Double = 0.0,
    val cashSales: Double = 0.0,
    val cashExpenses: Double = 0.0,
    val cashRefunds: Double = 0.0,
    val status: String = "OPEN", // OPEN, CLOSED
    val openedEpoch: Long = System.currentTimeMillis(),
    val closedEpoch: Long? = null
)

@Entity(tableName = "mpesa_reconciliations")
data class MpesaRecon(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val dateEpoch: Long = System.currentTimeMillis(),
    val expectedMpesa: Double,
    val actualMpesa: Double,
    val difference: Double,
    val notes: String = "",
    val staffName: String = "Staff"
)

@Entity(tableName = "return_orders")
data class ReturnOrder(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val returnNumber: String,
    val originalSaleId: Long,
    val originalReceiptNumber: String,
    val customerName: String,
    val totalRefundAmount: Double,
    val reason: String,
    val authorizedBy: String,
    val dateEpoch: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "return_items",
    indices = [Index(value = ["returnOrderId"]), Index(value = ["productId"])]
)
data class ReturnItem(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val returnOrderId: Long,
    val saleItemId: Long,
    val productId: Long,
    val productName: String,
    val unitName: String,
    val quantity: Double,
    val refundUnitPrice: Double,
    val totalRefund: Double,
    val baseUnitsRestored: Double
)

@Entity(tableName = "audit_logs")
data class AuditLog(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val staffName: String,
    val action: String,
    val recordInfo: String,
    val dateEpoch: Long = System.currentTimeMillis()
)

@Entity(tableName = "app_settings")
data class AppSetting(
    @PrimaryKey val key: String,
    val value: String
)
