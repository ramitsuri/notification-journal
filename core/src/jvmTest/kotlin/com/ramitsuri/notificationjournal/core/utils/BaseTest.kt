package com.ramitsuri.notificationjournal.core.utils

import com.ramitsuri.notificationjournal.core.data.AppDatabase
import com.ramitsuri.notificationjournal.core.data.getTestDb
import com.ramitsuri.notificationjournal.core.di.DiFactory
import com.ramitsuri.notificationjournal.core.di.ServiceLocator
import com.ramitsuri.notificationjournal.core.repository.JournalRepository
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import java.nio.file.Paths
import kotlin.io.path.deleteRecursively
import kotlin.time.Clock

abstract class BaseTest {
    lateinit var dataSendHelper: TestDataSendHelper
    lateinit var clock: Clock
    lateinit var db: AppDatabase
    lateinit var prefManager: PrefManager
    lateinit var repository: JournalRepository

    @Before
    fun before() =
        runTest {
            db = getTestDb()
            dataSendHelper = TestDataSendHelper()
            clock = TestClock()
            prefManager =
                PrefManager(
                    testJson,
                    getTestDataKeyValueStore(),
                )
            repository =
                JournalRepository(
                    dao = db.journalEntryDao(),
                    conflictDao = db.entryConflictDao(),
                    clock = clock,
                    dataSendHelper = dataSendHelper,
                    prefManager = prefManager,
                )

            ServiceLocator.init(DiFactory())
        }

    @After
    fun after() {
        Paths.get(Constants.BASE_DIR).deleteRecursively()
    }
}
