package org.jetbrains.plugins.verifier.service.service.verifier

import com.intellij.structure.ide.IdeVersion
import com.intellij.structure.plugin.Plugin
import com.intellij.structure.resolvers.Resolver
import com.jetbrains.pluginverifier.api.*
import com.jetbrains.pluginverifier.ide.IdeCreator
import com.jetbrains.pluginverifier.plugin.CreatePluginResult
import com.jetbrains.pluginverifier.plugin.PluginCreator
import com.jetbrains.pluginverifier.tasks.CheckPluginParams
import com.jetbrains.pluginverifier.tasks.CheckPluginTask
import org.jetbrains.plugins.verifier.service.ide.IdeFileLock
import org.jetbrains.plugins.verifier.service.ide.IdeFilesManager
import org.jetbrains.plugins.verifier.service.params.CheckRangeRunnerParams
import org.jetbrains.plugins.verifier.service.progress.BridgeVerifierProgress
import org.jetbrains.plugins.verifier.service.progress.TaskProgress
import org.jetbrains.plugins.verifier.service.storage.JdkManager
import org.jetbrains.plugins.verifier.service.tasks.Task
import org.slf4j.LoggerFactory

class CheckPluginSinceUntilRangeTask(val pluginInfo: PluginInfo,
                                     val pluginCoordinate: PluginCoordinate,
                                     val params: CheckRangeRunnerParams,
                                     val ideVersions: List<IdeVersion>? = null) : Task<CheckRangeResults>() {
  override fun presentableName(): String = "Check $pluginCoordinate with IDE from [since; until]"

  companion object {
    private val LOG = LoggerFactory.getLogger(CheckPluginSinceUntilRangeTask::class.java)
  }

  private fun doRangeVerification(createOk: CreatePluginResult.OK, progress: TaskProgress): CheckRangeResults {
    val plugin = createOk.plugin
    val sinceBuild = plugin.sinceBuild!!
    val untilBuild = plugin.untilBuild

    LOG.debug("Verifying plugin $plugin against its specified [$sinceBuild; $untilBuild] builds")

    val ideLocks: List<IdeFileLock> = getIdesMatchingSinceUntilBuild(sinceBuild, untilBuild)
    try {
      return checkRangeResults(sinceBuild, untilBuild, ideLocks, plugin, progress)
    } finally {
      ideLocks.forEach { it.release() }
    }
  }

  private fun checkRangeResults(sinceBuild: IdeVersion, untilBuild: IdeVersion?, ideLocks: List<IdeFileLock>, plugin: Plugin, progress: TaskProgress): CheckRangeResults {
    LOG.debug("IDE-s on the server: ${IdeFilesManager.ideList().joinToString()}; IDE-s compatible with [$sinceBuild; $untilBuild]: [${ideLocks.joinToString { it.getIdeFile().name }}}]")
    if (ideLocks.isEmpty()) {
      LOG.info("There are no IDEs compatible with the Plugin $plugin; [since; until] = [$sinceBuild; $untilBuild]")
      return CheckRangeResults(pluginInfo, CheckRangeResults.ResultType.NO_COMPATIBLE_IDES, emptyList(), emptyList())
    }

    val ideDescriptors = ideLocks.map { IdeCreator.createByFile(it.getIdeFile(), null) }
    val jdkDescriptor = JdkDescriptor(JdkManager.getJdkHome(params.jdkVersion))
    val params = CheckPluginParams(listOf(pluginCoordinate), ideDescriptors, jdkDescriptor, emptyList(), ProblemsFilter.AlwaysTrue, Resolver.getEmptyResolver(), BridgeVerifierProgress(progress))

    LOG.debug("CheckPlugin with [since; until] #$taskId arguments: $params")

    val checkPluginResults = CheckPluginTask(params).execute()
    val results: List<Result> = checkPluginResults.results
    return CheckRangeResults(pluginInfo, CheckRangeResults.ResultType.CHECKED, ideDescriptors.map { it.ideVersion }, results)
  }

  private fun getIdesMatchingSinceUntilBuild(sinceBuild: IdeVersion, untilBuild: IdeVersion?): List<IdeFileLock> = IdeFilesManager.locked {
    (ideVersions ?: IdeFilesManager.ideList())
        .filter { sinceBuild <= it && (untilBuild == null || it <= untilBuild) }
        .mapNotNull { IdeFilesManager.getIde(it) }
  }

  override fun computeResult(progress: TaskProgress): CheckRangeResults = PluginCreator.createPlugin(pluginCoordinate).use { createPluginResult ->
    when (createPluginResult) {
      is CreatePluginResult.NotFound -> CheckRangeResults(pluginInfo, CheckRangeResults.ResultType.NOT_FOUND, emptyList(), emptyList())
      is CreatePluginResult.BadPlugin -> CheckRangeResults(pluginInfo, CheckRangeResults.ResultType.BAD_PLUGIN, emptyList(), emptyList())
      is CreatePluginResult.OK -> doRangeVerification(createPluginResult, progress)
    }
  }
}