/*
 * Copyright 2000-2020 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.jetbrains.pluginverifier.tasks.checkIde

import com.jetbrains.plugin.structure.base.utils.closeLogged
import com.jetbrains.pluginverifier.PluginVerificationDescriptor
import com.jetbrains.pluginverifier.PluginVerificationTarget
import com.jetbrains.pluginverifier.filtering.ProblemsFilter
import com.jetbrains.pluginverifier.ide.IdeDescriptor
import com.jetbrains.pluginverifier.tasks.TaskParameters


class CheckIdeParams(
  val verificationTarget: PluginVerificationTarget.IDE,
  val verificationDescriptors: List<PluginVerificationDescriptor.IDE>,
  val problemsFilters: List<ProblemsFilter>,
  val missingCompatibleVersionsProblems: List<MissingCompatibleVersionProblem>,
  private val ideDescriptor: IdeDescriptor
) : TaskParameters {

  override val presentableText
    get() = buildString {
      appendln("Scheduled verifications against ${verificationTarget.ideVersion.asString()} (${verificationDescriptors.size}):")
      appendln(verificationDescriptors.joinToString { it.checkedPlugin.presentableName })
    }

  override fun close() {
    ideDescriptor.closeLogged()
  }

}