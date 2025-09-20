package com.ramitsuri.notificationjournal.core.model

sealed interface ForceUploadAllStatus {
    data object Initial : ForceUploadAllStatus

    data object NothingToUpload : ForceUploadAllStatus

    data class Uploading(
        val uploadedDaysCount: Int,
        val failedDaysCount: Int,
        val totalToUploadDaysCount: Int,
    ) : ForceUploadAllStatus

    data class Done(
        val uploadedDaysCount: Int,
        val failedDaysCount: Int,
        val totalToUploadDaysCount: Int,
    ) : ForceUploadAllStatus
}
