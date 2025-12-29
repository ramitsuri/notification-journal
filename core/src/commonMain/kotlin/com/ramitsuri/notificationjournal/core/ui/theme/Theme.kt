package com.ramitsuri.notificationjournal.core.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
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

private val greenDark = Color(0xFF64a880)
private val redDark = Color(0xFFFD9891)

private val greenLight = Color(0xFF578a4f)
private val redLight = Color(0xFFC9372C)

val greenColor: Color
    @Composable
    get() = if (isDarkMode()) greenDark else greenLight

val redColor: Color
    @Composable
    get() = if (isDarkMode()) redDark else redLight
