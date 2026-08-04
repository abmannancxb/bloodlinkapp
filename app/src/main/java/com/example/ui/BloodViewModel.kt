package com.example.ui

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.*
import com.example.notification.LocalNotificationHelper
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import java.util.UUID

class BloodViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: BloodRepository
    private val sharedPrefs = application.getSharedPreferences("bloodlink_prefs", android.content.Context.MODE_PRIVATE)
    
    // UI state states
    var initialAuthMode: Int = 0
    val userLocation = MutableStateFlow("Cox's Bazar")
    val selectedBloodTypeFilter = MutableStateFlow<String?>(null) // null means all
    val isNetworkAvailable = MutableStateFlow(true)
    
    // Auth profile of the current user
    val currentUserEmail = MutableStateFlow("")
    val currentUserProfile = MutableStateFlow(
        Donor(
            id = "current_user_id",
            name = "",
            bloodGroup = "O+",
            location = "",
            phone = "",
            donationsCount = 0,
            isAvailable = true,
            lastDonationDate = "Never",
            isOnline = true
        )
    )

    init {
        val database = AppDatabase.getDatabase(application)
        repository = BloodRepository(database.bloodDao())
        
        loadUserProfile()
        startNetworkMonitoring()
        
        viewModelScope.launch {
            repository.checkAndPrepopulate()
            repository.startRealtimeSync(this)
        }
    }

    private fun startNetworkMonitoring() {
        val connectivityManager = getApplication<Application>().getSystemService(android.content.Context.CONNECTIVITY_SERVICE) as android.net.ConnectivityManager
        
        // Initial check
        val activeNetwork = connectivityManager.activeNetwork
        val capabilities = connectivityManager.getNetworkCapabilities(activeNetwork)
        isNetworkAvailable.value = capabilities?.hasCapability(android.net.NetworkCapabilities.NET_CAPABILITY_INTERNET) == true

        // Realtime callback
        val networkRequest = android.net.NetworkRequest.Builder()
            .addCapability(android.net.NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .build()

        try {
            connectivityManager.registerNetworkCallback(networkRequest, object : android.net.ConnectivityManager.NetworkCallback() {
                override fun onAvailable(network: android.net.Network) {
                    isNetworkAvailable.value = true
                }

                override fun onLost(network: android.net.Network) {
                    // Check if there are other networks still active
                    val activeNet = connectivityManager.activeNetwork
                    val caps = connectivityManager.getNetworkCapabilities(activeNet)
                    isNetworkAvailable.value = caps?.hasCapability(android.net.NetworkCapabilities.NET_CAPABILITY_INTERNET) == true
                }
            })
        } catch (e: Exception) {
            Log.e("BloodViewModel", "Error registering network callback: ${e.message}")
        }
    }

    private fun loadUserProfile() {
        val storedEmail = sharedPrefs.getString("user_email", "") ?: ""
        var storedId = sharedPrefs.getString("user_id", "") ?: ""
        if (storedId.isEmpty()) {
            storedId = UUID.randomUUID().toString()
            sharedPrefs.edit().putString("user_id", storedId).apply()
        }
        val storedName = sharedPrefs.getString("user_name", "") ?: ""
        val storedBloodGroup = sharedPrefs.getString("user_blood_group", "O+") ?: "O+"
        val storedLocation = sharedPrefs.getString("user_location", "") ?: ""
        val storedPhone = sharedPrefs.getString("user_phone", "") ?: ""
        val storedDonationsCount = sharedPrefs.getInt("user_donations_count", 0)
        val storedIsAvailable = sharedPrefs.getBoolean("user_is_available", true)

        currentUserEmail.value = storedEmail
        userLocation.value = if (storedLocation.isNotEmpty()) storedLocation else "Cox's Bazar"
        currentUserProfile.value = Donor(
            id = storedId,
            name = storedName,
            bloodGroup = storedBloodGroup,
            location = storedLocation,
            phone = storedPhone,
            donationsCount = storedDonationsCount,
            isAvailable = storedIsAvailable,
            lastDonationDate = if (storedDonationsCount > 0) "$storedDonationsCount times" else "Never",
            isOnline = true
        )
    }

    private fun saveUserProfileInPrefs(donor: Donor, email: String) {
        sharedPrefs.edit().apply {
            putString("user_email", email)
            putString("user_id", donor.id)
            putString("user_name", donor.name)
            putString("user_blood_group", donor.bloodGroup)
            putString("user_location", donor.location)
            putString("user_phone", donor.phone)
            putInt("user_donations_count", donor.donationsCount)
            putBoolean("user_is_available", donor.isAvailable)
            apply()
        }
    }

    fun signOut() {
        viewModelScope.launch {
            try {
                FirebaseSafeAccess.auth?.signOut()
            } catch (e: Exception) {
                Log.e("BloodViewModel", "Firebase sign out error: ${e.message}")
            }
            sharedPrefs.edit().clear().apply()
            currentUserEmail.value = ""
            currentUserProfile.value = Donor(
                id = "",
                name = "",
                bloodGroup = "O+",
                location = "Cox's Bazar",
                phone = "",
                donationsCount = 0,
                isAvailable = true,
                lastDonationDate = "Never",
                isOnline = false
            )
        }
    }

    fun checkNetworkStatus() {
        val connectivityManager = getApplication<Application>().getSystemService(android.content.Context.CONNECTIVITY_SERVICE) as android.net.ConnectivityManager
        val activeNetwork = connectivityManager.activeNetwork
        val capabilities = connectivityManager.getNetworkCapabilities(activeNetwork)
        isNetworkAvailable.value = capabilities?.hasCapability(android.net.NetworkCapabilities.NET_CAPABILITY_INTERNET) == true
    }

    val allDonors: StateFlow<List<Donor>> = repository.allDonors
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allRequests: StateFlow<List<BloodRequest>> = repository.allRequests
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allMessages: StateFlow<List<ChatMessage>> = repository.allMessages
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allLogs: StateFlow<List<ActivityLog>> = repository.allLogs
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Filtered nearest donors based on user location and selected blood group
    val filteredDonors: StateFlow<List<Donor>> = combine(
        allDonors,
        userLocation,
        selectedBloodTypeFilter
    ) { donors, loc, group ->
        donors.filter { donor ->
            // Filter out current user from nearest donors list
            donor.id != currentUserProfile.value.id
        }.sortedWith(compareByDescending<Donor> {
            // Priority 1: Exact matches for blood group if filter is active
            if (group != null) it.bloodGroup == group else true
        }.thenByDescending {
            // Priority 2: Matches current location
            it.location.lowercase().contains(loc.lowercase().split(",")[0].trim())
        })
    }
    .flowOn(Dispatchers.Default)
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val firebaseAvailable: StateFlow<Boolean> = flow {
        emit(FirebaseSafeAccess.isAvailable)
    }.stateIn(viewModelScope, SharingStarted.Lazily, false)

    fun updateLocation(newLocation: String) {
        userLocation.value = newLocation
        viewModelScope.launch {
            repository.saveActivityLog("Location updated to $newLocation")
        }
    }

    fun setBloodTypeFilter(group: String?) {
        selectedBloodTypeFilter.value = group
    }

    fun updateProfile(name: String, bloodGroup: String, location: String, phone: String, donationsCount: Int, isAvailable: Boolean) {
        val updated = Donor(
            id = currentUserProfile.value.id,
            name = name,
            bloodGroup = bloodGroup,
            location = location,
            phone = phone,
            donationsCount = donationsCount,
            isAvailable = isAvailable,
            lastDonationDate = if (donationsCount > 0) "$donationsCount times" else "Never",
            isOnline = true
        )
        currentUserProfile.value = updated
        saveUserProfileInPrefs(updated, currentUserEmail.value)
        viewModelScope.launch {
            repository.saveDonor(updated)
            repository.saveActivityLog("${updated.name} registered as a donor (${updated.bloodGroup}) in ${updated.location}.")
        }
    }

    fun loginWithGoogle(email: String, displayName: String?) {
        currentUserEmail.value = email
        val finalName = displayName ?: email.substringBefore("@").split(".", "_", "-")
            .joinToString(" ") { word -> 
                word.replaceFirstChar { char -> if (char.isLowerCase()) char.titlecase() else char.toString() } 
            }
        
        val storedId = if (sharedPrefs.getString("user_email", "") == email) {
            currentUserProfile.value.id
        } else {
            UUID.randomUUID().toString()
        }

        val current = currentUserProfile.value
        val updated = current.copy(
            id = storedId,
            name = finalName
        )
        currentUserProfile.value = updated
        saveUserProfileInPrefs(updated, email)
        viewModelScope.launch {
            repository.saveDonor(updated)
            repository.saveActivityLog("Logged in with Google Account: $email")
        }
    }

    fun createNewAccount(
        name: String,
        email: String,
        bloodGroup: String,
        location: String,
        phone: String,
        password: String = "",
        onSuccess: () -> Unit = {},
        onError: (String) -> Unit = {}
    ) {
        val newId = UUID.randomUUID().toString()
        val auth = FirebaseSafeAccess.auth

        fun completeAccountCreation(uid: String) {
            currentUserEmail.value = email
            val donor = Donor(
                id = uid,
                name = name,
                bloodGroup = bloodGroup,
                location = location,
                phone = phone,
                donationsCount = 0,
                isAvailable = true,
                lastDonationDate = "Never",
                isOnline = true
            )
            currentUserProfile.value = donor
            saveUserProfileInPrefs(donor, email)
            viewModelScope.launch {
                try {
                    repository.saveDonor(donor)
                    repository.saveActivityLog("Created new donor account for $name ($email)")
                } catch (e: Exception) {
                    Log.e("BloodViewModel", "Error saving donor: ${e.message}")
                }
            }
            onSuccess()
        }

        if (auth != null && email.isNotBlank() && password.length >= 6) {
            try {
                auth.createUserWithEmailAndPassword(email, password)
                    .addOnSuccessListener { result ->
                        val uid = result.user?.uid ?: newId
                        completeAccountCreation(uid)
                    }
                    .addOnFailureListener { e ->
                        Log.w("BloodViewModel", "Firebase auth createUser failed: ${e.message}")
                        if (e.message?.contains("already in use") == true) {
                            auth.signInWithEmailAndPassword(email, password)
                                .addOnSuccessListener { signInResult ->
                                    val uid = signInResult.user?.uid ?: newId
                                    completeAccountCreation(uid)
                                }
                                .addOnFailureListener {
                                    completeAccountCreation(newId)
                                }
                        } else {
                            completeAccountCreation(newId)
                        }
                    }
            } catch (e: Exception) {
                Log.e("BloodViewModel", "Exception during createUserWithEmailAndPassword: ${e.message}")
                completeAccountCreation(newId)
            }
        } else {
            completeAccountCreation(newId)
        }
    }

    fun loginWithEmailAndPassword(
        email: String,
        password: String,
        onSuccess: () -> Unit = {},
        onError: (String) -> Unit = {}
    ) {
        val auth = FirebaseSafeAccess.auth
        val prefix = email.substringBefore("@")
        val defaultName = prefix.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }

        fun completeLogin(uid: String) {
            currentUserEmail.value = email
            val donor = Donor(
                id = uid,
                name = defaultName,
                bloodGroup = "O+",
                location = "Cox's Bazar",
                phone = "",
                donationsCount = 0,
                isAvailable = true,
                lastDonationDate = "Never",
                isOnline = true
            )
            currentUserProfile.value = donor
            saveUserProfileInPrefs(donor, email)
            viewModelScope.launch {
                try {
                    repository.saveDonor(donor)
                    repository.saveActivityLog("Signed in with email: $email")
                } catch (e: Exception) {
                    Log.e("BloodViewModel", "Error saving donor during login: ${e.message}")
                }
            }
            onSuccess()
        }

        if (auth != null && email.isNotBlank() && password.isNotEmpty()) {
            try {
                auth.signInWithEmailAndPassword(email, password)
                    .addOnSuccessListener { result ->
                        val uid = result.user?.uid ?: UUID.randomUUID().toString()
                        completeLogin(uid)
                    }
                    .addOnFailureListener { e ->
                        Log.w("BloodViewModel", "Firebase sign in failed: ${e.message}")
                        completeLogin(UUID.randomUUID().toString())
                    }
            } catch (e: Exception) {
                Log.e("BloodViewModel", "Exception during signInWithEmailAndPassword: ${e.message}")
                completeLogin(UUID.randomUUID().toString())
            }
        } else {
            completeLogin(UUID.randomUUID().toString())
        }
    }

    fun loginWithEmail(newEmail: String) {
        currentUserEmail.value = newEmail
        val prefix = newEmail.substringBefore("@")
        val formattedName = prefix.split(".", "_", "-")
            .joinToString(" ") { word -> 
                word.replaceFirstChar { char -> if (char.isLowerCase()) char.titlecase() else char.toString() } 
            }
        
        // When logging in with a new email, create a new unique ID for this email or keep the existing if same email.
        val storedId = if (sharedPrefs.getString("user_email", "") == newEmail) {
            currentUserProfile.value.id
        } else {
            UUID.randomUUID().toString()
        }

        val current = currentUserProfile.value
        val updated = current.copy(
            id = storedId,
            name = formattedName
        )
        currentUserProfile.value = updated
        saveUserProfileInPrefs(updated, newEmail)
        viewModelScope.launch {
            repository.saveDonor(updated)
            repository.saveActivityLog("Logged in with Gmail: $newEmail")
        }
    }

    fun createBloodRequest(patientName: String, bloodGroup: String, location: String, phone: String, condition: String, dateRequired: String, urgency: String) {
        val request = BloodRequest(
            id = UUID.randomUUID().toString(),
            patientName = patientName,
            bloodGroup = bloodGroup,
            location = location,
            phone = phone,
            condition = condition,
            dateRequired = dateRequired,
            urgency = urgency
        )
        viewModelScope.launch {
            repository.saveRequest(request)
            
            // Trigger a physical notification popup on the device/emulator!
            LocalNotificationHelper.showUrgentRequestNotification(
                getApplication(),
                bloodGroup,
                location,
                condition
            )
        }
    }

    fun sendChatMessage(receiverId: String, text: String, receiverName: String) {
        val message = ChatMessage(
            id = UUID.randomUUID().toString(),
            senderId = currentUserProfile.value.id,
            receiverId = receiverId,
            senderName = currentUserProfile.value.name,
            text = text
        )
        viewModelScope.launch {
            repository.saveMessage(message)
            repository.saveActivityLog("Sent message to $receiverName: \"${if (text.length > 20) text.take(20) + "..." else text}\"")
        }
    }

    fun deleteRequest(id: String) {
        viewModelScope.launch {
            repository.deleteRequest(id)
        }
    }
}
