package com.ramitsuri.notificationjournal.tile

import android.content.Context
import androidx.wear.protolayout.ActionBuilders
import androidx.wear.protolayout.ActionBuilders.AndroidActivity
import androidx.wear.protolayout.DeviceParametersBuilders
import androidx.wear.protolayout.LayoutElementBuilders
import androidx.wear.protolayout.ModifiersBuilders
import androidx.wear.protolayout.ResourceBuilders
import androidx.wear.protolayout.material.Button
import androidx.wear.protolayout.material.ButtonColors
import androidx.wear.protolayout.material.ButtonDefaults
import androidx.wear.protolayout.material.ChipColors
import androidx.wear.protolayout.material.CompactChip
import androidx.wear.protolayout.material.layouts.MultiButtonLayout
import androidx.wear.protolayout.material.layouts.PrimaryLayout
import com.google.android.horologist.annotations.ExperimentalHorologistApi
import com.google.android.horologist.tiles.images.drawableResToImageResource
import com.google.android.horologist.tiles.render.SingleTileLayoutRenderer
import com.ramitsuri.notificationjournal.R
import com.ramitsuri.notificationjournal.core.model.template.JournalEntryTemplate
import com.ramitsuri.notificationjournal.presentation.MainActivity

@OptIn(ExperimentalHorologistApi::class)
class TileRenderer(context: Context) :
    SingleTileLayoutRenderer<TileState, Unit>(context) {
    override fun renderTile(
        state: TileState,
        deviceParameters: DeviceParametersBuilders.DeviceParameters
    ): LayoutElementBuilders.LayoutElement {
        return PrimaryLayout.Builder(deviceParameters)
            .setContent(
                MultiButtonLayout.Builder()
                    .apply {
                        // This layout can only comfortably show 5 items. Show 4 templates and 1
                        // button to launch into the app
                        state.templates.take(4).forEach {
                            addTextButton(it.shortDisplayText, templateAction(it))
                        }
                        addTextButton(
                            "\uD83D\uDCF2",
                            launchActivityAction(MainActivity.SHOW_ADDITIONAL_TEMPLATES)
                        )
                    }
                    .build()
            )
            .setPrimaryChipContent(
                CompactChip.Builder(
                    context,
                    context.getString(R.string.add_new),
                    launchActivityClickable(launchActivityAction()),
                    deviceParameters
                )
                    .setChipColors(ChipColors.secondaryChipColors(theme))
                    .build()
            )
            .build()
    }

    override fun ResourceBuilders.Resources.Builder.produceRequestedResources(
        resourceState: Unit,
        deviceParameters: DeviceParametersBuilders.DeviceParameters,
        resourceIds: MutableList<String>
    ) {
        for (icon in Icon.entries) {
            addIdToImageMapping(icon.id, drawableResToImageResource(icon.resId))
        }
    }

    private fun launchActivityAction(action: String = MainActivity.ADD): AndroidActivity =
        AndroidActivity.Builder()
            .setPackageName(context.packageName)
            .setClassName(ACTIVITY)
            .addKeyToExtraMapping(
                MainActivity.EXTRA_KEY,
                ActionBuilders.stringExtra(action)
            )
            .build()

    private fun templateAction(template: JournalEntryTemplate): AndroidActivity =
        AndroidActivity.Builder()
            .setPackageName(context.packageName)
            .setClassName(ACTIVITY)
            .addKeyToExtraMapping(
                MainActivity.EXTRA_KEY,
                ActionBuilders.stringExtra(MainActivity.TEMPLATE)
            )
            .addKeyToExtraMapping(
                MainActivity.TEMPLATE_ID,
                ActionBuilders.stringExtra(template.id)
            )
            .build()

    private fun MultiButtonLayout.Builder.addTextButton(
        text: String,
        activity: AndroidActivity
    ) {
        val clickable = launchActivityClickable(activity)
        addButtonContent(
            Button.Builder(context, clickable)
                .setTextContent(text)
                .setSize(ButtonDefaults.LARGE_SIZE)
                .setButtonColors(ButtonColors.secondaryButtonColors(theme))
                .build()
        )
    }

    private fun launchActivityClickable(activity: AndroidActivity): ModifiersBuilders.Clickable =
        ModifiersBuilders.Clickable.Builder()
            .setOnClick(
                ActionBuilders.LaunchAction.Builder()
                    .setAndroidActivity(activity)
                    .build()
            )
            .build()

    companion object {
        private const val ACTIVITY =
            "com.ramitsuri.notificationjournal.presentation.MainActivity"
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