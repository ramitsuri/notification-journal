package com.ramitsuri.notificationjournal.core.ui.nav

import androidx.navigation.NavDeepLink
import androidx.navigation.navDeepLink

fun DeepLink.uriWithArgsValues(args: List<String> = listOf()): String {
    return when (this) {
        DeepLink.REMINDER -> {
            uri.plus("/").plus(args.joinToString("/"))
        }

        DeepLink.HOME_SCREEN -> {
            uri
        }

        DeepLink.ADD_ENTRY -> {
            uri
        }
    }
}

fun DeepLink.uriWithArgNames(): NavDeepLink {
    val pattern =
        when (this) {
            DeepLink.REMINDER -> {
                uri.plus("/").plus("{${Args.JOURNAL_ENTRY_ID}}")
            }

            DeepLink.HOME_SCREEN -> {
                uri
            }

            DeepLink.ADD_ENTRY -> {
                uri
            }
        }
    return navDeepLink {
        uriPattern = pattern
    }
}
