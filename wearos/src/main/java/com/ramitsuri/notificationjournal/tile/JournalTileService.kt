package com.ramitsuri.notificationjournal.tile

import android.app.Service
import android.content.Context
import androidx.wear.protolayout.ResourceBuilders.Resources
import androidx.wear.protolayout.TimelineBuilders
import androidx.wear.tiles.RequestBuilders
import androidx.wear.tiles.RequestBuilders.ResourcesRequest
import androidx.wear.tiles.TileBuilders.Tile
import com.google.android.horologist.annotations.ExperimentalHorologistApi
import com.google.android.horologist.tiles.SuspendingTileService
import com.ramitsuri.notificationjournal.core.data.JournalEntryTemplateDao
import com.ramitsuri.notificationjournal.core.di.AppScope
import dev.zacsweers.metro.ContributesIntoMap
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.binding
import dev.zacsweers.metrox.android.ServiceKey

@OptIn(ExperimentalHorologistApi::class)
@Inject
@ContributesIntoMap(AppScope::class, binding = binding<Service>())
@ServiceKey
class JournalTileService(
    private val templateDao: JournalEntryTemplateDao,
) : SuspendingTileService() {
    override suspend fun tileRequest(requestParams: RequestBuilders.TileRequest): Tile {
        val tileState = TileState(templateDao.getAll())
        val layoutElement = tileLayout(this, requestParams.deviceConfiguration, tileState)
        return Tile
            .Builder()
            .setResourcesVersion("")
            .setTileTimeline(TimelineBuilders.Timeline.fromLayoutElement(layoutElement))
            .build()
    }

    override suspend fun resourcesRequest(requestParams: ResourcesRequest): Resources =
        Resources
            .Builder()
            .setVersion(requestParams.version)
            .build()

    companion object {
        fun update(applicationContext: Context) {
            getUpdater(applicationContext).requestUpdate(JournalTileService::class.java)
        }
    }
}
