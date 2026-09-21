package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.database.AppDatabase
import com.example.data.model.*
import com.example.data.repository.PosRepository
import com.example.util.NetworkMonitor
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.Calendar
import java.util.UUID

data class CartItem(
    val id: String = UUID.randomUUID().toString(),
    val product: Product,
    val unitName: String,
    val conversionFactor: Double = 1.0,
    val quantity: Double = 1.0,
    val unitPrice: Double,
    val costPrice: Double,
    val isCustomPrice: Boolean = false,
    val discount: Double = 0.0
) {
    val subtotal: Double
        get() = ((unitPrice * quantity) - discount).coerceAtLeast(0.0)

    val totalBaseUnits: Double
        get() = quantity * conversionFactor
}

class PosViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getDatabase(application)
    val repository = PosRepository(db)
    private val networkMonitor = NetworkMonitor(application)

    // Global App State
    val business: StateFlow<Business?> = repository.businessFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val isOnline: StateFlow<Boolean> = networkMonitor.isOnline
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    val allProducts: StateFlow<List<Product>> = repository.allProducts
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val lowStockProducts: StateFlow<List<Product>> = repository.lowStockProducts
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allCustomers: StateFlow<List<Customer>> = repository.allCustomers
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allSuppliers: StateFlow<List<Supplier>> = repository.allSuppliers
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val recentSales: StateFlow<List<Sale>> = repository.recentSales
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val heldSales: StateFlow<List<Sale>> = repository.heldSales
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val activeCashSession: StateFlow<CashSession?> = repository.activeCashSession
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val allCashSessions: StateFlow<List<CashSession>> = repository.allCashSessions
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allExpenses: StateFlow<List<Expense>> = repository.allExpenses
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allPurchases: StateFlow<List<Purchase>> = repository.allPurchases
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allMovements: StateFlow<List<StockMovement>> = repository.allMovements
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allUsers: StateFlow<List<User>> = repository.allUsers
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val auditLogs: StateFlow<List<AuditLog>> = repository.auditLogs
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allAuditLogs: StateFlow<List<AuditLog>> = repository.auditLogs
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allReturns: StateFlow<List<ReturnOrder>> = repository.allReturns
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val mpesaRecons: StateFlow<List<MpesaRecon>> = repository.mpesaRecons
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allMpesaPayments: StateFlow<List<Payment>> = repository.allMpesaPayments
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allPayments: StateFlow<List<Payment>> = repository.allPayments
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allCompletedSales: StateFlow<List<Sale>> = repository.allCompletedSales
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allSaleItems: StateFlow<List<SaleItem>> = repository.allSaleItems
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allStaff: StateFlow<List<User>> = repository.allUsers
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val isSetupCompleted: StateFlow<Boolean> = repository.businessFlow.map { it != null && it.isConfigured }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    // Active User
    private val _currentUser = MutableStateFlow("Owner")
    val currentUser: StateFlow<String> = _currentUser.asStateFlow()

    fun setCurrentUser(user: String) {
        _currentUser.value = user
    }

    // POS Cart State
    private val _cartItems = MutableStateFlow<List<CartItem>>(emptyList())
    val cartItems: StateFlow<List<CartItem>> = _cartItems.asStateFlow()

    val cartItemCount: StateFlow<Int> = _cartItems.map { it.size }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    private val _selectedCustomer = MutableStateFlow<Customer?>(null)
    val selectedCustomer: StateFlow<Customer?> = _selectedCustomer.asStateFlow()

    private val _saleType = MutableStateFlow("RETAIL") // "RETAIL" or "WHOLESALE"
    val saleType: StateFlow<String> = _saleType.asStateFlow()

    private val _orderDiscount = MutableStateFlow(0.0)
    val orderDiscount: StateFlow<Double> = _orderDiscount.asStateFlow()

    val cartSubtotal: StateFlow<Double> = _cartItems.map { items ->
        items.sumOf { it.subtotal }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    val cartTotal: StateFlow<Double> = combine(cartSubtotal, _orderDiscount, business) { subtotal, discount, biz ->
        val taxableSubtotal = (subtotal - discount).coerceAtLeast(0.0)
        val tax = if (biz != null && biz.taxRatePercent > 0) {
            taxableSubtotal * (biz.taxRatePercent / 100.0)
        } else 0.0
        taxableSubtotal + tax
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    init {
        viewModelScope.launch {
            repository.seedStarterProductsIfEmpty()
        }
    }

    fun setSaleType(type: String) {
        _saleType.value = type
        // Recalculate cart item prices based on retail vs wholesale
        val updated = _cartItems.value.map { item ->
            val newPrice = if (type == "WHOLESALE") {
                item.product.wholesalePrice * item.conversionFactor
            } else {
                item.product.retailPrice * item.conversionFactor
            }
            if (!item.isCustomPrice) item.copy(unitPrice = newPrice) else item
        }
        _cartItems.value = updated
    }

    fun selectCustomer(customer: Customer?) {
        _selectedCustomer.value = customer
    }

    fun setOrderDiscount(discount: Double) {
        _orderDiscount.value = discount.coerceAtLeast(0.0)
    }

    fun addToCart(
        product: Product,
        unitName: String,
        conversionFactor: Double,
        quantity: Double = 1.0,
        unitPrice: Double? = null,
        costPrice: Double? = null
    ) {
        val calculatedPrice = unitPrice ?: (
            if (_saleType.value == "WHOLESALE") {
                product.wholesalePrice * conversionFactor
            } else {
                product.retailPrice * conversionFactor
            }
        )
        val calculatedCost = costPrice ?: (product.buyingCost * conversionFactor)

        val currentList = _cartItems.value.toMutableList()
        val existingIndex = currentList.indexOfFirst {
            it.product.id == product.id && it.unitName.equals(unitName, ignoreCase = true)
        }

        if (existingIndex >= 0) {
            val existing = currentList[existingIndex]
            currentList[existingIndex] = existing.copy(quantity = existing.quantity + quantity)
        } else {
            currentList.add(
                CartItem(
                    product = product,
                    unitName = unitName,
                    conversionFactor = conversionFactor,
                    quantity = quantity,
                    unitPrice = calculatedPrice,
                    costPrice = calculatedCost
                )
            )
        }
        _cartItems.value = currentList
    }

    fun updateCartItemQuantity(itemId: String, newQty: Double) {
        if (newQty <= 0) {
            removeCartItem(itemId)
            return
        }
        _cartItems.value = _cartItems.value.map {
            if (it.id == itemId) it.copy(quantity = newQty) else it
        }
    }

    fun updateCartItemPrice(itemId: String, newPrice: Double) {
        _cartItems.value = _cartItems.value.map {
            if (it.id == itemId) it.copy(unitPrice = newPrice.coerceAtLeast(0.0), isCustomPrice = true) else it
        }
    }

    fun updateCartItemDiscount(itemId: String, discount: Double) {
        _cartItems.value = _cartItems.value.map {
            if (it.id == itemId) it.copy(discount = discount.coerceAtLeast(0.0)) else it
        }
    }

    fun removeCartItem(itemId: String) {
        _cartItems.value = _cartItems.value.filterNot { it.id == itemId }
    }

    fun clearCart() {
        _cartItems.value = emptyList()
        _selectedCustomer.value = null
        _orderDiscount.value = 0.0
    }

    fun holdCurrentSale(onSuccess: (Long) -> Unit) {
        if (_cartItems.value.isEmpty()) return
        viewModelScope.launch {
            val receiptNum = repository.generateNextReceiptNumber()
            val total = cartTotal.value
            val subtotal = cartSubtotal.value
            val sale = Sale(
                receiptNumber = receiptNum,
                customerId = _selectedCustomer.value?.id,
                customerName = _selectedCustomer.value?.name ?: "Walk-in Customer",
                subtotal = subtotal,
                discount = _orderDiscount.value,
                total = total,
                saleType = _saleType.value,
                status = "HELD",
                cashierName = _currentUser.value
            )
            val saleItems = _cartItems.value.map { item ->
                SaleItem(
                    saleId = 0,
                    productId = item.product.id,
                    productName = item.product.name,
                    unitName = item.unitName,
                    quantity = item.quantity,
                    unitPrice = item.unitPrice,
                    costPrice = item.costPrice,
                    subtotal = item.subtotal,
                    baseQuantityDeducted = item.totalBaseUnits
                )
            }
            val id = repository.holdSale(sale, saleItems)
            clearCart()
            onSuccess(id)
        }
    }

    fun resumeHeldSale(saleId: Long, onResume: () -> Unit) {
        viewModelScope.launch {
            val (sale, items) = repository.getSaleDetails(saleId)
            if (sale != null) {
                // Restore customer
                if (sale.customerId != null) {
                    val customer = allCustomers.value.firstOrNull { it.id == sale.customerId }
                    _selectedCustomer.value = customer
                } else {
                    _selectedCustomer.value = null
                }
                _saleType.value = sale.saleType
                _orderDiscount.value = sale.discount

                // Rebuild cart items
                val restoredItems = items.mapNotNull { item ->
                    val product = repository.getProductById(item.productId) ?: return@mapNotNull null
                    val factor = if (item.unitName.equals(product.baseUnit, ignoreCase = true)) {
                        1.0
                    } else {
                        val conversions = repository.getUnitConversionsSync(product.id)
                        conversions.firstOrNull { it.unitName.equals(item.unitName, ignoreCase = true) }?.conversionFactor ?: 1.0
                    }
                    CartItem(
                        product = product,
                        unitName = item.unitName,
                        conversionFactor = factor,
                        quantity = item.quantity,
                        unitPrice = item.unitPrice,
                        costPrice = item.costPrice
                    )
                }
                _cartItems.value = restoredItems
                repository.deleteHeldSale(saleId)
                onResume()
            }
        }
    }

    fun completeSale(
        cashAmount: Double,
        mpesaAmount: Double,
        mpesaRef: String,
        creditAmount: Double,
        onComplete: (Long) -> Unit,
        onError: (String) -> Unit
    ) {
        val total = cartTotal.value
        val paidTotal = cashAmount + mpesaAmount + creditAmount

        // Validate payment total (within 0.01 precision)
        if (Math.abs(paidTotal - total) > 0.05) {
            onError("Payment total (KSh %.2f) does not match sale total (KSh %.2f)".format(paidTotal, total))
            return
        }

        // Validate customer selection if credit used (Part 10 & Non-negotiable requirement)
        if (creditAmount > 0 && _selectedCustomer.value == null) {
            onError("Customer selection is mandatory for Credit payment")
            return
        }

        viewModelScope.launch {
            try {
                val receiptNum = repository.generateNextReceiptNumber()
                val subtotal = cartSubtotal.value
                val biz = business.value
                val tax = if (biz != null && biz.taxRatePercent > 0) {
                    (subtotal - _orderDiscount.value) * (biz.taxRatePercent / 100.0)
                } else 0.0

                val costTotal = _cartItems.value.sumOf { it.costPrice * it.quantity }

                val sale = Sale(
                    receiptNumber = receiptNum,
                    customerId = _selectedCustomer.value?.id,
                    customerName = _selectedCustomer.value?.name ?: "Walk-in Customer",
                    subtotal = subtotal,
                    discount = _orderDiscount.value,
                    tax = tax,
                    total = total,
                    costTotal = costTotal,
                    saleType = _saleType.value,
                    status = "COMPLETED",
                    cashierName = _currentUser.value
                )

                val saleItems = _cartItems.value.map { item ->
                    SaleItem(
                        saleId = 0,
                        productId = item.product.id,
                        productName = item.product.name,
                        unitName = item.unitName,
                        quantity = item.quantity,
                        unitPrice = item.unitPrice,
                        costPrice = item.costPrice,
                        subtotal = item.subtotal,
                        baseQuantityDeducted = item.totalBaseUnits
                    )
                }

                val payments = mutableListOf<Payment>()
                if (cashAmount > 0) {
                    payments.add(Payment(saleId = 0, paymentMethod = "CASH", amount = cashAmount))
                }
                if (mpesaAmount > 0) {
                    payments.add(Payment(saleId = 0, paymentMethod = "MPESA", amount = mpesaAmount, mpesaRef = mpesaRef))
                }
                if (creditAmount > 0) {
                    payments.add(Payment(saleId = 0, paymentMethod = "CREDIT", amount = creditAmount))
                }

                val saleId = repository.completeSale(sale, saleItems, payments, _currentUser.value)
                clearCart()
                onComplete(saleId)
            } catch (e: Exception) {
                onError("Unable to save the sale. Nothing was changed: ${e.message}")
            }
        }
    }

    // Dashboard Calculations
    val todaySalesStats: StateFlow<Triple<Double, Double, Int>> = recentSales.map { sales ->
        val cal = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val startOfDay = cal.timeInMillis
        val todaysSales = sales.filter { it.dateEpoch >= startOfDay && it.status == "COMPLETED" }
        val revenue = todaysSales.sumOf { it.total }
        val profit = todaysSales.sumOf { it.total - it.costTotal }
        val count = todaysSales.size
        Triple(revenue, profit, count)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), Triple(0.0, 0.0, 0))

    // Payment breakdown for today
    fun getTodayPaymentSummary(onResult: (cash: Double, mpesa: Double, credit: Double) -> Unit) {
        viewModelScope.launch {
            val cal = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            val payments = repository.getPaymentsBetween(cal.timeInMillis, System.currentTimeMillis())
            val cash = payments.filter { it.paymentMethod == "CASH" }.sumOf { it.amount }
            val mpesa = payments.filter { it.paymentMethod == "MPESA" }.sumOf { it.amount }
            val credit = payments.filter { it.paymentMethod == "CREDIT" }.sumOf { it.amount }
            onResult(cash, mpesa, credit)
        }
    }

    val totalOutstandingCredit: StateFlow<Double> = allCustomers.map { customers ->
        customers.sumOf { it.outstandingCredit }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)
}
