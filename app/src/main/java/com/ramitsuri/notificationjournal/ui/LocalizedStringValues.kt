package com.ramitsuri.notificationjournal.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.ramitsuri.notificationjournal.R
import com.ramitsuri.notificationjournal.core.text.LocalizedString
import com.ramitsuri.notificationjournal.core.text.TextValue

@Composable
fun TextValue.string(): String {
    return when (this) {
        is TextValue.ForString -> {
            value.plus(args.joinToString(separator = ""))
        }

        is TextValue.ForKey -> {
            stringResource(id = getResId(key)).plus(args.joinToString(separator = ""))
        }
    }
}

private fun getResId(key: LocalizedString): Int {
    return when (key) {
        LocalizedString.TODAY -> R.string.today
        LocalizedString.TOMORROW -> R.string.tomorrow
        LocalizedString.YESTERDAY -> R.string.yesterday
    }
}