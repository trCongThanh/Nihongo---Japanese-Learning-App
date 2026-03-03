package com.example.nihongo.Admin.viewmodel

import Campaign
import android.content.Context
import android.util.Log
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.tasks.await

class CampaignWorker(context: Context, params: WorkerParameters) : Worker(context, params) {
    private val viewModel = AdminNotifyPageViewModel()

    override fun doWork(): Result {
        try {
            val campaignId = inputData.getString("campaignId") ?: return Result.failure()

            // Get campaign from Firestore
            val db = FirebaseFirestore.getInstance()
            val campaignDocument = db.collection("campaigns").document(campaignId)

            // Use runBlocking to handle the async Firestore call in a synchronous Worker
            runBlocking {
                try {
                    val documentSnapshot = campaignDocument.get().await()
                    val campaign = documentSnapshot.toObject(Campaign::class.java)

                    if (campaign != null) {
                        // Send the notification
                        viewModel.sendCampaign(campaign, applicationContext)

                        // Update the campaign's lastSent field
                        campaignDocument.update("lastSent", Timestamp.now())

                        Log.d("CampaignWorker", "Successfully sent scheduled campaign: ${campaign.title}")
                    } else {
                        Log.e("CampaignWorker", "Campaign not found with ID: $campaignId")
                    }
                } catch (e: Exception) {
                    Log.e("CampaignWorker", "Error processing campaign", e)
                    return@runBlocking
                }
            }

            return Result.success()
        } catch (e: Exception) {
            Log.e("CampaignWorker", "Worker failed", e)
            return Result.failure()
        }
    }
}