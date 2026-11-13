package com.example.firstkmp.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.firstkmp.data.model.*
import com.example.firstkmp.data.remote.AppContainer
import com.example.firstkmp.data.remote.AppRepository
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

// Generic UI State for Read Screens
sealed interface UiState<out T> {
    data object Loading : UiState<Nothing>
    data class Success<T>(val data: T) : UiState<T>
    data class Error(val message: String) : UiState<Nothing>
}

abstract class BaseDataViewModel<T>(
    private val fetcher: suspend () -> Result<T>
) : ViewModel() {
    private val _uiState = MutableStateFlow<UiState<T>>(UiState.Loading)
    val uiState: StateFlow<UiState<T>> = _uiState.asStateFlow()

    init { loadData() }

    fun loadData() {
        viewModelScope.launch {
            _uiState.value = UiState.Loading
            fetcher()
                .onSuccess { _uiState.value = UiState.Success(it) }
                .onFailure { _uiState.value = UiState.Error(it.message ?: "Network error") }
        }
    }
}

// --- 1. LOGIN VIEWMODEL ---
sealed interface LoginUiState {
    data object Idle : LoginUiState
    data object Loading : LoginUiState
    data class Success(val user: AuthResponse) : LoginUiState
    data class Error(val message: String) : LoginUiState
}

class LoginViewModel(private val repo: AppRepository = AppContainer.repository) : ViewModel() {
    // Pre-filled with valid DummyJSON test credentials for quick testing
    val username = MutableStateFlow("emilys")
    val password = MutableStateFlow("emilyspass")

    private val _loginState = MutableStateFlow<LoginUiState>(LoginUiState.Idle)
    val loginState: StateFlow<LoginUiState> = _loginState.asStateFlow()

    fun submitLogin() {
        if (username.value.isBlank() || password.value.isBlank()) {
            _loginState.value = LoginUiState.Error("Username and password cannot be empty")
            return
        }
        viewModelScope.launch {
            _loginState.value = LoginUiState.Loading
            repo.login(username.value, password.value)
                .onSuccess { _loginState.value = LoginUiState.Success(it) }
                .onFailure { _loginState.value = LoginUiState.Error("Invalid credentials (Try: emilys / emilyspass)") }
        }
    }
}

// --- 2. DASHBOARD SUMMARY VIEWMODEL (Calls Multiple APIs in Parallel for KPIs) ---
data class DashboardSummary(
    val adminProfile: User?,
    val productCount: Int,
    val activeOrdersCount: Int,
    val totalRevenue: Double,
    val pendingTasksCount: Int
)

class DashboardViewModel(private val repo: AppRepository = AppContainer.repository) :
    BaseDataViewModel<DashboardSummary>({
        runCatching {
            kotlinx.coroutines.coroutineScope {
                val profileDeferred = async { repo.fetchMyProfile().getOrNull() }
                val productsDeferred = async { repo.fetchProducts().getOrDefault(emptyList()) }
                val cartsDeferred = async { repo.fetchCarts().getOrDefault(emptyList()) }
                val todosDeferred = async { repo.fetchTodos().getOrDefault(emptyList()) }

                val carts = cartsDeferred.await()
                val todos = todosDeferred.await()

                DashboardSummary(
                    adminProfile = profileDeferred.await(),
                    productCount = productsDeferred.await().size,
                    activeOrdersCount = carts.size,
                    totalRevenue = carts.sumOf { it.discountedTotal },
                    pendingTasksCount = todos.count { !it.completed }
                )
            }
        }
    })

// --- 3. ADD PRODUCT VIEWMODEL (POST /products/add) ---
sealed interface SubmissionState {
    data object Idle : SubmissionState
    data object Submitting : SubmissionState
    data class Created(val product: Product) : SubmissionState
    data class Error(val message: String) : SubmissionState
}

class AddProductViewModel(private val repo: AppRepository = AppContainer.repository) : ViewModel() {
    val title = MutableStateFlow("")
    val price = MutableStateFlow("")
    val brand = MutableStateFlow("")
    val category = MutableStateFlow("")
    val description = MutableStateFlow("")

    private val _state = MutableStateFlow<SubmissionState>(SubmissionState.Idle)
    val state: StateFlow<SubmissionState> = _state.asStateFlow()

    fun createProduct() {
        val parsedPrice = price.value.toDoubleOrNull()
        if (title.value.isBlank() || parsedPrice == null) {
            _state.value = SubmissionState.Error("Please enter a valid title and numeric price.")
            return
        }
        viewModelScope.launch {
            _state.value = SubmissionState.Submitting
            val req = CreateProductRequest(
                title = title.value,
                price = parsedPrice,
                brand = brand.value.ifBlank { "In-House" },
                category = category.value.ifBlank { "General" },
                description = description.value
            )
            repo.createProduct(req)
                .onSuccess { _state.value = SubmissionState.Created(it) }
                .onFailure { _state.value = SubmissionState.Error(it.message ?: "Failed to create product") }
        }
    }
}

// --- 4 TO 11. STANDARD READ VIEWMODELS ---
class MyProfileViewModel(repo: AppRepository = AppContainer.repository) :
    BaseDataViewModel<User>({ repo.fetchMyProfile() })

class ProductsViewModel(repo: AppRepository = AppContainer.repository) :
    BaseDataViewModel<List<Product>>({ repo.fetchProducts() })

class ProductDetailViewModel(id: Int, repo: AppRepository = AppContainer.repository) :
    BaseDataViewModel<Product>({ repo.fetchProductDetail(id) })

class CartsViewModel(repo: AppRepository = AppContainer.repository) :
    BaseDataViewModel<List<Cart>>({ repo.fetchCarts() })

class CartDetailViewModel(id: Int, repo: AppRepository = AppContainer.repository) :
    BaseDataViewModel<Cart>({ repo.fetchCartDetail(id) })

class UsersViewModel(repo: AppRepository = AppContainer.repository) :
    BaseDataViewModel<List<User>>({ repo.fetchUsers() })

class UserDetailViewModel(id: Int, repo: AppRepository = AppContainer.repository) :
    BaseDataViewModel<User>({ repo.fetchUserDetail(id) })

class TodosViewModel(repo: AppRepository = AppContainer.repository) :
    BaseDataViewModel<List<Todo>>({ repo.fetchTodos() })

class QuotesViewModel(repo: AppRepository = AppContainer.repository) :
    BaseDataViewModel<List<Quote>>({ repo.fetchQuotes() })