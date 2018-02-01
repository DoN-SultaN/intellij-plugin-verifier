package com.jetbrains.pluginverifier.repository.bundled

import com.jetbrains.plugin.structure.ide.Ide
import com.jetbrains.plugin.structure.intellij.version.IdeVersion
import com.jetbrains.pluginverifier.repository.PluginRepository
import com.jetbrains.pluginverifier.repository.local.LocalPluginRepository
import java.net.URL

/**
 * [PluginRepository] consisting of plugins bundled to the [IDE] [ide].
 */
class BundledPluginsRepository(val ide: Ide, override val repositoryURL: URL) : PluginRepository {
  override fun getAllPlugins() =
      ide.bundledPlugins.map {
        BundledPluginInfo(this, it, ide)
      }

  override fun getLastCompatiblePlugins(ideVersion: IdeVersion) =
      getAllPlugins().filter { it.isCompatibleWith(ideVersion) }
          .groupBy { it.pluginId }
          .mapValues { it.value.maxWith(LocalPluginRepository.VERSION_COMPARATOR)!! }
          .values.toList()

  override fun getAllCompatibleVersionsOfPlugin(ideVersion: IdeVersion, pluginId: String) =
      getAllPlugins().filter { it.isCompatibleWith(ideVersion) && it.pluginId == pluginId }

  override fun getLastCompatibleVersionOfPlugin(ideVersion: IdeVersion, pluginId: String) =
      getAllCompatibleVersionsOfPlugin(ideVersion, pluginId).maxWith(LocalPluginRepository.VERSION_COMPARATOR)

  override fun getAllVersionsOfPlugin(pluginId: String) =
      getAllPlugins().filter { it.pluginId == pluginId }

  override fun getIdOfPluginDeclaringModule(moduleId: String) =
      ide.getPluginByModule(moduleId)?.pluginId

  fun findPluginById(pluginId: String) = getAllVersionsOfPlugin(pluginId).firstOrNull()

  fun findPluginByModule(moduleId: String) = getAllPlugins().find { moduleId in it.idePlugin!!.definedModules }
}