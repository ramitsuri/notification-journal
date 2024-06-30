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
    SingleTileLayoutRenderer<Unit, Unit>(context) {
    override fun renderTile(
        state: Unit,
        deviceParameters: DeviceParametersBuilders.DeviceParameters
    ): LayoutElementBuilders.LayoutElement {
        return PrimaryLayout.Builder(deviceParameters)
            .setContent(
                MultiButtonLayout.Builder()
                    .addButtonContent(
                        iconButton(
                            icon = Icon.APP,
                            launchActivityClickable(action = MainActivity.OPEN_APP)
                        )
                    )
                    .addButtonContent(
                        iconButton(
                            icon = Icon.ADD,
                            launchActivityClickable(action = MainActivity.ADD)
                        )
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
        for (icon in Icon.values()) {
            addIdToImageMapping(icon.id, drawableResToImageResource(icon.resId))
        }
    }

    private fun launchActivityClickable(action: String): ModifiersBuilders.Clickable {
        val activity = ActionBuilders.AndroidActivity.Builder()
            .setPackageName(context.packageName)
            .setClassName(ACTIVITY)
            .addKeyToExtraMapping(
                MainActivity.EXTRA_KEY,
                ActionBuilders.stringExtra(action)
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

    private fun iconButton(icon: Icon, clickable: ModifiersBuilders.Clickable) =
        Button.Builder(context, clickable)
            .setIconContent(icon.id)
            .setButtonColors(ButtonColors.secondaryButtonColors(theme))
            .build()

    companion object {
        private const val ACTIVITY = "com.ramitsuri.notificationjournal.presentation.MainActivity"
    }
}

enum class Icon(val id: String, val resId: Int) {
    APP(
        id = "icon_app",
        resId = R.drawable.ic_open_app
    ),
    ADD(
        id = "icon_add",
        resId = R.drawable.ic_add
    ),
    UPLOAD(
        id = "icon_upload",
        resId = R.drawable.ic_upload
    )
}