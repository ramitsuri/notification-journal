package com.ramitsuri.notificationjournal.tiles

import android.app.PendingIntent
import android.content.Intent
import android.graphics.drawable.Icon // Required for Icon.createWithResource
import android.os.Build
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import androidx.core.app.TaskStackBuilder
import androidx.core.net.toUri
import com.ramitsuri.notificationjournal.R // Required for R.drawable.ic_add_entry_tile
import com.ramitsuri.notificationjournal.core.ui.nav.DeepLink

class AddEntryTileService : TileService() {
    override fun onClick() {
        super.onClick()
        val mainIntent = Intent(Intent.ACTION_VIEW, DeepLink.ADD_ENTRY.uri.toUri())
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            val contentIntent =
                TaskStackBuilder.create(this).run {
                    addNextIntentWithParentStack(mainIntent)
                    val flags = PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
                    getPendingIntent(1001, flags)
                }
            startActivityAndCollapse(contentIntent!!)
        } else {
            startActivityAndCollapse(mainIntent)
        }
    }

    override fun onStartListening() {
        super.onStartListening()
        val tile = qsTile ?: return

        tile.label = getString(R.string.add_entry_qs_tile_label)
        tile.icon = Icon.createWithResource(this, R.drawable.ic_add)
        tile.state = Tile.STATE_ACTIVE
        tile.updateTile()
    }
}
