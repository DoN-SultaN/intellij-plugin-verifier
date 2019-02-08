package com.jetbrains.plugin.structure.domain

import com.jetbrains.plugin.structure.ide.IdeManager
import com.jetbrains.plugin.structure.intellij.version.IdeVersion
import com.jetbrains.plugin.structure.mocks.PluginXmlBuilder
import com.jetbrains.plugin.structure.mocks.modify
import com.jetbrains.plugin.structure.mocks.perfectXmlBuilder
import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.collection.IsCollectionWithSize.hasSize
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.rules.ExpectedException
import org.junit.rules.TemporaryFolder
import java.io.File

class IdeTest {

  @Rule
  @JvmField
  val temporaryFolder = TemporaryFolder()

  @Rule
  @JvmField
  val expectedEx: ExpectedException = ExpectedException.none()

  @Test
  fun `version is not specified in distributed IDE`() {
    val ideaFolder = temporaryFolder.newFolder("idea")
    ideaFolder.resolve("lib").mkdirs()

    expectedEx.expect(IllegalArgumentException::class.java)
    expectedEx.expectMessage(
        "Build number is not found in the following files relative to $ideaFolder: " +
            "build.txt, Resources${File.separator}build.txt, community${File.separator}build.txt, " +
            "ultimate${File.separator}community${File.separator}build.txt"
    )

    IdeManager.createManager().createIde(ideaFolder)
  }

  @Test
  fun `create idea from binaries`() {
    val ideaFolder = temporaryFolder.newFolder("idea")
    File(ideaFolder, "build.txt").writeText("IU-163.1.2.3")

    /**
     * Create /plugins/somePlugin/META-INF/plugin.xml
     * of some bundled plugin.
     */
    val pluginsFolder = File(ideaFolder, "plugins")
    val somePluginFolder = File(pluginsFolder, "somePlugin")

    val bundledPluginXmlContent = perfectXmlBuilder.asString()
    val bundledXml = File(somePluginFolder, "META-INF/plugin.xml")
    bundledXml.parentFile.mkdirs()
    bundledXml.writeText(bundledPluginXmlContent)

    /**
     * Create a /lib/resources.jar/META-INF/plugin.xml
     * that contains the `IDEA-CORE` plugin.
     */
    val ideaPluginXml = temporaryFolder.newFolder().resolve("META-INF").resolve("plugin.xml")
    ideaPluginXml.parentFile.mkdirs()
    ideaPluginXml.writeText(perfectXmlBuilder.apply {
      id = "<id>IDEA CORE</id>"
      modules = listOf("some.idea.module")
    }.asString())

    val resourcesJar = ideaFolder.resolve("lib").resolve("resources.jar")
    resourcesJar.parentFile.mkdirs()
    JarFileUtils.createJarFile(
        listOf(
            JarFileEntry(ideaPluginXml, "META-INF/plugin.xml")
        ),
        resourcesJar
    )

    val ide = IdeManager.createManager().createIde(ideaFolder)
    assertThat(ide.version, `is`(IdeVersion.createIdeVersion("IU-163.1.2.3")))
    assertThat(ide.bundledPlugins, hasSize(2))
    val bundledPlugin = ide.bundledPlugins[0]!!
    val ideaCorePlugin = ide.bundledPlugins[1]!!
    assertEquals("someId", bundledPlugin.pluginId)
    assertEquals("IDEA CORE", ideaCorePlugin.pluginId)
    assertEquals("some.idea.module", ideaCorePlugin.definedModules.single())
    assertEquals(ideaCorePlugin, ide.getPluginByModule("some.idea.module"))
  }

  @Test
  fun `create idea from ultimate compiled sources`() {
    val bundledPluginXmlContent = perfectXmlBuilder.asString()

    val ideaFolder = temporaryFolder.newFolder("idea")
    ideaFolder.resolve("build.txt").writeText("IU-163.1.2.3")

    ideaFolder.resolve(".idea").mkdirs()
    ideaFolder.resolve("community/.idea").mkdirs()

    val productionDir = ideaFolder.resolve("out/classes/production")
    productionDir.mkdirs()

    val bundledFolder = productionDir.resolve("somePlugin")
    val bundledXml = bundledFolder.resolve("META-INF").resolve("plugin.xml")
    bundledXml.parentFile.mkdirs()
    bundledXml.writeText(bundledPluginXmlContent)

    val ide = IdeManager.createManager().createIde(ideaFolder)
    assertThat(ide.version, `is`(IdeVersion.createIdeVersion("IU-163.1.2.3")))
    assertThat(ide.bundledPlugins, hasSize(1))

    val plugin = ide.bundledPlugins[0]!!
    assertThat(plugin.pluginId, `is`("someId"))
    assertThat(plugin.originalFile, `is`(bundledFolder))
  }

  @Test
  fun `plugins bundled to idea may not have versions in descriptors`() {
    val ideaFolder = temporaryFolder.newFolder("idea")
    ideaFolder.resolve(".idea").mkdirs()

    val compilationRoot = ideaFolder.resolve("out").resolve("compilation").resolve("classes").resolve("production")
    compilationRoot.mkdirs()
    ideaFolder.resolve("build.txt").writeText("IU-163.1.2.3")

    val incompleteDescriptor = PluginXmlBuilder().modify {
      name = "<name>Bundled</name>"
      id = "<id>Bundled</id>"
      vendor = "<vendor>JetBrains</vendor>"
      description = "<description>Short</description>"
      changeNotes = "<change-notes>Short</change-notes>"
    }
    val bundledPluginFolder = compilationRoot.resolve("Bundled")
    val bundledXml = bundledPluginFolder.resolve("META-INF/plugin.xml")
    bundledXml.parentFile.mkdirs()
    bundledXml.writeText(incompleteDescriptor)

    val ide = IdeManager.createManager().createIde(ideaFolder)
    assertEquals(IdeVersion.createIdeVersion("IU-163.1.2.3"), ide.version)
    assertEquals(1, ide.bundledPlugins.size)
    val plugin = ide.bundledPlugins[0]!!
    assertThat(plugin.pluginId, `is`("Bundled"))
  }
}