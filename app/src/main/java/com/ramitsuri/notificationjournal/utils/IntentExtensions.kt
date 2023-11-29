package com.ramitsuri.notificationjournal.utils

import android.content.Intent

fun Intent?.receivedText(): String? {
    if (this == null) {
        return null
    }
    return if (action == Intent.ACTION_SEND && type == "text/plain") {
        getStringExtra(Intent.EXTRA_TEXT)
    } else {
        null
    }
}