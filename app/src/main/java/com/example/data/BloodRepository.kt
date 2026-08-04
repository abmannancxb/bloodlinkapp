package com.example.data

import android.util.Log
import com.example.data.FirebaseSafeAccess
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import java.util.UUID

object FirebaseSafeAccess {
    val isAvailable: Boolean by lazy {
        try {
            com.google.firebase.FirebaseApp.getInstance()
            true
        } catch (e: Exception) {
            Log.w("FirebaseSafeAccess", "Firebase is not initialized. Using local offline Room mode. ${e.message}")
            false
        }
    }

    val firestore: com.google.firebase.firestore.FirebaseFirestore?
        get() = if (isAvailable) {
            try {
                com.google.firebase.firestore.FirebaseFirestore.getInstance()
            } catch (e: Exception) {
                null
            }
        } else null

    val auth: com.google.firebase.auth.FirebaseAuth?
        get() = if (isAvailable) {
            try {
                com.google.firebase.auth.FirebaseAuth.getInstance()
            } catch (e: Exception) {
                null
            }
        } else null
}

class BloodRepository(private val bloodDao: BloodDao) {

    val allDonors: Flow<List<Donor>> = bloodDao.getAllDonorsFlow()
    val allRequests: Flow<List<BloodRequest>> = bloodDao.getAllRequestsFlow()
    val allMessages: Flow<List<ChatMessage>> = bloodDao.getAllMessagesFlow()
    val allLogs: Flow<List<ActivityLog>> = bloodDao.getAllLogsFlow()

    fun startRealtimeSync(scope: kotlinx.coroutines.CoroutineScope) {
        val db = FirebaseSafeAccess.firestore ?: return
        Log.d("BloodRepository", "Starting Realtime Firebase Sync...")

        // 1. Sync Donors
        try {
            db.collection("donors").addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e("BloodRepository", "Error listening to donors", error)
                    return@addSnapshotListener
                }
                snapshot?.let { querySnapshot ->
                    val donorsList = querySnapshot.documents.mapNotNull { doc ->
                        try {
                            Donor(
                                id = doc.id,
                                name = doc.getString("name") ?: "",
                                bloodGroup = doc.getString("bloodGroup") ?: "",
                                location = doc.getString("location") ?: "",
                                phone = doc.getString("phone") ?: "",
                                donationsCount = doc.getLong("donationsCount")?.toInt() ?: 0,
                                isAvailable = doc.getBoolean("isAvailable") ?: true,
                                lastDonationDate = doc.getString("lastDonationDate") ?: "Never",
                                isOnline = doc.getBoolean("isOnline") ?: false,
                                imageUrl = doc.getString("imageUrl") ?: ""
                            )
                        } catch (e: Exception) {
                            null
                        }
                    }
                    scope.launch(Dispatchers.IO) {
                        if (donorsList.isNotEmpty()) {
                            bloodDao.insertDonors(donorsList)
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("BloodRepository", "Failed to start donors Firestore listener", e)
        }

        // 2. Sync Blood Requests
        try {
            db.collection("blood_requests").addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e("BloodRepository", "Error listening to requests", error)
                    return@addSnapshotListener
                }
                snapshot?.let { querySnapshot ->
                    scope.launch(Dispatchers.IO) {
                        querySnapshot.documents.forEach { doc ->
                            try {
                                val req = BloodRequest(
                                    id = doc.id,
                                    patientName = doc.getString("patientName") ?: "",
                                    bloodGroup = doc.getString("bloodGroup") ?: "",
                                    location = doc.getString("location") ?: "",
                                    phone = doc.getString("phone") ?: "",
                                    condition = doc.getString("condition") ?: "",
                                    dateRequired = doc.getString("dateRequired") ?: "",
                                    urgency = doc.getString("urgency") ?: "Urgent",
                                    timestamp = doc.getLong("timestamp") ?: System.currentTimeMillis()
                                )
                                bloodDao.insertRequest(req)
                            } catch (e: Exception) {
                                // Ignored
                            }
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("BloodRepository", "Failed to start requests Firestore listener", e)
        }

        // 3. Sync Chat Messages
        try {
            db.collection("chat_messages").addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e("BloodRepository", "Error listening to messages", error)
                    return@addSnapshotListener
                }
                snapshot?.let { querySnapshot ->
                    scope.launch(Dispatchers.IO) {
                        querySnapshot.documents.forEach { doc ->
                            try {
                                val msg = ChatMessage(
                                    id = doc.id,
                                    senderId = doc.getString("senderId") ?: "",
                                    receiverId = doc.getString("receiverId") ?: "",
                                    senderName = doc.getString("senderName") ?: "",
                                    text = doc.getString("text") ?: "",
                                    timestamp = doc.getLong("timestamp") ?: System.currentTimeMillis()
                                )
                                bloodDao.insertMessage(msg)
                            } catch (e: Exception) {
                                // Ignored
                            }
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("BloodRepository", "Failed to start chat Firestore listener", e)
        }

        // 4. Sync Activity Logs
        try {
            db.collection("activity_logs").addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e("BloodRepository", "Error listening to activity logs", error)
                    return@addSnapshotListener
                }
                snapshot?.let { querySnapshot ->
                    scope.launch(Dispatchers.IO) {
                        querySnapshot.documents.forEach { doc ->
                            try {
                                val log = ActivityLog(
                                    id = doc.id,
                                    text = doc.getString("text") ?: "",
                                    timestamp = doc.getLong("timestamp") ?: System.currentTimeMillis()
                                )
                                bloodDao.insertLog(log)
                            } catch (e: Exception) {
                                // Ignored
                            }
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("BloodRepository", "Failed to start activity logs Firestore listener", e)
        }
    }

    suspend fun checkAndPrepopulate() = withContext(Dispatchers.IO) {
        val existing = allDonors.first()
        if (existing.isEmpty()) {
            // Initial logs
            bloodDao.insertLog(ActivityLog(UUID.randomUUID().toString(), "App initialized. Welcome to BloodLink! Ready to sync with Firebase."))
        }
    }

    fun searchDonors(loc: String, bloodGroup: String): Flow<List<Donor>> {
        return bloodDao.searchDonorsFlow(loc, bloodGroup)
    }

    // Insert Donor & Sync with Firebase Firestore
    suspend fun saveDonor(donor: Donor) = withContext(Dispatchers.IO) {
        bloodDao.insertDonor(donor)
        
        // Push to Firestore if available
        FirebaseSafeAccess.firestore?.let { db ->
            try {
                val donorMap = mapOf(
                    "id" to donor.id,
                    "name" to donor.name,
                    "bloodGroup" to donor.bloodGroup,
                    "location" to donor.location,
                    "phone" to donor.phone,
                    "donationsCount" to donor.donationsCount,
                    "isAvailable" to donor.isAvailable,
                    "lastDonationDate" to donor.lastDonationDate,
                    "isOnline" to donor.isOnline,
                    "imageUrl" to donor.imageUrl
                )
                db.collection("donors").document(donor.id).set(donorMap)
                    .addOnSuccessListener {
                        Log.d("BloodRepository", "Donor successfully synced to Firestore")
                    }
                    .addOnFailureListener { e ->
                        Log.e("BloodRepository", "Error syncing donor to Firestore", e)
                    }
            } catch (e: Exception) {
                Log.e("BloodRepository", "Firestore write failed", e)
            }
        }

        // Log this action
        saveActivityLog("Donor ${donor.name} (${donor.bloodGroup}) updated profile.")
    }

    // Insert Request & Sync with Firebase Firestore
    suspend fun saveRequest(request: BloodRequest) = withContext(Dispatchers.IO) {
        bloodDao.insertRequest(request)

        // Push to Firestore if available
        FirebaseSafeAccess.firestore?.let { db ->
            try {
                val requestMap = mapOf(
                    "id" to request.id,
                    "patientName" to request.patientName,
                    "bloodGroup" to request.bloodGroup,
                    "location" to request.location,
                    "phone" to request.phone,
                    "condition" to request.condition,
                    "dateRequired" to request.dateRequired,
                    "urgency" to request.urgency,
                    "timestamp" to request.timestamp
                )
                db.collection("blood_requests").document(request.id).set(requestMap)
                    .addOnSuccessListener {
                        Log.d("BloodRepository", "Request synced to Firestore")
                    }
            } catch (e: Exception) {
                Log.e("BloodRepository", "Firestore write failed", e)
            }
        }

        // Log this action
        saveActivityLog("Urgent request created for ${request.bloodGroup} in ${request.location}.")
    }

    // Send Message & Sync with Firebase Firestore
    suspend fun saveMessage(message: ChatMessage) = withContext(Dispatchers.IO) {
        bloodDao.insertMessage(message)

        // Push to Firestore if available
        FirebaseSafeAccess.firestore?.let { db ->
            try {
                val messageMap = mapOf(
                    "id" to message.id,
                    "senderId" to message.senderId,
                    "receiverId" to message.receiverId,
                    "senderName" to message.senderName,
                    "text" to message.text,
                    "timestamp" to message.timestamp
                )
                db.collection("chat_messages").document(message.id).set(messageMap)
                    .addOnSuccessListener {
                        Log.d("BloodRepository", "Message synced to Firestore")
                    }
            } catch (e: Exception) {
                Log.e("BloodRepository", "Firestore message write failed", e)
            }
        }
    }

    // Insert Activity Log & Sync with Firebase Firestore
    suspend fun saveActivityLog(text: String) = withContext(Dispatchers.IO) {
        val log = ActivityLog(UUID.randomUUID().toString(), text)
        bloodDao.insertLog(log)

        // Push to Firestore if available
        FirebaseSafeAccess.firestore?.let { db ->
            try {
                val logMap = mapOf(
                    "id" to log.id,
                    "text" to log.text,
                    "timestamp" to log.timestamp
                )
                db.collection("activity_logs").document(log.id).set(logMap)
            } catch (e: Exception) {
                Log.e("BloodRepository", "Firestore log write failed", e)
            }
        }
    }

    suspend fun deleteDonor(id: String) = withContext(Dispatchers.IO) {
        bloodDao.deleteDonor(id)
        FirebaseSafeAccess.firestore?.collection("donors")?.document(id)?.delete()
    }

    suspend fun deleteRequest(id: String) = withContext(Dispatchers.IO) {
        bloodDao.deleteRequest(id)
        FirebaseSafeAccess.firestore?.collection("blood_requests")?.document(id)?.delete()
    }

    suspend fun clearLocalDatabase() = withContext(Dispatchers.IO) {
        bloodDao.deleteAllDonors()
        bloodDao.deleteAllRequests()
        bloodDao.deleteAllMessages()
        bloodDao.deleteAllLogs()
        bloodDao.insertLog(ActivityLog(UUID.randomUUID().toString(), "Local database cleared. Ready for real data."))
    }
}
