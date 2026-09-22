package com.example.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.example.data.model.*
import kotlinx.coroutines.flow.Flow

@Dao
interface BusinessDao {
    @Query("SELECT * FROM business WHERE id = 1 LIMIT 1")
    fun getBusinessFlow(): Flow<Business?>

    @Query("SELECT * FROM business WHERE id = 1 LIMIT 1")
    suspend fun getBusiness(): Business?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(business: Business)
}

@Dao
interface UserDao {
    @Query("SELECT * FROM users ORDER BY id ASC")
    fun getAllUsersFlow(): Flow<List<User>>

    @Query("SELECT * FROM users ORDER BY id ASC")
    suspend fun getAllUsersList(): List<User>

    @Query("SELECT * FROM users WHERE pin = :pin AND isActive = 1 LIMIT 1")
    suspend fun getUserByPin(pin: String): User?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUser(user: User): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUsers(users: List<User>)

    @Update
    suspend fun updateUser(user: User)

    @Query("DELETE FROM users WHERE id = :id")
    suspend fun deleteUser(id: Long)
}

@Dao
interface CategoryDao {
    @Query("SELECT * FROM categories ORDER BY name ASC")
    fun getAllCategories(): Flow<List<Category>>

    @Query("SELECT * FROM categories ORDER BY name ASC")
    suspend fun getAllCategoriesList(): List<Category>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCategory(category: Category): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCategories(categories: List<Category>)
}

@Dao
interface ProductDao {
    @Query("SELECT * FROM products WHERE isArchived = 0 ORDER BY name ASC")
    fun getAllProducts(): Flow<List<Product>>

    @Query("SELECT * FROM products ORDER BY id ASC")
    suspend fun getAllProductsList(): List<Product>

    @Query("SELECT * FROM products WHERE isArchived = 0 AND currentStockBase <= minStock ORDER BY currentStockBase ASC")
    fun getLowStockProducts(): Flow<List<Product>>

    @Query("SELECT * FROM products WHERE id = :id LIMIT 1")
    suspend fun getProductById(id: Long): Product?

    @Query("SELECT * FROM products WHERE barcode = :barcode AND isArchived = 0 LIMIT 1")
    suspend fun getProductByBarcode(barcode: String): Product?

    @Query("SELECT * FROM products WHERE (name LIKE '%' || :query || '%' OR sku LIKE '%' || :query || '%' OR barcode LIKE '%' || :query || '%') AND isArchived = 0 ORDER BY name ASC")
    fun searchProducts(query: String): Flow<List<Product>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProduct(product: Product): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProducts(products: List<Product>)

    @Update
    suspend fun updateProduct(product: Product)

    @Query("UPDATE products SET currentStockBase = currentStockBase + :deltaBase WHERE id = :id")
    suspend fun updateStock(id: Long, deltaBase: Double)

    @Query("SELECT * FROM unit_conversions WHERE productId = :productId")
    fun getUnitConversionsForProduct(productId: Long): Flow<List<UnitConversion>>

    @Query("SELECT * FROM unit_conversions WHERE productId = :productId")
    suspend fun getUnitConversionsSync(productId: Long): List<UnitConversion>

    @Query("SELECT * FROM unit_conversions")
    suspend fun getAllUnitConversions(): List<UnitConversion>

    @Query("SELECT * FROM unit_conversions WHERE barcode = :barcode AND barcode != '' LIMIT 1")
    suspend fun getPackagingProfileByBarcode(barcode: String): UnitConversion?

    @Query("SELECT * FROM unit_conversions WHERE barcode != ''")
    suspend fun getAllPackagingProfilesWithBarcode(): List<UnitConversion>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUnitConversion(conversion: UnitConversion): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUnitConversions(conversions: List<UnitConversion>)

    @Update
    suspend fun updateUnitConversion(conversion: UnitConversion)

    @Query("UPDATE unit_conversions SET intactCount = intactCount + :delta WHERE id = :id")
    suspend fun updateIntactCount(id: Long, delta: Int)

    @Query("DELETE FROM unit_conversions WHERE id = :id")
    suspend fun deleteUnitConversion(id: Long)
}

@Dao
interface StockMovementDao {
    @Query("SELECT * FROM stock_movements ORDER BY dateEpoch DESC LIMIT 200")
    fun getAllMovements(): Flow<List<StockMovement>>

    @Query("SELECT * FROM stock_movements ORDER BY dateEpoch DESC")
    suspend fun getAllMovementsList(): List<StockMovement>

    @Query("SELECT * FROM stock_movements WHERE productId = :productId ORDER BY dateEpoch DESC")
    fun getMovementsForProduct(productId: Long): Flow<List<StockMovement>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMovement(movement: StockMovement): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMovements(movements: List<StockMovement>)
}

@Dao
interface CustomerDao {
    @Query("SELECT * FROM customers ORDER BY name ASC")
    fun getAllCustomers(): Flow<List<Customer>>

    @Query("SELECT * FROM customers ORDER BY name ASC")
    suspend fun getAllCustomersList(): List<Customer>

    @Query("SELECT * FROM customers WHERE id = :id LIMIT 1")
    suspend fun getCustomerById(id: Long): Customer?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCustomer(customer: Customer): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCustomers(customers: List<Customer>)

    @Update
    suspend fun updateCustomer(customer: Customer)

    @Query("UPDATE customers SET outstandingCredit = outstandingCredit + :deltaCredit, totalPurchases = totalPurchases + :deltaPurchases WHERE id = :id")
    suspend fun updateCreditAndPurchases(id: Long, deltaCredit: Double, deltaPurchases: Double)

    @Query("SELECT * FROM customer_transactions WHERE customerId = :customerId ORDER BY dateEpoch DESC")
    fun getCustomerTransactions(customerId: Long): Flow<List<CustomerTransaction>>

    @Query("SELECT * FROM customer_transactions ORDER BY dateEpoch DESC")
    suspend fun getAllTransactionsList(): List<CustomerTransaction>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransaction(transaction: CustomerTransaction): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransactions(transactions: List<CustomerTransaction>)
}

@Dao
interface SupplierDao {
    @Query("SELECT * FROM suppliers ORDER BY name ASC")
    fun getAllSuppliers(): Flow<List<Supplier>>

    @Query("SELECT * FROM suppliers ORDER BY name ASC")
    suspend fun getAllSuppliersList(): List<Supplier>

    @Query("SELECT * FROM suppliers WHERE id = :id LIMIT 1")
    suspend fun getSupplierById(id: Long): Supplier?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSupplier(supplier: Supplier): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSuppliers(suppliers: List<Supplier>)

    @Update
    suspend fun updateSupplier(supplier: Supplier)
}

@Dao
interface PurchaseDao {
    @Query("SELECT * FROM purchases ORDER BY dateEpoch DESC")
    fun getAllPurchases(): Flow<List<Purchase>>

    @Query("SELECT * FROM purchases ORDER BY dateEpoch DESC")
    suspend fun getAllPurchasesList(): List<Purchase>

    @Query("SELECT * FROM purchase_items WHERE purchaseId = :purchaseId")
    suspend fun getPurchaseItems(purchaseId: Long): List<PurchaseItem>

    @Query("SELECT * FROM purchase_items")
    suspend fun getAllPurchaseItemsList(): List<PurchaseItem>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPurchase(purchase: Purchase): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPurchases(purchases: List<Purchase>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPurchaseItems(items: List<PurchaseItem>)
}

@Dao
interface SaleDao {
    @Query("SELECT * FROM sales ORDER BY dateEpoch DESC")
    fun getAllSales(): Flow<List<Sale>>

    @Query("SELECT * FROM sales WHERE status = 'COMPLETED' ORDER BY dateEpoch DESC")
    fun getAllCompletedSalesFlow(): Flow<List<Sale>>

    @Query("SELECT * FROM sales ORDER BY dateEpoch DESC")
    suspend fun getAllSalesList(): List<Sale>

    @Query("SELECT * FROM sales WHERE status = 'COMPLETED' ORDER BY dateEpoch DESC LIMIT 50")
    fun getRecentCompletedSales(): Flow<List<Sale>>

    @Query("SELECT * FROM sales WHERE status = 'HELD' ORDER BY dateEpoch DESC")
    fun getHeldSales(): Flow<List<Sale>>

    @Query("SELECT * FROM sales WHERE id = :id LIMIT 1")
    suspend fun getSaleById(id: Long): Sale?

    @Query("SELECT * FROM sales WHERE receiptNumber = :receiptNumber LIMIT 1")
    suspend fun getSaleByReceiptNumber(receiptNumber: String): Sale?

    @Query("SELECT * FROM sales WHERE dateEpoch >= :startTime AND dateEpoch <= :endTime AND status = 'COMPLETED'")
    suspend fun getSalesBetween(startTime: Long, endTime: Long): List<Sale>

    @Query("SELECT * FROM sales WHERE status = 'COMPLETED'")
    suspend fun getAllCompletedSalesList(): List<Sale>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSale(sale: Sale): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSales(sales: List<Sale>)

    @Update
    suspend fun updateSale(sale: Sale)

    @Query("DELETE FROM sales WHERE id = :id")
    suspend fun deleteSale(id: Long)

    @Query("SELECT * FROM sale_items WHERE saleId = :saleId")
    fun getSaleItemsFlow(saleId: Long): Flow<List<SaleItem>>

    @Query("SELECT * FROM sale_items WHERE saleId = :saleId")
    suspend fun getSaleItems(saleId: Long): List<SaleItem>

    @Query("SELECT * FROM sale_items")
    suspend fun getAllSaleItemsList(): List<SaleItem>

    @Query("SELECT * FROM sale_items")
    fun getAllSaleItemsFlow(): Flow<List<SaleItem>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSaleItems(items: List<SaleItem>)

    @Update
    suspend fun updateSaleItem(item: SaleItem)
}

@Dao
interface PaymentDao {
    @Query("SELECT * FROM payments WHERE saleId = :saleId")
    suspend fun getPaymentsForSale(saleId: Long): List<Payment>

    @Query("SELECT * FROM payments WHERE saleId = :saleId")
    fun getPaymentsForSaleFlow(saleId: Long): Flow<List<Payment>>

    @Query("SELECT * FROM payments ORDER BY dateEpoch DESC")
    fun getAllPaymentsFlow(): Flow<List<Payment>>

    @Query("SELECT * FROM payments WHERE paymentMethod = 'MPESA' ORDER BY dateEpoch DESC")
    fun getMpesaPaymentsFlow(): Flow<List<Payment>>

    @Query("SELECT * FROM payments WHERE dateEpoch >= :startTime AND dateEpoch <= :endTime")
    suspend fun getPaymentsBetween(startTime: Long, endTime: Long): List<Payment>

    @Query("SELECT * FROM payments")
    suspend fun getAllPayments(): List<Payment>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPayments(payments: List<Payment>)
}

@Dao
interface ExpenseDao {
    @Query("SELECT * FROM expenses ORDER BY dateEpoch DESC")
    fun getAllExpenses(): Flow<List<Expense>>

    @Query("SELECT * FROM expenses ORDER BY dateEpoch DESC")
    suspend fun getAllExpensesList(): List<Expense>

    @Query("SELECT * FROM expenses WHERE dateEpoch >= :startTime AND dateEpoch <= :endTime")
    suspend fun getExpensesBetween(startTime: Long, endTime: Long): List<Expense>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExpense(expense: Expense): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExpenses(expenses: List<Expense>)

    @Query("SELECT * FROM expense_categories ORDER BY name ASC")
    fun getExpenseCategories(): Flow<List<ExpenseCategory>>

    @Query("SELECT * FROM expense_categories ORDER BY name ASC")
    suspend fun getAllExpenseCategoriesList(): List<ExpenseCategory>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExpenseCategory(category: ExpenseCategory): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExpenseCategories(categories: List<ExpenseCategory>)
}

@Dao
interface CashSessionDao {
    @Query("SELECT * FROM cash_sessions WHERE status = 'OPEN' ORDER BY openedEpoch DESC LIMIT 1")
    fun getActiveSessionFlow(): Flow<CashSession?>

    @Query("SELECT * FROM cash_sessions WHERE status = 'OPEN' ORDER BY openedEpoch DESC LIMIT 1")
    suspend fun getActiveSession(): CashSession?

    @Query("SELECT * FROM cash_sessions ORDER BY openedEpoch DESC")
    fun getAllSessions(): Flow<List<CashSession>>

    @Query("SELECT * FROM cash_sessions ORDER BY openedEpoch DESC")
    suspend fun getAllSessionsList(): List<CashSession>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSession(session: CashSession): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSessions(sessions: List<CashSession>)

    @Update
    suspend fun updateSession(session: CashSession)
}

@Dao
interface MpesaReconDao {
    @Query("SELECT * FROM mpesa_reconciliations ORDER BY dateEpoch DESC")
    fun getAllReconciliations(): Flow<List<MpesaRecon>>

    @Query("SELECT * FROM mpesa_reconciliations ORDER BY dateEpoch DESC")
    suspend fun getAllReconsList(): List<MpesaRecon>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRecon(recon: MpesaRecon): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRecons(recons: List<MpesaRecon>)
}

@Dao
interface ReturnDao {
    @Query("SELECT * FROM return_orders ORDER BY dateEpoch DESC")
    fun getAllReturns(): Flow<List<ReturnOrder>>

    @Query("SELECT * FROM return_orders ORDER BY dateEpoch DESC")
    fun getAllReturnsFlow(): Flow<List<ReturnOrder>>

    @Query("SELECT * FROM return_orders ORDER BY dateEpoch DESC")
    suspend fun getAllReturnsList(): List<ReturnOrder>

    @Query("SELECT * FROM return_items WHERE returnOrderId = :returnOrderId")
    suspend fun getReturnItems(returnOrderId: Long): List<ReturnItem>

    @Query("SELECT * FROM return_items")
    suspend fun getAllReturnItems(): List<ReturnItem>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReturnOrder(order: ReturnOrder): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReturns(orders: List<ReturnOrder>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReturnItems(items: List<ReturnItem>)
}

@Dao
interface AuditLogDao {
    @Query("SELECT * FROM audit_logs ORDER BY dateEpoch DESC LIMIT 300")
    fun getAllLogs(): Flow<List<AuditLog>>

    @Query("SELECT * FROM audit_logs ORDER BY dateEpoch DESC")
    suspend fun getAllLogsList(): List<AuditLog>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLog(log: AuditLog): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLogs(logs: List<AuditLog>)
}

@Dao
interface AppSettingDao {
    @Query("SELECT value FROM app_settings WHERE `key` = :key LIMIT 1")
    suspend fun getSetting(key: String): String?

    @Query("SELECT * FROM app_settings")
    suspend fun getAllSettingsList(): List<AppSetting>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun setSetting(setting: AppSetting)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSettings(settings: List<AppSetting>)
}

@Dao
interface ImportBatchDao {
    @Query("SELECT * FROM import_batches ORDER BY dateEpoch DESC")
    fun getAllBatchesFlow(): Flow<List<ImportBatch>>

    @Query("SELECT * FROM import_batches ORDER BY dateEpoch DESC")
    suspend fun getAllBatchesList(): List<ImportBatch>

    @Query("SELECT * FROM import_batches WHERE id = :id LIMIT 1")
    suspend fun getBatchById(id: Long): ImportBatch?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBatch(batch: ImportBatch): Long
}
