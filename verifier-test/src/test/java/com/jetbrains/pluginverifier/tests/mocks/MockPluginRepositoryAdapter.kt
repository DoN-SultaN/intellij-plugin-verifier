package com.jetbrains.pluginverifier.tests.mocks

import com.jetbrains.plugin.structure.intellij.version.IdeVersion
import com.jetbrains.pluginverifier.repository.PluginInfo
import com.jetbrains.pluginverifier.repository.PluginRepository
import java.net.URL

open class MockPluginRepositoryAdapter : PluginRepository {

  override val repositoryURL: URL = URL("http://example.com")

  override fun getAllPlugins(): List<PluginInfo> = defaultAction()

  override fun getLastCompatiblePlugins(ideVersion: IdeVersion): List<PluginInfo> = defaultAction()

  override fun getLastCompatibleVersionOfPlugin(ideVersion: IdeVersion, pluginId: String): PluginInfo? = defaultAction()

  override fun getAllCompatibleVersionsOfPlugin(ideVersion: IdeVersion, pluginId: String): List<PluginInfo> = defaultAction()

  override fun getAllVersionsOfPlugin(pluginId: String): List<PluginInfo> = defaultAction()

  override fun getIdOfPluginDeclaringModule(moduleId: String): String? = defaultAction()

  open fun defaultAction(): Nothing = throw AssertionError("Not required in tests")

  fun createMockPluginInfo(pluginId: String,
                           pluginName: String,
                           version: String,
                           downloadUrl: URL = URL("http://example.com")) =
      PluginInfo(
          pluginId,
          pluginName,
          version,
          this@MockPluginRepositoryAdapter,
          null,
          null,
          "vendor",
          downloadUrl,
          null
      )

}

