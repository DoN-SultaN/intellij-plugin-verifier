/*
 * Copyright 2000-2020 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.jetbrains.pluginverifier.tasks.checkPlugin

import com.jetbrains.pluginverifier.PluginVerificationResult
import com.jetbrains.pluginverifier.output.OutputOptions
import com.jetbrains.pluginverifier.output.html.HtmlResultPrinter
import com.jetbrains.pluginverifier.output.stream.WriterResultPrinter
import com.jetbrains.pluginverifier.output.teamcity.TeamCityHistory
import com.jetbrains.pluginverifier.output.teamcity.TeamCityLog
import com.jetbrains.pluginverifier.output.teamcity.TeamCityResultPrinter
import com.jetbrains.pluginverifier.repository.PluginRepository
import com.jetbrains.pluginverifier.tasks.TaskResult
import com.jetbrains.pluginverifier.tasks.TaskResultPrinter
import java.io.PrintWriter

class CheckPluginResultPrinter(
  private val outputOptions: OutputOptions,
  private val pluginRepository: PluginRepository
) : TaskResultPrinter {

  override fun printResults(taskResult: TaskResult) {
    with(taskResult as CheckPluginResult) {
      if (outputOptions.teamCityLog != null) {
        val teamCityHistory = printTcLog(true, outputOptions.teamCityLog)
        outputOptions.postProcessTeamCityTests(teamCityHistory)
      } else {
        printOnStdout(this)
      }

      results.groupBy { it.verificationTarget }.forEach { (verificationTarget, resultsOfIde) ->
        HtmlResultPrinter(verificationTarget, outputOptions).printResults(resultsOfIde)
      }
    }
  }

  private fun CheckPluginResult.printTcLog(setBuildStatus: Boolean, tcLog: TeamCityLog): TeamCityHistory {
    val teamCityHistory = TeamCityResultPrinter(
      tcLog,
      outputOptions.teamCityGroupType,
      pluginRepository
    ).printResults(results)

    TeamCityResultPrinter.printInvalidPluginFiles(tcLog, invalidPluginFiles)

    if (setBuildStatus) {
      setTeamCityBuildStatus(tcLog)
    }
    return teamCityHistory
  }

  private fun CheckPluginResult.setTeamCityBuildStatus(tcLog: TeamCityLog) {
    val totalProblemsNumber = results.flatMap {
      when (it) {
        is PluginVerificationResult.InvalidPlugin -> setOf(Any())
        is PluginVerificationResult.Verified -> it.compatibilityProblems
        else -> emptySet()
      }
    }.distinct().size
    if (totalProblemsNumber > 0) {
      tcLog.buildStatusFailure("$totalProblemsNumber problem${if (totalProblemsNumber > 0) "s" else ""} found")
    }
  }

  private fun printOnStdout(checkPluginResult: CheckPluginResult) {
    with(checkPluginResult) {
      val printWriter = PrintWriter(System.out)
      val writerResultPrinter = WriterResultPrinter(printWriter)
      writerResultPrinter.printResults(results)
      writerResultPrinter.printInvalidPluginFiles(invalidPluginFiles)
      printWriter.flush()
    }
  }

}