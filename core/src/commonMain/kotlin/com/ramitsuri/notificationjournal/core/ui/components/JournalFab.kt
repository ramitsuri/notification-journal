package com.ramitsuri.notificationjournal.core.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.contentColorFor
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import notificationjournal.core.generated.resources.Res
import notificationjournal.core.generated.resources.add_entry_content_description
import org.jetbrains.compose.resources.stringResource

@Composable
fun JournalFab(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    onAssistantClick: (() -> Unit)? = null,
    content: (@Composable () -> Unit)? = null,
) {
    if (onAssistantClick != null) {
        val containerColor = FloatingActionButtonDefaults.containerColor
        val contentColor = contentColorFor(containerColor)

        Surface(
            modifier = modifier,
            shape = FloatingActionButtonDefaults.shape,
            color = containerColor,
            contentColor = contentColor,
            shadowElevation = 6.dp,
            tonalElevation = 6.dp,
        ) {
            Row(
                modifier = Modifier.height(56.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                // Mic icon action
                Box(
                    modifier =
                        Modifier
                            .fillMaxHeight()
                            .clickable(role = Role.Button, onClick = onAssistantClick)
                            .padding(horizontal = 16.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Filled.Mic,
                        contentDescription = "Voice Assistant",
                        modifier = Modifier.size(24.dp),
                    )
                }

                // Split divider
                Box(
                    modifier =
                        Modifier
                            .width(1.dp)
                            .height(28.dp)
                            .background(contentColor.copy(alpha = 0.2f)),
                )

                // Add entry action
                Row(
                    modifier =
                        Modifier
                            .fillMaxHeight()
                            .clickable(role = Role.Button, onClick = onClick)
                            .padding(start = 14.dp, end = 18.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        imageVector = Icons.Filled.Add,
                        contentDescription = null,
                        modifier = Modifier.size(22.dp),
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = stringResource(Res.string.add_entry_content_description),
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }
        }
    } else {
        FloatingActionButton(
            modifier = modifier,
            onClick = onClick,
            content =
                content ?: {
                    Icon(
                        Icons.Filled.Add,
                        stringResource(Res.string.add_entry_content_description),
                    )
                },
        )
    }
}
