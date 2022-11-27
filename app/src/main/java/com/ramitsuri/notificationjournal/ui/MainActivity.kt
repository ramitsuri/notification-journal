package com.ramitsuri.notificationjournal.ui

import android.content.Context
import android.content.ContextWrapper
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.displayCutoutPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.Divider
import androidx.compose.material.Icon
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.core.view.WindowCompat
import com.ramitsuri.notificationjournal.MainApplication
import com.ramitsuri.notificationjournal.R
import com.ramitsuri.notificationjournal.data.JournalEntry
import com.ramitsuri.notificationjournal.ui.theme.NotificationJournalTheme
import com.ramitsuri.notificationjournal.utils.Constants
import com.ramitsuri.notificationjournal.utils.formatForDisplay
import kotlin.system.exitProcess

@OptIn(ExperimentalMaterial3Api::class)
class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels {
        (applicationContext as MainApplication).getViewModelFactory()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        showNotification()
        setContent {
            NotificationJournalTheme {
                Scaffold { paddingValues ->
                    val viewState = viewModel.state.collectAsState().value
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(top = 16.dp, start = 16.dp, end = 16.dp)
                            .statusBarsPadding()
                            .displayCutoutPadding()
                            .padding(paddingValues),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        if (viewState.loading) {
                            LinearProgressIndicator(modifier = Modifier.fillMaxWidth())

                        } else {
                            if (!viewState.error.isNullOrEmpty()) {
                                Toast.makeText(
                                    this@MainActivity, viewState.error, Toast.LENGTH_LONG
                                ).show()

                            }

                            ActionButtons(viewState.serverText, viewModel::setApiUrl)

                            Spacer(modifier = Modifier.height(24.dp))

                            List(viewState.journalEntries)
                        }
                    }
                }
            }
        }
    }

    @Composable
    fun ActionButtons(serverText: String, onUrlSet: (String) -> Unit) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceAround
        ) {
            FilledTonalButton(onClick = { viewModel.upload() }) {
                Text(text = stringResource(id = R.string.button_text_upload_all))
            }
            FilledTonalButton(onClick = { viewModel.getAll() }) {
                Text(text = stringResource(id = R.string.button_text_refresh))
            }
            ChangeApiUrlView(serverText = serverText, onUrlSet = onUrlSet)
        }
    }

    @Composable
    fun List(items: List<JournalEntry>) {
        LazyColumn {
            items(items, key = { it.id }) {
                ListItem(item = it)
                Divider()
            }
            item {
                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }

    @Composable
    fun ListItem(item: JournalEntry) {
        Row(
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(8.dp)
            ) {
                Text(
                    text = formatForDisplay(item.entryTime, item.timeZone),
                    style = MaterialTheme.typography.labelSmall
                )
                Text(
                    text = item.text,
                    style = MaterialTheme.typography.bodyLarge
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            FilledTonalIconButton(
                onClick = { viewModel.delete(item) }) {
                Icon(
                    imageVector = Icons.Outlined.Delete,
                    contentDescription = "Delete",
                    tint = MaterialTheme.colorScheme.onBackground
                )
            }
        }
    }

    @Composable
    private fun ChangeApiUrlView(
        serverText: String?,
        onUrlSet: (String) -> Unit,
        modifier: Modifier = Modifier
    ) {
        var showDialog by rememberSaveable { mutableStateOf(false) }
        var serverSet by rememberSaveable { mutableStateOf(false) }

        if (showDialog) {
            SetApiUrlDialog(
                serverText ?: "",
                onPositiveClick = { value ->
                    showDialog = !showDialog
                    onUrlSet(value)
                    serverSet = true
                },
                onNegativeClick = {
                    showDialog = !showDialog
                },
                modifier = modifier
            )
        }
        val context = LocalContext.current
        FilledTonalButton(
            onClick = {
                Toast.makeText(context, "$serverText", Toast.LENGTH_SHORT).show()
                if (serverSet) {
                    val activity = context.getActivity()
                    val intent =
                        activity?.packageManager?.getLaunchIntentForPackage(activity.packageName)
                    activity?.finishAffinity()
                    activity?.startActivity(intent)
                    exitProcess(0)
                } else {
                    showDialog = true
                }
            }
        ) {
            if (serverSet) {
                Text(text = "Restart")
            } else {
                if (serverText.isNullOrEmpty() || serverText == Constants.DEFAULT_API_URL) {
                    Text("Set Server")
                } else {
                    Text(
                        "Server Set"
                    )
                }
            }
        }
    }


    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    private fun SetApiUrlDialog(
        previousText: String,
        onPositiveClick: (String) -> Unit,
        onNegativeClick: () -> Unit,
        modifier: Modifier = Modifier
    ) {
        var text by rememberSaveable { mutableStateOf(previousText.ifEmpty { "http://" }) }
        Dialog(onDismissRequest = { }) {
            Card {
                Column(modifier = modifier.padding(8.dp)) {
                    OutlinedTextField(
                        value = text,
                        singleLine = true,
                        onValueChange = { text = it },
                        modifier = modifier.fillMaxWidth()
                    )
                    Spacer(modifier = modifier.height(16.dp))
                    Row(
                        horizontalArrangement = Arrangement.End,
                        modifier = modifier.fillMaxWidth()
                    ) {
                        TextButton(onClick = {
                            onNegativeClick()
                        }) {
                            Text(text = stringResource(id = R.string.cancel))
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        TextButton(onClick = {
                            onPositiveClick(text)
                        }) {
                            Text(text = stringResource(id = R.string.ok))
                        }
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.getAll()
    }

    private fun showNotification() {
        (application as MainApplication).showJournalNotification()
    }
}

fun Context.getActivity(): AppCompatActivity? = when (this) {
    is AppCompatActivity -> this
    is ContextWrapper -> baseContext.getActivity()
    else -> null
}