package com.ramitsuri.notificationjournal.service

import com.ramitsuri.notificationjournal.core.work.WorkBridgeController
import org.junit.Assert.assertEquals
import org.junit.Test

class WorkBridgeServiceTest {
    @Test
    fun testActionConstants() {
        assertEquals("com.ramitsuri.notificationjournal.action.WORK_BRIDGE_START", WorkBridgeService.ACTION_START)
        assertEquals("com.ramitsuri.notificationjournal.action.WORK_BRIDGE_CONNECT", WorkBridgeService.ACTION_CONNECT)
        assertEquals(
            "com.ramitsuri.notificationjournal.action.WORK_BRIDGE_RECONNECT",
            WorkBridgeService.ACTION_RECONNECT,
        )
        assertEquals("com.ramitsuri.notificationjournal.action.WORK_BRIDGE_STOP", WorkBridgeService.ACTION_STOP)
        assertEquals("work_bridge", WorkBridgeService.CHANNEL_ID)
        assertEquals(2001, WorkBridgeService.NOTIFICATION_ID)
        assertEquals(2002, WorkBridgeService.REQUEST_CODE_STOP)
        assertEquals(2003, WorkBridgeService.REQUEST_CODE_REPAIR)
    }

    @Test
    fun testInitialServiceStateIsStopped() {
        assertEquals(WorkBridgeController.State.Stopped, WorkBridgeService.serviceState.value)
    }
}
