package com.jetbrains.pluginverifier.dependencies.resolution

import com.google.common.collect.ImmutableSet
import com.jetbrains.plugin.structure.intellij.plugin.PluginDependency
import com.jetbrains.pluginverifier.dependencies.resolution.repository.UpdateSelector
import com.jetbrains.pluginverifier.plugin.PluginDetailsProvider
import com.jetbrains.pluginverifier.repository.PluginRepository

/**
 * @author Sergey Patrikeev
 */
class RepositoryDependencyResolver(private val pluginRepository: PluginRepository,
                                   private val updateSelector: UpdateSelector,
                                   private val pluginDetailsProvider: PluginDetailsProvider) : DependencyResolver {

  private companion object {
    val IDEA_ULTIMATE_MODULES: Set<String> = ImmutableSet.of(
        "com.intellij.modules.platform",
        "com.intellij.modules.lang",
        "com.intellij.modules.vcs",
        "com.intellij.modules.xml",
        "com.intellij.modules.xdebugger",
        "com.intellij.modules.java",
        "com.intellij.modules.ultimate",
        "com.intellij.modules.all")

    fun isDefaultModule(moduleId: String): Boolean = moduleId in IDEA_ULTIMATE_MODULES
  }

  override fun findPluginDependency(dependency: PluginDependency): DependencyResolver.Result {
    if (dependency.isModule) {
      return resolveModuleDependency(dependency.id)
    }
    return selectPlugin(dependency.id)
  }

  private fun resolveModuleDependency(moduleId: String): DependencyResolver.Result {
    if (isDefaultModule(moduleId)) {
      return DependencyResolver.Result.Skip
    }
    return resolveDeclaringPlugin(moduleId)
  }

  private fun resolveDeclaringPlugin(moduleId: String): DependencyResolver.Result {
    val pluginId = pluginRepository.getIdOfPluginDeclaringModule(moduleId)
        ?: return DependencyResolver.Result.NotFound("Module '$moduleId' is not found")
    return selectPlugin(pluginId)
  }

  private fun selectPlugin(pluginId: String): DependencyResolver.Result {
    val selectResult = updateSelector.select(pluginId, pluginRepository)
    return when (selectResult) {
      is UpdateSelector.Result.Plugin -> DependencyResolver.Result.FoundCoordinates(selectResult.updateInfo, pluginDetailsProvider)
      is UpdateSelector.Result.NotFound -> DependencyResolver.Result.NotFound(selectResult.reason)
    }
  }
}