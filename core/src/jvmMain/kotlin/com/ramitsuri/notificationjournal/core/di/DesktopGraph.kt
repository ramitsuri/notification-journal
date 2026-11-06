package com.ramitsuri.notificationjournal.core.di

import dev.zacsweers.metro.DependencyGraph
import dev.zacsweers.metro.SingleIn

@SingleIn(AppScope::class)
@DependencyGraph(AppScope::class)
interface DesktopGraph : CoreGraph
