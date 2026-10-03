package com.ramitsuri.notificationjournal.core.ui.journalentry

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.updateTransition
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.CalendarToday
import androidx.compose.material.icons.outlined.Link
import androidx.compose.material.icons.outlined.LinkOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularWavyProgressIndicator
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isMetaPressed
import androidx.compose.ui.input.key.isShiftPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.platform.ClipboardManager
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.ramitsuri.notificationjournal.core.model.DayGroup
import com.ramitsuri.notificationjournal.core.model.EntryConflict
import com.ramitsuri.notificationjournal.core.model.Tag
import com.ramitsuri.notificationjournal.core.model.entry.JournalEntry
import com.ramitsuri.notificationjournal.core.ui.components.DayGroupAction
import com.ramitsuri.notificationjournal.core.ui.components.JournalEntryDay
import com.ramitsuri.notificationjournal.core.ui.components.JournalEntryDayConfig
import com.ramitsuri.notificationjournal.core.utils.dayMonthDate
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDate
import notificationjournal.core.generated.resources.Res
import notificationjournal.core.generated.resources.add_entry_content_description
import notificationjournal.core.generated.resources.alert
import notificationjournal.core.generated.resources.back
import notificationjournal.core.generated.resources.cancel
import notificationjournal.core.generated.resources.delete_warning_message
import notificationjournal.core.generated.resources.no_items
import notificationjournal.core.generated.resources.ok
import notificationjournal.core.generated.resources.search
import notificationjournal.core.generated.resources.settings
import notificationjournal.core.generated.resources.sync_up
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.resources.vectorResource
import kotlin.math.absoluteValue

@Composable
fun JournalEntryScreen(
    state: ViewState,
    dates: List<LocalDate>,
    selectedDate: LocalDate,
    onDateSettled: (LocalDate) -> Unit,
    showBackButton: Boolean,
    showContent: Boolean,
    onEntryScreenAction: (EntryScreenAction) -> Unit,
    onDayGroupAction: (DayGroupAction) -> Unit,
) {
    var journalEntryForDelete: JournalEntry? by rememberSaveable { mutableStateOf(null) }
    val clipboardManager: ClipboardManager = LocalClipboardManager.current

    val focusManager = LocalFocusManager.current
    val coroutineScope = rememberCoroutineScope()
    val dayPages = remember(dates) { dates.toDayPages() }
    val pagerState = rememberDayPagerState(dayPages, selectedDate, onDateSettled)

    // The view needs to be focussed for it to receive keyboard events
    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(focusRequester) {
        focusRequester.requestFocus()
    }

    LaunchedEffect(state.contentForCopy) {
        if (state.contentForCopy.isNotEmpty()) {
            clipboardManager.setText(AnnotatedString(state.contentForCopy))
            onEntryScreenAction(EntryScreenAction.Copy)
        }
    }

    if (journalEntryForDelete != null) {
        DeleteDialog(
            journalEntryForDelete = journalEntryForDelete,
            onDismiss = { journalEntryForDelete = null },
            onDayGroupAction = onDayGroupAction,
        )
    }

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        modifier =
            Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .focusRequester(focusRequester)
                .focusable()
                .onKeyEvent {
                    if (
                        it.isMetaPressed &&
                        it.key == Key.N &&
                        it.isShiftPressed &&
                        it.type == KeyEventType.KeyUp
                    ) {
                        onEntryScreenAction(EntryScreenAction.AddWithDate(null))
                        true
                    } else if (
                        it.isMetaPressed &&
                        it.key == Key.N &&
                        !it.isShiftPressed &&
                        it.type == KeyEventType.KeyUp
                    ) {
                        if (state.dayGroup == null) {
                            false
                        } else {
                            onEntryScreenAction(EntryScreenAction.AddWithDate(state.dayGroup.date))
                            true
                        }
                    } else if (
                        it.isMetaPressed &&
                        it.key == Key.Comma &&
                        it.type == KeyEventType.KeyUp
                    ) {
                        onEntryScreenAction(EntryScreenAction.NavToSettings)
                        true
                    } else if (it.key == Key.DirectionDown &&
                        it.type == KeyEventType.KeyDown
                    ) {
                        focusManager.moveFocus(FocusDirection.Down)
                    } else if (it.key == Key.DirectionUp &&
                        it.type == KeyEventType.KeyDown
                    ) {
                        focusManager.moveFocus(FocusDirection.Up)
                    } else if ((it.key == Key.J || it.key == Key.DirectionLeft) &&
                        it.type == KeyEventType.KeyDown
                    ) {
                        coroutineScope.launch { pagerState.animateScrollBy(pages = -1, dayPages = dayPages) }
                        true
                    } else if ((it.key == Key.K || it.key == Key.DirectionRight) &&
                        it.type == KeyEventType.KeyDown
                    ) {
                        coroutineScope.launch { pagerState.animateScrollBy(pages = 1, dayPages = dayPages) }
                        true
                    } else if (it.key == Key.S &&
                        it.type == KeyEventType.KeyDown
                    ) {
                        onEntryScreenAction(EntryScreenAction.Sync)
                        true
                    } else {
                        false
                    }
                },
        floatingActionButton = {
            FloatingActionButton(
                modifier = Modifier.padding(bottom = 32.dp),
                onClick = { onEntryScreenAction(EntryScreenAction.AddWithDate()) },
            ) {
                Icon(
                    Icons.Filled.Add,
                    stringResource(Res.string.add_entry_content_description),
                )
            }
        },
    ) { paddingValues ->
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .consumeWindowInsets(paddingValues)
                    .windowInsetsPadding(
                        WindowInsets.safeDrawing.only(
                            WindowInsetsSides.Horizontal,
                        ),
                    ),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            val scrollBehavior =
                TopAppBarDefaults.enterAlwaysScrollBehavior(
                    rememberTopAppBarState(),
                )

            Toolbar(
                showBackButton = showBackButton,
                isConnected = state.isConnected,
                notUploadedCount = state.notUploadedCount,
                onViewByDate = { onEntryScreenAction(EntryScreenAction.NavToViewJournalEntryDay) },
                onBackClick = { onEntryScreenAction(EntryScreenAction.NavBack) },
                onSyncClicked = { onEntryScreenAction(EntryScreenAction.Sync) },
                onSettingsClicked = { onEntryScreenAction(EntryScreenAction.NavToSettings) },
                onSearchClicked = { onEntryScreenAction(EntryScreenAction.NavToSearch) },
                onResetReceiveHelper = { onEntryScreenAction(EntryScreenAction.ResetReceiveHelper) },
                scrollBehavior = scrollBehavior,
            )

            if (dates.isEmpty()) {
                Column(
                    modifier =
                        Modifier
                            .fillMaxSize()
                            .navigationBarsPadding()
                            .padding(start = 16.dp, end = 16.dp, bottom = 64.dp),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    CircularWavyProgressIndicator()
                }
            } else {
                HorizontalPager(
                    state = pagerState,
                    key = { page -> dayPages[page].key },
                    modifier = Modifier.fillMaxSize(),
                ) { page ->
                    val date = dayPages[page].date
                    // Only the selected date's data is loaded. Other pages (the ones peeking in while
                    // swiping, or the new page until its data is loaded) show the date instead.
                    val dayGroup = state.dayGroup?.takeIf { it.date == date }
                    // Only crossfade between placeholder and content, not on every data update
                    updateTransition(targetState = dayGroup).Crossfade(
                        contentKey = { it == null },
                        modifier = Modifier.fillMaxSize(),
                    ) { loadedDayGroup ->
                        when {
                            loadedDayGroup == null -> {
                                DatePlaceholder(date = date)
                            }

                            loadedDayGroup.tagGroups.isEmpty() -> {
                                Column(
                                    modifier =
                                        Modifier
                                            .fillMaxSize()
                                            .navigationBarsPadding()
                                            .padding(start = 16.dp, end = 16.dp, bottom = 64.dp),
                                    verticalArrangement = Arrangement.Center,
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                ) {
                                    Text(
                                        text = stringResource(Res.string.no_items),
                                        style = MaterialTheme.typography.displaySmall,
                                    )
                                }
                            }

                            else -> {
                                Column(modifier = Modifier.fillMaxSize()) {
                                    List(
                                        dayGroup = loadedDayGroup,
                                        conflictCount = state.dayGroupConflictCount,
                                        conflicts = state.entryConflicts,
                                        tags = state.tags,
                                        showEmptyTags = state.showEmptyTags,
                                        showConflictDiffInline = state.showConflictDiffInline,
                                        allowNotify = state.allowNotify,
                                        scrollConnection = scrollBehavior.nestedScrollConnection,
                                        showContent = showContent,
                                        onAction = { action ->
                                            when (action) {
                                                is DayGroupAction.DeleteEntry -> {
                                                    journalEntryForDelete = action.entry
                                                }

                                                else -> onDayGroupAction(action)
                                            }
                                        },
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * A page in the day pager. To allow wrapping around, when there are at least 2 dates, the pages are
 * `[last*, first, ..., last, first*]` where `*` marks a copy. Settling on a copy jumps to the real
 * page of the same date, which has neighbors on both sides.
 */
private data class DayPage(
    val date: LocalDate,
    val isCopy: Boolean,
) {
    // Strings so that they can be saved by the pager
    val key: String = if (isCopy) "copy-$date" else "day-$date"
}

private fun List<LocalDate>.toDayPages(): List<DayPage> {
    val pages = map { DayPage(date = it, isCopy = false) }
    if (size < 2) {
        return pages
    }
    return listOf(DayPage(date = last(), isCopy = true)) + pages + DayPage(date = first(), isCopy = true)
}

private fun List<DayPage>.indexOfRealPage(date: LocalDate): Int = indexOfFirst { !it.isCopy && it.date == date }

/**
 * Keeps the pager and [selectedDate] in sync in both directions:
 * - When the user settles on a page, [onDateSettled] is called with that page's date. If that page is
 *   a copy, first jumps to the real page for that date.
 * - When [selectedDate] changes from elsewhere (list pane, reconcile, deep link), the pager scrolls to it.
 */
@Composable
private fun rememberDayPagerState(
    dayPages: List<DayPage>,
    selectedDate: LocalDate,
    onDateSettled: (LocalDate) -> Unit,
): PagerState {
    val currentDayPages by rememberUpdatedState(dayPages)
    val currentOnDateSettled by rememberUpdatedState(onDateSettled)
    val pagerState =
        rememberPagerState(
            initialPage = dayPages.indexOfRealPage(selectedDate).coerceAtLeast(0),
        ) { currentDayPages.size }

    LaunchedEffect(pagerState) {
        snapshotFlow {
            // Read the key from the measured pages rather than indexing into pages, so that the
            // page always matches what's on screen even while dates are changing.
            val settledPage = pagerState.settledPage
            val key =
                pagerState.layoutInfo.visiblePagesInfo
                    .firstOrNull { it.index == settledPage }
                    ?.key
            currentDayPages.firstOrNull { it.key == key }
        }
            .filterNotNull()
            .distinctUntilChanged()
            .collect { page ->
                if (page.isCopy) {
                    // Invisible jump as both pages show the same date. Settling on the real page
                    // will emit again and report the date.
                    val realPage = currentDayPages.indexOfRealPage(page.date)
                    if (realPage != -1) {
                        pagerState.scrollToPage(realPage)
                    }
                } else {
                    currentOnDateSettled(page.date)
                }
            }
    }

    LaunchedEffect(selectedDate, dayPages) {
        val targetPage = dayPages.indexOfRealPage(selectedDate)
        if (targetPage == -1 || pagerState.isScrollInProgress) {
            // Scroll in progress means the user (or keyboard) is moving the pager; it will report
            // back once it settles.
            return@LaunchedEffect
        }
        if (dayPages.getOrNull(pagerState.currentPage)?.date == selectedDate) {
            // Already showing it (possibly on a copy, which jumps to the real page on its own)
            return@LaunchedEffect
        }
        if ((targetPage - pagerState.currentPage).absoluteValue > 1) {
            pagerState.scrollToPage(targetPage)
        } else {
            pagerState.animateScrollToPage(targetPage)
        }
    }
    return pagerState
}

/**
 * Moves [pages] away from where the pager is headed (not where it currently is), so that repeated key
 * presses while an animation is running keep moving forward instead of restarting from the same page.
 */
private suspend fun PagerState.animateScrollBy(
    pages: Int,
    dayPages: List<DayPage>,
) {
    var from = targetPage
    val fromPage = dayPages.getOrNull(from)
    if (fromPage?.isCopy == true) {
        // Headed to a copy, jump to its real page first so that we don't run into the end
        val realPage = dayPages.indexOfRealPage(fromPage.date)
        if (realPage != -1) {
            scrollToPage(realPage)
            from = realPage
        }
    }
    val target = (from + pages).coerceIn(0, (dayPages.size - 1).coerceAtLeast(0))
    if (target != from) {
        animateScrollToPage(target, animationSpec = tween(durationMillis = 250, easing = FastOutSlowInEasing))
    }
}

@Composable
private fun DatePlaceholder(
    date: LocalDate,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier =
            modifier
                .fillMaxSize()
                .navigationBarsPadding()
                .padding(start = 16.dp, end = 16.dp, bottom = 64.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = dayMonthDate(toFormat = date),
            style = MaterialTheme.typography.displayLarge,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun DeleteDialog(
    journalEntryForDelete: JournalEntry?,
    onDismiss: () -> Unit,
    onDayGroupAction: (DayGroupAction) -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(text = stringResource(Res.string.alert))
        },
        text = {
            Text(stringResource(Res.string.delete_warning_message))
        },
        confirmButton = {
            TextButton(
                onClick = {
                    journalEntryForDelete?.let { forDeletion ->
                        onDayGroupAction(DayGroupAction.DeleteEntry(forDeletion))
                    }
                    onDismiss()
                },
            ) {
                Text(stringResource(Res.string.ok))
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
            ) {
                Text(stringResource(Res.string.cancel))
            }
        },
    )
}

@Composable
private fun Toolbar(
    scrollBehavior: TopAppBarScrollBehavior? = null,
    showBackButton: Boolean,
    isConnected: Boolean,
    notUploadedCount: Int,
    onViewByDate: () -> Unit,
    onBackClick: () -> Unit,
    onSyncClicked: () -> Unit,
    onSettingsClicked: () -> Unit,
    onSearchClicked: () -> Unit,
    onResetReceiveHelper: () -> Unit,
) {
    CenterAlignedTopAppBar(
        colors =
            TopAppBarDefaults
                .centerAlignedTopAppBarColors()
                .copy(scrolledContainerColor = MaterialTheme.colorScheme.background),
        title = { },
        navigationIcon = {
            if (showBackButton) {
                IconButton(
                    onClick = onBackClick,
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = stringResource(Res.string.back),
                    )
                }
            }
        },
        actions = {
            IconButton(
                onClick = onViewByDate,
                modifier =
                    Modifier
                        .size(48.dp)
                        .padding(4.dp),
            ) {
                Icon(
                    imageVector = Icons.Outlined.CalendarToday,
                    contentDescription = null,
                )
            }
            if (notUploadedCount > 0) {
                Box(
                    modifier =
                        Modifier
                            .height(48.dp)
                            .padding(4.dp)
                            .clip(CircleShape)
                            .clickable(onClick = onSyncClicked),
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.align(Alignment.Center),
                    ) {
                        Text("$notUploadedCount", style = MaterialTheme.typography.labelMedium)
                        Icon(
                            imageVector = vectorResource(Res.drawable.sync_up),
                            contentDescription = null,
                        )
                    }
                }
            }
            IconButton(
                onClick = onResetReceiveHelper,
                modifier =
                    Modifier
                        .size(48.dp)
                        .padding(4.dp),
            ) {
                Icon(
                    imageVector = if (isConnected) Icons.Outlined.Link else Icons.Outlined.LinkOff,
                    contentDescription = null,
                )
            }
            IconButton(
                onClick = onSearchClicked,
                modifier =
                    Modifier
                        .size(48.dp)
                        .padding(4.dp),
            ) {
                Icon(
                    imageVector = Icons.Filled.Search,
                    contentDescription = stringResource(Res.string.search),
                )
            }
            IconButton(
                onClick = onSettingsClicked,
                modifier =
                    Modifier
                        .size(48.dp)
                        .padding(4.dp),
            ) {
                Icon(
                    imageVector = Icons.Filled.Settings,
                    contentDescription = stringResource(Res.string.settings),
                )
            }
        },
        scrollBehavior = scrollBehavior,
    )
}

@Composable
private fun List(
    dayGroup: DayGroup,
    conflictCount: Int,
    conflicts: List<EntryConflict>,
    tags: List<Tag>,
    showEmptyTags: Boolean,
    showConflictDiffInline: Boolean,
    allowNotify: Boolean,
    modifier: Modifier = Modifier,
    scrollConnection: NestedScrollConnection,
    showContent: Boolean,
    onAction: (DayGroupAction) -> Unit,
) {
    JournalEntryDay(
        dayGroup = dayGroup,
        tags = tags,
        conflictCount = conflictCount,
        conflicts = conflicts,
        scrollConnection = scrollConnection,
        showEmptyTags = showEmptyTags,
        showConflictDiffInline = showConflictDiffInline,
        onAction = onAction,
        config = JournalEntryDayConfig.allEnabled,
        allowNotify = allowNotify,
        showContent = showContent,
        modifier = modifier,
    )
}
