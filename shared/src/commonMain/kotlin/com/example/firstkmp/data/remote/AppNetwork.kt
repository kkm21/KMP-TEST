package com.example.firstkmp.data.remote

import com.example.firstkmp.data.model.*
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.json.Json

// Manages the JWT token and current Admin User session across the app
object SessionManager {
    private val _authToken = MutableStateFlow<String?>(null)
    val authToken: StateFlow<String?> = _authToken.asStateFlow()

    private val _currentUser = MutableStateFlow<AuthResponse?>(null)
    val currentUser: StateFlow<AuthResponse?> = _currentUser.asStateFlow()

    fun saveSession(auth: AuthResponse) {
        _authToken.value = auth.accessToken
        _currentUser.value = auth
    }

    fun logout() {
        _authToken.value = null
        _currentUser.value = null
    }
}

class ApiService {
    private val baseUrl = "https://dummyjson.com"

    private val client = HttpClient {
        install(ContentNegotiation) {
            json(Json {
                ignoreUnknownKeys = true
                isLenient = true
            })
        }
    }

    // API 1: POST Login
    suspend fun login(request: LoginRequest): AuthResponse =
        client.post("$baseUrl/auth/login") {
            contentType(ContentType.Application.Json)
            setBody(request)
        }.body()

    // API 2: GET Current Authenticated User (Requires Bearer Token)
    suspend fun getCurrentUserProfile(): User =
        client.get("$baseUrl/auth/me") {
            SessionManager.authToken.value?.let { token ->
                header(HttpHeaders.Authorization, "Bearer $token")
            }
        }.body()

    // API 3: GET Products Inventory
    suspend fun getProducts(): List<Product> =
        client.get("$baseUrl/products?limit=25").body<ProductsResponse>().products

    // API 4: GET Single Product Detail
    suspend fun getProductById(id: Int): Product =
        client.get("$baseUrl/products/$id").body()

    // API 5: POST Add New Product
    suspend fun addProduct(request: CreateProductRequest): Product =
        client.post("$baseUrl/products/add") {
            contentType(ContentType.Application.Json)
            setBody(request)
        }.body()

    // API 6: GET Customer Carts / Orders
    suspend fun getCarts(): List<Cart> =
        client.get("$baseUrl/carts?limit=20").body<CartsResponse>().carts

    // API 7: GET Single Cart / Order Detail
    suspend fun getCartById(id: Int): Cart =
        client.get("$baseUrl/carts/$id").body()

    // API 8: GET Users / Employees
    suspend fun getUsers(): List<User> =
        client.get("$baseUrl/users?limit=25").body<UsersResponse>().users

    // API 9: GET Single User Detail
    suspend fun getUserById(id: Int): User =
        client.get("$baseUrl/users/$id").body()

    // API 10: GET Admin Todos
    suspend fun getTodos(): List<Todo> =
        client.get("$baseUrl/todos?limit=25").body<TodosResponse>().todos

    // API 11: GET Daily Quotes / Announcements
    suspend fun getQuotes(): List<Quote> =
        client.get("$baseUrl/quotes?limit=20").body<QuotesResponse>().quotes
}

class AppRepository(private val api: ApiService = ApiService()) {
    suspend fun login(username: String, pass: String) = runCatching {
        val response = api.login(LoginRequest(username.trim(), pass.trim()))
        SessionManager.saveSession(response)
        response
    }

    suspend fun fetchMyProfile() = runCatching { api.getCurrentUserProfile() }
    suspend fun fetchProducts() = runCatching { api.getProducts() }
    suspend fun fetchProductDetail(id: Int) = runCatching { api.getProductById(id) }
    suspend fun createProduct(req: CreateProductRequest) = runCatching { api.addProduct(req) }
    suspend fun fetchCarts() = runCatching { api.getCarts() }
    suspend fun fetchCartDetail(id: Int) = runCatching { api.getCartById(id) }
    suspend fun fetchUsers() = runCatching { api.getUsers() }
    suspend fun fetchUserDetail(id: Int) = runCatching { api.getUserById(id) }
    suspend fun fetchTodos() = runCatching { api.getTodos() }
    suspend fun fetchQuotes() = runCatching { api.getQuotes() }
}

object AppContainer {
    val repository = AppRepository()
}