package org.jetbrains.plugins.verifier.service.service.verifier

import com.jetbrains.pluginverifier.misc.pluralize
import com.jetbrains.pluginverifier.repository.repositories.marketplace.UpdateInfo
import com.jetbrains.pluginverifier.results.VerificationResult
import org.jetbrains.plugins.verifier.service.service.verifier.VerificationResultFilter.Result.Ignore
import org.jetbrains.plugins.verifier.service.service.verifier.VerificationResultFilter.Result.Send
import org.slf4j.LoggerFactory
import java.time.Duration
import java.time.Instant
import java.time.temporal.ChronoUnit

/**
 * Utility class used to control which verifications
 * should actually be sent to the Plugins Repository.
 *
 * This class maintains a set of [ignoredVerifications].
 * That is possible to manually [force][forceVerificationResult]
 * verifications so that they will be sent later on.
 */
class VerificationResultFilter {

  companion object {
    private const val RECHECK_ATTEMPTS = 3

    private val RECHECK_ATTEMPT_TIMEOUT = Duration.of(10, ChronoUnit.MINUTES)

    private val LOG = LoggerFactory.getLogger(VerificationResultFilter::class.java)
  }

  /**
   * Set of plugins for which verifications reported "Not found" or "Non downloadable".
   *
   * We will retry to verify failed plugins up to [RECHECK_ATTEMPTS] times
   * with timeout of [RECHECK_ATTEMPT_TIMEOUT] in order to mitigate
   * accidental failures of the Marketplace.
   */
  private val failedPlugins = hashMapOf<UpdateInfo, VerificationAttempt>()

  private val forcedVerifications = hashSetOf<ScheduledVerification>()

  private val _ignoredVerifications = hashMapOf<ScheduledVerification, Result.Ignore>()

  val ignoredVerifications: Map<ScheduledVerification, Result.Ignore>
    @Synchronized
    get() = _ignoredVerifications

  /**
   * Determines whether we have to ignore the [scheduledVerification].
   */
  @Synchronized
  fun shouldIgnoreVerification(
      scheduledVerification: ScheduledVerification,
      now: Instant
  ): Boolean {
    /**
     * We allow forced verifications.
     */
    if (scheduledVerification in forcedVerifications) {
      return false
    }

    /**
     * Check if this verification is ignored.
     */
    if (_ignoredVerifications.containsKey(scheduledVerification)) {
      return true
    }

    /**
     * Check if this plugin was failed to be checked and it is too early to recheck.
     */
    val failedAttempt = failedPlugins[scheduledVerification.updateInfo]
    return failedAttempt != null && failedAttempt.lastVerified.plus(RECHECK_ATTEMPT_TIMEOUT).isAfter(now)
  }

  /**
   * Accepts verification of a plugin against IDE specified by [scheduledVerification].
   *
   * When verification of the plugin against this IDE occurs next time
   * the verification result will be forcibly sent.
   */
  @Synchronized
  fun forceVerificationResult(scheduledVerification: ScheduledVerification) {
    LOG.info("Force verification result $scheduledVerification")
    forcedVerifications.add(scheduledVerification)
  }

  /**
   * Determines whether the verification result should be sent.
   */
  @Synchronized
  fun shouldSendVerificationResult(
      verificationResult: VerificationResult,
      verificationEndTime: Instant,
      scheduledVerification: ScheduledVerification
  ): Result {
    if (scheduledVerification in forcedVerifications) {
      LOG.info("Verification $scheduledVerification has been forced.")

      /**
       * Require to force the result again.
       */
      forcedVerifications.remove(scheduledVerification)
      return Send
    }

    val decision = verificationResult.checkFailedToFetch(scheduledVerification.updateInfo, verificationEndTime)
    if (decision is Ignore) {
      LOG.info("Verification $scheduledVerification is ignored: ${decision.ignoreReason}")
      _ignoredVerifications[scheduledVerification] = decision
    }

    if (decision === Send && _ignoredVerifications.containsKey(scheduledVerification)) {
      LOG.info("Do not ignore the verification $scheduledVerification anymore")
      _ignoredVerifications.remove(scheduledVerification)
    }

    return decision
  }

  /**
   * Checks if this verification says that the plugin is "Not found" or "Non downloadable".
   *
   * If it happens [RECHECK_ATTEMPTS] times with a timeout of [RECHECK_ATTEMPT_TIMEOUT]
   * then we have to send this verification, since it seems to be stable
   * and not caused by Marketplace's failures.
   */
  private fun VerificationResult.checkFailedToFetch(updateInfo: UpdateInfo, verificationEndTime: Instant): Result {
    if (this is VerificationResult.NotFound || this is VerificationResult.FailedToDownload) {
      var attempt = failedPlugins[updateInfo]
      /**
       * Check that it is too early to increment "attempts" counter
       * because the last failed attempt was not that long ago.
       */
      if (attempt != null && attempt.lastVerified.plus(RECHECK_ATTEMPT_TIMEOUT).isAfter(verificationEndTime)) {
        return Ignore(
            toString(),
            verificationEndTime,
            "Plugin $plugin couldn't be fetched from the repository in ${attempt.attempts} " + "attempt".pluralize(attempt.attempts) + ". " +
                "It is too early to re-fetch the failed plugin. Waiting for timeout."
        )
      }

      if (attempt == null) {
        attempt = VerificationAttempt(0, verificationEndTime)
        failedPlugins[updateInfo] = attempt
      }

      attempt.attempts++
      attempt.lastVerified = verificationEndTime
      if (attempt.attempts == RECHECK_ATTEMPTS) {
        /**
         * We have to send the result now, since we have tried enough.
         *
         * Delete the plugin info from memory for future rechecks.
         */
        failedPlugins.remove(updateInfo)
        return Send
      }
      return Ignore(
          toString(),
          verificationEndTime,
          "Plugin $plugin couldn't be fetched from the repository in ${attempt.attempts} " + "attempt".pluralize(attempt.attempts) + ". " +
              "Will recheck after timeout."
      )
    }
    return Send
  }

  /**
   * Possible decisions on whether to send a verification result: either [Send] or [Ignore].
   */
  sealed class Result {

    /**
     * The verification result should be sent.
     */
    object Send : Result()

    /**
     * The verification result has been ignored by some [reason][ignoreReason].
     */
    data class Ignore(
        val verificationVerdict: String,
        val verificationEndTime: Instant,
        val ignoreReason: String
    ) : Result()

  }

  /**
   * Descriptor of a plugin's verification attempt,
   * which contains a number of attempts and
   * last attempt time.
   */
  private data class VerificationAttempt(
      var attempts: Int,
      var lastVerified: Instant
  )

}