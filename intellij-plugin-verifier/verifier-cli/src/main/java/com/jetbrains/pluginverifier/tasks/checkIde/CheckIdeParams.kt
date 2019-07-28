package com.jetbrains.pluginverifier.tasks.checkIde

import com.jetbrains.plugin.structure.base.utils.closeLogged
import com.jetbrains.pluginverifier.dependencies.resolution.DependencyFinder
import com.jetbrains.pluginverifier.ide.IdeDescriptor
import com.jetbrains.pluginverifier.options.PluginsSet
import com.jetbrains.pluginverifier.filtering.ProblemsFilter
import com.jetbrains.pluginverifier.verifiers.packages.PackageFilter
import com.jetbrains.pluginverifier.tasks.TaskParameters
import java.nio.file.Path


class CheckIdeParams(
    val pluginsSet: PluginsSet,
    val jdkPath: Path,
    val ideDescriptor: IdeDescriptor,
    val externalClassesPackageFilter: PackageFilter,
    val problemsFilters: List<ProblemsFilter>,
    val dependencyFinder: DependencyFinder,
    val missingCompatibleVersionsProblems: List<MissingCompatibleVersionProblem>
) : TaskParameters {

  override val presentableText
    get() = """
      |IDE : $ideDescriptor
      |JDK : $jdkPath
      |$pluginsSet
    """.trimMargin()

  override fun close() {
    ideDescriptor.closeLogged()
  }

}