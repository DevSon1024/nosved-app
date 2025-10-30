package com.devson.nosved.ui.model

enum class DownloadTabType {
    ALL, ACTIVE, COMPLETED, FAILED
}

data class DownloadCounts(
    val all: Int = 0,
    val active: Int = 0,
    val completed: Int = 0,
    val failed: Int = 0
)

sealed class DownloadAction {
    data class Play(val filePath: String) : DownloadAction()
    data class Share(val filePath: String) : DownloadAction()
    data class Retry(val downloadId: String) : DownloadAction()
    data class Cancel(val downloadId: String) : DownloadAction()
    data class Delete(val downloadId: String) : DownloadAction()
}
