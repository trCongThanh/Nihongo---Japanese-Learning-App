package com.example.nihongo.Admin.utils

import android.R.attr.apiKey
import android.R.attr.description
import android.R.attr.prompt
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Parcelable
import android.util.Log
import com.example.nihongo.Admin.viewmodel.AdminCourseViewModel
import com.example.nihongo.Admin.viewmodel.AdminExerciseViewModel
import com.example.nihongo.Admin.viewmodel.AdminLessonViewModel
import com.example.nihongo.User.data.models.Course
import com.example.nihongo.User.data.models.Exercise
import com.example.nihongo.User.data.models.ExerciseType
import com.example.nihongo.User.data.models.Lesson
import com.example.nihongo.User.data.models.SubLesson
import com.example.nihongo.User.data.models.UnitItem
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.net.SocketTimeoutException
import java.net.URLEncoder
import java.util.concurrent.TimeUnit
import kotlin.random.Random



object AiCourseGenerate {
    private const val API_KEY_1="xxx"
    private const val API_KEY_2="xxx"
    private const val API_KEY_3="Axxx"
    private const val API_KEY_4="Axxxx"
    private const val API_KEY_5="Axxxx"
    // ---- 3 API KEY GEMINI
    private val apiKeys = listOf(
        API_KEY_1,
        API_KEY_2,
        API_KEY_3,
        API_KEY_4,
        API_KEY_5
    )

    // ---- Random 1 API KEY trong danh sách
    private fun getRandomKey(): String {
        return apiKeys[Random.nextInt(apiKeys.size)]
    }

    // ---- OKHttp client
    private val client = OkHttpClient.Builder()
        .connectTimeout(520, TimeUnit.SECONDS)
        .writeTimeout(520, TimeUnit.SECONDS)
        .readTimeout(520, TimeUnit.SECONDS) // response lâu nhất
        .callTimeout(550, TimeUnit.SECONDS) // tổng thời gian tối đa cho 1 call
        .build()

    // ---- Danh sách ảnh mặc định
    private val defaultImages = listOf(
        "https://img.freepik.com/premium-vector/man-wearing-hakama-with-crest-thinking-while-scratching-his-face_180401-12331.jpg",
        "https://thumb.ac-illust.com/95/95d92d1467ccf189f05af2b503a3e5a0_t.jpeg",
        "https://drive.google.com/uc?export=view&id=1TWpes3nKYbwSWyUj0uG0v5p-_a_zaVVh"
    )

    // ---- Gửi request tới Gemini API và tự động tạo course
    suspend fun generateCourseContent(
        description: String,
        lessons: Int,
        units: Int,
        exercises: Int,
        adminCourseViewModel: AdminCourseViewModel,
        adminLessonViewModel: AdminLessonViewModel,
        adminExerciseViewModel: AdminExerciseViewModel,
        onSuccess: (String) -> Unit,
        onError: (String) -> Unit
    ) {
        val apiKey = getRandomKey()
        val url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent?key=$apiKey"

        val prompt = """
            Bạn là một giáo viên song ngữ việt-nhật và đang dạy tiếng nhật cho người việt.
            Mô tả course: $description
            Số lessons: $lessons
            Số unit mỗi lessons: $units
            tổng số bài tập và bài lý thuyết video mỗi unit: $exercises
            Bạn sẽ tạo ra bài với format trả về như sau(lưu ý chỉ trả về theo bên dưới để áp vào code):
            
            &Title&
            (ghi title ở đây - Title bằng tiếng anh nhé)
            
            &Summary&
            (ghi tóm tắt những gì sẽ được học và giá trị của course này)
            
            &Lesson&
                &Leason1&
                     &Lesson1Name&
                    (ghi name của lesson1 ở đây)
                    &Lesson1Overview&
                    (ghi overview của lesson1 ở đây)
                        &Unit1&
                            &Unit1Name&
                            (ghi name của unit1 ở đây)
                            &Unit1Type&
                            (ghi type của unit1 ở đây)
                               (Bạn tự quyết định xem ở đây có bao nhiêu type video và type pratice 
                               dựa trên nội dung chính bài học và số lượng exercise mà course mong muốn.)
                               (-nếu type video:)
                                &Unit1Video&
                                    &Unit1VideoName&
                                    (ghi name của video ở đây)
                                    &Unit1Explation&
                                    (là nơi bạn mô tả các lý thuyết bạn muốn truyền đạt trong unit)
                                        &Unit1Explation1&
                                            &Unit1Explation1Name&
                                            (ghi name của lý thuyết ở đây)
                                            &Unit1Explation1Content&
                                            (ghi nội dung lý thuyết ở đây)
                                        &Unit1Explation2,3,4,...&
                                        (tương tự &Unit1Explation1&, nếu có)
                                
                                (-nếu type pratice:-)
                                &Unit1Practice&
                                    &Unit1Practice1&
                                        &Unit1Practice1Question&
                                        (ghi mô tả câu hỏi bài tập bằng tiếng việt kết hợp tiếng nhật)
                                        &Unit1Practice1Answer&
                                        (ghi đáp án đúng ở đây)
                                        &Unit1Practice1Option&
                                        (là nơi bạn đưa ra 3 đáp án sai hoặc đáp án gẫy nhiễu)
                                            &Unit1Practice1Option1&
                                            (đáp án sai 1)
                                            &Unit1Practice1Option2&
                                            (đáp án sai 2)
                                            &Unit1Practice1Option3&
                                            (đáp án sai 3)
                                        &Unit1Practice1Explanation&
                                        (ghi bạn giải thích câu hỏi và đáp án)
                                            
                                    &Unit1Practice2,3,4,...&
                                    (tương tự &Unit1Practice1&, nếu có)
                            

                        &Unit2,3,4,...&
                        (tương tự &Unit1&, nếu có)
                
                &Leason2,3,4...&
                (tương tự &Lesson1&, nếu có)
        """.trimIndent()

        val textObj = JSONObject()
        textObj.put("text", prompt)

        val partsArray = JSONArray()
        partsArray.put(textObj)

        val contentObj = JSONObject()
        contentObj.put("parts", partsArray)

        val contentsArray = JSONArray()
        contentsArray.put(contentObj)

        val jsonBody = JSONObject()
        jsonBody.put("contents", contentsArray)

        val requestBody = jsonBody.toString()
            .toRequestBody("application/json; charset=utf-8".toMediaType())

        val request = Request.Builder()
            .url(url)
            .post(requestBody)
            .build()

        withContext(Dispatchers.IO) {
            try {
                client.newCall(request).execute().use { response ->
                    val result = response.body?.string()
                    Log.d("AiCourseGenerate", "RAW Response: $result")

                    if (result != null) {
                        try {
                            val root = JSONObject(result)
                            val candidates = root.getJSONArray("candidates")
                            if (candidates.length() > 0) {
                                val content = candidates.getJSONObject(0).getJSONObject("content")
                                val parts = content.getJSONArray("parts")
                                if (parts.length() > 0) {
                                    val text = parts.getJSONObject(0).getString("text")
                                    Log.d("AiCourseGenerate", "AI TEXT: $text")

                                    // Parse và thêm vào database
                                    parseCourseAndAddToDatabase(
                                        text,
                                        description,
                                        adminCourseViewModel,
                                        adminLessonViewModel,
                                        adminExerciseViewModel,
                                        onSuccess,
                                        onError
                                    )
                                    return@withContext
                                }
                            }
                            onError("Không thể parse response từ AI")
                        } catch (e: Exception) {
                            Log.e("AiCourseGenerate", "Parse error: ${e.message}")
                            onError("Lỗi parse: ${e.message}")
                        }
                    } else {
                        onError("Response rỗng từ AI")
                    }
                }
            } catch (e: Exception) {
                Log.e("AiCourseGenerate", "Network error: ${e.message}")
                onError("Lỗi network: ${e.message}")
            }
        }
    }

    // ---- Parse response từ AI và thêm vào database
    private suspend fun parseCourseAndAddToDatabase(
        aiResponse: String,
        description: String,
        adminCourseViewModel: AdminCourseViewModel,
        adminLessonViewModel: AdminLessonViewModel,
        adminExerciseViewModel: AdminExerciseViewModel,
        onSuccess: (String) -> Unit,
        onError: (String) -> Unit
    ) {
        try {
            // Parse Title và Summary
            val title = extractContent(aiResponse, "&Title&", "&Summary&")
            val summary = extractContent(aiResponse, "&Summary&", "&Lesson&")

            if (title.isEmpty() || summary.isEmpty()) {
                onError("Không tìm thấy Title hoặc Summary")
                return
            }

            // ❌ XÓA DÒNG NÀY
            // val courseId = generateId()

            // Tạo Course object KHÔNG có ID (để Firebase tự generate)
            val course = Course(
                id = "", // ← Để trống
                title = title,
                description = description,
                rating = 0.0,
                reviews = 0,
                likes = 0,
                imageRes = "https://drive.google.com/uc?export=view&id=1uyNSW54w4stVjixb9ke_rOFhiGaeekEN",
                vip = false
            )

            // Thêm Course vào database và LẤY courseId từ Firebase
            val courseId = addCourseAndGetId(adminCourseViewModel, course)
            Log.d("AiCourseGenerate", "✅ Course added: $title with ID: $courseId")

            // Parse và thêm Lessons với courseId đúng
            val lessonsSection = aiResponse.substring(aiResponse.indexOf("&Lesson&"))
            parseLessons(
                lessonsSection,
                courseId,
                adminLessonViewModel,
                adminExerciseViewModel
            )

            onSuccess(courseId)

        } catch (e: Exception) {
            Log.e("AiCourseGenerate", "Error parsing course: ${e.message}")
            onError("Lỗi phân tích dữ liệu: ${e.message}")
        }
    }

    // Thêm hàm helper để lấy courseId sau khi add
    private suspend fun addCourseAndGetId(
        adminCourseViewModel: AdminCourseViewModel,
        course: Course
    ): String {
        return withContext(Dispatchers.IO) {
            val db = FirebaseFirestore.getInstance()
            val newDoc = db.collection("courses").document()
            val newCourse = course.copy(id = newDoc.id)
            newDoc.set(newCourse).await()
            newDoc.id
        }
    }

    // ---- Parse tất cả lessons
    private suspend fun parseLessons(
        lessonsSection: String,
        courseId: String,
        adminLessonViewModel: AdminLessonViewModel,
        adminExerciseViewModel: AdminExerciseViewModel
    ) {
        var currentIndex = 0
        var lessonNumber = 1

        while (true) {
            val lessonTag = "&Leason$lessonNumber&"
            val nextLessonTag = "&Leason${lessonNumber + 1}&"

            val lessonStart = lessonsSection.indexOf(lessonTag, currentIndex)
            if (lessonStart == -1) break

            val lessonEnd = lessonsSection.indexOf(nextLessonTag, lessonStart)
            val lessonContent = if (lessonEnd != -1) {
                lessonsSection.substring(lessonStart, lessonEnd)
            } else {
                lessonsSection.substring(lessonStart)
            }

            // Parse lesson
            parseLesson(
                lessonContent,
                courseId,
                lessonNumber,
                adminLessonViewModel,
                adminExerciseViewModel
            )

            lessonNumber++
            currentIndex = if (lessonEnd != -1) lessonEnd else break
        }
    }

    // ---- Parse 1 lesson
    private suspend fun parseLesson(
        lessonContent: String,
        courseId: String,
        lessonNumber: Int,
        adminLessonViewModel: AdminLessonViewModel,
        adminExerciseViewModel: AdminExerciseViewModel
    ) {
        try {
            val lessonName = extractContent(
                lessonContent,
                "&Lesson${lessonNumber}Name&",
                "&Lesson${lessonNumber}Overview&"
            )
            val lessonOverview = extractContent(
                lessonContent,
                "&Lesson${lessonNumber}Overview&",
                "&Unit1&"
            )

            val lessonId = generateId()

            // Parse units trước để tính tổng số units
            val units = parseUnits(lessonContent, lessonId, adminExerciseViewModel)

            // Tạo Lesson object
            val lesson = Lesson(
                id = lessonId,
                courseId = courseId,
                step = lessonNumber,
                stepTitle = lessonName,
                overview = lessonOverview,
                totalUnits = units.size,
                completedUnits = 0,
                units = units
            )

            // Thêm Lesson vào database
            adminLessonViewModel.createLesson(lesson.courseId, lesson)
            Log.d("AiCourseGenerate", "✅ Lesson $lessonNumber added: $lessonName")

        } catch (e: Exception) {
            Log.e("AiCourseGenerate", "Error parsing lesson $lessonNumber: ${e.message}")
        }
    }

    // ---- Parse tất cả units trong 1 lesson
    private suspend fun parseUnits(
        lessonContent: String,
        lessonId: String,
        adminExerciseViewModel: AdminExerciseViewModel
    ): List<UnitItem> {
        val units = mutableListOf<UnitItem>()
        var unitNumber = 1

        while (true) {
            val unitTag = "&Unit$unitNumber&"
            val nextUnitTag = "&Unit${unitNumber + 1}&"

            val unitStart = lessonContent.indexOf(unitTag)
            if (unitStart == -1) break

            val unitEnd = lessonContent.indexOf(nextUnitTag, unitStart)
            val unitContent = if (unitEnd != -1) {
                lessonContent.substring(unitStart, unitEnd)
            } else {
                lessonContent.substring(unitStart)
            }

            // Parse unit
            val unit = parseUnit(unitContent, unitNumber, lessonId, adminExerciseViewModel)
            units.add(unit)

            unitNumber++
            if (unitEnd == -1) break
        }

        return units
    }

    // ---- Parse 1 unit
    private suspend fun parseUnit(
        unitContent: String,
        unitNumber: Int,
        lessonId: String,
        adminExerciseViewModel: AdminExerciseViewModel
    ): UnitItem {
        val unitName = extractContent(
            unitContent,
            "&Unit${unitNumber}Name&",
            "&Unit${unitNumber}Type&"
        )

        val subLessons = mutableListOf<SubLesson>()

        // Kiểm tra có Video không
        if (unitContent.contains("&Unit${unitNumber}Video&")) {
            val subLesson = parseVideoSubLesson(unitContent, unitNumber, lessonId, adminExerciseViewModel)
            subLessons.add(subLesson)
        }

        // Kiểm tra có Practice không
        if (unitContent.contains("&Unit${unitNumber}Practice&")) {
            val practiceSubLessons = parsePracticeSubLessons(
                unitContent,
                unitNumber,
                lessonId,
                adminExerciseViewModel
            )
            subLessons.addAll(practiceSubLessons)
        }

        return UnitItem(
            unitTitle = unitName,
            progress = "",
            subLessons = subLessons
        )
    }

    // ---- Parse Video SubLesson
    private suspend fun parseVideoSubLesson(
        unitContent: String,
        unitNumber: Int,
        lessonId: String,
        adminExerciseViewModel: AdminExerciseViewModel
    ): SubLesson {
        val videoName = extractContent(
            unitContent,
            "&Unit${unitNumber}VideoName&",
            "&Unit${unitNumber}Explation&"
        )

        val subLessonId = generateId()

        // Parse explanations để tạo content cho Exercise
        val explanationContent = parseExplanations(unitContent, unitNumber)

        // Tạo Exercise cho Video
        val exercise = Exercise(
            id = null,
            subLessonId = subLessonId,
            question = null,
            answer = null,
            type = ExerciseType.VIDEO,
            options = null,
            videoUrl = null,
            romanji = null,
            kana = null,
            audioUrl = null,
            imageUrl = "https://drive.google.com/uc?id=1G2tiFAR1HRFKsNT8fLqri1Ucxim7GNo2&export=download",
            title = videoName,
            passed = false,
            explanation = explanationContent
        )

        // Thêm Exercise vào database
        adminExerciseViewModel.createExercise(
            lessonId = lessonId,
            exercise = exercise,
            onSuccess = {
                Log.d("AiCourseGenerate", "✅ Video Exercise added: $videoName")
            },
            onError = { error ->
                Log.e("AiCourseGenerate", "❌ Error adding Video Exercise: $error")
            }
        )

        return SubLesson(
            id = subLessonId,
            title = videoName,
            type = "Video",
            isCompleted = false
        )
    }

    // ---- Parse Explanations để tạo nội dung cho Video
    private fun parseExplanations(unitContent: String, unitNumber: Int): String {
        val explanationBuilder = StringBuilder()
        var explNumber = 1

        while (true) {
            val explNameTag = "&Unit${unitNumber}Explation${explNumber}Name&"
            val explContentTag = "&Unit${unitNumber}Explation${explNumber}Content&"
            val nextExplTag = "&Unit${unitNumber}Explation${explNumber + 1}&"

            val explNameStart = unitContent.indexOf(explNameTag)
            if (explNameStart == -1) break

            val explName = extractContent(
                unitContent,
                explNameTag,
                explContentTag
            )

            val explContent = if (unitContent.indexOf(nextExplTag) != -1) {
                extractContent(unitContent, explContentTag, nextExplTag)
            } else if (unitContent.indexOf("&Unit${unitNumber}Practice&") != -1) {
                extractContent(unitContent, explContentTag, "&Unit${unitNumber}Practice&")
            } else {
                extractContentToEnd(unitContent, explContentTag)
            }

            explanationBuilder.append("➤$explName➤$explContent")

            explNumber++
        }

        return explanationBuilder.toString()
    }

    // ---- Parse Practice SubLessons
    private suspend fun parsePracticeSubLessons(
        unitContent: String,
        unitNumber: Int,
        lessonId: String,
        adminExerciseViewModel: AdminExerciseViewModel
    ): List<SubLesson> {
        val subLessons = mutableListOf<SubLesson>()
        var practiceNumber = 1

        while (true) {
            val practiceTag = "&Unit${unitNumber}Practice${practiceNumber}&"
            val nextPracticeTag = "&Unit${unitNumber}Practice${practiceNumber + 1}&"

            val practiceStart = unitContent.indexOf(practiceTag)
            if (practiceStart == -1) break

            val practiceEnd = unitContent.indexOf(nextPracticeTag, practiceStart)
            val practiceContent = if (practiceEnd != -1) {
                unitContent.substring(practiceStart, practiceEnd)
            } else {
                unitContent.substring(practiceStart)
            }

            // Parse practice
            val subLesson = parsePractice(
                practiceContent,
                unitNumber,
                practiceNumber,
                lessonId,
                adminExerciseViewModel
            )
            subLessons.add(subLesson)

            practiceNumber++
            if (practiceEnd == -1) break
        }

        return subLessons
    }

    // ---- Parse 1 Practice
    private suspend fun parsePractice(
        practiceContent: String,
        unitNumber: Int,
        practiceNumber: Int,
        lessonId: String,
        adminExerciseViewModel: AdminExerciseViewModel
    ): SubLesson {
        val question = extractContent(
            practiceContent,
            "&Unit${unitNumber}Practice${practiceNumber}Question&",
            "&Unit${unitNumber}Practice${practiceNumber}Answer&"
        )

        val answer = extractContent(
            practiceContent,
            "&Unit${unitNumber}Practice${practiceNumber}Answer&",
            "&Unit${unitNumber}Practice${practiceNumber}Option&"
        )

        // Parse options
        val option1 = extractContent(
            practiceContent,
            "&Unit${unitNumber}Practice${practiceNumber}Option1&",
            "&Unit${unitNumber}Practice${practiceNumber}Option2&"
        )

        val option2 = extractContent(
            practiceContent,
            "&Unit${unitNumber}Practice${practiceNumber}Option2&",
            "&Unit${unitNumber}Practice${practiceNumber}Option3&"
        )

        val option3 = extractContent(
            practiceContent,
            "&Unit${unitNumber}Practice${practiceNumber}Option3&",
            "&Unit${unitNumber}Practice${practiceNumber}Explanation&"
        )

        val explanation = extractContentToEnd(
            practiceContent,
            "&Unit${unitNumber}Practice${practiceNumber}Explanation&"
        )

        // Trộn đáp án đúng và sai
        val allOptions = listOf(answer, option1, option2, option3).shuffled()

        val subLessonId = generateId()

        // Tạo Exercise cho Practice
        val exercise = Exercise(
            id = null,
            subLessonId = subLessonId,
            question = question,
            answer = answer,
            type = ExerciseType.PRACTICE,
            options = allOptions,
            videoUrl = null,
            romanji = null,
            kana = null,
            audioUrl = null,
            imageUrl = defaultImages.random(),
            title = "Bài tập $practiceNumber",
            passed = false,
            explanation = explanation
        )

        // Thêm Exercise vào database
        adminExerciseViewModel.createExercise(
            lessonId = lessonId,
            exercise = exercise,
            onSuccess = {
                Log.d("AiCourseGenerate", "✅ Practice Exercise added: Bài tập $practiceNumber")
            },
            onError = { error ->
                Log.e("AiCourseGenerate", "❌ Error adding Practice Exercise: $error")
            }
        )

        return SubLesson(
            id = subLessonId,
            title = "Bài tập $practiceNumber",
            type = "Practice",
            isCompleted = false
        )
    }

    // ---- Helper: Extract content giữa 2 tags
    private fun extractContent(text: String, startTag: String, endTag: String): String {
        return try {
            val start = text.indexOf(startTag)
            if (start == -1) return ""

            val contentStart = start + startTag.length
            val end = text.indexOf(endTag, contentStart)
            if (end == -1) return ""

            text.substring(contentStart, end).trim()
        } catch (e: Exception) {
            Log.e("AiCourseGenerate", "Extract error: ${e.message}")
            ""
        }
    }

    // ---- Helper: Extract content từ tag đến cuối
    private fun extractContentToEnd(text: String, startTag: String): String {
        return try {
            val start = text.indexOf(startTag)
            if (start == -1) return ""

            val contentStart = start + startTag.length

            // Tìm tag kế tiếp bất kỳ
            var minEnd = text.length
            val nextTags = listOf("&Unit", "&Leason", "&Lesson")
            for (tag in nextTags) {
                val nextTagIndex = text.indexOf(tag, contentStart)
                if (nextTagIndex != -1 && nextTagIndex < minEnd) {
                    minEnd = nextTagIndex
                }
            }

            text.substring(contentStart, minEnd).trim()
        } catch (e: Exception) {
            Log.e("AiCourseGenerate", "Extract to end error: ${e.message}")
            ""
        }
    }

    // ---- Helper: Generate ID
    private fun generateId(): String {
        return System.currentTimeMillis().toString() + Random.nextInt(1000, 9999)
    }



    // ---- Hàm generate
    suspend fun generateImageUrl(prompt: String): String? {
        return withContext(Dispatchers.IO) {
            try {
                val encodedPrompt = URLEncoder.encode(
                    "$prompt. Style Anime",
                    "UTF-8"
                )
                val imageUrl = "https://image.pollinations.ai/prompt/$encodedPrompt?width=1024&height=1024&nologo=true&model=flux"

                val request = Request.Builder()
                    .url(imageUrl)
                    .build()

                val response = client.newCall(request).execute()

                if (!response.isSuccessful) {
                    Log.e("AI_IMAGE", "Error: ${response.code}")
                    return@withContext null
                }

                val imageBytes = response.body?.bytes() ?: return@withContext null
                val tempFile = File.createTempFile("generated_image_", ".png")
                tempFile.writeBytes(imageBytes)
                val uploadedUrl = CatboxUploader.uploadVideo(tempFile)
                tempFile.delete()

                uploadedUrl
            } catch (e: Exception) {
                Log.e("AI_IMAGE", "Error: ${e.message}", e)
                null
            }
        }
    }












    suspend fun generateAIChallenge(
        content: String,
        number_of_question: Int,
        levelJLPT: String,
        mode: String
    ): String? {
        val apiKey = getRandomKey()
        val url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent?key=$apiKey"

        val prompt = """
        Bạn là một giáo viên song ngữ việt-nhật và đang dạy tiếng nhật cho người việt.
        Nội dung đang học gồm: "$content" (nếu là "" thì cho câu hỏi chủ đề ngẫu nhiên)
        Số câu hỏi trắc nghiệm: $number_of_question
        Trình độ hiện tại của người học: "$levelJLPT" (nếu là "" thì coi như là N5 hoặc thấp hơn)
        Độ khó: "$mode"
        
        QUAN TRỌNG: Bạn CHỈ được trả về theo format bên dưới, KHÔNG thêm bất kỳ text nào khác:
        
        &Question1&
        [Câu hỏi 1]
        &Answer1&
        [Đáp án đúng]
        &Option1Choice1&
        [Lựa chọn 1]
        &Option1Choice2&
        [Lựa chọn 2]
        &Option1Choice3&
        [Lựa chọn 3]
        &Option1Choice4&
        [Lựa chọn 4]
        &Explanation1&
        [Giải thích ngắn gọn bằng tiếng Việt]
        
        &Question2&
        [Câu hỏi 2]
        &Answer2&
        [Đáp án đúng]
        &Option2Choice1&
        [Lựa chọn 1]
        &Option2Choice2&
        [Lựa chọn 2]
        &Option2Choice3&
        [Lựa chọn 3]
        &Option2Choice4&
        [Lựa chọn 4]
        &Explanation2&
        [Giải thích]
        
        [Tiếp tục tương tự cho các câu còn lại...]
        
        LƯU Ý:
        - 4 lựa chọn PHẢI bao gồm cả đáp án đúng
        - Trộn vị trí đáp án đúng trong 4 lựa chọn
        - Mỗi câu hỏi phải có đủ 4 lựa chọn
        - Giải thích bằng tiếng Việt, ngắn gọn dễ hiểu
    """.trimIndent()

        val jsonBody = JSONObject().apply {
            put("contents", JSONArray().apply {
                put(JSONObject().apply {
                    put("parts", JSONArray().apply {
                        put(JSONObject().apply { put("text", prompt) })
                    })
                })
            })
        }

        val requestBody = jsonBody.toString().toRequestBody("application/json; charset=utf-8".toMediaType())
        val request = Request.Builder().url(url).post(requestBody).build()

        return withContext(Dispatchers.IO) {
            try {
                client.newCall(request).execute().use { response ->
                    response.body?.string()
                }
            } catch (e: Exception) {
                Log.e("AiCourseGenerate", "Network error: ${e.message}")
                null
            }
        }
    }












}
//val course = Course(
//    id = id.value,
//    title = title.value,
//    description = description.value,
//    rating = rating.value,
//    reviews = reviews.value,
//    likes = likes.value,
//    imageRes = finalImageUrl, mặc định https://drive.google.com/uc?export=view&id=1uyNSW54w4stVjixb9ke_rOFhiGaeekEN
//    vip = isVip.value //mặc định false
//)
//viewModel: AdminCourseViewModel = viewModel()
//viewModel.addCourse(course)
//
//data class Lesson(
//    val id: String = "",               // ID bài học
//    val courseId: String = "",         // ID khóa học
//    val step: Int = 0,                 // Ví dụ đây  là Lesson 1 thì step =1 nếu là Lesson 2 thì step =2 và tương tự
//    val stepTitle: String = "",        // Tên Lesson
//    val overview: String = "",         // Overview của Lesson
//    val totalUnits: Int = 0,           // Tổng số Unit
//    val completedUnits: Int = 0,       // mặc định = 0
//    val units: List<UnitItem> = listOf()  // Mặc định rỗng
//)
//data class UnitItem(
//    val unitTitle: String = "",          // Tên Unit
//    val progress: String = "",           // mặc định = ""
//    val subLessons: List<SubLesson> = listOf() // mặc định rỗng
//)
//
//data class SubLesson(
//    val id: String = "",
//    val title: String = "",               //tên title
//    val type: String = "",               //"Video" hoặc "Practice"
//    val isCompleted: Boolean = false     //mặc định false
//)
//
//viewModel: AdminLessonViewModel = viewModel(),
//viewModel.createLesson(courseId, updatedLesson)
//viewModel.addUnitToLesson(currentLessonId, newUnit)
//viewModel.addSubLessonToUnit(currentLessonId, currentUnitIndex, newSubLesson)
//trong sublesson sẽ add content vào
//nếu là Video type
//
//    data class Exercise(
//        var id: String? = null,              // Firestore Document ID
//        val subLessonId: String? = null,             // Firestore lessonId là String
//        val question: String? = null,             //để default
//        val answer: String? = null,               //để default
//        val type: ExerciseType? = null,           //để là "Video"
//        val options: List<String>? = null,        //để default
//        val videoUrl: String? = null,             //để default
//        val romanji: String? = null,              //để default
//        val kana: String? = null,             //để default
//        val audioUrl: String? = null,            //để default
//        val imageUrl: String? = null,         //chọn random trong val defaultImages = listOf(
//        "https://img.freepik.com/premium-vector/man-wearing-hakama-with-crest-thinking-while-scratching-his-face_180401-12331.jpg",
//        "https://thumb.ac-illust.com/95/95d92d1467ccf189f05af2b503a3e5a0_t.jpeg",
//        "https://drive.google.com/uc?export=view&id=1TWpes3nKYbwSWyUj0uG0v5p-_a_zaVVh"
//    )
//        val title: String? = null,            //để title
//        val passed: Boolean = false,          //để default
//        val explanation: String? =  "I. Giới thiệu các loại chữ trong tiếng Nhật➤Trong tiếng Nhật có 3 loại chữ:
//
//a. Kanji (chữ Hán): 日本
//- Chữ Kanji du nhập từ Trung Quốc vào Nhật Bản từ thế kỷ thứ 4.
//
//b. Hiragana (chữ mềm): にほん
//- Hiragana được tạo từ Kanji, dùng viết trợ từ, từ thuần Nhật.
//- VD: 世 ⇒ せ.
//
//c. Katakana (chữ cứng): 二ホン
//- Katakana dùng cho từ ngoại lai, tên nước, tên riêng.
//- VD: Orange ⇒ オレンジ.➤II. Giới thiệu bảng chữ cái Hiragana➤- Bảng Hiragana gồm 46 chữ cái.
//- Hàng あ: あ(a), い(i), う(u), え(e), お(o).
//- Hàng か: か(ka), き(ki), く(ku), け(ke), こ(ko).
//- Hàng さ: さ(sa), し(shi), す(su), せ(se), そ(so).
//- Hàng た: た(ta), ち(chi), つ(tsu), て(te), と(to).
//- Hàng な: な(na), に(ni), ぬ(nu), ね(ne), の(no).
//- Hàng は: は(ha), ひ(hi), ふ(fu), へ(he), ほ(ho).
//- Hàng ま: ま(ma), み(mi), む(mu), め(me), も(mo).
//- Hàng や: や(ya), ゆ(yu), よ(yo).
//- Hàng ら: ら(ra), り(ri), る(ru), れ(re), ろ(ro).
//- Hàng わ: わ(wa), を(wo), ん(n)."
//    ) //cấu trúc ➤title1➤content➤title2➤content2➤...
//
//                  viewModel.createExercise(
//                        lessonId = lessonId,
//                        exercise = updatedExercise,
//                        onSuccess = { showAddDialog = false },
//                        onError = { /* Error is handled by viewModel */ }
//                    )
//nếu là Video type
//
//    data class Exercise(
//        var id: String? = null,              // Firestore Document ID
//        val subLessonId: String? = null,             // Firestore lessonId là String
//        val question: String? = null,             //để title câu hỏi
//        val answer: String? = null,               //để đáp án đúng
//        val type: ExerciseType? = null,           //để là "Practice"
//        val options: List<String>? = null,        //để bảo gồm đáp án đúng và sai
//        val videoUrl: String? = null,             //để default
//        val romanji: String? = null,              //để romanji, nếu có hoặc cần
//        val kana: String? = null,             //để katakana, nếu có hoặc cần
//        val audioUrl: String? = null,            //để default
//        val imageUrl: String? = null,         //để default
//        val title: String? = null,            // để title
//        val passed: Boolean = false,          //để default
//        val explanation: String? =  ""
//                  ViewModel: AdminExerciseViewModel = viewModel()
//                  viewModel.createExercise(
//                        lessonId = lessonId,
//                        exercise = updatedExercise,
//                        onSuccess = { showAddDialog = false },
//                        onError = { /* Error is handled by viewModel */ }
//                    )
//mẫu trả về:
//"text": "Dưới đây là cấu trúc bài học theo yêu cầu, tập trung vào chủ đề gia đình trình độ N5, có lồng ghép tiếng Việt và tiếng Nhật:\n\n&Title&\nGia Đình Thân Yêu - 家族 (Kazoku) - N5\n\n&Summary&\nKhóa học \"Gia Đình Thân Yêu\" được thiết kế dành riêng cho người Việt mới bắt đầu học tiếng Nhật (trình độ N5). Khóa học sẽ giúp bạn làm quen với các từ vựng cơ bản về gia đình, cách xưng hô, và các mẫu câu đơn giản để giới thiệu về gia đình mình bằng tiếng Nhật. Với 3 bài học và nhiều bài tập thực hành thú vị, bạn sẽ tự tin hơn khi nói về gia đình mình trong tiếng Nhật.\n\n&Lesson&\n    &Leason1&\n         &Lesson1Name&\n        Chào Hỏi và Giới Thiệu Gia Đình (家族の挨拶と紹介 - Kazoku no aisatsu to shoukai)\n        &Lesson1Overview&\n        Trong bài học này, chúng ta sẽ học cách chào hỏi bằng tiếng Nhật và giới thiệu các thành viên trong gia đình mình. Chúng ta sẽ tập trung vào các từ vựng cơ bản và các mẫu câu đơn giản để giúp bạn tự tin hơn khi giao tiếp.\n            &Unit1&\n                &Unit1Name&\n                Chào Hỏi Cơ Bản (基本的な挨拶 - Kihonteki na aisatsu)\n                &Unit1Type&\n                    &Unit1Video&\n                        &Unit1VideoName&\n                        Video: Chào Hỏi Hàng Ngày (毎日の挨拶 - Mainichi no aisatsu)\n                        &Unit1Explation&\n                            &Unit1Explation1&\n                                &Unit1Explation1Name&\n                                Chào buổi sáng (おはようございます - Ohayou gozaimasu)\n                                &Unit1Explation1Content&\n                                Cách chào buổi sáng lịch sự. Sử dụng từ sáng đến khoảng 10 giờ sáng.\n                            &Unit1Explation2&\n                                &Unit1Explation2Name&\n                                Chào buổi trưa (こんにちは - Konnichiwa)\n                                &Unit1Explation2Content&\n                                Cách chào vào buổi trưa và chiều.\n                            &Unit1Explation3&\n                                &Unit1Explation3Name&\n                                Chào buổi tối (こんばんは - Konbanwa)\n                                &Unit1Explation3Content&\n                                Cách chào vào buổi tối.\n                            &Unit1Explation4&\n                                &Unit1Explation4Name&\n                                Tạm biệt (さようなら - Sayounara)\n                                &Unit1Explation4Content&\n                                Cách chào tạm biệt (ít dùng trong giao tiếp hàng ngày).\n                                \n                    &Unit1Practice&\n                        &Unit1Practice1&\n                            &Unit1Practice1Question&\n                            Chọn cách chào \"buổi sáng\" đúng trong tiếng Nhật:\n                            &Unit1Practice1Answer&\n                            おはようございます (Ohayou gozaimasu)\n                            &Unit1Practice1Option&\n                                &Unit1Practice1Option1&\n                                こんにちは (Konnichiwa)\n                                &Unit1Practice1Option2&\n                                こんばんは (Konbanwa)\n                                &Unit1Practice1Option3&\n                                ありがとうございます (Arigatou gozaimasu)\n                            &Unit1Practice1Explanation&\n                            \"おはようございます\" (Ohayou gozaimasu) là cách chào buổi sáng lịch sự. \"こんにちは\" (Konnichiwa) là chào buổi tr?
