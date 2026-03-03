package com.example.nihongo.Admin.utils

import Campaign
import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Context.ALARM_SERVICE
import android.content.Intent
import android.os.Build
import android.provider.Settings
import androidx.core.app.NotificationCompat
import com.example.nihongo.Admin.viewmodel.AdminNotifyPageViewModel
import com.example.nihongo.R
import com.google.firebase.firestore.FirebaseFirestore
import java.util.Calendar

class AlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        val campaignId = intent?.getStringExtra("campaignId") ?: return
        val isDaily = intent.getBooleanExtra("isDaily", false)
        
        // Get campaign from Firestore
        FirebaseFirestore.getInstance()
            .collection("campaigns")
            .document(campaignId)
            .get()
            .addOnSuccessListener { document ->
                document.toObject(Campaign::class.java)?.let { campaign ->
                    // Use AdminNotifyPageViewModel to send notification
                    val viewModel = AdminNotifyPageViewModel()
                    viewModel.sendOneSignalPushNotification(campaign)
                    
                    // If it's a daily notification, schedule the next one
                    if (isDaily) {
                        scheduleDailyNotification(context, campaign)
                    }
                }
            }
    }

    private fun showNotification(context: Context, campaign: Campaign) {
        val channelId = "campaign_channel_id"
        val channelName = "Campaign Notifications"

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                channelName,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Channel for campaign notifications"
            }
            val notificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }

        val builder = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(campaign.title)
            .setContentText(campaign.message)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)

        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(campaign.id.hashCode(), builder.build())
    }

    companion object {
        fun scheduleOneTimeNotification(context: Context, campaign: Campaign) {
            val alarmManager = context.getSystemService(ALARM_SERVICE) as AlarmManager
            val intent = Intent(context, AlarmReceiver::class.java).apply {
                putExtra("campaignId", campaign.id)
                putExtra("isDaily", false)
            }
            
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                campaign.id.hashCode(),
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val scheduledTime = campaign.scheduledFor?.toDate()?.time ?: return
            
            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                scheduledTime,
                pendingIntent
            )
        }

        fun scheduleDailyNotification(context: Context, campaign: Campaign) {
            val alarmManager = context.getSystemService(ALARM_SERVICE) as AlarmManager
            val intent = Intent(context, AlarmReceiver::class.java).apply {
                putExtra("campaignId", campaign.id)
                putExtra("isDaily", true)
            }
            
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                campaign.id.hashCode(),
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            // Calculate initial delay to next occurrence
            val calendar = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, campaign.dailyHour)
                set(Calendar.MINUTE, campaign.dailyMinute)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
                
                // If the time has already passed today, schedule for tomorrow
                if (before(Calendar.getInstance())) {
                    add(Calendar.DAY_OF_MONTH, 1)
                }
            }

            // Use setAlarmClock instead of setRepeating for more reliable delivery
            val alarmInfo = AlarmManager.AlarmClockInfo(
                calendar.timeInMillis,
                pendingIntent
            )
            
            alarmManager.setAlarmClock(
                alarmInfo,
                pendingIntent
            )
        }

        fun cancelNotification(context: Context, campaignId: String) {
            val alarmManager = context.getSystemService(ALARM_SERVICE) as AlarmManager
            val intent = Intent(context, AlarmReceiver::class.java)
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                campaignId.hashCode(),
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            alarmManager.cancel(pendingIntent)
        }
    }
}

