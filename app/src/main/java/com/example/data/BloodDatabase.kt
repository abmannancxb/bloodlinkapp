package com.example.data

import android.content.Context
import androidx.room.*
import kotlinx.coroutines.flow.Flow

// ==========================================
// 1. Entities
// ==========================================

@Entity(tableName = "donors")
data class Donor(
    @PrimaryKey val id: String,
    val name: String,
    val bloodGroup: String,
    val location: String,
    val phone: String,
    val donationsCount: Int,
    val isAvailable: Boolean = true,
    val lastDonationDate: String = "Never",
    val isOnline: Boolean = false,
    val imageUrl: String = ""
)

@Entity(tableName = "blood_requests")
data class BloodRequest(
    @PrimaryKey val id: String,
    val patientName: String,
    val bloodGroup: String,
    val location: String,
    val phone: String,
    val condition: String,
    val dateRequired: String,
    val urgency: String, // "Urgent", "Emergency", "Routine"
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "chat_messages")
data class ChatMessage(
    @PrimaryKey val id: String,
    val senderId: String,
    val receiverId: String,
    val senderName: String,
    val text: String,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "activity_logs")
data class ActivityLog(
    @PrimaryKey val id: String,
    val text: String,
    val timestamp: Long = System.currentTimeMillis()
)

// ==========================================
// 2. DAOs
// ==========================================

@Dao
interface BloodDao {
    // Donors
    @Query("SELECT * FROM donors")
    fun getAllDonorsFlow(): Flow<List<Donor>>

    @Query("SELECT * FROM donors WHERE location LIKE '%' || :loc || '%' AND bloodGroup = :bloodGroup")
    fun searchDonorsFlow(loc: String, bloodGroup: String): Flow<List<Donor>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDonor(donor: Donor)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDonors(donors: List<Donor>)

    @Query("DELETE FROM donors WHERE id = :id")
    suspend fun deleteDonor(id: String)

    @Query("DELETE FROM donors")
    suspend fun deleteAllDonors()

    // Blood Requests
    @Query("SELECT * FROM blood_requests ORDER BY timestamp DESC")
    fun getAllRequestsFlow(): Flow<List<BloodRequest>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRequest(request: BloodRequest)

    @Query("DELETE FROM blood_requests WHERE id = :id")
    suspend fun deleteRequest(id: String)

    @Query("DELETE FROM blood_requests")
    suspend fun deleteAllRequests()

    // Chat Messages
    @Query("SELECT * FROM chat_messages ORDER BY timestamp ASC")
    fun getAllMessagesFlow(): Flow<List<ChatMessage>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessage(message: ChatMessage)

    @Query("DELETE FROM chat_messages")
    suspend fun deleteAllMessages()

    // Activity Logs
    @Query("SELECT * FROM activity_logs ORDER BY timestamp DESC LIMIT 50")
    fun getAllLogsFlow(): Flow<List<ActivityLog>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLog(log: ActivityLog)

    @Query("DELETE FROM activity_logs")
    suspend fun deleteAllLogs()
}

// ==========================================
// 3. Database
// ==========================================

@Database(
    entities = [Donor::class, BloodRequest::class, ChatMessage::class, ActivityLog::class],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun bloodDao(): BloodDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "blood_link_db"
                )
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
