package com.ramitsuri.notificationjournal.core.deeplink

import androidx.core.net.toUri
import com.ramitsuri.notificationjournal.core.ui.nav.Args
import com.ramitsuri.notificationjournal.core.ui.nav.Route

val Route.Companion.deepLinksWithArgNames: List<DeepLinkPattern<out Route>>
    get() =
        listOf(
            DeepLinkPattern(
                Route.JournalEntry.serializer(),
                DEEP_LINK_BASE_URL
                    .plus("entry/")
                    .plus("date/")
                    .plus("{${Args.JOURNAL_ENTRY_DATE}}/")
                    .plus("id/")
                    .plus("{${Args.JOURNAL_ENTRY_ID}}")
                    .toUri(),
            ),
            DeepLinkPattern(
                Route.JournalEntryDays.serializer(),
                DEEP_LINK_BASE_URL.plus("home").toUri(),
            ),
            DeepLinkPattern(
                Route.AddEntry.serializer(),
                DEEP_LINK_BASE_URL.plus("add_entry").toUri(),
            ),
        )

val Route.JournalEntry.reminderDeepLink: String
    get() =
        DEEP_LINK_BASE_URL
            .plus("entry/")
            .plus("date/")
            .plus("$selectedDate/")
            .plus("id/")
            .plus(highlightEntryId)

val homeDeepLink: String
    get() = DEEP_LINK_BASE_URL.plus("home")

val addEntryDeepLink: String
    get() = DEEP_LINK_BASE_URL.plus("add_entry")

private const val DEEP_LINK_BASE_URL = "notification-journal://app/"
