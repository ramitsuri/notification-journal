package com.ramitsuri.notificationjournal.core.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import com.ramitsuri.notificationjournal.core.utils.isDarkMode

private val darkColorScheme =
    darkColorScheme(
        primary = Purple80,
        secondary = PurpleGrey80,
        tertiary = Pink80,
    )

private val lightColorScheme =
    lightColorScheme(
        primary = Purple40,
        secondary = PurpleGrey40,
        tertiary = Pink40,
    )

@Composable
fun NotificationJournalTheme(
    dynamicDarkColorScheme: ColorScheme? = null,
    dynamicLightColorScheme: ColorScheme? = null,
    content: @Composable () -> Unit,
) {
    val colorScheme =
        if (isDarkMode()) {
            dynamicDarkColorScheme ?: darkColorScheme
        } else {
            dynamicLightColorScheme ?: lightColorScheme
        }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content,
    )
}
