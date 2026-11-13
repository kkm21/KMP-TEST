package com.example.firstkmp.data.model

import kotlinx.serialization.Serializable

// --- AUTHENTICATION MODELS (POST /auth/login & GET /auth/me) ---
@Serializable
data class LoginRequest(
    val username: String,
    val password: String,
    val expiresInMins: Int = 60
)

@Serializable
data class AuthResponse(
    val id: Int,
    val username: String,
    val email: String,
    val firstName: String,
    val lastName: String,
    val gender: String = "",
    val accessToken: String = ""
)

// --- PRODUCTS & INVENTORY MODELS (GET & POST) ---
@Serializable
data class ProductsResponse(val products: List<Product>, val total: Int = 0)

@Serializable
data class Product(
    val id: Int = 0,
    val title: String,
    val description: String = "",
    val price: Double,
    val discountPercentage: Double = 0.0,
    val rating: Double = 0.0,
    val stock: Int = 0,
    val brand: String = "Generic",
    val category: String = "General"
)

@Serializable
data class CreateProductRequest(
    val title: String,
    val price: Double,
    val category: String,
    val brand: String,
    val description: String
)

// --- CARTS / ORDERS MODELS (GET /carts & GET /carts/{id}) ---
@Serializable
data class CartsResponse(val carts: List<Cart>, val total: Int = 0)

@Serializable
data class Cart(
    val id: Int,
    val products: List<CartItem> = emptyList(),
    val total: Double,
    val discountedTotal: Double,
    val userId: Int,
    val totalProducts: Int,
    val totalQuantity: Int
)

@Serializable
data class CartItem(
    val id: Int,
    val title: String,
    val price: Double,
    val quantity: Int,
    val total: Double
)

// --- USERS / STAFF DIRECTORY MODELS ---
@Serializable
data class UsersResponse(val users: List<User>, val total: Int = 0)

@Serializable
data class User(
    val id: Int,
    val firstName: String,
    val lastName: String,
    val email: String,
    val phone: String = "",
    val username: String = "",
    val role: String = "user",
    val company: Company? = null
)

@Serializable
data class Company(val name: String = "", val title: String = "", val department: String = "")

// --- TODOS & QUOTES MODELS ---
@Serializable
data class TodosResponse(val todos: List<Todo>)

@Serializable
data class Todo(val id: Int, val todo: String, val completed: Boolean, val userId: Int)

@Serializable
data class QuotesResponse(val quotes: List<Quote>)

@Serializable
data class Quote(val id: Int, val quote: String, val author: String)