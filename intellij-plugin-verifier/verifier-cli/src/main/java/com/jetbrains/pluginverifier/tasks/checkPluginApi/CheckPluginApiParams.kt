package com.jetbrains.pluginverifier.tasks.checkPluginApi

import com.jetbrains.plugin.structure.base.utils.closeLogged
import com.jetbrains.pluginverifier.options.PluginsSet
import com.jetbrains.pluginverifier.parameters.filtering.ProblemsFilter
import com.jetbrains.pluginverifier.parameters.jdk.JdkPath
import com.jetbrains.pluginverifier.parameters.packages.PackageFilter
import com.jetbrains.pluginverifier.plugin.PluginDetails
import com.jetbrains.pluginverifier.tasks.TaskParameters

class CheckPluginApiParams(
    val pluginsSet: PluginsSet,
    val basePluginDetails: PluginDetails,
    val newPluginDetails: PluginDetails,
    val jdkPath: JdkPath,
    val problemsFilters: List<ProblemsFilter>,
    val basePluginPackageFilter: PackageFilter
) : TaskParameters {

  override val presentableText
    get() = """
      |JDK              : $jdkPath
      |$pluginsSet
    """.trimMargin()

  override fun close() {
    basePluginDetails.closeLogged()
    newPluginDetails.closeLogged()
  }

}