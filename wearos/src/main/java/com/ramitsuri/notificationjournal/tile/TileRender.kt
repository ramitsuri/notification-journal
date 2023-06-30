package com.ramitsuri.notificationjournal.tile

import android.content.Context
import androidx.wear.protolayout.ActionBuilders
import androidx.wear.protolayout.DeviceParametersBuilders
import androidx.wear.protolayout.LayoutElementBuilders
import androidx.wear.protolayout.ModifiersBuilders
import androidx.wear.protolayout.ResourceBuilders
import androidx.wear.protolayout.material.Button
import androidx.wear.protolayout.material.ButtonColors
import androidx.wear.protolayout.material.layouts.MultiButtonLayout
import androidx.wear.protolayout.material.layouts.PrimaryLayout
import com.google.android.horologist.annotations.ExperimentalHorologistApi
import com.google.android.horologist.tiles.images.drawableResToImageResource
import com.google.android.horologist.tiles.render.SingleTileLayoutRenderer
import com.ramitsuri.notificationjournal.R
import com.ramitsuri.notificationjournal.presentation.MainActivity

@OptIn(ExperimentalHorologistApi::class)
class TileRenderer(context: Context) :
    SingleTileLayoutRenderer<Unit, Unit>(
        context
    ) {
    override fun renderTile(
        state: Unit,
        deviceParameters: DeviceParametersBuilders.DeviceParameters
    ): LayoutElementBuilders.LayoutElement {
        return PrimaryLayout.Builder(deviceParameters)
            .setContent(
                MultiButtonLayout.Builder()
                    .addButtonContent(
                        iconButton(launchActivityClickable())
                    )
                    .build()
            )
            .build()
    }

    override fun ResourceBuilders.Resources.Builder.produceRequestedResources(
        resourceState: Unit,
        deviceParameters: DeviceParametersBuilders.DeviceParameters,
        resourceIds: MutableList<String>
    ) {
        addIdToImageMapping(ICON_ID, drawableResToImageResource(ICON_RES_ID))
    }

    private fun launchActivityClickable(): ModifiersBuilders.Clickable {
        val activity = ActionBuilders.AndroidActivity.Builder()
            .setPackageName(PACKAGE)
            .setClassName(ACTIVITY)
            .addKeyToExtraMapping(
                MainActivity.EXTRA_KEY,
                ActionBuilders.stringExtra(MainActivity.ADD)
            )
            .build()

        return ModifiersBuilders.Clickable.Builder()
            .setOnClick(
                ActionBuilders.LaunchAction.Builder()
                    .setAndroidActivity(activity)
                    .build()
            )
            .build()
    }

    private fun iconButton(clickable: ModifiersBuilders.Clickable) =
        Button.Builder(context, clickable)
            .setIconContent(ICON_ID)
            .setButtonColors(ButtonColors.secondaryButtonColors(theme))
            .build()

    @Suppress("MayBeConstant")
    companion object {
        private const val ICON_ID = "icon"
        private const val ACTIVITY = "com.ramitsuri.notificationjournal.presentation.MainActivity"
        private const val PACKAGE = "com.ramitsuri.notificationjournal"
        private val ICON_RES_ID = R.drawable.ic_add
    }
}