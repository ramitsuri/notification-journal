package com.ramitsuri.notificationjournal

import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.DisposableEffect
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.core.view.WindowCompat
import com.ramitsuri.notificationjournal.core.deeplink.DeepLinkMatcher
import com.ramitsuri.notificationjournal.core.deeplink.DeepLinkPattern
import com.ramitsuri.notificationjournal.core.deeplink.DeepLinkRequest
import com.ramitsuri.notificationjournal.core.deeplink.KeyDecoder
import com.ramitsuri.notificationjournal.core.deeplink.deepLinksWithArgNames
import com.ramitsuri.notificationjournal.core.ui.nav.NavGraph
import com.ramitsuri.notificationjournal.core.ui.nav.Route
import com.ramitsuri.notificationjournal.core.ui.theme.NotificationJournalTheme

class MainActivity : ComponentActivity() {
    private val deepLinkPatterns: List<DeepLinkPattern<out Route>> = Route.deepLinksWithArgNames

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)

        WindowCompat.setDecorFitsSystemWindows(window, false)
        enableEdgeToEdge()
        showNotification()

        val deepLinkRoute = routeFromDeepLink()

        setContent {
            val darkTheme = isSystemInDarkTheme()
            DisposableEffect(darkTheme) {
                enableEdgeToEdge(
                    statusBarStyle =
                        SystemBarStyle.auto(
                            Color.TRANSPARENT,
                            Color.TRANSPARENT,
                        ) { darkTheme },
                    navigationBarStyle =
                        SystemBarStyle.auto(
                            lightScrim,
                            darkScrim,
                        ) { darkTheme },
                )
                onDispose {}
            }

            NotificationJournalTheme(
                dynamicDarkColorScheme = dynamicDarkColorScheme(this),
                dynamicLightColorScheme = dynamicLightColorScheme(this),
            ) {
                NavGraph(
                    startRoute = deepLinkRoute,
                )
            }
        }
    }

    private fun routeFromDeepLink(): Route? {
        val uri: Uri? = intent.data
        intent.data = null
        return uri?.let {
            val request = DeepLinkRequest(uri)
            val match =
                deepLinkPatterns.firstNotNullOfOrNull { pattern ->
                    DeepLinkMatcher(request, pattern).match()
                }

            match?.let {
                KeyDecoder(match.args)
                    .decodeSerializableValue(match.serializer)
            }
        }
    }

    private fun showNotification() {
        (application as MainApplication).showJournalNotification()
    }
}

/**
 * The default light scrim, as defined by androidx and the platform:
 * https://cs.android.com/androidx/platform/frameworks/support/+/androidx-main:
 * activity/activity/src/main/java/androidx/activity/EdgeToEdge.kt;l=35-38;
 * drc=27e7d52e8604a080133e8b842db10c89b4482598
 */
private val lightScrim = Color.argb(0xe6, 0xFF, 0xFF, 0xFF)

/**
 * The default dark scrim, as defined by androidx and the platform:
 * https://cs.android.com/androidx/platform/frameworks/support/+/androidx-main:
 * activity/activity/src/main/java/androidx/activity/EdgeToEdge.kt;l=40-44;
 * drc=27e7d52e8604a080133e8b842db10c89b4482598
 */
private val darkScrim = Color.argb(0x80, 0x1b, 0x1b, 0x1b)
