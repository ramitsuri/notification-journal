package com.ramitsuri.notificationjournal.broadcast

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.ramitsuri.notificationjournal.MainApplication

class RestartBroadcastReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (!(intent.action == Intent.ACTION_MY_PACKAGE_REPLACED ||
                    intent.action == Intent.ACTION_BOOT_COMPLETED ||
                    intent.action == Intent.ACTION_REBOOT ||
                    intent.action == Intent.ACTION_LOCKED_BOOT_COMPLETED ||
                    intent.action == "android.intent.action.QUICKBOOT_POWERON" ||
                    intent.action == "com.htc.intent.action.QUICKBOOT_POWERON")
        ) {
            return
        }

        (context.applicationContext as MainApplication).start()
    }
}