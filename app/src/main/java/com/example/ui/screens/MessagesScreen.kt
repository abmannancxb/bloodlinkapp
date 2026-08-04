package com.example.ui.screens

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.ChatMessage
import com.example.ui.BloodViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MessagesScreen(
    viewModel: BloodViewModel,
    initialRecipientId: String,
    initialRecipientName: String
) {
    val context = LocalContext.current
    val donors by viewModel.allDonors.collectAsStateWithLifecycle()
    val messages by viewModel.allMessages.collectAsStateWithLifecycle()
    val currentUserProfile by viewModel.currentUserProfile.collectAsStateWithLifecycle()

    var activeRecipientId by remember { mutableStateOf(initialRecipientId) }
    var activeRecipientName by remember { mutableStateOf(initialRecipientName) }
    var chatText by remember { mutableStateOf("") }

    val chatDonors = remember(donors, currentUserProfile.id) {
        donors.filter { it.id != currentUserProfile.id }
    }

    // Filter messages for current active chat
    val activeChatMessages = remember(messages, activeRecipientId) {
        messages.filter { msg ->
            (msg.senderId == currentUserProfile.id && msg.receiverId == activeRecipientId) ||
                    (msg.senderId == activeRecipientId && msg.receiverId == currentUserProfile.id)
        }.sortedBy { it.timestamp }
    }

    if (activeRecipientId.isBlank()) {
        // Show Conversations list
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("Messages", fontWeight = FontWeight.Bold) },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
                )
            }
        ) { innerPadding ->
            if (donors.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    contentAlignment = Alignment.Center
                ) {
                    Text("No donors available to chat.")
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color(0xFFF9FAFC))
                        .padding(innerPadding),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    item {
                        Text(
                            "Select a donor to start a conversation:",
                            color = Color.Gray,
                            fontSize = 14.sp,
                            modifier = Modifier.padding(bottom = 6.dp)
                        )
                    }

                    items(
                        items = chatDonors,
                        key = { it.id },
                        contentType = { "chat_donor" }
                    ) { donor ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    activeRecipientId = donor.id
                                    activeRecipientName = donor.name
                                },
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.White)
                        ) {
                            Row(
                                modifier = Modifier.padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box {
                                    Box(
                                        modifier = Modifier
                                            .size(44.dp)
                                            .background(Color(0xFFFFEBEE), CircleShape),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        val initials = donor.name.split(" ").take(2).map { it.firstOrNull() ?: "" }.joinToString("")
                                        Text(initials, color = Color(0xFFD32F2F), fontWeight = FontWeight.Bold)
                                    }
                                    if (donor.isOnline) {
                                        Box(
                                            modifier = Modifier
                                                .size(10.dp)
                                                .background(Color(0xFF4CAF50), CircleShape)
                                                .align(Alignment.BottomEnd)
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.width(16.dp))

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(donor.name, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                    Text(
                                        "Blood Group: ${donor.bloodGroup} • ${donor.location}",
                                        fontSize = 12.sp,
                                        color = Color.Gray
                                    )
                                }

                                Icon(
                                    imageVector = Icons.Default.Chat,
                                    contentDescription = "Chat",
                                    tint = Color(0xFFD32F2F)
                                )
                            }
                        }
                    }
                }
            }
        }
    } else {
        // Show Active Chat Screen
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Column {
                            Text(activeRecipientName, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                            Text("Active Donor (Online)", fontSize = 11.sp, color = Color(0xFF4CAF50))
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = { activeRecipientId = "" }) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
                )
            },
            bottomBar = {
                Surface(
                    tonalElevation = 8.dp,
                    color = Color.White,
                    modifier = Modifier.imePadding()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = chatText,
                            onValueChange = { chatText = it },
                            placeholder = { Text("Type message...") },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color(0xFFD32F2F)
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .padding(end = 8.dp),
                            shape = RoundedCornerShape(24.dp)
                        )

                        FloatingActionButton(
                            onClick = {
                                if (chatText.isNotBlank()) {
                                    viewModel.sendChatMessage(
                                        receiverId = activeRecipientId,
                                        text = chatText,
                                        receiverName = activeRecipientName
                                    )
                                    chatText = ""
                                }
                            },
                            containerColor = Color(0xFFD32F2F),
                            contentColor = Color.White,
                            shape = CircleShape,
                            modifier = Modifier.size(48.dp)
                        ) {
                            Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Send")
                        }
                    }
                }
            }
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xFFF5F5F5))
                    .padding(innerPadding)
            ) {
                if (activeChatMessages.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.Chat, contentDescription = "New", tint = Color.LightGray, modifier = Modifier.size(48.dp))
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("No messages yet. Send a greeting!", color = Color.Gray)
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .padding(horizontal = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        contentPadding = PaddingValues(vertical = 16.dp)
                    ) {
                        items(
                            items = activeChatMessages,
                            key = { it.id },
                            contentType = { "chat_message" }
                        ) { message ->
                            val isMe = message.senderId == currentUserProfile.id
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = if (isMe) Arrangement.End else Arrangement.Start
                            ) {
                                Box(
                                    modifier = Modifier
                                        .clip(
                                            RoundedCornerShape(
                                                topStart = 16.dp,
                                                topEnd = 16.dp,
                                                bottomStart = if (isMe) 16.dp else 2.dp,
                                                bottomEnd = if (isMe) 2.dp else 16.dp
                                            )
                                        )
                                        .background(if (isMe) Color(0xFFD32F2F) else Color.White)
                                        .padding(horizontal = 14.dp, vertical = 10.dp)
                                        .widthIn(max = 260.dp)
                                ) {
                                    Column {
                                        Text(
                                            text = message.text,
                                            color = if (isMe) Color.White else Color(0xFF1E1E1E),
                                            fontSize = 14.sp
                                        )
                                        Spacer(modifier = Modifier.height(2.dp))
                                        val timeStr = SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Date(message.timestamp))
                                        Text(
                                            text = timeStr,
                                            color = if (isMe) Color.White.copy(alpha = 0.7f) else Color.Gray,
                                            fontSize = 10.sp,
                                            modifier = Modifier.align(Alignment.End)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
