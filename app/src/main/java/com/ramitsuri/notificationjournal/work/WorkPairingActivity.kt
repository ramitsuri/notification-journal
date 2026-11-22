package com.ramitsuri.notificationjournal.work

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ramitsuri.notificationjournal.MainApplication
import com.ramitsuri.notificationjournal.R
import com.ramitsuri.notificationjournal.core.ui.nav.viewModelFactory
import com.ramitsuri.notificationjournal.core.ui.settings.QrCodeScanner
import com.ramitsuri.notificationjournal.core.ui.theme.NotificationJournalTheme
import com.ramitsuri.notificationjournal.service.WorkBridgeService
import kotlinx.coroutines.flow.filter

class WorkPairingActivity : ComponentActivity() {
    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        enableEdgeToEdge()

        val appGraph = (application as MainApplication).appGraph

        setContent {
            val pairingViewModel: WorkPairingViewModel =
                viewModel(
                    factory = viewModelFactory { appGraph.workPairingViewModel },
                )

            LaunchedEffect(Unit) {
                pairingViewModel.isPaired
                    .filter { it }
                    .collect {
                        WorkBridgeService.reconnect(this@WorkPairingActivity)
                        finish()
                    }
            }

            NotificationJournalTheme(
                dynamicDarkColorScheme =
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                        dynamicDarkColorScheme(this)
                    } else {
                        null
                    },
                dynamicLightColorScheme =
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                        dynamicLightColorScheme(this)
                    } else {
                        null
                    },
            ) {
                val context = LocalContext.current
                var hasCameraPermission by remember {
                    mutableStateOf(
                        ContextCompat.checkSelfPermission(
                            context,
                            Manifest.permission.CAMERA,
                        ) == PackageManager.PERMISSION_GRANTED,
                    )
                }

                val permissionLauncher =
                    rememberLauncherForActivityResult(
                        contract = ActivityResultContracts.RequestPermission(),
                    ) { isGranted ->
                        hasCameraPermission = isGranted
                    }

                LaunchedEffect(Unit) {
                    if (!hasCameraPermission) {
                        permissionLauncher.launch(Manifest.permission.CAMERA)
                    }
                }

                Scaffold(
                    topBar = {
                        TopAppBar(
                            title = { Text(stringResource(R.string.work_pairing_title)) },
                            navigationIcon = {
                                IconButton(onClick = { finish() }) {
                                    Icon(Icons.Default.Close, contentDescription = "Close")
                                }
                            },
                        )
                    },
                ) { paddingValues ->
                    if (hasCameraPermission) {
                        Column(
                            modifier =
                                Modifier
                                    .fillMaxSize()
                                    .padding(paddingValues),
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            Text(
                                text = stringResource(R.string.work_pairing_instruction),
                                modifier = Modifier.padding(16.dp),
                                style = MaterialTheme.typography.bodyMedium,
                                textAlign = TextAlign.Center,
                            )
                            Card(
                                modifier =
                                    Modifier
                                        .padding(16.dp)
                                        .fillMaxWidth()
                                        .weight(1f),
                                shape = RoundedCornerShape(16.dp),
                            ) {
                                QrCodeScanner(
                                    modifier = Modifier.fillMaxSize(),
                                    onQrCodeScanned = { content ->
                                        pairingViewModel.onQrCodeScanned(content)
                                    },
                                )
                            }
                        }
                    } else {
                        Column(
                            modifier =
                                Modifier
                                    .fillMaxSize()
                                    .padding(paddingValues)
                                    .padding(24.dp),
                            verticalArrangement = Arrangement.Center,
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            Text(
                                text = stringResource(R.string.work_pairing_permission_rationale),
                                textAlign = TextAlign.Center,
                                style = MaterialTheme.typography.bodyMedium,
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Button(
                                onClick = {
                                    permissionLauncher.launch(Manifest.permission.CAMERA)
                                },
                            ) {
                                Text(stringResource(R.string.work_pairing_grant_permission))
                            }
                        }
                    }
                }
            }
        }
    }
}
