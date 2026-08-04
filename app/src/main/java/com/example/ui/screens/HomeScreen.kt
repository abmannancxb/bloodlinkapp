package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import android.location.Geocoder
import android.location.Location
import android.location.LocationManager
import android.content.Context
import androidx.core.content.ContextCompat
import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import androidx.compose.animation.*
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.zIndex
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.R
import com.example.data.Donor
import com.example.ui.BloodViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: BloodViewModel,
    onNavigateToSearch: () -> Unit,
    onNavigateToServices: () -> Unit,
    onNavigateToMessages: (String, String) -> Unit, // recipientId, recipientName
    onNavigateToProfile: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val userLocation by viewModel.userLocation.collectAsStateWithLifecycle()
    val nearestDonors by viewModel.filteredDonors.collectAsStateWithLifecycle()
    val firebaseActive by viewModel.firebaseAvailable.collectAsStateWithLifecycle()

    var showLocationDialog by remember { mutableStateOf(false) }
    var showRequestDialog by remember { mutableStateOf(false) }
    var isLocatingLive by remember { mutableStateOf(false) }
    var tempLocation by remember { mutableStateOf(userLocation) }

    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val fineGranted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true
        val coarseGranted = permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        if (fineGranted || coarseGranted) {
            isLocatingLive = true
            fetchLiveLocation(context, viewModel, coroutineScope) { resolved ->
                isLocatingLive = false
                tempLocation = resolved
            }
        } else {
            Toast.makeText(context, "Location permission is required to detect live location.", Toast.LENGTH_LONG).show()
        }
    }

    // Dialog state for Urgent Request
    var patientName by remember { mutableStateOf("") }
    var reqBloodGroup by remember { mutableStateOf("O+") }
    var reqLocation by remember { mutableStateOf(userLocation) }
    var reqPhone by remember { mutableStateOf("") }
    var reqCondition by remember { mutableStateOf("") }
    var reqUrgency by remember { mutableStateOf("Urgent") }

    val listState = rememberLazyListState()
    val isScrolled by remember {
        derivedStateOf {
            listState.firstVisibleItemIndex > 0 || listState.firstVisibleItemScrollOffset > 0
        }
    }
    val elevation by animateDpAsState(targetValue = if (isScrolled) 8.dp else 6.dp, label = "ElevationAnimation")

    Scaffold(
        topBar = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .zIndex(10f)
            ) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(64.dp)
                        .shadow(
                            elevation = elevation,
                            shape = androidx.compose.ui.graphics.RectangleShape,
                            clip = false,
                            ambientColor = Color.Black.copy(alpha = 0.35f),
                            spotColor = Color.Black.copy(alpha = 0.35f)
                        ),
                    color = Color.White,
                    shadowElevation = elevation
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = {
                                Toast.makeText(context, "Menu opened", Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Menu,
                                contentDescription = "Menu Drawer",
                                tint = Color(0xFF1E1E1E),
                                modifier = Modifier.size(24.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Blood",
                                color = Color(0xFFE53935),
                                fontWeight = FontWeight.Black,
                                fontSize = 26.sp
                            )
                            Text(
                                text = "Link",
                                color = Color(0xFF1E1E1E),
                                fontWeight = FontWeight.Bold,
                                fontSize = 26.sp
                            )
                        }

                        Spacer(modifier = Modifier.weight(1f))

                        IconButton(
                            onClick = { onNavigateToMessages("", "") },
                            modifier = Modifier.size(40.dp)
                        ) {
                            Box {
                                Icon(
                                    imageVector = Icons.Default.MailOutline,
                                    contentDescription = "Messages",
                                    tint = Color(0xFF1E1E1E),
                                    modifier = Modifier.size(24.dp)
                                )
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .background(Color(0xFFE53935), CircleShape)
                                        .align(Alignment.TopEnd)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        IconButton(
                            onClick = { onNavigateToServices() },
                            modifier = Modifier.size(40.dp)
                        ) {
                            Box {
                                Icon(
                                    imageVector = Icons.Default.Notifications,
                                    contentDescription = "Notifications",
                                    tint = Color(0xFF1E1E1E),
                                    modifier = Modifier.size(24.dp)
                                )
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .background(Color(0xFFE53935), CircleShape)
                                        .align(Alignment.TopEnd)
                                )
                            }
                        }
                    }
                }

                // Downward soft shadow layer below the top bar
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .align(Alignment.BottomCenter)
                        .offset(y = 6.dp)
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color.Black.copy(alpha = 0.12f),
                                    Color.Black.copy(alpha = 0.04f),
                                    Color.Transparent
                                )
                            )
                        )
                )
            }
        }
    ) { innerPadding ->
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFFF9FAFC))
                .padding(innerPadding),
            contentPadding = PaddingValues(bottom = 16.dp)
        ) {
            // 1. Red Hero Banner Card
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                        .shadow(8.dp, RoundedCornerShape(24.dp))
                        .clip(RoundedCornerShape(24.dp))
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(Color(0xFFFF5252), Color(0xFFD32F2F))
                            )
                        )
                        .padding(20.dp)
                ) {
                    Column {
                        Text(
                            text = "Save a life",
                            color = Color.White.copy(alpha = 0.9f),
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = "Donate Blood",
                            color = Color.White,
                            fontSize = 32.sp,
                            fontWeight = FontWeight.Black,
                            lineHeight = 36.sp
                        )
                        Text(
                            text = "Every drop counts",
                            color = Color.White.copy(alpha = 0.85f),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Normal,
                            modifier = Modifier.padding(top = 4.dp, bottom = 20.dp)
                        )

                        // 2 buttons in horizontal row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            // "Request Blood" Action Card
                            Card(
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable { showRequestDialog = true },
                                shape = RoundedCornerShape(14.dp),
                                colors = CardDefaults.cardColors(containerColor = Color.White)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(30.dp)
                                                .background(Color(0xFFFFEBEE), CircleShape),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.WaterDrop,
                                                contentDescription = "Request",
                                                tint = Color(0xFFD32F2F),
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Column {
                                            Text(
                                                text = "Request Blood",
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color(0xFF1E1E1E),
                                                maxLines = 1,
                                                softWrap = false
                                            )
                                            Text(
                                                text = "Need blood?",
                                                fontSize = 9.sp,
                                                color = Color.Gray,
                                                maxLines = 1,
                                                softWrap = false
                                            )
                                        }
                                    }
                                    Icon(
                                        imageVector = Icons.Default.ChevronRight,
                                        contentDescription = "Go",
                                        tint = Color(0xFFD32F2F),
                                        modifier = Modifier.size(14.dp)
                                    )
                                }
                            }

                            // "Donate Blood" Action Card
                            Card(
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable { onNavigateToProfile() },
                                shape = RoundedCornerShape(14.dp),
                                colors = CardDefaults.cardColors(containerColor = Color.White)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(30.dp)
                                                .background(Color(0xFFE8F5E9), CircleShape),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Favorite,
                                                contentDescription = "Donate",
                                                tint = Color(0xFF4CAF50),
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Column {
                                            Text(
                                                text = "Donate Blood",
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color(0xFF1E1E1E),
                                                maxLines = 1,
                                                softWrap = false
                                            )
                                            Text(
                                                text = "Become donor",
                                                fontSize = 9.sp,
                                                color = Color.Gray,
                                                maxLines = 1,
                                                softWrap = false
                                            )
                                        }
                                    }
                                    Icon(
                                        imageVector = Icons.Default.ChevronRight,
                                        contentDescription = "Go",
                                        tint = Color(0xFF4CAF50),
                                        modifier = Modifier.size(14.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // 2. Map Pin Location Selector with Live Map Preview Thumbnail Removed (Cleaner Layout)
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .shadow(2.dp, RoundedCornerShape(12.dp))
                        .border(1.dp, Color(0xFFFFCDD2), RoundedCornerShape(12.dp))
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { 
                            tempLocation = userLocation
                            showLocationDialog = true 
                        },
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF1F1))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.LocationOn,
                                contentDescription = "Pin Icon",
                                tint = Color(0xFFD32F2F),
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Column {
                                Text(
                                    text = "CURRENT REGION",
                                    color = Color(0xFFC62828),
                                    fontSize = 8.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = userLocation,
                                    color = Color(0xFF1E1E1E),
                                    fontWeight = FontWeight.Black,
                                    fontSize = 12.sp
                                )
                            }
                        }
                        
                        FilledIconButton(
                            onClick = { 
                                locationPermissionLauncher.launch(
                                    arrayOf(
                                        Manifest.permission.ACCESS_FINE_LOCATION,
                                        Manifest.permission.ACCESS_COARSE_LOCATION
                                    )
                                )
                            },
                            modifier = Modifier.size(32.dp),
                            colors = IconButtonDefaults.filledIconButtonColors(containerColor = Color(0xFFD32F2F)),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            if (isLocatingLive) {
                                CircularProgressIndicator(
                                    color = Color.White,
                                    modifier = Modifier.size(14.dp),
                                    strokeWidth = 2.dp
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Default.MyLocation,
                                    contentDescription = "Live Location",
                                    modifier = Modifier.size(16.dp),
                                    tint = Color.White
                                )
                            }
                        }
                    }
                }
            }

            // 3. Nearest Donors Header
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Nearest Donors",
                        color = Color(0xFF1E1E1E),
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                    Text(
                        text = "View All >",
                        color = Color(0xFFD32F2F),
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        modifier = Modifier.clickable { onNavigateToSearch() }
                    )
                }
            }

            // 4. List of Nearest Donors
            if (nearestDonors.isEmpty()) {
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.People,
                            contentDescription = "No donors",
                            tint = Color.Gray,
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "No nearest donors found in $userLocation",
                            color = Color.Gray,
                            fontSize = 14.sp,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            } else {
                items(
                    items = nearestDonors,
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

        // Location selection Dialog with Phone Map Preview
        if (showLocationDialog) {
            var selectedPinCity by remember { mutableStateOf<String?>(null) }
            
            val donorsList by viewModel.allDonors.collectAsState()
            
            // Function to count active donors in a city
            fun countDonors(citySub: String): Int {
                return donorsList.count { it.location.contains(citySub, ignoreCase = true) && it.isAvailable }
            }

            AlertDialog(
                onDismissRequest = { showLocationDialog = false },
                title = { 
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Map, contentDescription = "Map Icon", tint = Color(0xFFD32F2F))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Interactive Radar Map")
                    }
                },
                text = {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            "Tap map pins below or search directly to find nearby blood donors:",
                            color = Color.DarkGray,
                            fontSize = 13.sp
                        )
                        
                        // Map Picture with Overlay Pins
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(180.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .border(1.dp, Color.LightGray, RoundedCornerShape(16.dp))
                        ) {
                            Image(
                                painter = painterResource(id = R.drawable.img_map_location_preview),
                                contentDescription = "Phone Map",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                            
                            // Pin 1: Ramu, Cox's Bazar (Center)
                            Box(
                                modifier = Modifier
                                    .align(Alignment.Center)
                                    .offset(x = (-10).dp, y = (-20).dp)
                                    .clickable {
                                        selectedPinCity = "Ramu, Cox's Bazar"
                                        tempLocation = "Ramu, Cox's Bazar"
                                    }
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Box(
                                        modifier = Modifier
                                            .background(Color(0xFFD32F2F), CircleShape)
                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                    ) {
                                        Text("Ramu (${countDonors("Ramu")})", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                    }
                                    Icon(
                                        imageVector = Icons.Default.LocationOn,
                                        contentDescription = "Pin 1",
                                        tint = Color(0xFFD32F2F),
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                            }

                            // Pin 2: Cox's Bazar Sadar (Left-ish)
                            Box(
                                modifier = Modifier
                                    .align(Alignment.CenterStart)
                                    .offset(x = 30.dp, y = 20.dp)
                                    .clickable {
                                        selectedPinCity = "Cox's Bazar Sadar"
                                        tempLocation = "Cox's Bazar Sadar"
                                    }
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Box(
                                        modifier = Modifier
                                            .background(Color(0xFF1976D2), CircleShape)
                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                    ) {
                                        Text("Sadar (${countDonors("Sadar")})", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                    }
                                    Icon(
                                        imageVector = Icons.Default.LocationOn,
                                        contentDescription = "Pin 2",
                                        tint = Color(0xFF1976D2),
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                            }

                            // Pin 3: Chittagong Sadar (Top Right)
                            Box(
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .offset(x = (-20).dp, y = 20.dp)
                                    .clickable {
                                        selectedPinCity = "Chittagong Sadar"
                                        tempLocation = "Chittagong Sadar"
                                    }
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Box(
                                        modifier = Modifier
                                            .background(Color(0xFF388E3C), CircleShape)
                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                    ) {
                                        Text("Ctg (${countDonors("Chittagong")})", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                    }
                                    Icon(
                                        imageVector = Icons.Default.LocationOn,
                                        contentDescription = "Pin 3",
                                        tint = Color(0xFF388E3C),
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                            }
                        }

                        // Selected Pin Info
                        AnimatedVisibility(
                            visible = selectedPinCity != null,
                            enter = fadeIn() + expandVertically(),
                            exit = fadeOut() + shrinkVertically()
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(Color(0xFFE8F5E9), RoundedCornerShape(8.dp))
                                    .padding(8.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.CheckCircle, "Selected", tint = Color(0xFF4CAF50), modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "Selected on Map: $selectedPinCity",
                                        color = Color(0xFF2E7D32),
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp
                                    )
                                }
                            }
                        }

                        // Live GPS Location Picker Button
                        Button(
                            onClick = {
                                locationPermissionLauncher.launch(
                                    arrayOf(
                                        Manifest.permission.ACCESS_FINE_LOCATION,
                                        Manifest.permission.ACCESS_COARSE_LOCATION
                                    )
                                )
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1976D2)),
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            if (isLocatingLive) {
                                CircularProgressIndicator(
                                    color = Color.White,
                                    modifier = Modifier.size(18.dp),
                                    strokeWidth = 2.dp
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Detecting GPS Location...", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            } else {
                                Icon(Icons.Default.LocationOn, contentDescription = "GPS", tint = Color.White, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("🎯 Use Live GPS Location", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            }
                        }

                        // Text Field search input
                        OutlinedTextField(
                            value = tempLocation,
                            onValueChange = { 
                                tempLocation = it
                                selectedPinCity = null // clear map selection if they type
                            },
                            label = { Text("Search location name") },
                            placeholder = { Text("e.g. Ramu, Cox's Bazar") },
                            leadingIcon = { Icon(Icons.Default.Search, "LocSearch") },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        )
                    }
                },
                confirmButton = {
                    Button(
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD32F2F)),
                        onClick = {
                            if (tempLocation.isNotBlank()) {
                                viewModel.updateLocation(tempLocation)
                            }
                            showLocationDialog = false
                        }
                    ) {
                        Text("Confirm Location")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showLocationDialog = false }) {
                        Text("Cancel", color = Color.Gray)
                    }
                }
            )
        }

        // Create Urgent Blood Request Dialog
        if (showRequestDialog) {
            AlertDialog(
                onDismissRequest = { showRequestDialog = false },
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.WaterDrop, contentDescription = "Add Request", tint = Color(0xFFD32F2F))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("New Blood Request")
                    }
                },
                text = {
                    LazyColumn(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        item {
                            OutlinedTextField(
                                value = patientName,
                                onValueChange = { patientName = it },
                                label = { Text("Patient Name") },
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                        item {
                            Text("Required Blood Group:", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            val bloodTypes = listOf("A+", "A-", "B+", "B-", "O+", "O-", "AB+", "AB-")
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                bloodTypes.take(4).forEach { type ->
                                    FilterChip(
                                        selected = reqBloodGroup == type,
                                        onClick = { reqBloodGroup = type },
                                        label = { Text(type) }
                                    )
                                }
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                bloodTypes.drop(4).forEach { type ->
                                    FilterChip(
                                        selected = reqBloodGroup == type,
                                        onClick = { reqBloodGroup = type },
                                        label = { Text(type) }
                                    )
                                }
                            }
                        }
                        item {
                            OutlinedTextField(
                                value = reqLocation,
                                onValueChange = { reqLocation = it },
                                label = { Text("Hospital / Location") },
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                        item {
                            OutlinedTextField(
                                value = reqPhone,
                                onValueChange = { reqPhone = it },
                                label = { Text("Contact Phone") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                        item {
                            OutlinedTextField(
                                value = reqCondition,
                                onValueChange = { reqCondition = it },
                                label = { Text("Patient's Condition / Medical Case") },
                                placeholder = { Text("e.g. Dengue, Surgery") },
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                },
                confirmButton = {
                    Button(
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD32F2F)),
                        onClick = {
                            if (patientName.isNotBlank() && reqPhone.isNotBlank() && reqLocation.isNotBlank()) {
                                viewModel.createBloodRequest(
                                    patientName = patientName,
                                    bloodGroup = reqBloodGroup,
                                    location = reqLocation,
                                    phone = reqPhone,
                                    condition = reqCondition.ifBlank { "Urgent Requirement" },
                                    dateRequired = "Immediate",
                                    urgency = "Urgent"
                                )
                                Toast.makeText(context, "Urgent request registered!", Toast.LENGTH_LONG).show()
                                showRequestDialog = false
                                // Clear fields
                                patientName = ""
                                reqPhone = ""
                                reqCondition = ""
                            } else {
                                Toast.makeText(context, "Please complete all fields!", Toast.LENGTH_SHORT).show()
                            }
                        }
                    ) {
                        Text("Create Request")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showRequestDialog = false }) {
                        Text("Cancel", color = Color.Gray)
                    }
                }
            )
        }
    }
}

@Composable
fun DonorListItem(
    donor: Donor,
    onCallClick: () -> Unit,
    onMessageClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .shadow(2.dp, RoundedCornerShape(16.dp)),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Left side: Profile image/Avatar + text
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Box {
                    // Styled Initial Placeholder Avatar
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .background(Color(0xFFF5F5F5), CircleShape)
                            .border(1.5.dp, Color(0xFFECEFF1), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        val initials = remember(donor.name) { donor.name.split(" ").take(2).map { it.firstOrNull() ?: "" }.joinToString("") }
                        Text(
                            text = initials,
                            color = Color(0xFFD32F2F),
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    // Green online indicator
                    if (donor.isOnline) {
                        Box(
                            modifier = Modifier
                                .size(14.dp)
                                .background(Color.White, CircleShape)
                                .align(Alignment.BottomEnd)
                                .padding(2.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(Color(0xFF4CAF50), CircleShape)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                // Donor Info
                Column {
                    Text(
                        text = donor.name,
                        color = Color(0xFF1E1E1E),
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    
                    // Blood Group Row
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.WaterDrop,
                            contentDescription = "Blood Group",
                            tint = Color(0xFFD32F2F),
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(2.dp))
                        Text(
                            text = donor.bloodGroup,
                            color = Color(0xFFD32F2F),
                            fontWeight = FontWeight.Black,
                            fontSize = 14.sp
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "🩸 Donated ${donor.donationsCount} times",
                        color = Color.Gray,
                        fontSize = 12.sp
                    )
                }
            }

            // Right side: Message Button + Call Button
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Pink Message Button
                IconButton(
                    onClick = onMessageClick,
                    modifier = Modifier
                        .size(40.dp)
                        .background(Color(0xFFFFEBEE), CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.Default.Chat,
                        contentDescription = "Message",
                        tint = Color(0xFFD32F2F),
                        modifier = Modifier.size(18.dp)
                    )
                }

                // Blue Call Button
                Button(
                    onClick = onCallClick,
                    shape = RoundedCornerShape(20.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF3F51B5)),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                    modifier = Modifier.height(36.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Call,
                            contentDescription = "Call",
                            tint = Color.White,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = "Call",
                            color = Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

fun fetchLiveLocation(
    context: Context,
    viewModel: BloodViewModel,
    scope: kotlinx.coroutines.CoroutineScope,
    onFinish: (String) -> Unit
) {
    val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager
    if (locationManager == null) {
        Toast.makeText(context, "Location services not available", Toast.LENGTH_SHORT).show()
        onFinish(viewModel.userLocation.value)
        return
    }

    val hasFine = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
    val hasCoarse = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED

    if (!hasFine && !hasCoarse) {
        Toast.makeText(context, "Location permission not granted", Toast.LENGTH_SHORT).show()
        onFinish(viewModel.userLocation.value)
        return
    }

    val provider = if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
        LocationManager.GPS_PROVIDER
    } else if (locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
        LocationManager.NETWORK_PROVIDER
    } else {
        null
    }

    if (provider == null) {
        Toast.makeText(context, "Please turn on GPS/Location in system settings.", Toast.LENGTH_LONG).show()
        onFinish(viewModel.userLocation.value)
        return
    }

    try {
        val lastLocation = locationManager.getLastKnownLocation(provider)
        if (lastLocation != null) {
            resolveAddressAndSet(context, lastLocation, viewModel, scope, onFinish)
        } else {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                locationManager.getCurrentLocation(
                    provider,
                    null,
                    context.mainExecutor
                ) { location ->
                    if (location != null) {
                        resolveAddressAndSet(context, location, viewModel, scope, onFinish)
                    } else {
                        Toast.makeText(context, "Could not fetch GPS fix. Enter manually.", Toast.LENGTH_SHORT).show()
                        onFinish(viewModel.userLocation.value)
                    }
                }
            } else {
                locationManager.requestSingleUpdate(provider, object : android.location.LocationListener {
                    override fun onLocationChanged(location: Location) {
                        resolveAddressAndSet(context, location, viewModel, scope, onFinish)
                    }
                    override fun onStatusChanged(p0: String?, status: Int, extras: android.os.Bundle?) {}
                    override fun onProviderEnabled(p0: String) {}
                    override fun onProviderDisabled(p0: String) {}
                }, context.mainLooper)
            }
        }
    } catch (e: SecurityException) {
        Toast.makeText(context, "Permission error", Toast.LENGTH_SHORT).show()
        onFinish(viewModel.userLocation.value)
    }
}

fun resolveAddressAndSet(
    context: Context,
    location: Location,
    viewModel: BloodViewModel,
    scope: kotlinx.coroutines.CoroutineScope,
    onFinish: (String) -> Unit
) {
    scope.launch(Dispatchers.IO) {
        var resolvedName = ""
        try {
            val geocoder = Geocoder(context, java.util.Locale.getDefault())
            val addresses = geocoder.getFromLocation(location.latitude, location.longitude, 1)
            if (!addresses.isNullOrEmpty()) {
                val addr = addresses[0]
                val subLocality = addr.subLocality ?: ""
                val locality = addr.locality ?: addr.subAdminArea ?: addr.adminArea ?: ""
                resolvedName = if (subLocality.isNotBlank() && locality.isNotBlank()) {
                    "$subLocality, $locality"
                } else if (locality.isNotBlank()) {
                    locality
                } else {
                    addr.getAddressLine(0) ?: ""
                }
            }
        } catch (e: Exception) {
            Log.e("LocationResolver", "Geocoder failed", e)
        }

        if (resolvedName.isBlank()) {
            val lat = location.latitude
            val lon = location.longitude
            val distToSadar = Math.hypot(lat - 21.4272, lon - 92.0058)
            val distToRamu = Math.hypot(lat - 21.4320, lon - 92.1022)
            val distToChittagong = Math.hypot(lat - 22.3569, lon - 91.7832)

            resolvedName = when {
                distToRamu < distToSadar && distToRamu < distToChittagong -> "Ramu, Cox's Bazar"
                distToSadar < distToChittagong -> "Cox's Bazar Sadar"
                else -> "Chittagong Sadar"
            }
        }

        withContext(Dispatchers.Main) {
            viewModel.updateLocation(resolvedName)
            Toast.makeText(context, "Location updated to: $resolvedName", Toast.LENGTH_LONG).show()
            onFinish(resolvedName)
        }
    }
}
