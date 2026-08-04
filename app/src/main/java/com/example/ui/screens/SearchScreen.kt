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
import androidx.compose.material.icons.filled.Search
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
import com.example.ui.BloodViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(
    viewModel: BloodViewModel,
    onNavigateToMessages: (String, String) -> Unit
) {
    val context = LocalContext.current
    val donors by viewModel.allDonors.collectAsStateWithLifecycle()
    
    var searchQuery by remember { mutableStateOf("") }
    var selectedGroup by remember { mutableStateOf<String?>(null) }

    val bloodTypes = listOf("A+", "A-", "B+", "B-", "O+", "O-", "AB+", "AB-")

    val filteredList = remember(donors, searchQuery, selectedGroup) {
        donors.filter { donor ->
            val matchesQuery = searchQuery.isBlank() || 
                    donor.name.contains(searchQuery, ignoreCase = true) ||
                    donor.location.contains(searchQuery, ignoreCase = true)
            val matchesGroup = selectedGroup == null || donor.bloodGroup == selectedGroup
            matchesQuery && matchesGroup
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Find Nearby Donors", fontWeight = FontWeight.Bold, color = Color(0xFF1E1E1E)) },
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
            // Search Input
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                label = { Text("Search by name or location...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = "SearchIcon") },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color(0xFFD32F2F),
                    focusedLabelColor = Color(0xFFD32F2F)
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                shape = RoundedCornerShape(16.dp)
            )

            // Blood Type Chips
            Text(
                text = "Filter by Blood Group:",
                modifier = Modifier.padding(start = 16.dp, bottom = 8.dp),
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = Color.DarkGray
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                // "All" filter chip
                FilterChip(
                    selected = selectedGroup == null,
                    onClick = { selectedGroup = null },
                    label = { Text("All") },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = Color(0xFFFFEBEE),
                        selectedLabelColor = Color(0xFFD32F2F)
                    )
                )

                // Render first 4 types
                bloodTypes.take(4).forEach { type ->
                    FilterChip(
                        selected = selectedGroup == type,
                        onClick = { selectedGroup = type },
                        label = { Text(type) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Color(0xFFFFEBEE),
                            selectedLabelColor = Color(0xFFD32F2F)
                        )
                    )
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                // Render remaining 4 types
                bloodTypes.drop(4).forEach { type ->
                    FilterChip(
                        selected = selectedGroup == type,
                        onClick = { selectedGroup = type },
                        label = { Text(type) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Color(0xFFFFEBEE),
                            selectedLabelColor = Color(0xFFD32F2F)
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Count header
            Text(
                text = "Found ${filteredList.size} Donors",
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = Color.Gray,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )

            // Results List
            if (filteredList.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "No donors match your criteria.",
                        color = Color.Gray,
                        fontSize = 14.sp,
                        textAlign = TextAlign.Center
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(bottom = 16.dp)
                ) {
                    items(
                        items = filteredList,
                        key = { it.id },
                        contentType = { "donor_item" }
                    ) { donor ->
                        DonorListItem(
                            donor = donor,
                            onCallClick = {
                                val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:${donor.phone}"))
                                context.startActivity(intent)
                            },
                            onMessageClick = {
                                onNavigateToMessages(donor.id, donor.name)
                            }
                        )
                    }
                }
            }
        }
    }
}
