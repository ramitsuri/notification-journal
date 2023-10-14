package com.ramitsuri.notificationjournal.core.data.migrations

import androidx.room.testing.MigrationTestHelper
import androidx.room.util.TableInfo
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.ramitsuri.notificationjournal.core.data.AppDatabase
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MigrationFrom2To3Test {
    private val testDb = "migration-test"

    @get:Rule
    val helper: MigrationTestHelper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        AppDatabase::class.java,
        listOf(),
        FrameworkSQLiteOpenHelperFactory(),
    )

    @Test
    fun testMigrateFrom2To3() {
        try {
            // Arrange
            createDbVersionTwo()

            // Act
            val dbV3 = migrateAndDbVersionThree()
            val tableInfo = TableInfo.read(dbV3, "Tags")

            // Assert
            assertTrue(tableInfo.columns["id"]!!.isPrimaryKey)
            assertTrue(tableInfo.columns["id"]!!.notNull)
            assertTrue(tableInfo.columns["order"]!!.notNull)
            assertTrue(tableInfo.columns["value"]!!.notNull)
            val index = tableInfo.indices!!.first { it.name == "index_Tags_value" }
            assertNotNull(index)
            assertTrue(index.unique)
            assertTrue(index.columns.contains("value"))

        } catch (e: Exception) {
            fail(e.message)
        }

    }

    private fun migrateAndDbVersionThree(): SupportSQLiteDatabase {
        return helper.runMigrationsAndValidate(testDb, 3, true, MigrationFrom2To3())
    }

    private fun createDbVersionTwo() {
        helper.createDatabase(testDb, 2)
    }
}