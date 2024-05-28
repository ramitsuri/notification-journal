package com.ramitsuri.notificationjournal.core.data.migrations

import androidx.room.migration.Migration
import androidx.room.testing.MigrationTestHelper
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import com.ramitsuri.notificationjournal.core.data.AppDatabase
import org.junit.After
import org.junit.Rule
import kotlin.io.path.ExperimentalPathApi
import kotlin.io.path.Path
import kotlin.io.path.deleteRecursively

open class BaseMigrationTest {
    @get:Rule
    val helper: MigrationTestHelper = MigrationTestHelper(
        schemaDirectoryPath = Path("schemas"),
        databasePath = Path("test/migration-test"),
        driver = BundledSQLiteDriver(),
        databaseClass = AppDatabase::class,
    )

    @OptIn(ExperimentalPathApi::class)
    @After
    fun tearDown() {
        Path("test").deleteRecursively()
    }

    fun createDatabase(version: Int): SQLiteConnection {
        return helper.createDatabase(version)
    }

    fun runMigrationAndValidate(version: Int, migration: Migration): SQLiteConnection {
        return helper.runMigrationsAndValidate(version, listOf(migration))
    }
}