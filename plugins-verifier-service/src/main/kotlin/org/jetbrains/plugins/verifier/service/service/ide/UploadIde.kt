package org.jetbrains.plugins.verifier.service.service.ide

import com.jetbrains.plugin.structure.intellij.version.IdeVersion
import com.jetbrains.pluginverifier.misc.deleteLogged
import com.jetbrains.pluginverifier.repository.AvailableIde
import com.jetbrains.pluginverifier.repository.IdeRepository
import org.jetbrains.plugins.verifier.service.ide.IdeFilesManager
import org.jetbrains.plugins.verifier.service.tasks.Task
import org.jetbrains.plugins.verifier.service.tasks.TaskProgress

/**
 * @author Sergey Patrikeev
 */
class UploadIdeRunner(val ideVersion: IdeVersion? = null,
                      val availableIde: AvailableIde? = null,
                      val fromSnapshots: Boolean = false) : Task<Boolean>() {

  init {
    require(ideVersion != null || availableIde != null, { "IDE version to be uploaded is not specified" })
  }

  override fun presentableName(): String = "Downloading IDE #${availableIde?.version ?: ideVersion!!}"

  override fun computeResult(progress: TaskProgress): Boolean {
    val artifact = getArtifactInfo() ?: throw IllegalArgumentException("Unable to find the IDE #$ideVersion in snapshots = $fromSnapshots")

    val ideFile = IdeRepository.getOrDownloadIde(artifact) { progress.setFraction(it) }

    try {
      return IdeFilesManager.addIde(ideFile)
    } finally {
      ideFile.deleteLogged()
    }
  }

  private fun getArtifactInfo(): AvailableIde? = availableIde ?: IdeRepository.fetchIndex(fromSnapshots)
      .find { it.version.asStringWithoutProductCode() == ideVersion!!.asStringWithoutProductCode() }

}