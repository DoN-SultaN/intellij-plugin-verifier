package com.jetbrains.pluginverifier.tasks

import com.jetbrains.pluginverifier.plugin.PluginCreator
import com.jetbrains.pluginverifier.repository.IdeRepository
import com.jetbrains.pluginverifier.repository.PluginRepository

class CheckTrunkApiRunner : TaskRunner() {
  override val commandName: String = "check-trunk-api"

  override fun getParametersBuilder(
      pluginRepository: PluginRepository,
      ideRepository: IdeRepository,
      pluginCreator: PluginCreator
  ) = CheckTrunkApiParamsBuilder(ideRepository)

  override fun createTask(
      parameters: TaskParameters,
      pluginRepository: PluginRepository,
      pluginCreator: PluginCreator
  ) = CheckTrunkApiTask(parameters as CheckTrunkApiParams, pluginRepository, pluginCreator)

}