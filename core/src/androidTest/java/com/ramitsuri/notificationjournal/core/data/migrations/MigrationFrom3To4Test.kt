package com.ramitsuri.notificationjournal.core.data.migrations

import androidx.room.testing.MigrationTestHelper
import androidx.room.util.TableInfo
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.ramitsuri.notificationjournal.core.data.AppDatabase
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MigrationFrom3To4Test {
    private val testDb = "migration-test"

    @get:Rule
    val helper: MigrationTestHelper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        AppDatabase::class.java,
        listOf(),
        FrameworkSQLiteOpenHelperFactory(),
    )

    @Test
    fun testMigrateFrom3To4() {
        try {
            // Arrange
            createDbVersionThree()

            // Act
            val dbV4 = migrateAndDbVersionFour()
            val tableInfo = TableInfo.read(dbV4, "JournalEntryTemplate")

            // Assert
            assertTrue(tableInfo.columns["id"]!!.isPrimaryKey)
            assertTrue(tableInfo.columns["id"]!!.notNull)
            assertTrue(tableInfo.columns["text"]!!.notNull)
            assertTrue(tableInfo.columns["tag"]!!.notNull)
        } catch (e: Exception) {
            fail(e.message)
        }
    }

    private fun migrateAndDbVersionFour(): SupportSQLiteDatabase {
        return helper.runMigrationsAndValidate(testDb, 4, true, MigrationFrom3To4())
    }

    private fun createDbVersionThree() {
        helper.createDatabase(testDb, 3)
    }
}