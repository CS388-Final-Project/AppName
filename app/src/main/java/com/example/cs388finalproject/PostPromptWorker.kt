package com.example.cs388finalproject

import android.Manifest
import android.content.pm.PackageManager
import androidx.core.app.ActivityCompat
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.Worker
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.google.firebase.firestore.FirebaseFirestore
import java.util.concurrent.TimeUnit
import kotlin.random.Random
import com.example.cs388finalproject.R

// Runs worker to update prompt timestamp
class PostPromptWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : Worker(appContext, workerParams) {

    // Executes background work and reschedules prompt
    override fun doWork(): Result {
        val uid = inputData.getString("uid") ?: return Result.success()

        val now = System.currentTimeMillis()

        FirebaseFirestore.getInstance()
            .collection("users")
            .document(uid)
            .update("lastPromptAt", now)

        //  Show the notification
        showNotification()

        // Schedule the next random notification for the next day
        scheduleNextPrompt(applicationContext, uid)

        return Result.success()
    }

    // Builds and displays the notification UI
    private fun showNotification() {
        val channelId = "post_prompt_channel"

        val intent = Intent(applicationContext, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            applicationContext,
            0,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val builder = NotificationCompat.Builder(applicationContext, channelId)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("Time to share a song 🎧")
            .setContentText("Post what you're listening to for today's feed window.")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)

        val manager = NotificationManagerCompat.from(applicationContext)

        if (ActivityCompat.checkSelfPermission(
                applicationContext,
                Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }

        manager.notify(1001, builder.build())
    }

    //Real Notificaitons

    companion object {
        /**
         * Schedule the next prompt at a random time within the next 24 hours.
         */
        fun scheduleNextPrompt(context: Context, uid: String) {
            val minutesInDay = 24 * 60
            val randomMinutes = Random.nextInt(minutesInDay)   // 0 .. 1439
            val delayMinutes = randomMinutes.toLong()

            val request = OneTimeWorkRequestBuilder<PostPromptWorker>()
                .setInitialDelay(delayMinutes, TimeUnit.MINUTES)
                .addTag("post_prompt")
                .setInputData(workDataOf("uid" to uid))
                .build()

            WorkManager.getInstance(context).enqueueUniqueWork(
                "post_prompt_$uid",
                ExistingWorkPolicy.REPLACE,
                request
            )
        }
    }
    /*
    //Test Notifications
    companion object {
        // Schedules test prompt run for user
        fun scheduleNextPrompt(context: Context, uid: String) {
            // ⚠️ TEST MODE: trigger quickly (10 seconds)
            val delaySeconds = 100L

            val request = OneTimeWorkRequestBuilder<PostPromptWorker>()
                .setInitialDelay(delaySeconds, TimeUnit.SECONDS)
                .addTag("post_prompt")
                .setInputData(workDataOf("uid" to uid))
                .build()

            WorkManager.getInstance(context).enqueueUniqueWork(
                "post_prompt_$uid",
                ExistingWorkPolicy.REPLACE,
                request
            )
        }
    }*/
}
