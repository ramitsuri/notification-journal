@file:OptIn(ExperimentalResourceApi::class)

package com.ramitsuri.notificationjournal.core.ui

import androidx.compose.runtime.Composable
import com.ramitsuri.notificationjournal.core.text.LocalizedString
import com.ramitsuri.notificationjournal.core.text.TextValue
import notificationjournal.core.generated.resources.Res
import notificationjournal.core.generated.resources.today
import notificationjournal.core.generated.resources.tomorrow
import notificationjournal.core.generated.resources.yesterday
import org.jetbrains.compose.resources.ExperimentalResourceApi
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

@Composable
fun TextValue.string(): String {
    return when (this) {
        is TextValue.ForString -> {
            value.plus(args.joinToString(separator = ""))
        }

        is TextValue.ForKey -> {
            stringResource(getResId(key)).plus(args.joinToString(separator = ""))
        }
    }
}

private fun getResId(key: LocalizedString): StringResource {
    return when (key) {
        LocalizedString.TODAY -> Res.string.today
        LocalizedString.TOMORROW -> Res.string.tomorrow
        LocalizedString.YESTERDAY -> Res.string.yesterday
    }
}