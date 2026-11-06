package com.ramitsuri.notificationjournal.core.log

import co.touchlab.kermit.Logger
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class InMemoryLogWriterTest {
    @Test
    fun testLogReceivesMessageAndTag() =
        runTest {
            val writer = InMemoryLogWriter()
            Logger.setLogWriters(writer)

            Logger.i("TestTag") { "Hello world" }

            val logs = writer.logs.first { it.isNotEmpty() }
            assertEquals(1, logs.size)
            assertEquals("TestTag", logs[0].tag)
            assertEquals("Hello world", logs[0].message)

            val tags = writer.getTags().first()
            assertTrue(tags.contains("TestTag"))

            writer.clear()
            val clearedLogs = writer.logs.first()
            assertTrue(clearedLogs.isEmpty())
        }
}
