package com.jetbrains.pluginverifier.reporting.verification

import com.jetbrains.pluginverifier.VerificationTarget
import com.jetbrains.pluginverifier.repository.PluginInfo
import java.io.Closeable

/**
 * Allows to report, log and save the verification stages and results in a configurable way.
 */
interface Reportage : Closeable {

  /**
   * Creates a [Reporters] for saving the reports
   * of the verification of the [pluginInfo] against [verificationTarget].
   */
  fun createPluginReporters(pluginInfo: PluginInfo, verificationTarget: VerificationTarget): Reporters

  /**
   * Logs the verification stage.
   */
  fun logVerificationStage(stageMessage: String)

  /**
   * Logs that the verification of [pluginInfo] against [verificationTarget] is ignored due to some [reason].
   */
  fun logPluginVerificationIgnored(pluginInfo: PluginInfo, verificationTarget: VerificationTarget, reason: String)

}