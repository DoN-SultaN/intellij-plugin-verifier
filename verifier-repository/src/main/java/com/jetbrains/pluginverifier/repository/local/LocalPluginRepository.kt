package com.jetbrains.pluginverifier.repository.local

import com.jetbrains.plugin.structure.intellij.plugin.IdePlugin
import com.jetbrains.plugin.structure.intellij.version.IdeVersion
import com.jetbrains.pluginverifier.misc.VersionComparatorUtil
import com.jetbrains.pluginverifier.repository.PluginInfo
import com.jetbrains.pluginverifier.repository.PluginRepository
import java.net.URL

/**
 * [PluginRepository] consisting of [locally] [LocalPluginInfo] stored plugins.
 */
class LocalPluginRepository(override val repositoryURL: URL,
                            private val plugins: MutableList<LocalPluginInfo> = arrayListOf()) : PluginRepository {
  companion object {
    val VERSION_COMPARATOR = compareBy<PluginInfo, String>(VersionComparatorUtil.COMPARATOR, { it.version })
  }

  fun addLocalPlugin(idePlugin: IdePlugin): LocalPluginInfo {
    val localPluginInfo = createLocalPluginInfo(idePlugin)
    plugins.add(localPluginInfo)
    return localPluginInfo
  }

  private fun createLocalPluginInfo(idePlugin: IdePlugin) = LocalPluginInfo(idePlugin, this@LocalPluginRepository)

  override fun getAllPlugins() = plugins

  override fun getLastCompatiblePlugins(ideVersion: IdeVersion) =
      plugins.filter { it.isCompatibleWith(ideVersion) }
          .groupBy { it.pluginId }
          .mapValues { it.value.maxWith(VERSION_COMPARATOR)!! }
          .values.toList()

  override fun getAllCompatibleVersionsOfPlugin(ideVersion: IdeVersion, pluginId: String) =
      plugins.filter { it.isCompatibleWith(ideVersion) && it.pluginId == pluginId }

  override fun getLastCompatibleVersionOfPlugin(ideVersion: IdeVersion, pluginId: String) =
      getAllCompatibleVersionsOfPlugin(ideVersion, pluginId).maxWith(VERSION_COMPARATOR)

  override fun getAllVersionsOfPlugin(pluginId: String) =
      plugins.filter { it.pluginId == pluginId }

  override fun getIdOfPluginDeclaringModule(moduleId: String) =
      plugins.find { moduleId in it.definedModules }?.pluginId

  fun findPluginById(pluginId: String): LocalPluginInfo? = plugins.find { it.pluginId == pluginId }

  fun findPluginByModule(moduleId: String): LocalPluginInfo? = plugins.find { moduleId in it.definedModules }
}