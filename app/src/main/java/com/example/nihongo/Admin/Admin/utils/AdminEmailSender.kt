package com.example.nihongo.Admin.utils

import android.util.Log
import java.util.Properties
import javax.mail.*
import javax.mail.internet.InternetAddress
import javax.mail.internet.MimeMessage

object AdminEmailSender {
    private const val senderEmail = "phongtt.23it@vku.udn.vn"
    private const val appPassword = "olrq gqil nyxe mbci"

    fun sendEmail(toEmail: String, subject: String, body: String): Boolean {
        val backgroundImage = when (body.trim()) {
            "Bạn chưa hoàn thành bài học hôm nay, hãy quay lại học nhé!" ->
                "https://img.freepik.com/free-vector/pink-trees-sky-banner-vector_53876-127811.jpg"
            "Lớp học online đang bắt đầu, mời bạn tham gia." ->
                "https://encrypted-tbn0.gstatic.com/images?q=tbn:ANd9GcTKI8Y45JaVyO1yhqU-6zAABP6DJ6S3Dbp_Ug&s"
            "Bạn còn bài kiểm tra chưa làm, hãy hoàn thành sớm nhất." ->
                "https://img.freepik.com/free-vector/watercolor-chinese-style-illustration_23-2149751205.jpg?semt=ais_hybrid&w=740"
            else ->
                "https://www.freepptbackgrounds.net/wp-content/uploads/2019/04/Japanese-Presentation-Template.jpg"
        }

        val htmlContent = """
            <div style="margin:0; padding:0; background-image: url('$backgroundImage'); background-size: cover; background-position: center; width: 100%; min-height: 100vh; font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif;">
                <div style="background-color: rgba(255, 255, 255, 0.9); max-width: 600px; margin: auto; padding: 40px 30px; border-radius: 16px; box-shadow: 0 8px 16px rgba(0,0,0,0.3); text-align: center;">
                    <h1 style="font-size: 26px; color: #2b2b2b; margin-bottom: 20px;">📚 Thông báo từ App Ninhongo</h1>
                    <p style="font-size: 18px; color: #444; line-height: 1.6; margin-bottom: 30px;">$body</p>
                    <a href="https://yourapp.com" style="padding: 14px 28px; background-color: #22c55e; color: white; font-weight: bold; text-decoration: none; border-radius: 8px; font-size: 16px; box-shadow: 0 4px 8px rgba(0,0,0,0.2); transition: background-color 0.3s;">
                        👉 Truy cập ngay
                    </a>
                    <p style="margin-top: 40px; font-size: 14px; color: #888;">Bạn nhận được email này vì đã đăng ký sử dụng hệ thống của chúng tôi.</p>
                </div>
            </div>
        """.trimIndent()


        return try {
            val props = Properties().apply {
                put("mail.smtp.auth", "true")
                put("mail.smtp.starttls.enable", "true")
                put("mail.smtp.host", "smtp.gmail.com")
                put("mail.smtp.port", "587")
            }

            val session = Session.getInstance(props, object : Authenticator() {
                override fun getPasswordAuthentication(): PasswordAuthentication {
                    return PasswordAuthentication(senderEmail, appPassword)
                }
            })

            val message = MimeMessage(session).apply {
                setFrom(InternetAddress(senderEmail))
                setRecipients(Message.RecipientType.TO, InternetAddress.parse(toEmail))
                setSubject(subject)
                setContent(htmlContent, "text/html; charset=utf-8")
            }

            Transport.send(message)
            true
        } catch (e: Exception) {
            Log.d("SendButton", "Error sending email: ${e}")
            false
        }
    }
}

