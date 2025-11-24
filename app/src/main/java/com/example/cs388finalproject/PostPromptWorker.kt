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
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.util.concurrent.TimeUnit
import kotlin.random.Random
import com.example.cs388finalproject.R

class PostPromptWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : Worker(appContext, workerParams) {

    override fun doWork(): Result {
        val user = FirebaseAuth.getInstance().currentUser ?: return Result.success()

        val now = System.currentTimeMillis()

        // 1) Save the start of this "posting window" for this user
        FirebaseFirestore.getInstance()
            .collection("users")
            .document(user.uid)
            .update("lastPromptAt", now)

        // 2) Show the notification
        showNotification()

        // 3) Schedule the next random prompt in ~24 hours
        scheduleNextPrompt(applicationContext)

        return Result.success()
    }

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
        fun scheduleNextPrompt(context: Context) {
            val minutesInDay = 24 * 60
            val randomMinutes = Random.nextInt(minutesInDay)   // 0 .. 1439
            val delayMinutes = randomMinutes.toLong()

            val request = OneTimeWorkRequestBuilder<PostPromptWorker>()
                .setInitialDelay(delayMinutes, TimeUnit.MINUTES)
                .addTag("post_prompt")
                .build()

            WorkManager.getInstance(context).enqueueUniqueWork(
                "post_prompt",
                ExistingWorkPolicy.REPLACE,
                request
            )
        }
    }

    //Test Notifications
    /*
    companion object {
        fun scheduleNextPrompt(context: Context) {
            // ⚠️ TEST MODE: trigger quickly (10 seconds)
            val delaySeconds = 10L

            val request = OneTimeWorkRequestBuilder<PostPromptWorker>()
                .setInitialDelay(delaySeconds, TimeUnit.SECONDS)
                .addTag("post_prompt")
                .build()

            WorkManager.getInstance(context).enqueueUniqueWork(
                "post_prompt",
                ExistingWorkPolicy.REPLACE,
                request
            )
        }
    }*/
}
