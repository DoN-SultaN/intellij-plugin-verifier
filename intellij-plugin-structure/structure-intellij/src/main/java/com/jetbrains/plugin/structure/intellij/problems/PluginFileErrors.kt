package com.jetbrains.plugin.structure.intellij.problems

import com.jetbrains.plugin.structure.base.plugin.PluginProblem
import com.jetbrains.plugin.structure.base.problems.IncorrectPluginFile

class IncorrectIntellijFile(fileName: String) : IncorrectPluginFile(fileName, ".zip or .jar archive or a directory.")

class PluginZipIsEmpty : PluginProblem() {

  override val level
    get() = Level.ERROR

  override val message
    get() = "Plugin file is empty"

}

class PluginZipContainsUnknownFile(private val fileName: String) : PluginProblem() {

  override val level
    get() = Level.ERROR

  override val message
    get() = "Plugin .zip file contains an unexpected file '$fileName'"

}

class PluginZipContainsMultipleFiles(private val fileNames: List<String>) : PluginProblem() {

  override val level
    get() = Level.ERROR

  override val message
    get() = "Plugin root directory must not contain multiple files: ${fileNames.joinToString()}"

}

class UnableToReadJarFile : PluginProblem() {

  override val level
    get() = Level.ERROR

  override val message
    get() = "Invalid jar file"

}

class PluginLibDirectoryIsEmpty : PluginProblem() {

  override val level
    get() = Level.ERROR

  override val message
    get() = "Directory 'lib' must not be empty"

}