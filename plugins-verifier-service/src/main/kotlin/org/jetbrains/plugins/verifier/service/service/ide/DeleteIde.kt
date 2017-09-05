package org.jetbrains.plugins.verifier.service.service.ide

import com.jetbrains.plugin.structure.intellij.version.IdeVersion
import org.jetbrains.plugins.verifier.service.ide.IdeFilesManager
import org.jetbrains.plugins.verifier.service.tasks.Task
import org.jetbrains.plugins.verifier.service.tasks.TaskProgress
import org.slf4j.Logger
import org.slf4j.LoggerFactory

/**
 * @author Sergey Patrikeev
 */
class DeleteIdeRunner(val ideVersion: IdeVersion) : Task<Boolean>() {

  private val LOG: Logger = LoggerFactory.getLogger(DeleteIdeRunner::class.java)

  override fun presentableName(): String = "DeleteIde #$ideVersion"

  override fun computeResult(progress: TaskProgress): Boolean {
    IdeFilesManager.deleteIde(ideVersion)
    LOG.info("Delete IDE #$ideVersion task is enqueued for IdeFilesManager")
    return true
  }

}