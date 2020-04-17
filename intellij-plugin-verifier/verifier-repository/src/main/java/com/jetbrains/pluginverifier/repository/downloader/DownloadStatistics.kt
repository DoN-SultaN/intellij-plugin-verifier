/*
 * Copyright 2000-2020 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.jetbrains.pluginverifier.repository.downloader

import com.jetbrains.pluginverifier.repository.cleanup.SpaceAmount
import java.time.Duration
import java.time.Instant
import java.util.*

/**
 * Accumulates downloading events and provides statistics.
 */
class DownloadStatistics {

  private val events = Collections.synchronizedList(arrayListOf<DownloadEvent>())

  @Synchronized
  private fun reportEvent(downloadEvent: DownloadEvent) {
    events += downloadEvent
  }

  fun getTotalDownloadedAmount(): SpaceAmount =
    events.fold(SpaceAmount.ZERO_SPACE) { acc, event ->
      acc + event.downloadedAmount
    }

  fun getTotalAstronomicalDownloadDuration(): Duration {
    //Use sweep line algorithm, because events may intersect.
    val startEvents = events.groupBy { it.startInstant }
    val endEvents = events.groupBy { it.endInstant }
    val allInstants = (events.map { it.startInstant } + events.map { it.endInstant }).sorted()
    var totalDuration = Duration.ZERO
    val activeEvents = hashSetOf<DownloadEvent>()
    for (i in 0 until allInstants.size - 1) {
      val segmentStart = allInstants[i]
      val segmentEnd = allInstants[i + 1]

      activeEvents -= endEvents[segmentStart].orEmpty()
      activeEvents += startEvents[segmentStart].orEmpty()

      if (activeEvents.isNotEmpty()) {
        totalDuration += Duration.between(segmentStart, segmentEnd)
      }
    }
    return totalDuration
  }

  fun downloadStarted(): DownloadEvent =
    DownloadEvent(Instant.now())

  inner class DownloadEvent(
    internal val startInstant: Instant,
    internal var endInstant: Instant = Instant.EPOCH,
    internal var downloadedAmount: SpaceAmount = SpaceAmount.ZERO_SPACE
  ) {
    @Synchronized
    fun downloadEnded(downloadedAmount: SpaceAmount) {
      this.endInstant = Instant.now()
      this.downloadedAmount = downloadedAmount
      reportEvent(this)
    }
  }
}