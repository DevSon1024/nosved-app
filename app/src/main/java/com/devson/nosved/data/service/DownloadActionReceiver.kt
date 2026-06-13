package com.devson.nosved.data.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class DownloadActionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return
        val downloadId = intent.getStringExtra("download_id") ?: return
        val service = DownloadService.instance ?: return

        service.coroutineScope.launch(Dispatchers.IO) {
            when (action) {
                "com.devson.nosved.ACTION_PAUSE" -> {
                    service.pauseDownload(downloadId)
                }
                "com.devson.nosved.ACTION_RESUME" -> {
                    service.resumeDownload(downloadId)
                }
                "com.devson.nosved.ACTION_CANCEL" -> {
                    service.cancelDownload(downloadId)
                }
            }
        }
    }
}
