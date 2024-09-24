package com.ramitsuri.notificationjournal.core.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.ramitsuri.notificationjournal.core.utils.isDarkMode

val Purple80 = Color(0xFFD0BCFF)
val PurpleGrey80 = Color(0xFFCCC2DC)
val Pink80 = Color(0xFFEFB8C8)

val Purple40 = Color(0xFF6650a4)
val PurpleGrey40 = Color(0xFF625b71)
val Pink40 = Color(0xFF7D5260)

val ColorScheme.green: Color
    @Composable
    get() = if (isDarkMode()) Color(0xFF1D572C) else Color(0xFFACEEBB)

val ColorScheme.red: Color
    @Composable
    get() = if (isDarkMode()) Color(0xFF803030) else Color(0xFFFFC0C0)

