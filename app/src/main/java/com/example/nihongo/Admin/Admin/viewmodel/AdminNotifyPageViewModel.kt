package com.example.nihongo.Admin.viewmodel

import Campaign
import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.nihongo.Admin.utils.AlarmReceiver
import com.google.auth.oauth2.GoogleCredentials
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.io.InputStream
import java.io.OutputStream
import java.net.HttpURLConnection
import java.net.URL

class AdminNotifyPageViewModel : ViewModel() {
    private val projectId = "nihongo-ae96a"
    private val db = FirebaseFirestore.getInstance()
    private val campaignsCollection = db.collection("campaigns")

    private val _campaigns = MutableStateFlow<List<Campaign>>(emptyList())
    val campaigns: StateFlow<List<Campaign>> = _campaigns

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    init {
        loadCampaigns()
    }

    fun loadCampaigns() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                campaignsCollection
                    .orderBy("createdAt", Query.Direction.DESCENDING)
                    .addSnapshotListener { snapshot, error ->
                        if (error != null) {
                            Log.e("AdminViewModel", "Error loading campaigns", error)
                            return@addSnapshotListener
                        }

                        val campaignsList = snapshot?.documents?.mapNotNull { doc ->
                            doc.toObject(Campaign::class.java)
                        } ?: emptyList()

                        _campaigns.value = campaignsList
                    }
            } catch (e: Exception) {
                Log.e("AdminViewModel", "Failed to load campaigns", e)
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun sendCampaign(campaign: Campaign, context: Context) {
        viewModelScope.launch {
            try {
                // First save to Firestore
                val campaignToSave = campaign.copy(sent = true, lastSent = Timestamp.now())
                saveCampaignToFirestore(campaignToSave)

                // Then send the notification
                sendFirebaseNotification(campaign, context)
            } catch (e: Exception) {
                Log.e("AdminViewModel", "Failed to send campaign", e)
            }
        }
    }

    fun saveCampaign(context: Context, campaign: Campaign) {
        viewModelScope.launch {
            try {
                val savedCampaign = saveCampaignToFirestore(campaign)

                if (savedCampaign.isScheduled && savedCampaign.scheduledFor != null) {
                    AlarmReceiver.scheduleOneTimeNotification(context, savedCampaign)
                }

                if (savedCampaign.isDaily) {
                    AlarmReceiver.scheduleDailyNotification(context, savedCampaign)
                }
            } catch (e: Exception) {
                Log.e("AdminViewModel", "Failed to save campaign", e)
            }
        }
    }

    fun updateCampaign(campaign: Campaign, context: Context) {
        viewModelScope.launch {
            try {
                val updatedCampaign = saveCampaignToFirestore(campaign)

                // Cancel any existing scheduled notifications
                AlarmReceiver.cancelNotification(context, campaign.id)

                // Reschedule according to updated settings
                if (updatedCampaign.isScheduled && updatedCampaign.scheduledFor != null) {
                    AlarmReceiver.scheduleOneTimeNotification(context, updatedCampaign)
                }

                if (updatedCampaign.isDaily) {
                    AlarmReceiver.scheduleDailyNotification(context, updatedCampaign)
                }
            } catch (e: Exception) {
                Log.e("AdminViewModel", "Failed to update campaign", e)
            }
        }
    }

    private suspend fun saveCampaignToFirestore(campaign: Campaign): Campaign {
        return try {
            if (campaign.id.isEmpty()) {
                // Create new document and get ID to assign
                val docRef = campaignsCollection.add(campaign).await()
                val updatedCampaign = campaign.copy(
                    id = docRef.id,
                    createdAt = Timestamp.now()  // Add creation timestamp
                )
                campaignsCollection.document(docRef.id).set(updatedCampaign).await()
                updatedCampaign
            } else {
                // Update if ID exists already
                val updatedCampaign = campaign.copy(
                    updatedAt = Timestamp.now()  // Add updated timestamp
                )
                campaignsCollection.document(campaign.id).set(updatedCampaign).await()
                updatedCampaign
            }
        } catch (e: Exception) {
            Log.e("AdminViewModel", "Error saving campaign to Firestore", e)
            throw e
        }
    }

    fun deleteCampaign(campaignId: String, context: Context) {
        viewModelScope.launch {
            try {
                // Cancel any scheduled notifications
                AlarmReceiver.cancelNotification(context, campaignId)

                // Delete from Firestore
                campaignsCollection.document(campaignId).delete().await()
            } catch (e: Exception) {
                Log.e("AdminViewModel", "Failed to delete campaign", e)
            }
        }
    }

    private fun sendFirebaseNotification(campaign: Campaign, context: Context) {
        sendOneSignalPushNotification(campaign)
    }


    fun sendOneSignalPushNotification(campaign: Campaign) {
        CoroutineScope(Dispatchers.IO).launch {
            val title = campaign.title
            val message = campaign.message
            val imageUrl = campaign.imageUrl

            Log.d("PushNotification", "Preparing notification...")
            Log.d("PushNotification", "Title: $title")
            Log.d("PushNotification", "Message: $message")
            Log.d("PushNotification", "Image URL: $imageUrl")

            try {
                val url = URL("https://onesignal.com/api/v1/notifications")
                val connection = url.openConnection() as HttpURLConnection
                connection.requestMethod = "POST"
                connection.setRequestProperty(
                    "Authorization",
                    "Basic os_v2_app_i74wkoclfnethgm2veasbagu5h5ouj2skp7eurmr3sf6xri2sjjeiyk7dniz7iequz5qntoqd7gcxij7ncxvi2ciz627g4o4g3qwccy"
                )
                connection.setRequestProperty("Content-Type", "application/json")
                connection.doOutput = true

                val jsonPayload = """
                {
                    "app_id": "47f96538-4b2b-4933-999a-a9012080d4e9",
                    "headings": {"en": "$title"},
                    "contents": {"en": "$message"},
                    "included_segments": ["All"],
                    "big_picture": "$imageUrl"
                }
            """.trimIndent()

                Log.d("PushNotification", "JSON Payload: $jsonPayload")

                val os: OutputStream = connection.outputStream
                os.write(jsonPayload.toByteArray())
                os.flush()

                val responseCode = connection.responseCode
                val responseMessage = connection.inputStream.bufferedReader().use { it.readText() }

                Log.d("PushNotification", "Response Code: $responseCode")
                Log.d("PushNotification", "Response Message: $responseMessage")

            } catch (e: Exception) {
                Log.e("PushNotification", "Error sending notification", e)
            }
        }
    }


    fun sendNotificationToUser(campaign: Campaign, context: Context, token: String) {
        sendOneSignalPushNotificationToUser(campaign, token)
    }

    fun sendOneSignalPushNotificationToUser(campaign: Campaign, token: String) {
        CoroutineScope(Dispatchers.IO).launch {
            val title = campaign.title
            val message = campaign.message
            val imageUrl = campaign.imageUrl

            Log.d("PushNotification", "Preparing notification...")
            Log.d("PushNotification", "Title: $title")
            Log.d("PushNotification", "Message: $message")
            Log.d("PushNotification", "Image URL: $imageUrl")

            try {
                val url = URL("https://onesignal.com/api/v1/notifications")
                val connection = url.openConnection() as HttpURLConnection
                connection.requestMethod = "POST"
                connection.setRequestProperty(
                    "Authorization",
                    "Basic os_v2_app_i74wkoclfnethgm2veasbagu5h5ouj2skp7eurmr3sf6xri2sjjeiyk7dniz7iequz5qntoqd7gcxij7ncxvi2ciz627g4o4g3qwccy"
                )
                connection.setRequestProperty("Content-Type", "application/json")
                connection.doOutput = true

                val jsonPayload = """
                {
                    "app_id": "47f96538-4b2b-4933-999a-a9012080d4e9",
                    "headings": {"en": "$title"},
                    "contents": {"en": "$message"},
                    "filters": [
                        { "field": "tag", "key": "$token", "relation": "=", "value": "true" }
                    ]
                }
            """.trimIndent()

                Log.d("PushNotification", "JSON Payload: $jsonPayload")

                val os: OutputStream = connection.outputStream
                os.write(jsonPayload.toByteArray())
                os.flush()

                val responseCode = connection.responseCode
                val responseMessage = connection.inputStream.bufferedReader().use { it.readText() }

                Log.d("PushNotification", "Response Code: $responseCode")
                Log.d("PushNotification", "Response Message: $responseMessage")

            } catch (e: Exception) {
                Log.e("PushNotification", "Error sending notification", e)
            }
        }
    }
    fun sendAdminOnlyNotification(campaign: Campaign) {
        CoroutineScope(Dispatchers.IO).launch {
            val title = campaign.title
            val message = campaign.message
            val imageUrl = campaign.imageUrl

            Log.d("PushNotification", "Preparing admin-only notification...")
            Log.d("PushNotification", "Title: $title")
            Log.d("PushNotification", "Message: $message")

            try {
                val url = URL("https://onesignal.com/api/v1/notifications")
                val connection = url.openConnection() as HttpURLConnection
                connection.requestMethod = "POST"
                connection.setRequestProperty(
                    "Authorization",
                    "Basic os_v2_app_i74wkoclfnethgm2veasbagu5h5ouj2skp7eurmr3sf6xri2sjjeiyk7dniz7iequz5qntoqd7gcxij7ncxvi2ciz627g4o4g3qwccy"
                )
                connection.setRequestProperty("Content-Type", "application/json")
                connection.doOutput = true

                // Sử dụng cấu trúc JSON đơn giản giống như sendOneSignalPushNotificationToUser
                val jsonPayload = """
                {
                    "app_id": "47f96538-4b2b-4933-999a-a9012080d4e9",
                    "headings": {"en": "$title"},
                    "contents": {"en": "$message"},
                    "filters": [
                        {"field": "tag", "key": "admin", "relation": "=", "value": "true"}
                    ]
                }
            """.trimIndent()

                Log.d("PushNotification", "JSON Payload: $jsonPayload")

                val os: OutputStream = connection.outputStream
                os.write(jsonPayload.toByteArray())
                os.flush()

                val responseCode = connection.responseCode

                if (responseCode >= 200 && responseCode < 300) {
                    val responseMessage = connection.inputStream.bufferedReader().use { it.readText() }
                    Log.d("PushNotification", "Success Response: $responseMessage")
                } else {
                    val errorMessage = connection.errorStream.bufferedReader().use { it.readText() }
                    Log.e("PushNotification", "Error Response: $errorMessage")
                }

            } catch (e: Exception) {
                Log.e("PushNotification", "Error sending notification", e)
            }
        }
    }
    // Phương thức gửi thông báo đến một người dùng cụ thể bằng userId
    fun sendNotificationToSpecificUser(title: String, message: String, userEmail: String) {
        CoroutineScope(Dispatchers.IO).launch {
            Log.d("PushNotification", "Preparing notification to user email: $userEmail")
            Log.d("PushNotification", "Title: $title")
            Log.d("PushNotification", "Message: $message")

            try {
                val url = URL("https://onesignal.com/api/v1/notifications")
                val connection = url.openConnection() as HttpURLConnection
                connection.requestMethod = "POST"
                connection.setRequestProperty(
                    "Authorization",
                    "Basic os_v2_app_i74wkoclfnethgm2veasbagu5h5ouj2skp7eurmr3sf6xri2sjjeiyk7dniz7iequz5qntoqd7gcxij7ncxvi2ciz627g4o4g3qwccy"
                )
                connection.setRequestProperty("Content-Type", "application/json")
                connection.doOutput = true

                val jsonPayload = """
                {
                    "app_id": "47f96538-4b2b-4933-999a-a9012080d4e9",
                    "headings": {"en": "$title"},
                    "contents": {"en": "$message"},
                    "filters": [
                        {"field": "tag", "key": "$userEmail", "relation": "=", "value": "true"}
                    ]
                }
            """.trimIndent()
            
                Log.d("PushNotification", "JSON Payload: $jsonPayload")
            
                // Gửi request
                val outputStream = connection.outputStream
                outputStream.write(jsonPayload.toByteArray())
                outputStream.close()

                val responseCode = connection.responseCode
                Log.d("PushNotification", "Response Code: $responseCode")
            } catch (e: Exception) {
                Log.e("PushNotification", "Error sending notification", e)
            }
        }
    }

    // Phương thức gửi thông báo cho nhóm chat - gửi đến tất cả trừ người gửi
    fun sendGroupChatNotification(title: String, message: String, senderEmail: String) {
        CoroutineScope(Dispatchers.IO).launch {
            Log.d("PushNotification", "Preparing group notification to all users except sender: $senderEmail")
            Log.d("PushNotification", "Title: $title")
            Log.d("PushNotification", "Message: $message")

            try {
                val url = URL("https://onesignal.com/api/v1/notifications")
                val connection = url.openConnection() as HttpURLConnection
                connection.requestMethod = "POST"
                connection.setRequestProperty(
                    "Authorization",
                    "Basic os_v2_app_i74wkoclfnethgm2veasbagu5h5ouj2skp7eurmr3sf6xri2sjjeiyk7dniz7iequz5qntoqd7gcxij7ncxvi2ciz627g4o4g3qwccy"
                )
                connection.setRequestProperty("Content-Type", "application/json")
                connection.doOutput = true

                // Gửi đến tất cả người dùng nhưng loại trừ người gửi
                val jsonPayload = """
                {
                    "app_id": "47f96538-4b2b-4933-999a-a9012080d4e9",
                    "headings": {"en": "$title"},
                    "contents": {"en": "$message"},
                    "included_segments": ["All"],
                    "filters": [
                        {"field": "tag", "key": "$senderEmail", "relation": "!=", "value": "true"}
                    ]
                }
                """.trimIndent()

                Log.d("PushNotification", "JSON Payload: $jsonPayload")

                val os: OutputStream = connection.outputStream
                os.write(jsonPayload.toByteArray())
                os.flush()

                val responseCode = connection.responseCode
                
                if (responseCode >= 200 && responseCode < 300) {
                    val responseMessage = connection.inputStream.bufferedReader().use { it.readText() }
                    Log.d("PushNotification", "Success Response: $responseMessage")
                } else {
                    val errorMessage = connection.errorStream.bufferedReader().use { it.readText() }
                    Log.e("PushNotification", "Error Response: $errorMessage")
                }

            } catch (e: Exception) {
                Log.e("PushNotification", "Error sending group notification", e)
            }
        }
    }
    private suspend fun getAccessToken(context: Context): String? {
        return try {
            withContext(Dispatchers.IO) {
                val inputStream: InputStream = context.assets.open("nihongo-ae96a-firebase-adminsdk-fbsvc-d68fa60724.json")
                val googleCredentials = GoogleCredentials.fromStream(inputStream)
                    .createScoped(listOf("https://www.googleapis.com/auth/firebase.messaging"))
                googleCredentials.refreshIfExpired()
                googleCredentials.accessToken.tokenValue
            }
        } catch (e: Exception) {
            Log.e("FCMResponse", "Failed to get access token", e)
            null
        }
    }
}

