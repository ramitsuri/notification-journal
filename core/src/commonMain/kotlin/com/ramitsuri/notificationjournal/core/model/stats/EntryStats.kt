package com.ramitsuri.notificationjournal.core.model.stats

data class EntryStats(
    val uploadedAndReconciled: Count,
    val uploadedAndNotReconciled: Count,
    val notUploadedAndReconciled: Count,
    val notUploadedAndNotReconciled: Count,
    val all: Count,
) {
    data class Count(val days: String, val entries: String)
}
