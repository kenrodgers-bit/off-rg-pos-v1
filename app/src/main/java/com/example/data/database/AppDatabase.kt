package com.example.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.data.dao.*
import com.example.data.model.*

@Database(
    entities = [
        Business::class,
        User::class,
        Category::class,
        Product::class,
        UnitConversion::class,
        StockMovement::class,
        Customer::class,
        CustomerTransaction::class,
        Supplier::class,
        Purchase::class,
        PurchaseItem::class,
        Sale::class,
        SaleItem::class,
        Payment::class,
        Expense::class,
        ExpenseCategory::class,
        CashSession::class,
        MpesaRecon::class,
        ReturnOrder::class,
        ReturnItem::class,
        AuditLog::class,
        AppSetting::class
    ],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun businessDao(): BusinessDao
    abstract fun userDao(): UserDao
    abstract fun categoryDao(): CategoryDao
    abstract fun productDao(): ProductDao
    abstract fun stockMovementDao(): StockMovementDao
    abstract fun customerDao(): CustomerDao
    abstract fun supplierDao(): SupplierDao
    abstract fun purchaseDao(): PurchaseDao
    abstract fun saleDao(): SaleDao
    abstract fun paymentDao(): PaymentDao
    abstract fun expenseDao(): ExpenseDao
    abstract fun cashSessionDao(): CashSessionDao
    abstract fun mpesaReconDao(): MpesaReconDao
    abstract fun returnDao(): ReturnDao
    abstract fun auditLogDao(): AuditLogDao
    abstract fun appSettingDao(): AppSettingDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "rg_pos_database"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
