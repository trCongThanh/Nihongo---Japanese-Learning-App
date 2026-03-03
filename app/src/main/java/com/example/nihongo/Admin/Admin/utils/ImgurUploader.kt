package com.example.nihongo.Admin.utils

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File

class ImgurUploader() {
    //23956d3e1f3ead8

    private val clientId="23956d3e1f3ead8"

    private val client = OkHttpClient()

    suspend fun uploadImage(file: File): String? = withContext(Dispatchers.IO) {
        val requestBody = MultipartBody.Builder()
            .setType(MultipartBody.FORM)
            .addFormDataPart("image", file.name,
                file.asRequestBody("image/*".toMediaTypeOrNull()))
            .build()

        val request = Request.Builder()
            .url("https://api.imgur.com/3/image")
            .addHeader("Authorization", "Client-ID $clientId")
            .post(requestBody)
            .build()

        try {
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@withContext null

                val json = response.body?.string()
                val imageUrl = Regex("\"link\":\"(.*?)\"")
                    .find(json ?: "")?.groupValues?.get(1)
                    ?.replace("\\/", "/") // Fix escaped slashes

                return@withContext imageUrl
            }
        } catch (e: Exception) {
            e.printStackTrace()
            return@withContext null
        }
    }
}
