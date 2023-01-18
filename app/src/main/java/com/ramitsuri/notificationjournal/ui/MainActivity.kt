package com.ramitsuri.notificationjournal.ui

import android.content.Context
import android.content.ContextWrapper
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.annotation.StringRes
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.displayCutoutPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.Divider
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material3.Card
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.ClipboardManager
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.core.view.WindowCompat
import com.ramitsuri.notificationjournal.MainApplication
import com.ramitsuri.notificationjournal.R
import com.ramitsuri.notificationjournal.data.JournalEntry
import com.ramitsuri.notificationjournal.ui.theme.NotificationJournalTheme
import com.ramitsuri.notificationjournal.utils.Constants
import com.ramitsuri.notificationjournal.utils.formatForDisplay
import kotlinx.coroutines.delay
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
                var showDialog by rememberSaveable { mutableStateOf(false) }
                var initialDialogText by rememberSaveable { mutableStateOf("") }
                var journalEntryId by rememberSaveable { mutableStateOf(-1) }
                val clipboardManager: ClipboardManager = LocalClipboardManager.current

                if (showDialog) {
                    AddEditEntryDialog(
                        initialText = initialDialogText,
                        onPositiveClick = { value ->
                            showDialog = !showDialog
                            if (journalEntryId == -1) {
                                viewModel.add(value)
                            } else {
                                viewModel.edit(journalEntryId, value)
                            }
                        },
                        onNegativeClick = {
                            showDialog = !showDialog
                        }
                    )
                }
                Scaffold(floatingActionButton = {
                    FloatingActionButton(
                        onClick = {
                            showDialog = true
                            journalEntryId = -1
                            initialDialogText = ""
                        },
                        modifier = Modifier.navigationBarsPadding()
                    ) {
                        Icon(
                            Icons.Filled.Add,
                            stringResource(id = R.string.add_entry_content_description)
                        )
                    }
                }) { paddingValues ->
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
                                viewModel.onErrorAcknowledged()
                            }

                            MoreMenu(viewState.serverText, viewModel::setApiUrl)

                            if (viewState.journalEntries.isEmpty()) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .navigationBarsPadding()
                                        .padding(bottom = 64.dp),
                                    verticalArrangement = Arrangement.Center,
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(
                                        text = "No items",
                                        style = MaterialTheme.typography.displaySmall
                                    )
                                }
                            } else {
                                List(viewState.journalEntries,
                                    onCopyRequested = { item ->
                                        clipboardManager.setText(AnnotatedString(item.text))
                                    },
                                    onEditRequested = { item ->
                                        journalEntryId = item.id
                                        initialDialogText = item.text
                                        showDialog = true
                                    },
                                    onDeleteRequested = { item ->
                                        viewModel.delete(item)
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    @Composable
    fun MoreMenu(
        serverText: String,
        onUrlSet: (String) -> Unit,
    ) {
        var expanded by remember { mutableStateOf(false) }
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
                }
            )
        }
        val context = LocalContext.current

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
            Box {
                IconButton(
                    onClick = {
                        expanded = !expanded
                    },
                    modifier = Modifier
                        .size(48.dp)
                        .padding(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.MoreVert,
                        contentDescription = stringResource(id = R.string.menu_content_description)
                    )
                }
                DropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false },
                ) {

                    DropdownMenuItem(
                        text = { Text(stringResource(id = JournalMenuItem.REFRESH.textResId)) },
                        onClick = {
                            expanded = false
                            viewModel.getAll()
                        }
                    )

                    DropdownMenuItem(
                        text = { Text(stringResource(id = JournalMenuItem.UPLOAD.textResId)) },
                        onClick = {
                            expanded = false
                            viewModel.upload()
                        }
                    )
                    if (serverSet) {
                        DropdownMenuItem(
                            text = { Text(stringResource(id = JournalMenuItem.RESTART.textResId)) },
                            onClick = {
                                expanded = false
                                context.shutdown()
                            }
                        )
                    } else if (serverText.isEmpty() || serverText == Constants.DEFAULT_API_URL) {
                        DropdownMenuItem(
                            text = { Text(stringResource(id = JournalMenuItem.SET_SERVER.textResId)) },
                            onClick = {
                                expanded = false
                                showDialog = true
                            }
                        )
                    } else {
                        DropdownMenuItem(
                            text = { Text(stringResource(id = JournalMenuItem.SERVER_SET.textResId)) },
                            onClick = {
                                expanded = false
                                showDialog = true
                            }
                        )
                    }

                    DropdownMenuItem(
                        text = { Text(stringResource(id = JournalMenuItem.DELETE_ALL.textResId)) },
                        onClick = {
                            expanded = false
                            viewModel.delete()
                        }
                    )
                }
            }
        }
    }

    @Composable
    fun List(
        items: List<JournalEntry>,
        onCopyRequested: (JournalEntry) -> Unit,
        onEditRequested: (JournalEntry) -> Unit,
        onDeleteRequested: (JournalEntry) -> Unit
    ) {
        LazyColumn {
            items(items, key = { it.id }) {
                ListItem(
                    item = it,
                    onCopyRequested = onCopyRequested,
                    onDeleteRequested = onDeleteRequested,
                    onEditRequested = onEditRequested
                )
                Divider()
            }
            item {
                Spacer(modifier = Modifier.height(128.dp))
            }
        }
    }

    @Composable
    fun ListItem(
        item: JournalEntry,
        onCopyRequested: (JournalEntry) -> Unit,
        onEditRequested: (JournalEntry) -> Unit,
        onDeleteRequested: (JournalEntry) -> Unit
    ) {
        Row(
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.clickable {
                onCopyRequested(item)
            }
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
                onClick = { onDeleteRequested(item) }) {
                Icon(
                    imageVector = Icons.Outlined.Delete,
                    contentDescription = "Delete",
                    tint = MaterialTheme.colorScheme.onBackground
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            FilledTonalIconButton(
                onClick = { onEditRequested(item) }) {
                Icon(
                    imageVector = Icons.Outlined.Edit,
                    contentDescription = "Edit",
                    tint = MaterialTheme.colorScheme.onBackground
                )
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


    @OptIn(ExperimentalMaterial3Api::class, ExperimentalComposeUiApi::class)
    @Composable
    private fun AddEditEntryDialog(
        initialText: String,
        onPositiveClick: (String) -> Unit,
        onNegativeClick: () -> Unit
    ) {
        var text by remember { mutableStateOf(TextFieldValue(initialText)) }

        val focusRequester = remember { FocusRequester() }
        val showKeyboard by remember { mutableStateOf(true) }
        val keyboard = LocalSoftwareKeyboardController.current

        Dialog(onDismissRequest = { }) {
            Card(modifier = Modifier.height(320.dp)) {
                Column(modifier = Modifier.padding(8.dp)) {
                    LaunchedEffect(focusRequester) {
                        if (showKeyboard) {
                            delay(100)
                            focusRequester.requestFocus()
                            keyboard?.show()
                        }
                    }
                    OutlinedTextField(
                        value = text,
                        onValueChange = { text = it },
                        keyboardOptions = KeyboardOptions(
                            capitalization = KeyboardCapitalization.Sentences
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .focusRequester(focusRequester = focusRequester)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(
                        horizontalArrangement = Arrangement.End,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        TextButton(onClick = {
                            onNegativeClick()
                        }) {
                            Text(text = stringResource(id = R.string.cancel))
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        TextButton(onClick = {
                            onPositiveClick(text.text)
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

fun Context.shutdown() {
    val activity = getActivity()
    val intent =
        activity?.packageManager?.getLaunchIntentForPackage(activity.packageName)
    activity?.finishAffinity()
    activity?.startActivity(intent)
    exitProcess(0)
}

enum class JournalMenuItem(val id: Int, @StringRes val textResId: Int) {
    UPLOAD(1, R.string.button_text_upload_all),
    REFRESH(2, R.string.button_text_refresh),
    SET_SERVER(3, R.string.button_text_set_server),
    SERVER_SET(3, R.string.button_text_server_set),
    RESTART(4, R.string.button_text_restart),
    DELETE_ALL(5, R.string.button_text_delete)
}