package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.BloodViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ServicesScreen(viewModel: BloodViewModel) {
    val context = LocalContext.current
    val requests by viewModel.allRequests.collectAsStateWithLifecycle()
    val logs by viewModel.allLogs.collectAsStateWithLifecycle()

    var activeTab by remember { mutableStateOf(0) } // 0: Requests, 1: Activity Logs, 2: Compatibility

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Blood Services", fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFFF9FAFC))
                .padding(innerPadding)
        ) {
            // Tab row selector
            TabRow(
                selectedTabIndex = activeTab,
                containerColor = Color.White,
                contentColor = Color(0xFFD32F2F),
                indicator = { tabPositions ->
                    TabRowDefaults.SecondaryIndicator(
                        Modifier.tabIndicatorOffset(tabPositions[activeTab]),
                        color = Color(0xFFD32F2F)
                    )
                }
            ) {
                Tab(
                    selected = activeTab == 0,
                    onClick = { activeTab = 0 },
                    text = { Text("Urgent Requests", fontWeight = FontWeight.Bold) },
                    icon = { Icon(Icons.Default.WaterDrop, contentDescription = "Req") }
                )
                Tab(
                    selected = activeTab == 1,
                    onClick = { activeTab = 1 },
                    text = { Text("Activity Logs", fontWeight = FontWeight.Bold) },
                    icon = { Icon(Icons.Default.List, contentDescription = "Logs") }
                )
                Tab(
                    selected = activeTab == 2,
                    onClick = { activeTab = 2 },
                    text = { Text("Compatibility", fontWeight = FontWeight.Bold) },
                    icon = { Icon(Icons.Default.Info, contentDescription = "Compat") }
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            when (activeTab) {
                0 -> {
                    // Urgent Requests tab
                    if (requests.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(Icons.Default.CheckCircle, "No requests", tint = Color.Gray, modifier = Modifier.size(48.dp))
                                Spacer(modifier = Modifier.height(12.dp))
                                Text(
                                    text = "No current active requests! All patients are stable and donors have helped.",
                                    color = Color.Gray,
                                    fontSize = 14.sp,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    } else {
                        LazyColumn(
                            contentPadding = PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.fillMaxSize()
                        ) {
                            items(
                                items = requests,
                                key = { it.id },
                                contentType = { "blood_request" }
                            ) { request ->
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .shadow(3.dp, RoundedCornerShape(16.dp)),
                                    shape = RoundedCornerShape(16.dp),
                                    colors = CardDefaults.cardColors(containerColor = Color.White)
                                ) {
                                    Column(modifier = Modifier.padding(16.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(36.dp)
                                                        .background(Color(0xFFFFEBEE), CircleShape),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Text(
                                                        request.bloodGroup,
                                                        color = Color(0xFFD32F2F),
                                                        fontWeight = FontWeight.Black,
                                                        fontSize = 14.sp
                                                    )
                                                }
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Text(
                                                    "Patient: ${request.patientName}",
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 16.sp
                                                )
                                            }
                                            Box(
                                                modifier = Modifier
                                                    .background(Color(0xFFFFEBEE), RoundedCornerShape(8.dp))
                                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                                            ) {
                                                Text(
                                                    "Urgent",
                                                    color = Color(0xFFD32F2F),
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }
                                        }

                                        Spacer(modifier = Modifier.height(8.dp))

                                        Text("🏥 Hospital: ${request.location}", fontSize = 13.sp, color = Color.Gray)
                                        Text("🩺 Condition: ${request.condition}", fontSize = 13.sp, color = Color.Gray)
                                        Text("📅 Time: ${request.dateRequired}", fontSize = 13.sp, color = Color.Gray)

                                        Spacer(modifier = Modifier.height(12.dp))

                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            Button(
                                                onClick = {
                                                    val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:${request.phone}"))
                                                    context.startActivity(intent)
                                                },
                                                modifier = Modifier.weight(1f),
                                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD32F2F)),
                                                shape = RoundedCornerShape(10.dp)
                                            ) {
                                                Icon(Icons.Default.Call, contentDescription = "Call")
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text("Call Contact", fontWeight = FontWeight.Bold)
                                            }

                                            OutlinedButton(
                                                onClick = { viewModel.deleteRequest(request.id) },
                                                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.Gray),
                                                shape = RoundedCornerShape(10.dp),
                                                modifier = Modifier.weight(1f)
                                            ) {
                                                Icon(Icons.Default.Done, contentDescription = "Resolved")
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text("Resolved")
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                1 -> {
                    // Activity logs tab (Real-time logs)
                    if (logs.isEmpty()) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("No recent activities logged.", color = Color.Gray)
                        }
                    } else {
                        val timeFormatter = remember { SimpleDateFormat("hh:mm a", Locale.getDefault()) }
                        LazyColumn(
                            contentPadding = PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.fillMaxSize()
                        ) {
                            items(
                                items = logs,
                                key = { it.id },
                                contentType = { "activity_log" }
                            ) { log ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(Color.White, RoundedCornerShape(12.dp))
                                        .padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(8.dp)
                                            .background(Color(0xFF4CAF50), CircleShape)
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column {
                                        Text(log.text, fontSize = 14.sp, color = Color(0xFF1E1E1E), fontWeight = FontWeight.Medium)
                                        val date = remember(log.timestamp) { timeFormatter.format(Date(log.timestamp)) }
                                        Text(date, fontSize = 11.sp, color = Color.Gray)
                                    }
                                }
                            }
                        }
                    }
                }

                2 -> {
                    // Blood Compatibility
                    val compatibility = remember {
                        listOf(
                            Triple("O-", "Universal Donor", "Can give to ALL. Can receive ONLY from O-."),
                            Triple("O+", "Common Type", "Can give to O+, A+, B+, AB+. Can receive from O-, O+."),
                            Triple("A-", "Rare Type", "Can give to A-, A+, AB-, AB+. Can receive from O-, A-."),
                            Triple("A+", "Common Type", "Can give to A+, AB+. Can receive from O-, O+, A-, A+."),
                            Triple("B-", "Rare Type", "Can give to B-, B+, AB-, AB+. Can receive from O-, B-."),
                            Triple("B+", "Common Type", "Can give to B+, AB+. Can receive from O-, O+, B-, B+."),
                            Triple("AB-", "Rare Type", "Can give to AB-, AB+. Can receive from O-, A-, B-, AB-."),
                            Triple("AB+", "Universal Recipient", "Can give ONLY to AB+. Can receive from ANY blood type.")
                        )
                    }

                    LazyColumn(
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        item {
                            Text(
                                "Blood Compatibility Guide",
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                color = Color(0xFF1E1E1E),
                                modifier = Modifier.padding(bottom = 8.dp)
                            )
                        }

                        items(
                            items = compatibility,
                            key = { it.first },
                            contentType = { "compatibility_item" }
                        ) { item ->
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(containerColor = Color.White)
                            ) {
                                Row(
                                    modifier = Modifier.padding(16.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(44.dp)
                                            .background(Color(0xFFFFEBEE), CircleShape),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            item.first,
                                            color = Color(0xFFD32F2F),
                                            fontWeight = FontWeight.Black,
                                            fontSize = 16.sp
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(16.dp))
                                    Column {
                                        Text(item.second, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                        Text(item.third, fontSize = 12.sp, color = Color.Gray)
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
