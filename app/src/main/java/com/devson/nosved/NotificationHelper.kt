package com.devson.nosved

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.content.FileProvider
import com.devson.nosved.download.MimeTypeUtil
import java.io.File

class NotificationHelper(private val context: Context) {

    private val notificationManager =
        context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Download Notifications",
                NotificationManager.IMPORTANCE_LOW
            )
            notificationManager.createNotificationChannel(channel)
        }
    }

    /**
     * Shows a progress notification for a specific download.
     *
     * @param notificationId A unique ID for this download's notification.
     * @param title The title of the video being downloaded.
     * @param line The progress line from yt-dlp (e.g., percentage, speed).
     */
    fun showDownloadProgressNotification(notificationId: Int, title: String, line: String) {
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setContentTitle(title) // Use the video title
            .setContentText(line)
            .setSmallIcon(R.drawable.ic_launcher_foreground) // TODO: Use a proper download icon
            .setOngoing(true)
            .setOnlyAlertOnce(true) // Don't make sound for every progress update
            .build()
        notificationManager.notify(notificationId, notification)
    }

    /**
     * Shows a completion notification for a specific download.
     * Uses the same notificationId to replace the progress one.
     *
     * @param notificationId A unique ID for this download's notification.
     * @param videoTitle The title of the completed download.
     * @param downloadPath The file path to the completed download.
     * @param isAudioOnly Hint to determine the correct MIME type.
     */
    fun showDownloadCompleteNotification(
        notificationId: Int,
        videoTitle: String,
        downloadPath: String,
        isAudioOnly: Boolean
    ) {
        val intent = Intent(Intent.ACTION_VIEW)
        val file = File(downloadPath)

        // Use FileProvider for secure and correct file access
        val fileUri: Uri = try {
            FileProvider.getUriForFile(
                context,
                "${context.packageName}.provider", // Make sure this matches AndroidManifest.xml
                file
            )
        } catch (e: Exception) {
            // Fallback for older/debug builds, not recommended for production
            Uri.parse(downloadPath)
        }

        // Get the specific MIME type
        val mimeType = MimeTypeUtil.getMimeType(downloadPath, isAudioOnly)

        intent.setDataAndType(fileUri, mimeType)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION

        val pendingIntent = PendingIntent.getActivity(
            context,
            notificationId, // Use the notificationId as a unique request code
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setContentTitle("Download Complete")
            .setContentText(videoTitle)
            .setSmallIcon(R.drawable.ic_launcher_foreground) // TODO: Use a proper complete icon
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()
        notificationManager.notify(notificationId, notification)
    }

    /**
     * Cancels a specific notification.
     *
     * @param notificationId The ID of the notification to cancel.
     */
    fun cancelNotification(notificationId: Int) {
        notificationManager.cancel(notificationId)
    }

    companion object {
        private const val CHANNEL_ID = "download_channel"
    }
}