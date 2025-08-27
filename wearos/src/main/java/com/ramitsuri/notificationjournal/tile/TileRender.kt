package com.ramitsuri.notificationjournal.tile

import android.content.Context
import androidx.wear.protolayout.ActionBuilders
import androidx.wear.protolayout.ActionBuilders.AndroidActivity
import androidx.wear.protolayout.DeviceParametersBuilders
import androidx.wear.protolayout.DimensionBuilders.expand
import androidx.wear.protolayout.LayoutElementBuilders
import androidx.wear.protolayout.LayoutElementBuilders.LayoutElement
import androidx.wear.protolayout.ModifiersBuilders
import androidx.wear.protolayout.material3.ButtonDefaults.filledTonalButtonColors
import androidx.wear.protolayout.material3.ButtonGroupDefaults
import androidx.wear.protolayout.material3.MaterialScope
import androidx.wear.protolayout.material3.TextButtonStyle.Companion.largeTextButtonStyle
import androidx.wear.protolayout.material3.buttonGroup
import androidx.wear.protolayout.material3.materialScope
import androidx.wear.protolayout.material3.primaryLayout
import androidx.wear.protolayout.material3.text
import androidx.wear.protolayout.material3.textButton
import androidx.wear.protolayout.material3.textEdgeButton
import androidx.wear.protolayout.modifiers.padding
import androidx.wear.protolayout.types.layoutString
import com.ramitsuri.notificationjournal.R
import com.ramitsuri.notificationjournal.core.model.template.JournalEntryTemplate
import com.ramitsuri.notificationjournal.presentation.MainActivity

fun tileLayout(
    context: Context,
    deviceParameters: DeviceParametersBuilders.DeviceParameters,
    state: TileState,
): LayoutElement {
    return materialScope(
        context = context,
        deviceConfiguration = deviceParameters,
        allowDynamicTheme = true,
    ) {
        val (row1, row2) = state.templates.take(5).chunked(3)
        primaryLayout(
            mainSlot = {
                column {
                    setWidth(expand())
                    setHeight(expand())
                    addContent(
                        buttonGroup {
                            row1.forEach {
                                buttonGroupItem {
                                    button(
                                        action = templateAction(context, it),
                                        text = it.shortDisplayText,
                                    )
                                }
                            }
                        },
                    )
                    addContent(ButtonGroupDefaults.DEFAULT_SPACER_BETWEEN_BUTTON_GROUPS)
                    addContent(
                        buttonGroup {
                            row2.forEach {
                                buttonGroupItem {
                                    button(
                                        action = templateAction(context, it),
                                        text = it.shortDisplayText,
                                    )
                                }
                            }
                            buttonGroupItem {
                                button(
                                    action = launchActivityAction(context, MainActivity.SHOW_ADDITIONAL_TEMPLATES),
                                    text = "\uD83D\uDCF2",
                                )
                            }
                        },
                    )
                }
            },
            bottomSlot = {
                textEdgeButton(
                    onClick = launchActivityClickable(launchActivityAction(context)),
                    labelContent = { text(context.getString(R.string.add_new).layoutString) },
                    colors = filledTonalButtonColors(),
                )
            },
        )
    }
}

private fun MaterialScope.button(
    action: AndroidActivity,
    text: String,
): LayoutElement {
    return textButton(
        onClick = launchActivityClickable(action),
        labelContent = {
            text(text = text.layoutString, maxLines = 2)
        },
        width = expand(),
        height = expand(),
        style = largeTextButtonStyle(),
        contentPadding = padding(0f),
    )
}

private fun column(builder: LayoutElementBuilders.Column.Builder.() -> Unit) =
    LayoutElementBuilders.Column.Builder().apply(builder).build()

private fun launchActivityAction(
    context: Context,
    action: String = MainActivity.ADD,
): AndroidActivity =
    AndroidActivity.Builder()
        .setPackageName(context.packageName)
        .setClassName(ACTIVITY)
        .addKeyToExtraMapping(
            MainActivity.EXTRA_KEY,
            ActionBuilders.stringExtra(action),
        )
        .build()

private fun templateAction(
    context: Context,
    template: JournalEntryTemplate,
): AndroidActivity =
    AndroidActivity.Builder()
        .setPackageName(context.packageName)
        .setClassName(ACTIVITY)
        .addKeyToExtraMapping(
            MainActivity.EXTRA_KEY,
            ActionBuilders.stringExtra(MainActivity.TEMPLATE),
        )
        .addKeyToExtraMapping(
            MainActivity.TEMPLATE_ID,
            ActionBuilders.stringExtra(template.id),
        )
        .build()

private fun launchActivityClickable(activity: AndroidActivity): ModifiersBuilders.Clickable =
    ModifiersBuilders.Clickable.Builder()
        .setOnClick(
            ActionBuilders.LaunchAction.Builder()
                .setAndroidActivity(activity)
                .build(),
        )
        .build()

private const val ACTIVITY =
    "com.ramitsuri.notificationjournal.presentation.MainActivity"
