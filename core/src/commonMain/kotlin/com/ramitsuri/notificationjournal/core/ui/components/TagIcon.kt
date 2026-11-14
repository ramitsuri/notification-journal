package com.ramitsuri.notificationjournal.core.ui.components

import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import notificationjournal.core.generated.resources.Res
import notificationjournal.core.generated.resources.allDrawableResources
import org.jetbrains.compose.resources.painterResource

@Composable
fun TagIcon(
    iconName: String?,
    contentDescription: String? = null,
    modifier: Modifier = Modifier,
    tint: Color = LocalContentColor.current,
) {
    if (!iconName.isNullOrBlank()) {
        val cleanName = remember(iconName) {
            val name = iconName.lowercase().trim()
            if (name.startsWith("ic_")) name else "ic_$name"
        }

        val resource = remember(cleanName) {
            Res.allDrawableResources[cleanName]
        }

        if (resource != null) {
            Icon(
                painter = painterResource(resource),
                contentDescription = contentDescription,
                modifier = modifier,
                tint = tint,
            )
        }
    }
}
