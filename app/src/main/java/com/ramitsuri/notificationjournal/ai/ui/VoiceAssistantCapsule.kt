package com.ramitsuri.notificationjournal.ai.ui

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.BiasAlignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.ramitsuri.notificationjournal.ai.AssistantUiState
import com.ramitsuri.notificationjournal.ai.JournalAssistantViewModel
import com.ramitsuri.notificationjournal.core.model.VoiceAssistantMode
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun VoiceAssistantCapsule(
    viewModel: JournalAssistantViewModel,
    mode: VoiceAssistantMode = VoiceAssistantMode.IN_APP,
    onClose: () -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val state by viewModel.uiState.collectAsState()
    val amplitude by viewModel.amplitudeFlow.collectAsState()
    val savedPosition by viewModel.getCapsulePosition(mode).collectAsState(initial = null)

    var isVisible by remember { mutableStateOf(false) }
    var hasInitializedPosition by remember { mutableStateOf(false) }
    var showConfirmDiscardDialog by remember { mutableStateOf(false) }

    var xPercent by remember { mutableFloatStateOf(1f) }
    var yPercent by remember { mutableFloatStateOf(1f) }
    var isDragging by remember { mutableStateOf(false) }

    var containerSize by remember { mutableStateOf(IntSize.Zero) }
    var capsuleSize by remember { mutableStateOf(IntSize.Zero) }

    val currentContainerSize by rememberUpdatedState(containerSize)
    val currentCapsuleSize by rememberUpdatedState(capsuleSize)

    LaunchedEffect(savedPosition) {
        if (!hasInitializedPosition && savedPosition != null) {
            xPercent = savedPosition!!.x.coerceIn(0f, 1f)
            yPercent = savedPosition!!.y.coerceIn(0f, 1f)
            hasInitializedPosition = true
            isVisible = true
        }
    }

    LaunchedEffect(Unit) {
        delay(150)
        if (!hasInitializedPosition) {
            hasInitializedPosition = true
            isVisible = true
        }
    }

    val closeWithAnimation: () -> Unit = {
        scope.launch {
            isVisible = false
            delay(180)
            onClose()
        }
    }

    val stopAndClose: () -> Unit = {
        viewModel.discardRecording()
        closeWithAnimation()
    }

    DisposableEffect(Unit) {
        onDispose {
            viewModel.discardRecording()
        }
    }

    val requestDismiss = {
        val current = state
        if (showConfirmDiscardDialog) {
            showConfirmDiscardDialog = false
        } else if (current is AssistantUiState.Recording && (current.durationSeconds > 1 || current.segmentIndex > 1)) {
            showConfirmDiscardDialog = true
        } else {
            stopAndClose()
        }
    }

    BackHandler {
        requestDismiss()
    }

    val startRecording = {
        viewModel.startRecording(context)
    }

    val permissionLauncher =
        rememberLauncherForActivityResult(
            contract = ActivityResultContracts.RequestPermission(),
        ) { isGranted ->
            if (isGranted) {
                startRecording()
            }
        }

    val requestPermissionAndStart = {
        val hasPermission =
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.RECORD_AUDIO,
            ) == PackageManager.PERMISSION_GRANTED

        if (hasPermission) {
            startRecording()
        } else {
            permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }
    }

    LaunchedEffect(Unit) {
        requestPermissionAndStart()
    }

    Box(
        modifier =
            Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.20f))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                ) {
                    requestDismiss()
                },
    ) {
        val horizontalBias = (xPercent * 2f) - 1f
        val verticalBias = (yPercent * 2f) - 1f

        Box(
            modifier =
                Modifier
                    .fillMaxSize()
                    .systemBarsPadding()
                    .padding(16.dp)
                    .onSizeChanged { containerSize = it },
            contentAlignment = BiasAlignment(horizontalBias, verticalBias),
        ) {
            AnimatedVisibility(
                visible = isVisible && hasInitializedPosition,
                enter =
                    scaleIn(
                        initialScale = 0.6f,
                        transformOrigin = TransformOrigin(xPercent, yPercent),
                        animationSpec =
                            spring(
                                dampingRatio = Spring.DampingRatioMediumBouncy,
                                stiffness = Spring.StiffnessMediumLow,
                            ),
                    ) + fadeIn(animationSpec = tween(150)),
                exit =
                    scaleOut(
                        targetScale = 0.6f,
                        transformOrigin = TransformOrigin(xPercent, yPercent),
                        animationSpec = tween(160, easing = FastOutSlowInEasing),
                    ) + fadeOut(animationSpec = tween(160)),
            ) {
                val elevation by animateDpAsState(
                    targetValue = if (isDragging) 16.dp else 10.dp,
                    label = "capsuleElevation",
                )

                Surface(
                    modifier =
                        Modifier
                            .onSizeChanged { capsuleSize = it }
                            .pointerInput(Unit) {
                                detectDragGestures(
                                    onDragStart = {
                                        isDragging = true
                                    },
                                    onDragEnd = {
                                        isDragging = false
                                        viewModel.saveCapsulePosition(mode, xPercent, yPercent)
                                    },
                                    onDragCancel = {
                                        isDragging = false
                                        viewModel.saveCapsulePosition(mode, xPercent, yPercent)
                                    },
                                    onDrag = { change, dragAmount ->
                                        change.consume()
                                        val cWidth = currentContainerSize.width
                                        val cHeight = currentContainerSize.height
                                        val capWidth = currentCapsuleSize.width
                                        val capHeight = currentCapsuleSize.height

                                        val availableWidth = (cWidth - capWidth).coerceAtLeast(1)
                                        val availableHeight = (cHeight - capHeight).coerceAtLeast(1)

                                        xPercent = (xPercent + dragAmount.x / availableWidth).coerceIn(0f, 1f)
                                        yPercent = (yPercent + dragAmount.y / availableHeight).coerceIn(0f, 1f)
                                    },
                                )
                            }.clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null,
                            ) {
                                // Consume clicks inside capsule so backdrop isn't triggered
                            },
                    shape = RoundedCornerShape(28.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.8f)),
                    tonalElevation = elevation,
                    shadowElevation = elevation,
                ) {
                    Column(
                        modifier =
                            Modifier
                                .width(IntrinsicSize.Max)
                                .defaultMinSize(minWidth = 200.dp)
                                .animateContentSize()
                                .padding(horizontal = 24.dp, vertical = 16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                    ) {
                        when (val current = state) {
                            is AssistantUiState.Recording -> {
                                val infiniteTransition = rememberInfiniteTransition(label = "pulse")
                                val pulseAlpha by infiniteTransition.animateFloat(
                                    initialValue = 0.35f,
                                    targetValue = 1f,
                                    animationSpec =
                                        infiniteRepeatable(
                                            animation = tween(700),
                                            repeatMode = RepeatMode.Reverse,
                                        ),
                                    label = "dotAlpha",
                                )

                                if (showConfirmDiscardDialog) {
                                    // LINE 1: Red pulsing dot + "Discard?"
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    ) {
                                        Box(
                                            modifier =
                                                Modifier
                                                    .size(10.dp)
                                                    .background(
                                                        MaterialTheme.colorScheme.error.copy(alpha = pulseAlpha),
                                                        shape = CircleShape,
                                                    ),
                                        )
                                        Text(
                                            text = "Discard?",
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurface,
                                            fontSize = 17.sp,
                                        )
                                    }

                                    // Subtle divider
                                    Box(
                                        modifier =
                                            Modifier
                                                .fillMaxWidth()
                                                .height(1.dp)
                                                .background(
                                                    MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f),
                                                ),
                                    )

                                    // LINE 2: No | Yes
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceEvenly,
                                        verticalAlignment = Alignment.CenterVertically,
                                    ) {
                                        TextButton(
                                            onClick = { showConfirmDiscardDialog = false },
                                            modifier = Modifier.weight(1f),
                                        ) {
                                            Text(
                                                text = "No",
                                                style = MaterialTheme.typography.titleSmall,
                                                fontWeight = FontWeight.SemiBold,
                                                color = MaterialTheme.colorScheme.onSurface,
                                            )
                                        }

                                        Box(
                                            modifier =
                                                Modifier
                                                    .width(1.dp)
                                                    .height(24.dp)
                                                    .background(
                                                        MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                                                    ),
                                        )

                                        TextButton(
                                            onClick = { stopAndClose() },
                                            modifier = Modifier.weight(1f),
                                        ) {
                                            Text(
                                                text = "Yes",
                                                style = MaterialTheme.typography.titleSmall,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.error,
                                            )
                                        }
                                    }
                                } else {
                                    // LINE 1: Recording status, timer, and waveform
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        ) {
                                            Box(
                                                modifier =
                                                    Modifier
                                                        .size(10.dp)
                                                        .background(
                                                            MaterialTheme.colorScheme.error.copy(alpha = pulseAlpha),
                                                            shape = CircleShape,
                                                        ),
                                            )

                                            val minutes = current.durationSeconds / 60
                                            val seconds = current.durationSeconds % 60
                                            val timeFormatted = "%02d:%02d".format(minutes, seconds)

                                            if (current.segmentIndex > 1) {
                                                Text(
                                                    text = "#${current.segmentIndex} • ",
                                                    style = MaterialTheme.typography.titleMedium,
                                                    color = MaterialTheme.colorScheme.primary,
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 15.sp,
                                                )
                                            }
                                            Text(
                                                text = timeFormatted,
                                                style = MaterialTheme.typography.titleMedium,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.onSurface,
                                                fontSize = 17.sp,
                                            )
                                        }

                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                                        ) {
                                            if (current.silentMicWarning) {
                                                Icon(
                                                    Icons.Default.WarningAmber,
                                                    contentDescription = "Silent mic warning",
                                                    tint = MaterialTheme.colorScheme.error,
                                                    modifier = Modifier.size(18.dp),
                                                )
                                            }
                                            MiniWaveform(amplitude = amplitude)
                                        }
                                    }

                                    // Subtle divider
                                    Box(
                                        modifier =
                                            Modifier
                                                .fillMaxWidth()
                                                .height(1.dp)
                                                .background(
                                                    MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f),
                                                ),
                                    )

                                    // LINE 2: Action buttons (Discard, Next Entry, Done)
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(28.dp),
                                    ) {
                                        // Discard button
                                        IconButton(
                                            onClick = {
                                                if (current.durationSeconds > 1 || current.segmentIndex > 1) {
                                                    showConfirmDiscardDialog = true
                                                } else {
                                                    stopAndClose()
                                                }
                                            },
                                            modifier =
                                                Modifier
                                                    .size(44.dp)
                                                    .background(
                                                        MaterialTheme.colorScheme.surfaceContainerHighest,
                                                        CircleShape,
                                                    ),
                                        ) {
                                            Icon(
                                                Icons.Default.Close,
                                                contentDescription = "Discard",
                                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                                modifier = Modifier.size(22.dp),
                                            )
                                        }

                                        // Next entry button
                                        IconButton(
                                            onClick = { viewModel.nextEntry(context) },
                                            modifier =
                                                Modifier
                                                    .size(44.dp)
                                                    .background(
                                                        MaterialTheme.colorScheme.secondaryContainer,
                                                        CircleShape,
                                                    ),
                                        ) {
                                            Icon(
                                                Icons.Default.Add,
                                                contentDescription = "Next entry",
                                                tint = MaterialTheme.colorScheme.onSecondaryContainer,
                                                modifier = Modifier.size(24.dp),
                                            )
                                        }

                                        // Done button (prominent primary circle)
                                        IconButton(
                                            onClick = {
                                                viewModel.commitRecording(context) {
                                                    closeWithAnimation()
                                                }
                                            },
                                            modifier =
                                                Modifier
                                                    .size(48.dp)
                                                    .background(MaterialTheme.colorScheme.primary, CircleShape),
                                        ) {
                                            Icon(
                                                Icons.Default.Check,
                                                contentDescription = "Save",
                                                tint = MaterialTheme.colorScheme.onPrimary,
                                                modifier = Modifier.size(26.dp),
                                            )
                                        }
                                    }
                                }
                            }

                            is AssistantUiState.Idle -> {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                                ) {
                                    IconButton(
                                        onClick = requestPermissionAndStart,
                                        modifier =
                                            Modifier
                                                .size(46.dp)
                                                .background(MaterialTheme.colorScheme.primaryContainer, CircleShape),
                                    ) {
                                        Icon(
                                            Icons.Default.Mic,
                                            contentDescription = "Record",
                                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                            modifier = Modifier.size(26.dp),
                                        )
                                    }
                                    Text(
                                        text = "Tap to record",
                                        style = MaterialTheme.typography.bodyLarge,
                                        fontWeight = FontWeight.Medium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                            }

                            is AssistantUiState.Error -> {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(8.dp),
                                ) {
                                    Text(
                                        text = current.error,
                                        color = MaterialTheme.colorScheme.error,
                                        style = MaterialTheme.typography.bodySmall,
                                    )
                                    IconButton(
                                        onClick = stopAndClose,
                                        modifier = Modifier.size(40.dp),
                                    ) {
                                        Icon(Icons.Default.Close, contentDescription = "Close")
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MiniWaveform(amplitude: Float) {
    val clamped = amplitude.coerceIn(0f, 1f)

    val bar1Height by animateDpAsState(
        targetValue = (6 + (16 * clamped * 0.7f)).dp,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
        label = "b1",
    )
    val bar2Height by animateDpAsState(
        targetValue = (6 + (22 * clamped * 0.9f)).dp,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
        label = "b2",
    )
    val bar3Height by animateDpAsState(
        targetValue = (6 + (26 * clamped)).dp,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
        label = "b3",
    )
    val bar4Height by animateDpAsState(
        targetValue = (6 + (20 * clamped * 0.85f)).dp,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
        label = "b4",
    )
    val bar5Height by animateDpAsState(
        targetValue = (6 + (14 * clamped * 0.6f)).dp,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
        label = "b5",
    )

    Row(
        modifier = Modifier.height(28.dp).padding(horizontal = 2.dp),
        horizontalArrangement = Arrangement.spacedBy(3.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        listOf(bar1Height, bar2Height, bar3Height, bar4Height, bar5Height).forEach { height ->
            Box(
                modifier =
                    Modifier
                        .width(3.5.dp)
                        .height(height)
                        .background(MaterialTheme.colorScheme.primary, RoundedCornerShape(2.dp)),
            )
        }
    }
}
