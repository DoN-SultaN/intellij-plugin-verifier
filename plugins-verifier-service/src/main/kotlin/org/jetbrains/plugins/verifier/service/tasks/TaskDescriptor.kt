/*
 * Copyright 2000-2020 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.plugins.verifier.service.tasks

import com.jetbrains.plugin.structure.base.utils.formatDuration
import org.jetbrains.plugins.verifier.service.tasks.TaskDescriptor.State
import org.jetbrains.plugins.verifier.service.tasks.TaskDescriptor.State.*
import java.time.Duration
import java.time.Instant

/**
 * Descriptor of a [task] [Task] being executed.
 */
data class TaskDescriptor(
  /**
   * Unique ID of the task that can be
   * used to identify the task in the tasks queue.
   */
  val taskId: Long,

  /**
   * Presentable name of the task that
   * can be shown on the status page.
   */
  val presentableName: String,

  /**
   * Progress indicator that can be
   * used to monitor and control the execution
   * of the task.
   */
  val progress: ProgressIndicator,

  /**
   * Point in time when the task was started.
   */
  val startTime: Instant,

  /**
   * Point in time when the task was completed,
   * either successfully or abnormally.
   *
   * `null` if the task is not completed yet.
   */
  @Volatile
  var endTime: Instant?,

  /**
   * [State] [State] of the task.
   */
  @Volatile
  var state: State
) {

  /**
   * If the task is completed, returns the total execution time,
   * otherwise returns the amount of time since the task has started.
   */
  val elapsedTime: Duration
    get() = Duration.between(startTime, endTime ?: Instant.now())

  override fun toString(): String = buildString {
    append("(")
    append("Id=$taskId; ")
    append("State=$state; ")
    append("Time=${elapsedTime.formatDuration()}; ")
    append("Progress=" + progress.fraction + "; ")
    append("Text=" + progress.text + "; ")
    append("Task-name=$presentableName")
    append(")")
  }

  override fun equals(other: Any?) = other is TaskDescriptor && taskId == other.taskId

  override fun hashCode() = taskId.hashCode()

  /**
   * Represents state of a [task] [Task].
   *
   * Initially, the task is in the [waiting] [WAITING] queue.
   * Then the task is [running] [RUNNING].
   * Finally its result is either [success] [SUCCESS],
   * [failure] [ERROR] or [cancelled] [CANCELLED]
   */
  enum class State {
    WAITING,

    RUNNING,

    SUCCESS,

    ERROR,

    CANCELLED
  }

}