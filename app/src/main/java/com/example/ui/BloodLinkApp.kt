package com.example.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.background
import androidx.compose.ui.Alignment
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.font.FontWeight
import com.example.ui.screens.*

sealed class Screen(val route: String, val title: String) {
    object Home : Screen("home", "Home")
    object Search : Screen("search", "Search")
    object Services : Screen("services", "Services")
    object Messages : Screen("messages", "Messages")
    object Profile : Screen("profile", "Profile")
    object CreateAccount : Screen("create_account", "Create Account")
}

@Composable
fun BloodLinkApp(viewModel: BloodViewModel) {
    val isNetworkAvailable by viewModel.isNetworkAvailable.collectAsState()
    
    var currentScreen by remember { mutableStateOf<Screen>(Screen.Home) }
    
    // Chat recipient routing states
    var chatRecipientId by remember { mutableStateOf("") }
    var chatRecipientName by remember { mutableStateOf("") }

    if (!isNetworkAvailable) {
        NetworkErrorScreen(onRetry = { viewModel.checkNetworkStatus() })
    } else {
        Scaffold(
        bottomBar = {
            NavigationBar(
                modifier = Modifier.shadow(
                    elevation = 8.dp,
                    shape = RoundedCornerShape(0.dp),
                    clip = false
                ),
                containerColor = Color.White,
                tonalElevation = 0.dp
            ) {
                val items = listOf(
                    Triple(Screen.Home, Icons.Default.Home, Icons.Outlined.Home),
                    Triple(Screen.Search, Icons.Default.Search, Icons.Outlined.Search),
                    Triple(Screen.Services, Icons.Default.Dashboard, Icons.Outlined.Dashboard),
                    Triple(Screen.Messages, Icons.Default.Chat, Icons.Outlined.Chat),
                    Triple(Screen.Profile, Icons.Default.Person, Icons.Outlined.Person)
                )

                items.forEach { (screen, selectedIcon, unselectedIcon) ->
                    val isSelected = currentScreen.route == screen.route
                    NavigationBarItem(
                        selected = isSelected,
                        onClick = {
                            currentScreen = screen
                            // Reset chat recipient when navigating directly to chat list tab
                            if (screen == Screen.Messages) {
                                chatRecipientId = ""
                                chatRecipientName = ""
                            }
                        },
                        label = { 
                            Text(
                                text = screen.title,
                                fontSize = 11.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            )
                        },
                        icon = {
                            Icon(
                                imageVector = if (isSelected) selectedIcon else unselectedIcon,
                                contentDescription = screen.title
                            )
                        },
                        alwaysShowLabel = true,
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = Color(0xFFD32F2F),
                            unselectedIconColor = Color.Gray,
                            selectedTextColor = Color(0xFFD32F2F),
                            unselectedTextColor = Color.Gray,
                            indicatorColor = Color(0xFFFFECEF)
                        )
                    )
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            AnimatedContent(
                targetState = currentScreen,
                transitionSpec = {
                    fadeIn().togetherWith(fadeOut())
                },
                label = "ScreenTransition"
            ) { targetScreen ->
                when (targetScreen) {
                    Screen.Home -> {
                        HomeScreen(
                            viewModel = viewModel,
                            onNavigateToSearch = { currentScreen = Screen.Search },
                            onNavigateToServices = { currentScreen = Screen.Services },
                            onNavigateToMessages = { id, name ->
                                chatRecipientId = id
                                chatRecipientName = name
                                currentScreen = Screen.Messages
                            },
                            onNavigateToProfile = { currentScreen = Screen.Profile }
                        )
                    }
                    Screen.Search -> {
                        SearchScreen(
                            viewModel = viewModel,
                            onNavigateToMessages = { id, name ->
                                chatRecipientId = id
                                chatRecipientName = name
                                currentScreen = Screen.Messages
                            }
                        )
                    }
                    Screen.Services -> {
                        ServicesScreen(viewModel = viewModel)
                    }
                    Screen.Messages -> {
                        MessagesScreen(
                            viewModel = viewModel,
                            initialRecipientId = chatRecipientId,
                            initialRecipientName = chatRecipientName
                        )
                    }
                    Screen.Profile -> {
                        ProfileScreen(
                            viewModel = viewModel,
                            onNavigateToCreateAccount = { currentScreen = Screen.CreateAccount }
                        )
                    }
                    Screen.CreateAccount -> {
                        CreateAccountScreen(
                            viewModel = viewModel,
                            onBack = { currentScreen = Screen.Profile },
                            onAccountCreated = { currentScreen = Screen.Profile }
                        )
                    }
                }
            }
        }
    }
}
}

@Composable
fun NetworkErrorScreen(onRetry: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFFAFAFA)),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(32.dp)
        ) {
            // Icon Background
            Box(
                modifier = Modifier
                    .size(100.dp)
                    .background(Color(0xFFFFEBEE), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = "No Internet",
                    tint = Color(0xFFD32F2F),
                    modifier = Modifier.size(48.dp)
                )
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            Text(
                text = "কোনো ইন্টারনেট সংযোগ নেই",
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF1E1E1E),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
            
            Text(
                text = "No Internet Connection",
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                color = Color.Gray,
                modifier = Modifier.padding(top = 4.dp),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Text(
                text = "অ্যাপটি সচল করতে ইন্টারনেট সংযোগ প্রয়োজন। অনুগ্রহ করে আপনার ওয়াই-ফাই বা মোবাইল ডাটা চালু আছে কিনা তা নিশ্চিত করুন।",
                fontSize = 14.sp,
                color = Color.Gray,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                lineHeight = 20.sp
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = "An active internet connection is required. Please check your Wi-Fi or mobile data settings and try again.",
                fontSize = 12.sp,
                color = Color.LightGray,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                lineHeight = 18.sp
            )
            
            Spacer(modifier = Modifier.height(32.dp))
            
            Button(
                onClick = onRetry,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD32F2F)),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = "Retry"
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "পুনরায় চেষ্টা করুন / Retry",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
            }
        }
    }
}
