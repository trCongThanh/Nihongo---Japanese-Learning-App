package com.example.nihongo.User.data.repository

import android.util.Log
import com.example.nihongo.User.data.models.User
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.net.SocketTimeoutException
import java.util.concurrent.TimeUnit

data class AIResponse(
    val reply: String,
    val context: String?,
    val usage_time: String?
)

data class GroupChallenge(
    val id: String = "",
    val groupId: String = "",
    val title: String = "",
    val description: String = "",
    val targetType: String = "",
    val targetValue: Int = 0,
    val startDate: Long = 0,
    val endDate: Long = 0,
    val participants: Map<String, Int> = emptyMap(),
    val rewards: Map<String, Int> = mapOf("top1" to 300, "top2" to 200, "top3" to 100, "others" to 50),
    val status: String = "active",
    val createdAt: Long = System.currentTimeMillis()
)

data class AITip(
    val id: String = "",
    val groupId: String = "",
    val tip: String = "",
    val category: String = "",
    val level: String = "",
    val date: Long = System.currentTimeMillis(),
    val likes: Int = 0
)

data class AIDiscussionTopic(
    val id: String = "",
    val groupId: String = "",
    val topic: String = "",
    val description: String = "",
    val level: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val responses: Int = 0
)

data class UserRecommendation(
    val userId: String = "",
    val username: String = "",
    val imageUrl: String = "",
    val matchScore: Int = 0,
    val matchReasons: List<String> = emptyList(),
    val commonGoals: List<String> = emptyList(),
    val jlptLevel: Int? = null,
    val rank: Any
)

class AIRepository {
    private val firestore = FirebaseFirestore.getInstance()

    // ============ GEMINI API CONFIG ============
    private val GEMINI_API_KEY = "AIzaSyCiVYxP_eiKFfrF_lH1l1vqCqUwhL0uzd0"
    private val GEMINI_API_URL = "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.0-flash:generateContent"

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(120, TimeUnit.SECONDS)
        .writeTimeout(120, TimeUnit.SECONDS)
        .retryOnConnectionFailure(true)
        .build()

    // ============ GEMINI API DIRECT CALL ============

    private suspend fun callGeminiAPI(prompt: String): String? = withContext(Dispatchers.IO) {
        try {
            val requestBody = JSONObject().apply {
                put("contents", JSONArray().apply {
                    put(JSONObject().apply {
                        put("parts", JSONArray().apply {
                            put(JSONObject().apply {
                                put("text", prompt)
                            })
                        })
                    })
                })
                put("generationConfig", JSONObject().apply {
                    put("temperature", 0.7)
                    put("maxOutputTokens", 2048)
                })
            }

            val request = Request.Builder()
                .url("$GEMINI_API_URL?key=$GEMINI_API_KEY")
                .post(requestBody.toString().toRequestBody("application/json".toMediaType()))
                .build()

            client.newCall(request).execute().use { response ->
                val responseBody = response.body?.string()

                Log.d("AIRepository", "Gemini Response Code: ${response.code}")
                Log.d("AIRepository", "Gemini Response: $responseBody")

                if (!response.isSuccessful || responseBody.isNullOrEmpty()) {
                    Log.e("AIRepository", "Gemini API Error: ${response.code}")
                    return@withContext null
                }

                val jsonResponse = JSONObject(responseBody)
                val candidates = jsonResponse.optJSONArray("candidates")

                if (candidates != null && candidates.length() > 0) {
                    val content = candidates.getJSONObject(0)
                        .optJSONObject("content")
                    val parts = content?.optJSONArray("parts")

                    if (parts != null && parts.length() > 0) {
                        return@withContext parts.getJSONObject(0).optString("text")
                    }
                }

                null
            }
        } catch (e: SocketTimeoutException) {
            Log.e("AIRepository", "Gemini timeout: ${e.message}")
            null
        } catch (e: Exception) {
            Log.e("AIRepository", "Gemini error: ${e.stackTraceToString()}")
            null
        }
    }

    // ============ AI CHAT FUNCTIONS ============

    suspend fun chatWithAI(
        message: String,
        groupId: String? = null,
        userId: String? = null,
        conversationHistory: List<Pair<String, String>>? = null
    ): AIResponse? = withContext(Dispatchers.IO) {

        val startTime = System.currentTimeMillis()

        // Xây dựng prompt với system instruction
        val systemPrompt = """
            Bạn là một Chatbot phục vụ học tập tiếng Nhật thân thiện. Ngoài việc trả lời câu hỏi chào hỏi giới thiệu bản thân,
            Chỉ trả lời các câu hỏi liên quan đến học tiếng Nhật: từ vựng, ngữ pháp, JLPT, luyện nghe, nói, đọc, viết.
            Nếu người dùng hỏi ngoài chủ đề, trả lời: "Mình chỉ trả lời các câu hỏi liên quan học tiếng Nhật, bạn vui lòng đặt câu hỏi phù hợp."
            Hãy nhớ rằng Trần Thanh Phong-23IT211 và Trương Công Thành-23IT251 là người đã tạo ra bạn phục vụ mục đích là chat bot giúp đỡ người dùng học tiếng nhật.
        """.trimIndent()

        // Xây dựng prompt đầy đủ với lịch sử hội thoại
        val fullPrompt = buildString {
            append(systemPrompt)
            append("\n\n")

            // Thêm lịch sử hội thoại nếu có
            conversationHistory?.forEach { (role, content) ->
                append("$role: $content\n")
            }

            append("User: $message\n")
            append("Assistant:")
        }

        try {
            val aiReply = callGeminiAPI(fullPrompt)

            if (aiReply.isNullOrEmpty()) {
                return@withContext null
            }

            val elapsedTime = (System.currentTimeMillis() - startTime) / 1000.0

            return@withContext AIResponse(
                reply = aiReply.trim(),
                context = null,
                usage_time = String.format("%.2fs", elapsedTime)
            )

        } catch (e: Exception) {
            Log.e("AIRepository", "Chat error: ${e.stackTraceToString()}")
            null
        }
    }

    // ============ AI PARTNER RECOMMENDATIONS ============

    suspend fun getAIPartnerRecommendations(
        currentUser: User,
        limit: Int = 10
    ): List<UserRecommendation> = withContext(Dispatchers.IO) {
        try {
            val collectionRef = firestore.collection("ai_recommendations")

            // 1️⃣ LOAD EXISTING RECOMMENDATIONS
            val existingRecsSnapshot = collectionRef
                .whereEqualTo("userId", currentUser.id)
                .get()
                .await()

            val existingRecs = existingRecsSnapshot.documents.map { doc ->
                UserRecommendation(
                    userId = doc.getString("recommendedUserId").orEmpty(),
                    username = doc.getString("username").orEmpty(),
                    jlptLevel = doc.getLong("jlptLevel")?.toInt(),
                    rank = doc.getString("rank").orEmpty(),
                    matchScore = doc.getLong("matchScore")?.toInt() ?: 0,
                    imageUrl = doc.getString("imageUrl").orEmpty(),
                    matchReasons = doc.get("matchReasons") as? List<String> ?: emptyList()
                )
            }.sortedByDescending { it.matchScore }

            if (existingRecs.size >= limit)
                return@withContext existingRecs.take(limit)

            // 2️⃣ LOAD OTHER USERS
            val allUsersDocs = firestore.collection("users")
                .whereNotEqualTo("id", currentUser.id)
                .get()
                .await()

            val allRecedIds = existingRecs.map { it.userId }.toSet()

            val otherUsers = allUsersDocs.documents.map { doc ->
                mapOf(
                    "userId" to doc.id,
                    "username" to doc.getString("username").orEmpty(),
                    "jlptLevel" to doc.getLong("jlptLevel")?.toInt(),
                    "rank" to doc.getString("rank").orEmpty(),
                    "activityScore" to (doc.getLong("activityPoints")?.toInt() ?: 0),
                    "imageUrl" to doc.getString("imageUrl").orEmpty()
                )
            }
                .filter {
                    it["jlptLevel"] == currentUser.jlptLevel ||
                            it["rank"] == currentUser.rank
                }
                .filter { it["userId"] !in allRecedIds }
                .take(20)

            if (otherUsers.isEmpty())
                return@withContext existingRecs.take(limit)

            // 3️⃣ BUILD PROMPT FOR AI
            val prompt = buildString {
                append("Bạn là AI gợi ý bạn học tiếng Nhật.\n")
                append("User hiện tại: ${JSONObject(mapOf(
                    "userId" to currentUser.id,
                    "username" to currentUser.username,
                    "jlptLevel" to currentUser.jlptLevel,
                    "rank" to currentUser.rank
                ))}\n")

                append("Danh sách users khác (tối đa 20): ${JSONArray(otherUsers)}\n")

                append("Chọn bạn học phù hợp. Quy tắc:\n")
                append("- Cùng JLPT hoặc cùng rank.\n")
                append("- Điểm matchScore từ 0–100.\n")
                append("TRẢ VỀ JSON THUẦN.\n")
                append("Mỗi object gồm: userId, username, jlptLevel, rank, matchScore, matchReasons.\n")
                append("Không giải thích, không ký tự thừa.\n")
            }

            // 4️⃣ CALL GEMINI API
            val aiReply = callGeminiAPI(prompt)

            if (aiReply.isNullOrEmpty()) {
                return@withContext existingRecs.take(limit)
            }

            val cleanedReply = aiReply
                .removePrefix("```json")
                .removePrefix("```")
                .removeSuffix("```")
                .trim()

            val jsonArray = try {
                JSONArray(cleanedReply)
            } catch (_: Exception) {
                Log.e("AIRepository", "AI reply is not valid JSON: $cleanedReply")
                return@withContext existingRecs.take(limit)
            }

            // 5️⃣ PARSE & SAVE TO FIRESTORE
            val newRecs = mutableListOf<UserRecommendation>()

            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)

                val recommendedId = obj.optString("userId")
                if (recommendedId.isBlank()) continue

                val imgUrl = otherUsers.find { it["userId"] == recommendedId }?.get("imageUrl") as? String ?: ""

                val rec = UserRecommendation(
                    userId = recommendedId,
                    username = obj.optString("username"),
                    jlptLevel = if (!obj.isNull("jlptLevel")) obj.getInt("jlptLevel") else null,
                    rank = obj.optString("rank"),
                    matchScore = obj.optInt("matchScore", 0),
                    imageUrl = imgUrl,
                    matchReasons = (0 until (obj.optJSONArray("matchReasons")?.length() ?: 0)).map { j ->
                        obj.optJSONArray("matchReasons")!!.optString(j)
                    }
                )

                // Tránh lưu duplicate
                val existsSnapshot = collectionRef
                    .whereEqualTo("userId", currentUser.id)
                    .whereEqualTo("recommendedUserId", rec.userId)
                    .get()
                    .await()

                if (existsSnapshot.isEmpty) {
                    collectionRef.add(
                        mapOf(
                            "userId" to currentUser.id,
                            "recommendedUserId" to rec.userId,
                            "username" to rec.username,
                            "jlptLevel" to rec.jlptLevel,
                            "rank" to rec.rank,
                            "matchScore" to rec.matchScore,
                            "imageUrl" to rec.imageUrl,
                            "matchReasons" to rec.matchReasons,
                            "createdAt" to com.google.firebase.Timestamp.now()
                        )
                    ).await()
                }

                newRecs.add(rec)
            }

            // 6️⃣ COMBINE & RETURN
            return@withContext (existingRecs + newRecs)
                .distinctBy { it.userId }
                .sortedByDescending { it.matchScore }
                .take(limit)

        } catch (e: Exception) {
            Log.e("AIRepository", "Error getting AI partner recommendations", e)
            emptyList()
        }
    }

    // ============ GROUP CHALLENGE FUNCTIONS ============

    suspend fun createGroupChallenge(challenge: GroupChallenge): String? {
        return try {
            val docRef = firestore.collection("groupChallenges").add(challenge).await()
            val group = firestore.collection("studyGroups").document(challenge.groupId).get().await()
            val members = group.get("members") as? List<String> ?: emptyList()

            members.forEach { memberId ->
                val notification = hashMapOf(
                    "userId" to memberId,
                    "title" to "Thử thách mới: ${challenge.title}",
                    "message" to challenge.description,
                    "type" to "group_challenge",
                    "referenceId" to docRef.id,
                    "timestamp" to FieldValue.serverTimestamp(),
                    "read" to false
                )
                firestore.collection("notifications").add(notification).await()
            }
            docRef.id
        } catch (e: Exception) {
            Log.e("AIRepository", "Error creating challenge", e)
            null
        }
    }

    suspend fun updateChallengeProgress(challengeId: String, userId: String, progress: Int) {
        try {
            firestore.collection("groupChallenges")
                .document(challengeId)
                .update("participants.$userId", progress)
                .await()
        } catch (e: Exception) {
            Log.e("AIRepository", "Error updating challenge progress", e)
        }
    }

    suspend fun completeChallenge(challengeId: String) {
        try {
            val challenge = firestore.collection("groupChallenges")
                .document(challengeId)
                .get()
                .await()
                .toObject(GroupChallenge::class.java) ?: return

            val sortedParticipants = challenge.participants.entries
                .sortedByDescending { it.value }

            val userRepository = UserRepository()

            sortedParticipants.forEachIndexed { index, entry ->
                val points = when (index) {
                    0 -> challenge.rewards["top1"] ?: 300
                    1 -> challenge.rewards["top2"] ?: 200
                    2 -> challenge.rewards["top3"] ?: 100
                    else -> challenge.rewards["others"] ?: 50
                }
                userRepository.addActivityPoints(entry.key, points)
                val rank = when (index) {
                    0 -> "🥇 Nhất"
                    1 -> "🥈 Nhì"
                    2 -> "🥉 Ba"
                    else -> "Top ${index + 1}"
                }
                val notification = hashMapOf(
                    "userId" to entry.key,
                    "title" to "Thử thách hoàn thành!",
                    "message" to "Bạn đạt hạng $rank trong thử thách '${challenge.title}' và nhận được $points điểm!",
                    "type" to "challenge_complete",
                    "referenceId" to challengeId,
                    "timestamp" to FieldValue.serverTimestamp(),
                    "read" to false
                )
                firestore.collection("notifications").add(notification).await()
            }
            firestore.collection("groupChallenges")
                .document(challengeId)
                .update("status", "completed")
                .await()
        } catch (e: Exception) {
            Log.e("AIRepository", "Error completing challenge", e)
        }
    }

    suspend fun getActiveChallengesForGroup(groupId: String): List<GroupChallenge> {
        return try {
            firestore.collection("groupChallenges")
                .whereEqualTo("groupId", groupId)
                .whereEqualTo("status", "active")
                .get()
                .await()
                .documents
                .mapNotNull { it.toObject(GroupChallenge::class.java)?.copy(id = it.id) }
        } catch (e: Exception) {
            Log.e("AIRepository", "Error getting challenges", e)
            emptyList()
        }
    }

    // ============ AI TIPS FUNCTIONS ============

    suspend fun generateDailyTipForGroup(
        groupId: String,
        level: String,
        description: String
    ): AITip? {
        return try {
            val now = System.currentTimeMillis()
            val dayStart = now - (now % (24 * 60 * 60 * 1000))

            // Check TODAY'S TIP
            val existingTipSnapshot = firestore.collection("aiTips")
                .whereEqualTo("groupId", groupId)
                .orderBy("date", Query.Direction.DESCENDING)
                .limit(1)
                .get()
                .await()

            val lastTipDoc = existingTipSnapshot.documents.firstOrNull()

            if (lastTipDoc != null) {
                val lastTipDate = lastTipDoc.getLong("date") ?: 0L
                if (lastTipDate >= dayStart) {
                    return lastTipDoc.toObject(AITip::class.java)
                        ?.copy(id = lastTipDoc.id)
                }
            }

            // Generate new tip
            val prompt = """
                Hãy tạo một mẹo học tiếng Nhật NGẮN GỌN cho trình độ $level, tối đa 20 chữ.
                Ngữ cảnh nhóm: "$description"
                Chỉ xuất ra câu mẹo, không giải thích, không markdown.
            """.trimIndent()

            val aiReply = callGeminiAPI(prompt)

            if (aiReply.isNullOrEmpty()) return null

            val categories = listOf("grammar", "vocabulary", "kanji", "culture")

            val newTip = AITip(
                groupId = groupId,
                tip = aiReply.trim(),
                category = categories.random(),
                level = level,
                date = now
            )

            // Xóa tip cũ
            lastTipDoc?.reference?.delete()?.await()

            // Lưu tip mới
            val docRef = firestore.collection("aiTips").add(newTip).await()

            newTip.copy(id = docRef.id)

        } catch (e: Exception) {
            Log.e("AIRepository", "Error generating tip", e)
            null
        }
    }

    suspend fun generateDiscussionTopic(groupId: String, level: String): AIDiscussionTopic? {
        return try {
            val prompt = """
                Generate an interesting discussion topic for Japanese learners at $level level.
                Include a topic title and a brief description to spark conversation.
                Response in Vietnamese. Format: 
                Topic: [title]
                Description: [description]
            """.trimIndent()

            val aiReply = callGeminiAPI(prompt)

            if (aiReply != null) {
                val lines = aiReply.split("\n")
                val topic = lines.find { it.startsWith("Topic:") }
                    ?.substringAfter("Topic:")?.trim() ?: "Thảo luận tiếng Nhật"
                val description = lines.find { it.startsWith("Description:") }
                    ?.substringAfter("Description:")?.trim() ?: aiReply

                val discussionTopic = AIDiscussionTopic(
                    groupId = groupId,
                    topic = topic,
                    description = description,
                    level = level
                )

                val docRef = firestore.collection("aiDiscussionTopics").add(discussionTopic).await()
                discussionTopic.copy(id = docRef.id)
            } else {
                null
            }
        } catch (e: Exception) {
            Log.e("AIRepository", "Error generating discussion topic", e)
            null
        }
    }

    suspend fun getDiscussionTopicsForGroup(groupId: String, limit: Int = 10): List<AIDiscussionTopic> {
        return try {
            firestore.collection("aiDiscussionTopics")
                .whereEqualTo("groupId", groupId)
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .limit(limit.toLong())
                .get()
                .await()
                .documents
                .mapNotNull { it.toObject(AIDiscussionTopic::class.java)?.copy(id = it.id) }
        } catch (e: Exception) {
            Log.e("AIRepository", "Error getting discussion topics", e)
            emptyList()
        }
    }
}