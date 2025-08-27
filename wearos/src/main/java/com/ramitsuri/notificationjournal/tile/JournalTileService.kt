package com.ramitsuri.notificationjournal.tile

import android.content.Context
import androidx.wear.protolayout.ResourceBuilders.Resources
import androidx.wear.protolayout.TimelineBuilders
import androidx.wear.tiles.RequestBuilders
import androidx.wear.tiles.RequestBuilders.ResourcesRequest
import androidx.wear.tiles.TileBuilders.Tile
import com.google.android.horologist.annotations.ExperimentalHorologistApi
import com.google.android.horologist.tiles.SuspendingTileService
import com.ramitsuri.notificationjournal.core.data.JournalEntryTemplateDao
import com.ramitsuri.notificationjournal.core.di.ServiceLocator

@OptIn(ExperimentalHorologistApi::class)
class JournalTileService : SuspendingTileService() {
    private val templateDao: JournalEntryTemplateDao by lazy { ServiceLocator.templatesDao }

    override suspend fun tileRequest(requestParams: RequestBuilders.TileRequest): Tile {
        val tileState = TileState(templateDao.getAll())
        val layoutElement = tileLayout(this, requestParams.deviceConfiguration, tileState)
        return Tile.Builder()
            .setResourcesVersion("")
            .setTileTimeline(TimelineBuilders.Timeline.fromLayoutElement(layoutElement))
            .build()
    }

    override suspend fun resourcesRequest(requestParams: ResourcesRequest): Resources {
        return Resources.Builder()
            .setVersion(requestParams.version)
            .build()
    }

    companion object {
        fun update(applicationContext: Context) {
            getUpdater(applicationContext).requestUpdate(JournalTileService::class.java)
        }
    }
}
