package com.ramitsuri.notificationjournal.tiles

import android.content.Intent
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import com.ramitsuri.notificationjournal.MainActivity
import com.ramitsuri.notificationjournal.R // Required for R.drawable.ic_add_entry_tile
import android.graphics.drawable.Icon // Required for Icon.createWithResource

class AddEntryTileService : TileService() {

    override fun onClick() {
        super.onClick()
        val intent = Intent(this, MainActivity::class.java).apply {
            action = "com.ramitsuri.notificationjournal.ADD_ENTRY"
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        startActivityAndCollapse(intent)
    }

    override fun onStartListening() {
        super.onStartListening()
        val tile = qsTile ?: return

        tile.label = "Add Entry"
        // Use the app-specific icon resource
        tile.icon = Icon.createWithResource(this, R.drawable.ic_add_entry_tile)
        tile.state = Tile.STATE_ACTIVE
        tile.updateTile()
    }

    override fun onStopListening() {
        super.onStopListening()
        // Optional: Update tile state if needed when not visible
    }
}
