package com.intellij.structure.impl.extractor

import com.intellij.structure.impl.utils.ZipUtil
import com.intellij.structure.problems.PluginZipContainsUnknownFile
import com.intellij.structure.problems.PluginZipIsEmpty
import com.jetbrains.structure.plugin.PluginProblem
import com.jetbrains.structure.utils.FileUtil
import org.apache.commons.io.FileUtils
import java.io.File
import java.io.IOException

/**
 * @author Sergey Patrikeev
 */
object PluginExtractor {

  @Throws(IOException::class)
  fun extractPlugin(pluginZip: File): ExtractorResult {
    if (!FileUtil.isZip(pluginZip)) {
      throw IllegalArgumentException("Must be a zip archive: " + pluginZip)
    }

    val extractedDirectory = FileUtil.extractedPluginsDirectory
    val extractedPlugin = FileUtil.createTempDir(extractedDirectory, "plugin_")

    try {
      ZipUtil.extractZip(pluginZip, extractedPlugin)
    } catch (e: Throwable) {
      FileUtils.deleteQuietly(extractedPlugin)
      throw e
    }

    return getExtractorResult(pluginZip, extractedPlugin)
  }

  private fun success(actualFile: File, fileToDelete: File): ExtractorResult =
      ExtractorSuccess(ExtractedPluginFile(actualFile, fileToDelete))

  private fun fail(problem: PluginProblem, extractedPlugin: File): ExtractorResult {
    try {
      return ExtractorFail(problem)
    } finally {
      FileUtils.deleteQuietly(extractedPlugin)
    }
  }

  private fun getExtractorResult(pluginZip: File, extractedPlugin: File): ExtractorResult {
    val rootFiles = extractedPlugin.listFiles() ?: return fail(PluginZipIsEmpty(pluginZip), extractedPlugin)
    if (rootFiles.isEmpty()) {
      return fail(PluginZipIsEmpty(pluginZip), extractedPlugin)
    } else if (rootFiles.size == 1) {
      val singleFile = rootFiles[0]
      if (singleFile.name.endsWith(".jar")) {
        return success(singleFile, extractedPlugin)
      } else if (singleFile.isDirectory) {
        if (singleFile.name == "lib") {
          return success(extractedPlugin, extractedPlugin)
        } else {
          return success(singleFile, extractedPlugin)
        }
      } else {
        return fail(PluginZipContainsUnknownFile(pluginZip, singleFile.name), extractedPlugin)
      }
    } else {
      return success(extractedPlugin, extractedPlugin)
    }
  }

}
