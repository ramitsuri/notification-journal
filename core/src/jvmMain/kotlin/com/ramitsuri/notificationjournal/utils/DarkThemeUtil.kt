import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.jthemedetecor.OsThemeDetector
import org.jetbrains.skiko.SystemTheme
import org.jetbrains.skiko.currentSystemTheme

@Composable
actual fun isDarkMode(): Boolean {
    var darkTheme by remember {
        mutableStateOf(currentSystemTheme == SystemTheme.DARK)
    }

    DisposableEffect(Unit) {
        val darkThemeListener: (Boolean) -> Unit = {
            darkTheme = it
        }

        val detector = OsThemeDetector.getDetector().apply {
            registerListener(darkThemeListener)
        }

        onDispose {
            detector.removeListener(darkThemeListener)
        }
    }

    return darkTheme
}
