import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.ServerTimestamp

// Extended Campaign data class with Firebase integration
data class Campaign(
    val id: String = "",
    val title: String = "",
    val message: String = "",
    val createdAt: Timestamp? = null,
    val scheduledFor: Timestamp? = null,
    val isScheduled: Boolean = false,
    val isDaily: Boolean = false,
    val dailyHour: Int = 9, // Default to 9 AM
    val dailyMinute: Int = 0,
    val imageUrl: String? = null,
    val sent: Boolean = false,
    val lastSent: Timestamp? = null,
    val updatedAt: Timestamp? = null
)