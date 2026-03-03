package com.example.nihongo.utils

import java.util.Properties
import javax.mail.Authenticator
import javax.mail.Message
import javax.mail.PasswordAuthentication
import javax.mail.Session
import javax.mail.Transport
import javax.mail.internet.InternetAddress
import javax.mail.internet.MimeMessage

object EmailSender {
    private const val senderEmail = "phongtt.23it@vku.udn.vn"
    private const val appPassword = "olrq gqil nyxe mbci"

    fun generateOTP(): String {
        return (100000..999999).random().toString()
    }

    fun sendOTP(
        recipientEmail: String,
        otp: String,
        onSuccess: () -> Unit,
        onFailure: (Exception) -> Unit
    ) {
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

        try {
            val message = MimeMessage(session).apply {
                setFrom(InternetAddress(senderEmail, "Nihongo App"))
                setRecipients(Message.RecipientType.TO, InternetAddress.parse(recipientEmail))
                subject = "🔒 Mã OTP đăng nhập - Nihongo App"

                val htmlContent = """
                    <html>
                        <body style="font-family: Arial, sans-serif; background-color: #f9f9f9; padding: 20px;">
                            <div style="max-width: 600px; margin: auto; background-color: #ffffff; padding: 24px; border-radius: 10px; box-shadow: 0 4px 8px rgba(0,0,0,0.05);">
                                <h2 style="color: #4CAF50;">🎉 Chào mừng bạn đến với Nihongo App!</h2>
                                <p style="color: #333;">Bạn vừa yêu cầu đăng nhập với email <strong>$recipientEmail</strong>.</p>
                                <p style="color: #333;">Dưới đây là mã OTP của bạn:</p>

                                <div style="margin: 20px 0; text-align: center;">
                                    <span style="font-size: 28px; font-weight: bold; color: #ffffff; background-color: #4CAF50; padding: 12px 24px; border-radius: 8px; display: inline-block; letter-spacing: 2px;">$otp</span>
                                </div>

                                <p style="color: #333;">Mã này sẽ hết hạn sau <strong>5 phút</strong>. Vui lòng không chia sẻ mã này với bất kỳ ai.</p>
                                <p style="color: #666; font-size: 14px;">Nếu bạn không thực hiện yêu cầu này, hãy bỏ qua email này hoặc đổi mật khẩu nếu nghi ngờ có truy cập trái phép.</p>

                                <hr style="margin: 32px 0; border: none; border-top: 1px solid #eee;">

                                <p style="color: #888; font-size: 12px; text-align: center;">
                                    © ${java.time.Year.now()} Nihongo App. All rights reserved.
                                </p>
                            </div>
                        </body>
                    </html>
                """.trimIndent()

                setContent(htmlContent, "text/html; charset=utf-8")
            }

            Transport.send(message)
            onSuccess()
        } catch (e: Exception) {
            onFailure(e)
        }
    }
}
