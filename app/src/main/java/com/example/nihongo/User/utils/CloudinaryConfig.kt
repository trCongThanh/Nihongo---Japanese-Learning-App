package com.example.nihongo.User.utils

import android.content.Context
import android.net.Uri
import android.util.Log
import com.cloudinary.Cloudinary
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

object CloudinaryConfig {
    // Thông tin Cloudinary - Đã sửa cloud_name
    private const val CLOUD_NAME = "ddjrbkhpx" // Cloud name đúng
    private const val API_KEY = "534297453884984"
    private const val API_SECRET = "23OLY_AqI11rISnQ5EHl66OHahU"
    
    // Khởi tạo Cloudinary
    private val cloudinary by lazy {
        val config = HashMap<String, String>()
        config["cloud_name"] = CLOUD_NAME
        config["api_key"] = API_KEY
        config["api_secret"] = API_SECRET
        Cloudinary(config)
    }
    
    /**
     * Tải ảnh lên Cloudinary
     * @param uri Uri của ảnh cần tải lên
     * @param context Context để truy cập ContentResolver
     * @param folder Thư mục trên Cloudinary (mặc định là "nihongo_app")
     * @return URL của ảnh đã tải lên
     */
    suspend fun uploadImage(uri: Uri, context: Context, folder: String = "nihongo_app"): String {
        return withContext(Dispatchers.IO) {
            try {
                Log.d("CloudinaryConfig", "Starting image upload to Cloudinary")
                
                // Chuyển đổi Uri thành File
                val inputStream = context.contentResolver.openInputStream(uri)
                val file = File.createTempFile("image", ".jpg", context.cacheDir)
                inputStream?.use { input ->
                    file.outputStream().use { output ->
                        input.copyTo(output)
                    }
                }
                
                // Tạo các tùy chọn tải lên
                val options = HashMap<String, Any>()
                options["folder"] = "$folder/profile_images"
                options["resource_type"] = "image"
                options["unique_filename"] = true
                
                Log.d("CloudinaryConfig", "Uploading to cloud: $CLOUD_NAME, folder: $folder/profile_images")
                
                // Tải lên Cloudinary
                val uploadResult = cloudinary.uploader().upload(file, options)
                
                // Xóa file tạm sau khi tải lên
                file.delete()
                
                // Trả về URL của ảnh đã tải lên
                val resultUrl = uploadResult["secure_url"] as String
                Log.d("CloudinaryConfig", "Upload successful, URL: $resultUrl")
                
                resultUrl
            } catch (e: Exception) {
                Log.e("CloudinaryConfig", "Error uploading image", e)
                throw e
            }
        }
    }
}

