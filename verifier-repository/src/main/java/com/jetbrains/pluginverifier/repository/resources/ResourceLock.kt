package com.jetbrains.pluginverifier.repository.resources

import java.io.Closeable
import java.time.Duration
import java.time.Instant

/**
 * Resource lock is used to indicate that a [resource]
 * stored in the [repository] [ResourceRepository] is used at
 * the moment, thus it cannot be removed
 * until the lock is [released] [release] by the lock owner.
 */
abstract class ResourceLock<out R, W : ResourceWeight<W>>(
    /**
     * The point in the time when the resource was locked
     */
    val lockTime: Instant,

    /**
     * The descriptor of the locked resource.
     */
    val resourceInfo: ResourceInfo<R, W>,

    /**
     * Amount of time spent on fetching the resource.
     */
    val fetchDuration: Duration

) : Closeable {

  /**
   * The locked resource.
   */
  val resource: R
    get() = resourceInfo.resource

  /**
   * The [weight] [ResourceWeight] of the locked resource.
   */
  val resourceWeight: W
    get() = resourceInfo.weight

  /**
   * Releases the lock in the [repository] [ResourceRepository].
   *
   * If there are no more locks of the [resource], the resource
   * can be safely removed.
   */
  abstract fun release()

  /**
   * The close method allows to use the try-with-resources expression.
   */
  final override fun close() = release()

}