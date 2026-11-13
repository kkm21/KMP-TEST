package com.example.firstkmp

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeContentPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import firstkmp.shared.generated.resources.Res
import firstkmp.shared.generated.resources.dice1
import firstkmp.shared.generated.resources.dice2
import firstkmp.shared.generated.resources.dice3
import firstkmp.shared.generated.resources.dice4
import firstkmp.shared.generated.resources.dice5
import firstkmp.shared.generated.resources.dice6
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource
import kotlin.random.Random
import kotlin.random.nextInt
import androidx.compose.runtime.*
import com.example.firstkmp.ui.*

@Composable
//@Preview
fun App() {
    MaterialTheme {
        val backStack = remember { mutableStateListOf<Screen>(Screen.Login) }
        val currentScreen = backStack.last()

        val navigateTo: (Screen) -> Unit = { screen -> backStack.add(screen) }
        val popBack: () -> Unit = {
            if (backStack.size > 1) backStack.removeLast()
        }

        when (val screen = currentScreen) {
            is Screen.Login -> LoginScreen(
                onLoginSuccess = {
                    backStack.clear()
                    backStack.add(Screen.Dashboard)
                }
            )
            is Screen.Dashboard -> DashboardScreen(
                onNavigate = navigateTo,
                onLogout = {
                    backStack.clear()
                    backStack.add(Screen.Login)
                }
            )
            is Screen.MyProfile -> MyProfileScreen(onBack = popBack)
            is Screen.ProductList -> ProductListScreen(
                onBack = popBack,
                onProductClick = { id -> navigateTo(Screen.ProductDetail(id)) },
                onAddClick = { navigateTo(Screen.AddProduct) }
            )
            is Screen.ProductDetail -> ProductDetailScreen(
                productId = screen.id,
                onBack = popBack
            )
            is Screen.AddProduct -> AddProductScreen(onBack = popBack)
            is Screen.CartsList -> CartsListScreen(
                onBack = popBack,
                onCartClick = { id -> navigateTo(Screen.CartDetail(id)) }
            )
            is Screen.CartDetail -> CartDetailScreen(
                cartId = screen.id,
                onBack = popBack
            )
            is Screen.UserList -> UserListScreen(
                onBack = popBack,
                onUserClick = { id -> navigateTo(Screen.UserDetail(id)) }
            )
            is Screen.UserDetail -> UserDetailScreen(
                userId = screen.id,
                onBack = popBack
            )
            is Screen.Todos -> TodosScreen(onBack = popBack)
            is Screen.Announcements -> AnnouncementsScreen(onBack = popBack)
        }
    }
}

@Composable
@Preview
fun SimpleDiceRollerScreen() {

    var diceValue by remember { mutableStateOf(1) }
    var isRolling by remember { mutableStateOf(false) }
    var coroutineScope = rememberCoroutineScope()

    //Rolling animation

    val infiniteTransition = rememberInfiniteTransition()

    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = 300,
                easing = LinearEasing
            ),
            repeatMode = RepeatMode.Restart
        )
    )

    val scale by animateFloatAsState(
        targetValue = if (isRolling) 0.8f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        )
    )

    Box(modifier =
        Modifier.fillMaxSize().background(
            Brush.verticalGradient(
                colors = listOf(
                    Color(0xFF667eea),
                    Color(0xFF764ba2)
                )
            )
        ).safeContentPadding(),
        contentAlignment = Alignment.Center
    ){
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(48.dp),
            modifier = Modifier.padding(32.dp)
        ) {
            Text(
                text = "Simple Dice Roller",
                fontSize = 36.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )

            Card(
                modifier = Modifier.size(200.dp).scale(scale).rotate(if (isRolling)rotation else 0f),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 12.dp)
            ){
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ){
                    Image(
                        painter = painterResource(getDiceImage(diceValue)),
                        contentDescription = null,
                        modifier = Modifier.size(150.dp)
                    )
                }

            }



            Text(
                text = if (isRolling) "Rolling..." else "you rolled: $diceValue",
                fontSize = 24.sp,
                fontWeight = FontWeight.Medium,
                color = Color.White.copy(alpha = 0.9f)
            )

            //Rolling button

            Button(
                onClick = {
                    if (!isRolling) {
                        isRolling = true
                        coroutineScope.launch {
                            delay(800)
                            diceValue = Random.nextInt(1..6)
                            isRolling = false
                        }
                    }

                },
                modifier = Modifier.fillMaxWidth(fraction = 0.7f).height(64.dp),
                shape = RoundedCornerShape(32.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.White,
                    contentColor = Color.Black
                ),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 8.dp, pressedElevation = 4.dp),
                enabled = !isRolling
            ){

                Text(
                    text = if (isRolling) "Rolling..." else "Roll Dice",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
            }

        }
    }
}


fun getDiceImage(value: Int): DrawableResource{
    return when(value){
        1 -> Res.drawable.dice1
        2 -> Res.drawable.dice2
        3 -> Res.drawable.dice3
        4 -> Res.drawable.dice4
        5 -> Res.drawable.dice5
        6 -> Res.drawable.dice6
        else -> Res.drawable.dice1
    }

}