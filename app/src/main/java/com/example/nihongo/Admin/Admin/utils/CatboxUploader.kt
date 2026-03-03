package com.example.nihongo.Admin.utils

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.*
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File

object CatboxUploader {

    private const val TAG = "CatboxUploader"
    private const val UPLOAD_URL = "https://catbox.moe/user/api.php"
    private const val USERHASH = "" // Tùy chọn: Nếu bạn có tài khoản Catbox và lấy hash tại https://catbox.moe/user/manage.php

    suspend fun uploadVideo(file: File): String? = withContext(Dispatchers.IO) {
        try {
            val client = OkHttpClient()

            val builder = MultipartBody.Builder().setType(MultipartBody.FORM)
                .addFormDataPart("reqtype", "fileupload")
                .addFormDataPart("fileToUpload", file.name, file.asRequestBody())

            // Optional userhash
            if (USERHASH.isNotEmpty()) {
                builder.addFormDataPart("userhash", USERHASH)
            }

            val requestBody = builder.build()

            val request = Request.Builder()
                .url(UPLOAD_URL)
                .post(requestBody)
                .build()

            val response = client.newCall(request).execute()
            val responseBody = response.body?.string()

            if (response.isSuccessful && responseBody != null && responseBody.startsWith("https://")) {
                Log.d(TAG, "Upload success: $responseBody")
                return@withContext responseBody
            } else {
                Log.e(TAG, "Upload failed: $responseBody")
                return@withContext null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Exception during upload: ${e.message}")
            return@withContext null
        }
    }
}
