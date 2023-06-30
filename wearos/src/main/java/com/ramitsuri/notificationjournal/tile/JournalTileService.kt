package com.ramitsuri.notificationjournal.tile

import androidx.wear.protolayout.ResourceBuilders.Resources
import androidx.wear.tiles.RequestBuilders
import androidx.wear.tiles.TileBuilders.Tile
import com.google.android.horologist.annotations.ExperimentalHorologistApi
import com.google.android.horologist.tiles.SuspendingTileService

@OptIn(ExperimentalHorologistApi::class)
class JournalTileService : SuspendingTileService() {
    private lateinit var renderer: TileRenderer

    override fun onCreate() {
        super.onCreate()
        renderer = TileRenderer(this)
    }

    override suspend fun tileRequest(requestParams: RequestBuilders.TileRequest): Tile {
        return renderer.renderTimeline(Unit, requestParams)
    }

    override suspend fun resourcesRequest(requestParams: RequestBuilders.ResourcesRequest): Resources {
        return renderer.produceRequestedResources(Unit, requestParams)
    }
}
