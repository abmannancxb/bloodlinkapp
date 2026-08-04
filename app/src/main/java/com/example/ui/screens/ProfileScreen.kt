package com.example.ui.screens

import android.util.Log
import android.widget.Toast
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.credentials.CustomCredential
import androidx.credentials.exceptions.GetCredentialException
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import kotlinx.coroutines.launch
import com.google.firebase.auth.GoogleAuthProvider
import com.example.data.FirebaseSafeAccess
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import com.example.ui.BloodViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    viewModel: BloodViewModel,
    onNavigateToCreateAccount: () -> Unit = {}
) {
    val context = LocalContext.current
    val currentUserProfile by viewModel.currentUserProfile.collectAsStateWithLifecycle()
    val currentUserEmail by viewModel.currentUserEmail.collectAsStateWithLifecycle()

    var name by remember { mutableStateOf(currentUserProfile.name) }
    var bloodGroup by remember { mutableStateOf(currentUserProfile.bloodGroup) }
    var location by remember { mutableStateOf(currentUserProfile.location) }
    var phone by remember { mutableStateOf(currentUserProfile.phone) }
    var donationsCount by remember { mutableStateOf(currentUserProfile.donationsCount.toString()) }
    var isAvailable by remember { mutableStateOf(currentUserProfile.isAvailable) }

    val coroutineScope = rememberCoroutineScope()
    var showSignOutDialog by remember { mutableStateOf(false) }

    LaunchedEffect(currentUserProfile) {
        name = currentUserProfile.name
        bloodGroup = currentUserProfile.bloodGroup
        location = currentUserProfile.location
        phone = currentUserProfile.phone
        donationsCount = currentUserProfile.donationsCount.toString()
        isAvailable = currentUserProfile.isAvailable
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("My Profile & Registration", fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White),
                modifier = Modifier.shadow(
                    elevation = 4.dp,
                    shape = RoundedCornerShape(0.dp),
                    clip = false
                )
            )
        }
    ) { innerPadding ->
        if (currentUserEmail.isBlank()) {
            // Unauthenticated: Show ONLY Login with Google Screen
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xFFF9FAFC))
                    .padding(innerPadding)
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .shadow(8.dp, RoundedCornerShape(24.dp)),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(28.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(90.dp)
                                .background(Color(0xFFFFEBEE), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Person,
                                contentDescription = "Profile Placeholder",
                                tint = Color(0xFFD32F2F),
                                modifier = Modifier.size(54.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(24.dp))

                        Text(
                            text = "Welcome to BloodLink",
                            fontWeight = FontWeight.Bold,
                            fontSize = 22.sp,
                            color = Color(0xFF1E1E1E)
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = "Connect with blood donors, request blood, and save lives. Please sign in or create an account to build your donor profile.",
                            color = Color.Gray,
                            fontSize = 14.sp,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(horizontal = 8.dp)
                        )

                        Spacer(modifier = Modifier.height(24.dp))

                        // Sign In with Email & Password
                        Button(
                            onClick = {
                                viewModel.initialAuthMode = 1
                                onNavigateToCreateAccount()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD32F2F)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .shadow(2.dp, RoundedCornerShape(12.dp)),
                            shape = RoundedCornerShape(12.dp),
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Person,
                                    contentDescription = "Sign In",
                                    tint = Color.White,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Sign In / লগইন করুন",
                                    color = Color.White,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Create New Account
                        OutlinedButton(
                            onClick = {
                                viewModel.initialAuthMode = 0
                                onNavigateToCreateAccount()
                            },
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFD32F2F)),
                            modifier = Modifier.fillMaxWidth(),
                            border = BorderStroke(1.5.dp, Color(0xFFD32F2F)),
                            shape = RoundedCornerShape(12.dp),
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Person,
                                    contentDescription = "Create Account",
                                    tint = Color(0xFFD32F2F),
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Create New Account / নতুন অ্যাকাউন্ট তৈরি করুন",
                                    color = Color(0xFFD32F2F),
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Divider
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            HorizontalDivider(
                                modifier = Modifier.weight(1f),
                                color = Color(0xFFE0E0E0)
                            )
                            Text(
                                text = "  OR / অথবা  ",
                                fontSize = 12.sp,
                                color = Color.Gray,
                                fontWeight = FontWeight.Medium
                            )
                            HorizontalDivider(
                                modifier = Modifier.weight(1f),
                                color = Color(0xFFE0E0E0)
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Button(
                            onClick = {
                                coroutineScope.launch {
                                    try {
                                        val credentialManager = CredentialManager.create(context)
                                        val googleIdOption = GetGoogleIdOption.Builder()
                                            .setFilterByAuthorizedAccounts(false)
                                            .setServerClientId("732358520046-dummy.apps.googleusercontent.com")
                                            .setAutoSelectEnabled(false)
                                            .build()

                                        val request = GetCredentialRequest.Builder()
                                            .addCredentialOption(googleIdOption)
                                            .build()

                                        val result = credentialManager.getCredential(context, request)
                                        val credential = result.credential
                                        if (credential is CustomCredential && credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                                            val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)
                                            val idToken = googleIdTokenCredential.idToken
                                            val email = googleIdTokenCredential.id
                                            val displayName = googleIdTokenCredential.displayName

                                            val auth = FirebaseSafeAccess.auth
                                            if (auth != null && idToken != null) {
                                                val fbCredential = GoogleAuthProvider.getCredential(idToken, null)
                                                auth.signInWithCredential(fbCredential)
                                                    .addOnCompleteListener { task ->
                                                        if (task.isSuccessful) {
                                                            viewModel.loginWithGoogle(email, displayName)
                                                            Toast.makeText(context, "Logged in successfully!", Toast.LENGTH_SHORT).show()
                                                        } else {
                                                            viewModel.loginWithGoogle(email, displayName)
                                                            Toast.makeText(context, "Local Sign-in successful (Firebase failed)", Toast.LENGTH_SHORT).show()
                                                        }
                                                    }
                                            } else {
                                                viewModel.loginWithGoogle(email, displayName)
                                                Toast.makeText(context, "Logged in as: $email", Toast.LENGTH_LONG).show()
                                            }
                                        } else {
                                            Toast.makeText(context, "Unexpected credential type", Toast.LENGTH_SHORT).show()
                                        }
                                    } catch (e: Exception) {
                                        Log.e("ProfileScreen", "Google Sign-In Error: ${e.message}")
                                        Toast.makeText(context, "Google Sign-In failed or cancelled", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color.White),
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(1.dp, Color(0xFFE0E0E0), RoundedCornerShape(12.dp))
                                .shadow(1.dp, RoundedCornerShape(12.dp)),
                            shape = RoundedCornerShape(12.dp),
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Text("G ", color = Color(0xFF4285F4), fontWeight = FontWeight.Black, fontSize = 18.sp)
                                Text("Login with Google / গুগল লগইন", color = Color.DarkGray, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        } else {
            // Authenticated: Show full donor profile editor
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xFFF9FAFC))
                    .padding(innerPadding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Profile Header Card
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .shadow(4.dp, RoundedCornerShape(20.dp)),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(20.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(80.dp)
                                    .background(Color(0xFFFFEBEE), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Person,
                                    contentDescription = "Profile",
                                    tint = Color(0xFFD32F2F),
                                    modifier = Modifier.size(44.dp)
                                )
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            Text(
                                text = if (currentUserProfile.name.isBlank()) "Register Your Profile" else currentUserProfile.name,
                                fontWeight = FontWeight.Bold,
                                fontSize = 20.sp,
                                color = Color(0xFF1E1E1E)
                            )
                            Text(
                                text = if (currentUserProfile.name.isBlank()) "Please fill out the form below to register as a donor" else "Blood Group: ${currentUserProfile.bloodGroup} • ${currentUserProfile.location}",
                                color = Color.Gray,
                                fontSize = 14.sp,
                                modifier = Modifier.padding(top = 2.dp)
                            )

                            Spacer(modifier = Modifier.height(12.dp))

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .background(Color(0xFFF5F5F5), RoundedCornerShape(10.dp))
                                    .padding(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Text("G ", color = Color(0xFF4285F4), fontWeight = FontWeight.Black, fontSize = 14.sp)
                                Text("Logged in: ", fontSize = 12.sp, color = Color.Gray)
                                Text(currentUserEmail, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1E1E1E))
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceEvenly
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text("Donations", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color(0xFFD32F2F))
                                    Text("${currentUserProfile.donationsCount} times", fontSize = 12.sp, color = Color.Gray)
                                }
                                Divider(
                                    modifier = Modifier
                                        .height(30.dp)
                                        .width(1.dp)
                                )
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text("Status", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color(0xFF4CAF50))
                                    Text(if (currentUserProfile.isAvailable) "Active Donor" else "Unavailable", fontSize = 12.sp, color = Color.Gray)
                                }
                            }
                        }
                    }
                }

                // Edit Profile Form
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White)
                    ) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Text(
                                "Register / Edit Donor Profile",
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                color = Color(0xFF1E1E1E),
                                modifier = Modifier.padding(bottom = 4.dp)
                            )

                            OutlinedTextField(
                                value = name,
                                onValueChange = { name = it },
                                label = { Text("Full Name") },
                                modifier = Modifier.fillMaxWidth()
                            )

                            // Blood type dropdown selector
                            Column {
                                Text("Blood Group", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color.Gray)
                                val bloodTypes = listOf("A+", "A-", "B+", "B-", "O+", "O-", "AB+", "AB-")
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp),
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    bloodTypes.take(4).forEach { type ->
                                        FilterChip(
                                            selected = bloodGroup == type,
                                            onClick = { bloodGroup = type },
                                            label = { Text(type) }
                                        )
                                    }
                                }
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp),
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    bloodTypes.drop(4).forEach { type ->
                                        FilterChip(
                                            selected = bloodGroup == type,
                                            onClick = { bloodGroup = type },
                                            label = { Text(type) }
                                        )
                                    }
                                }
                            }

                            OutlinedTextField(
                                value = location,
                                onValueChange = { location = it },
                                label = { Text("Donation Location / City") },
                                modifier = Modifier.fillMaxWidth()
                            )

                            OutlinedTextField(
                                value = phone,
                                onValueChange = { phone = it },
                                label = { Text("Contact Phone Number") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                                modifier = Modifier.fillMaxWidth()
                            )

                            OutlinedTextField(
                                value = donationsCount,
                                onValueChange = { donationsCount = it },
                                label = { Text("Number of Past Donations") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.fillMaxWidth()
                            )

                            // Donor Availability Switch
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text("Available to Donate", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                    Text("Turn off if you cannot donate currently", fontSize = 11.sp, color = Color.Gray)
                                }
                                Switch(
                                    checked = isAvailable,
                                    onCheckedChange = { isAvailable = it },
                                    colors = SwitchDefaults.colors(checkedThumbColor = Color(0xFFD32F2F), checkedTrackColor = Color(0xFFFFEBEE))
                                )
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            Button(
                                onClick = {
                                    val count = donationsCount.toIntOrNull() ?: 0
                                    if (name.isNotBlank() && phone.isNotBlank() && location.isNotBlank()) {
                                        viewModel.updateProfile(
                                            name = name,
                                            bloodGroup = bloodGroup,
                                            location = location,
                                            phone = phone,
                                            donationsCount = count,
                                            isAvailable = isAvailable
                                        )
                                        Toast.makeText(context, "Profile updated successfully!", Toast.LENGTH_SHORT).show()
                                    } else {
                                        Toast.makeText(context, "Please complete all fields!", Toast.LENGTH_SHORT).show()
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD32F2F)),
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(Icons.Default.Done, contentDescription = "Save")
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Save Profile & Register as Donor", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            OutlinedButton(
                                onClick = onNavigateToCreateAccount,
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFD32F2F)),
                                modifier = Modifier.fillMaxWidth(),
                                border = BorderStroke(1.dp, Color(0xFFD32F2F)),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(Icons.Default.Person, contentDescription = "Create New Account", tint = Color(0xFFD32F2F))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Create New Account / নতুন অ্যাকাউন্ট তৈরি করুন", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            OutlinedButton(
                                onClick = { showSignOutDialog = true },
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFD32F2F)),
                                modifier = Modifier.fillMaxWidth(),
                                border = BorderStroke(1.dp, Color(0xFFE0E0E0)),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(Icons.AutoMirrored.Filled.ExitToApp, contentDescription = "Sign Out", tint = Color(0xFFD32F2F))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Sign Out / সাইন আউট", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                            }
                        }
                    }
                }
            }
        }

        // Sign Out Confirmation Dialog
        if (showSignOutDialog) {
            AlertDialog(
                onDismissRequest = { showSignOutDialog = false },
                title = { Text("Sign Out / সাইন আউট", fontWeight = FontWeight.Bold) },
                text = {
                    Text(
                        text = "Are you sure you want to sign out? Your profile data will remain safely saved in BloodLink.\n\nআপনি কি সাইন আউট করতে চান?",
                        fontSize = 14.sp,
                        color = Color.DarkGray
                    )
                },
                confirmButton = {
                    Button(
                        onClick = {
                            viewModel.signOut()
                            showSignOutDialog = false
                            Toast.makeText(context, "Signed out successfully!", Toast.LENGTH_SHORT).show()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD32F2F))
                    ) {
                        Text("Sign Out")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showSignOutDialog = false }) {
                        Text("Cancel", color = Color.Gray)
                    }
                }
            )
        }
    }
}
