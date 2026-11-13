package com.example.firstkmp.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.firstkmp.data.model.*
import com.example.firstkmp.data.remote.SessionManager
import kotlin.math.roundToInt

// Navigation Routes for all 11 Screens
sealed interface Screen {
    data object Login : Screen
    data object Dashboard : Screen
    data object MyProfile : Screen
    data object ProductList : Screen
    data class ProductDetail(val id: Int) : Screen
    data object AddProduct : Screen
    data object CartsList : Screen
    data class CartDetail(val id: Int) : Screen
    data object UserList : Screen
    data class UserDetail(val id: Int) : Screen
    data object Todos : Screen
    data object Announcements : Screen
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun <T> StateScaffold(
    title: String,
    onBack: (() -> Unit)? = null,
    actionButton: (@Composable () -> Unit)? = null,
    viewModel: BaseDataViewModel<T>,
    content: @Composable (T) -> Unit
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(title, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    if (onBack != null) {
                        TextButton(onClick = onBack) { Text("← Back") }
                    }
                },
                actions = { actionButton?.invoke() },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            )
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            when (val current = state) {
                is UiState.Loading -> CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                is UiState.Error -> {
                    Column(
                        modifier = Modifier.align(Alignment.Center).padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("Error: ${current.message}", color = MaterialTheme.colorScheme.error)
                        Spacer(Modifier.height(12.dp))
                        Button(onClick = { viewModel.loadData() }) { Text("Retry") }
                    }
                }
                is UiState.Success -> content(current.data)
            }
        }
    }
}

// =========================================================================
// SCREEN 1: LOGIN SCREEN (POST /auth/login)
// =========================================================================
@Composable
fun LoginScreen(
    onLoginSuccess: () -> Unit,
    vm: LoginViewModel = viewModel { LoginViewModel() }
) {
    val username by vm.username.collectAsStateWithLifecycle()
    val password by vm.password.collectAsStateWithLifecycle()
    val loginState by vm.loginState.collectAsStateWithLifecycle()

    LaunchedEffect(loginState) {
        if (loginState is LoginUiState.Success) {
            onLoginSuccess()
        }
    }

    Box(modifier = Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
        Card(
            modifier = Modifier.widthIn(max = 420.dp).fillMaxWidth(),
            elevation = CardDefaults.cardElevation(6.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("Enterprise Admin Portal", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(4.dp))
                Text("Sign in to manage inventory, orders & staff", style = MaterialTheme.typography.bodyMedium)
                Spacer(Modifier.height(24.dp))

                OutlinedTextField(
                    value = username,
                    onValueChange = { vm.username.value = it },
                    label = { Text("Admin Username") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(12.dp))

                OutlinedTextField(
                    value = password,
                    onValueChange = { vm.password.value = it },
                    label = { Text("Password") },
                    visualTransformation = PasswordVisualTransformation(),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    text = "Demo Account: emilys / emilyspass",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(Modifier.height(20.dp))

                if (loginState is LoginUiState.Error) {
                    Text(
                        text = (loginState as LoginUiState.Error).message,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                    Spacer(Modifier.height(12.dp))
                }

                Button(
                    onClick = { vm.submitLogin() },
                    enabled = loginState !is LoginUiState.Loading,
                    modifier = Modifier.fillMaxWidth().height(48.dp)
                ) {
                    if (loginState is LoginUiState.Loading) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                    } else {
                        Text("Sign In to Dashboard")
                    }
                }
            }
        }
    }
}

// =========================================================================
// SCREEN 2: MANAGEMENT DASHBOARD (Parallel KPIs + Module Launcher)
// =========================================================================
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    onNavigate: (Screen) -> Unit,
    onLogout: () -> Unit,
    vm: DashboardViewModel = viewModel { DashboardViewModel() }
) {
    val sessionUser by SessionManager.currentUser.collectAsStateWithLifecycle()
    val summaryState by vm.uiState.collectAsStateWithLifecycle()

    val modules = listOf(
        Triple("📦 Inventory Catalog", "View & audit products", Screen.ProductList),
        Triple("➕ Add New Product", "Create SKU via POST API", Screen.AddProduct),
        Triple("🛒 Customer Orders", "Track carts & revenue", Screen.CartsList),
        Triple("👥 Staff & Users", "Manage employee directory", Screen.UserList),
        Triple("✅ Operations Tasks", "Admin to-do checklist", Screen.Todos),
        Triple("📢 Announcements", "Company-wide quotes", Screen.Announcements),
        Triple("🔐 My Admin Profile", "View JWT session & role", Screen.MyProfile)
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Management Dashboard", fontWeight = FontWeight.Bold)
                        Text(
                            text = "Logged in as: ${sessionUser?.firstName ?: "Admin"} (@${sessionUser?.username ?: ""})",
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                },
                actions = {
                    TextButton(onClick = {
                        SessionManager.logout()
                        onLogout()
                    }) {
                        Text("Logout", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Section 1: Live KPI Metrics Row
            item {
                Text("Live Store Metrics", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(8.dp))
                when (val s = summaryState) {
                    is UiState.Loading -> LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                    is UiState.Error -> Text("Could not load KPIs: ${s.message}", color = MaterialTheme.colorScheme.error)
                    is UiState.Success -> {
                        val data = s.data
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            KpiCard("SKUs", "${data.productCount}", Modifier.weight(1f))
                            KpiCard("Orders", "${data.activeOrdersCount}", Modifier.weight(1f))
                            KpiCard("Revenue", "$${data.totalRevenue.roundToInt()}", Modifier.weight(1f))
                            KpiCard("Open Tasks", "${data.pendingTasksCount}", Modifier.weight(1f))
                        }
                    }
                }
            }

            // Section 2: Management Modules Grid
            item {
                Text("Management Modules", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }

            items(modules.chunked(2)) { rowItems ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    rowItems.forEach { (title, subtitle, screen) ->
                        Card(
                            modifier = Modifier
                                .weight(1f)
                                .height(110.dp)
                                .clickable { onNavigate(screen) },
                            elevation = CardDefaults.cardElevation(3.dp)
                        ) {
                            Column(
                                modifier = Modifier.fillMaxSize().padding(16.dp),
                                verticalArrangement = Arrangement.Center
                            ) {
                                Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                Spacer(Modifier.height(4.dp))
                                Text(subtitle, style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                    if (rowItems.size == 1) {
                        Spacer(Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

@Composable
fun KpiCard(label: String, value: String, modifier: Modifier = Modifier) {
    ElevatedCard(modifier = modifier) {
        Column(Modifier.padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            Text(label, style = MaterialTheme.typography.labelSmall)
        }
    }
}

// =========================================================================
// SCREEN 3: MY ADMIN PROFILE SCREEN (GET /auth/me with Bearer Token)
// =========================================================================
@Composable
fun MyProfileScreen(
    onBack: () -> Unit,
    vm: MyProfileViewModel = viewModel { MyProfileViewModel() }
) {
    val token by SessionManager.authToken.collectAsStateWithLifecycle()
    StateScaffold(title = "Authenticated Admin Profile", onBack = onBack, viewModel = vm) { user ->
        Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("${user.firstName} ${user.lastName}", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                    Text("System Role: ${user.role.uppercase()}", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                    Text("Username: @${user.username}")
                    Text("Email: ${user.email}")
                    Text("Phone: ${user.phone}")
                    user.company?.let {
                        HorizontalDivider(Modifier.padding(vertical = 8.dp))
                        Text("Department: ${it.department} — ${it.title}")
                        Text("Organization: ${it.name}")
                    }
                }
            }
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp)) {
                    Text("Active JWT Bearer Token (Truncated)", fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(4.dp))
                    Text("${token?.take(60)}...", style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}

// =========================================================================
// SCREEN 4: INVENTORY PRODUCT LIST SCREEN (GET /products)
// =========================================================================
@Composable
fun ProductListScreen(
    onBack: () -> Unit,
    onProductClick: (Int) -> Unit,
    onAddClick: () -> Unit,
    vm: ProductsViewModel = viewModel { ProductsViewModel() }
) {
    StateScaffold(
        title = "Inventory Catalog",
        onBack = onBack,
        actionButton = {
            Button(onClick = onAddClick, modifier = Modifier.padding(end = 8.dp)) {
                Text("+ Add SKU")
            }
        },
        viewModel = vm
    ) { products ->
        LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            items(products, key = { it.id }) { product ->
                Card(
                    modifier = Modifier.fillMaxWidth().clickable { onProductClick(product.id) },
                    elevation = CardDefaults.cardElevation(2.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(product.title, fontWeight = FontWeight.Bold)
                            Text("Brand: ${product.brand} • Category: ${product.category}", style = MaterialTheme.typography.bodySmall)
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text("$${product.price}", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                            Text("Stock: ${product.stock}", style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
            }
        }
    }
}

// =========================================================================
// SCREEN 5: PRODUCT DETAIL SCREEN (GET /products/{id})
// =========================================================================
@Composable
fun ProductDetailScreen(
    productId: Int,
    onBack: () -> Unit,
    vm: ProductDetailViewModel = viewModel(key = "prod_$productId") { ProductDetailViewModel(productId) }
) {
    StateScaffold(title = "SKU #$productId Details", onBack = onBack, viewModel = vm) { product ->
        Column(Modifier.fillMaxSize().padding(16.dp).verticalScroll(rememberScrollState())) {
            Text(product.title, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            Text("Brand: ${product.brand} | Category: ${product.category}")
            Spacer(Modifier.height(16.dp))
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Unit Price: $${product.price} (${product.discountPercentage}% Discount)", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.primary)
                    Text("Warehouse Stock: ${product.stock} units")
                    Text("Customer Rating: ⭐ ${product.rating} / 5.0")
                }
            }
            Spacer(Modifier.height(16.dp))
            Text("Product Description", fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(4.dp))
            Text(product.description)
        }
    }
}

// =========================================================================
// SCREEN 6: ADD NEW PRODUCT SCREEN (POST /products/add)
// =========================================================================
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddProductScreen(
    onBack: () -> Unit,
    vm: AddProductViewModel = viewModel { AddProductViewModel() }
) {
    val title by vm.title.collectAsStateWithLifecycle()
    val price by vm.price.collectAsStateWithLifecycle()
    val brand by vm.brand.collectAsStateWithLifecycle()
    val category by vm.category.collectAsStateWithLifecycle()
    val description by vm.description.collectAsStateWithLifecycle()
    val state by vm.state.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Create New SKU (POST API)") },
                navigationIcon = { TextButton(onClick = onBack) { Text("← Back") } }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedTextField(value = title, onValueChange = { vm.title.value = it }, label = { Text("Product Title *") }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(value = price, onValueChange = { vm.price.value = it }, label = { Text("Price (USD) *") }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(value = brand, onValueChange = { vm.brand.value = it }, label = { Text("Brand Name") }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(value = category, onValueChange = { vm.category.value = it }, label = { Text("Category") }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(value = description, onValueChange = { vm.description.value = it }, label = { Text("Description") }, modifier = Modifier.fillMaxWidth(), minLines = 3)

            Button(
                onClick = { vm.createProduct() },
                enabled = state !is SubmissionState.Submitting,
                modifier = Modifier.fillMaxWidth().height(48.dp)
            ) {
                Text(if (state is SubmissionState.Submitting) "Submitting to Server..." else "Create Product")
            }

            when (val s = state) {
                is SubmissionState.Error -> Text(s.message, color = MaterialTheme.colorScheme.error)
                is SubmissionState.Created -> {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(Modifier.padding(16.dp)) {
                            Text("✅ Server Created SKU #${s.product.id}", fontWeight = FontWeight.Bold)
                            Text("Title: ${s.product.title} — $${s.product.price}")
                        }
                    }
                }
                else -> {}
            }
        }
    }
}

// =========================================================================
// SCREEN 7: CUSTOMER ORDERS / CARTS LIST (GET /carts)
// =========================================================================
@Composable
fun CartsListScreen(
    onBack: () -> Unit,
    onCartClick: (Int) -> Unit,
    vm: CartsViewModel = viewModel { CartsViewModel() }
) {
    StateScaffold(title = "Customer Orders & Carts", onBack = onBack, viewModel = vm) { carts ->
        LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            items(carts, key = { it.id }) { cart ->
                Card(
                    modifier = Modifier.fillMaxWidth().clickable { onCartClick(cart.id) },
                    elevation = CardDefaults.cardElevation(2.dp)
                ) {
                    Row(Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                        Column {
                            Text("Order #${cart.id} (User ID: ${cart.userId})", fontWeight = FontWeight.Bold)
                            Text("${cart.totalProducts} unique items • ${cart.totalQuantity} total units", style = MaterialTheme.typography.bodySmall)
                        }
                        Text("$${cart.discountedTotal}", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

// =========================================================================
// SCREEN 8: ORDER / CART DETAIL SCREEN (GET /carts/{id})
// =========================================================================
@Composable
fun CartDetailScreen(
    cartId: Int,
    onBack: () -> Unit,
    vm: CartDetailViewModel = viewModel(key = "cart_$cartId") { CartDetailViewModel(cartId) }
) {
    StateScaffold(title = "Order #$cartId Invoice", onBack = onBack, viewModel = vm) { cart ->
        Column(Modifier.fillMaxSize().padding(16.dp)) {
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp)) {
                    Text("Customer User ID: #${cart.userId}", fontWeight = FontWeight.Bold)
                    Text("Subtotal: $${cart.total}")
                    Text("Final Discounted Total: $${cart.discountedTotal}", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                }
            }
            Spacer(Modifier.height(16.dp))
            Text("Line Items (${cart.products.size})", fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(cart.products, key = { it.id }) { item ->
                    Card(Modifier.fillMaxWidth()) {
                        Row(Modifier.fillMaxWidth().padding(12.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                            Column(Modifier.weight(1f)) {
                                Text(item.title, fontWeight = FontWeight.Bold)
                                Text("Qty: ${item.quantity} × $${item.price}", style = MaterialTheme.typography.bodySmall)
                            }
                            Text("$${item.total}", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

// =========================================================================
// SCREEN 9: STAFF & USER DIRECTORY SCREEN (GET /users)
// =========================================================================
@Composable
fun UserListScreen(
    onBack: () -> Unit,
    onUserClick: (Int) -> Unit,
    vm: UsersViewModel = viewModel { UsersViewModel() }
) {
    StateScaffold(title = "Employee & User Directory", onBack = onBack, viewModel = vm) { users ->
        LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            items(users, key = { it.id }) { user ->
                Card(
                    modifier = Modifier.fillMaxWidth().clickable { onUserClick(user.id) },
                    elevation = CardDefaults.cardElevation(2.dp)
                ) {
                    Row(Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                        Column {
                            Text("${user.firstName} ${user.lastName}", fontWeight = FontWeight.Bold)
                            Text(user.email, style = MaterialTheme.typography.bodySmall)
                        }
                        AssistChip(onClick = { onUserClick(user.id) }, label = { Text(user.role.uppercase()) })
                    }
                }
            }
        }
    }
}

// =========================================================================
// SCREEN 10: STAFF DETAIL SCREEN (GET /users/{id})
// =========================================================================
@Composable
fun UserDetailScreen(
    userId: Int,
    onBack: () -> Unit,
    vm: UserDetailViewModel = viewModel(key = "usr_$userId") { UserDetailViewModel(userId) }
) {
    StateScaffold(title = "Staff Record #$userId", onBack = onBack, viewModel = vm) { user ->
        Column(Modifier.fillMaxSize().padding(16.dp)) {
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("${user.firstName} ${user.lastName}", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                    Text("Access Role: ${user.role.uppercase()}", color = MaterialTheme.colorScheme.primary)
                    Text("Username: @${user.username}")
                    Text("Email: ${user.email}")
                    Text("Phone: ${user.phone}")
                    user.company?.let {
                        HorizontalDivider(Modifier.padding(vertical = 8.dp))
                        Text("Company: ${it.name}", fontWeight = FontWeight.Bold)
                        Text("Title: ${it.title} (${it.department})")
                    }
                }
            }
        }
    }
}

// =========================================================================
// SCREEN 11: ADMIN OPERATIONS TASKS & ANNOUNCEMENTS (GET /todos & /quotes)
// =========================================================================
@Composable
fun TodosScreen(
    onBack: () -> Unit,
    vm: TodosViewModel = viewModel { TodosViewModel() }
) {
    StateScaffold(title = "Operations Task Checklist", onBack = onBack, viewModel = vm) { todos ->
        LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(todos, key = { it.id }) { item ->
                Card(Modifier.fillMaxWidth()) {
                    Row(Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = item.completed, onCheckedChange = null)
                        Spacer(Modifier.width(10.dp))
                        Text(item.todo, modifier = Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

@Composable
fun AnnouncementsScreen(
    onBack: () -> Unit,
    vm: QuotesViewModel = viewModel { QuotesViewModel() }
) {
    StateScaffold(title = "Company Notices & Quotes", onBack = onBack, viewModel = vm) { quotes ->
        LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            items(quotes, key = { it.id }) { quote ->
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp)) {
                        Text("\"${quote.quote}\"", fontStyle = FontStyle.Italic)
                        Spacer(Modifier.height(6.dp))
                        Text("— ${quote.author}", modifier = Modifier.align(Alignment.End), fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}