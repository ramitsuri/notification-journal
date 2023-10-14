package com.ramitsuri.notificationjournal.core.utils

import android.content.Context
import android.content.Intent

fun Context.shutdown() {
    val intent = packageManager.getLaunchIntentForPackage(packageName)
    val componentName = intent?.component
    val mainIntent = Intent.makeRestartActivityTask(componentName)
    startActivity(mainIntent)
    Runtime.getRuntime().exit(0)
}