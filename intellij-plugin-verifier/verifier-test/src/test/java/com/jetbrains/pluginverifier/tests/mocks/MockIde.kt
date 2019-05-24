package com.jetbrains.pluginverifier.tests.mocks

import com.jetbrains.plugin.structure.ide.Ide
import com.jetbrains.plugin.structure.intellij.plugin.IdePlugin
import com.jetbrains.plugin.structure.intellij.version.IdeVersion
import java.io.File

data class MockIde(
    private val ideVersion: IdeVersion,
    private val idePath: File = File(""),
    private val bundledPlugins: List<IdePlugin> = emptyList()
) : Ide() {

  override fun getIdePath() = idePath

  override fun getVersion() = ideVersion

  override fun getBundledPlugins() = bundledPlugins
}