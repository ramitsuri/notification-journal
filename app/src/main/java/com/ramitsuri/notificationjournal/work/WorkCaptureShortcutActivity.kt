package com.ramitsuri.notificationjournal.work

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.lifecycle.lifecycleScope
import com.ramitsuri.notificationjournal.MainApplication
import com.ramitsuri.notificationjournal.service.WorkBridgeService
import kotlinx.coroutines.launch

class WorkCaptureShortcutActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val appGraph = (application as MainApplication).appGraph
        lifecycleScope.launch {
            if (appGraph.workProfileManager.hasProfile()) {
                WorkBridgeService.start(this@WorkCaptureShortcutActivity)
            } else {
                val intent = Intent(this@WorkCaptureShortcutActivity, WorkPairingActivity::class.java)
                startActivity(intent)
            }
            finish()
        }
    }
}
