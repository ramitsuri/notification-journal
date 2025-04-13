package com.ramitsuri.notificationjournal.core.ui.components

import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier

@Composable
fun CyclicViewPager(
    initialPage: Int,
    pageCount: Int,
    modifier: Modifier = Modifier,
    isScrollInProgress: (Boolean) -> Unit,
    // When page change has finished, used to show the actual content of the page
    onActualPageChange: (Int) -> Unit,
    // Page change when swiping is in progress, used to show content mid scroll
    onPageChange: @Composable (Int) -> Unit,
) {
    val totalPages = pageCount * 10
    val pagerState = rememberPagerState(initialPage = (totalPages / 2) + initialPage, pageCount = { totalPages })
    LaunchedEffect(Unit) {
        snapshotFlow { pagerState.currentPage }.collect { currentPage ->
            onActualPageChange(currentPage % (pageCount))
        }
    }
    LaunchedEffect(Unit) {
        snapshotFlow { pagerState.isScrollInProgress }.collect {
            isScrollInProgress(it)
        }
    }
    HorizontalPager(
        modifier = modifier,
        state = pagerState,
    ) { page ->
        onPageChange(page % (pageCount))
    }
}
