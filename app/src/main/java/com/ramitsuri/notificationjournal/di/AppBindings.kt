import android.app.Application
import com.ramitsuri.notificationjournal.core.di.AppScope
import com.ramitsuri.notificationjournal.core.di.PlatformConfig
import com.ramitsuri.notificationjournal.core.model.entry.JournalEntry
import com.ramitsuri.notificationjournal.core.utils.JournalEntryNotificationHelper
import com.ramitsuri.notificationjournal.work.ShowNotificationWorker
import dev.zacsweers.metro.BindingContainer
import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.Provides
import dev.zacsweers.metro.SingleIn
import kotlin.time.Duration

@ContributesTo(AppScope::class)
@BindingContainer
object AppBindings {
    @Provides
    @SingleIn(AppScope::class)
    fun providePlatformConfig(): PlatformConfig =
        PlatformConfig(
            isDesktop = false,
            allowJournalImport = false,
            actsAsServer = false,
            allowJournalEntryNotify = true,
            ipAddress = null,
        )

    @Provides
    @SingleIn(AppScope::class)
    fun provideJournalEntryNotificationHelper(application: Application): JournalEntryNotificationHelper? =
        object : JournalEntryNotificationHelper {
            override fun show(
                journalEntry: JournalEntry,
                showIn: Duration,
            ) {
                ShowNotificationWorker.enqueue(application, journalEntry.id, showIn)
            }
        }
}
