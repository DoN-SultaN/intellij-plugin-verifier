package com.jetbrains.pluginverifier.tests.tasks

import com.jetbrains.plugin.structure.classes.resolvers.EmptyResolver
import com.jetbrains.plugin.structure.intellij.classes.plugin.IdePluginClassesLocations
import com.jetbrains.plugin.structure.intellij.plugin.PluginDependencyImpl
import com.jetbrains.plugin.structure.intellij.version.IdeVersion
import com.jetbrains.plugin.structure.intellij.version.IdeVersion.createIdeVersion
import com.jetbrains.pluginverifier.dependencies.DependencyNode
import com.jetbrains.pluginverifier.ide.IdeDescriptor
import com.jetbrains.pluginverifier.plugin.PluginDetails
import com.jetbrains.pluginverifier.plugin.PluginDetailsCache
import com.jetbrains.pluginverifier.plugin.PluginDetailsProvider
import com.jetbrains.pluginverifier.reporting.verification.VerificationReportageImpl
import com.jetbrains.pluginverifier.repository.PluginInfo
import com.jetbrains.pluginverifier.repository.files.FileLock
import com.jetbrains.pluginverifier.repository.files.IdleFileLock
import com.jetbrains.pluginverifier.repository.local.LocalPluginRepository
import com.jetbrains.pluginverifier.results.Verdict
import com.jetbrains.pluginverifier.tasks.checkTrunkApi.CheckTrunkApiParams
import com.jetbrains.pluginverifier.tasks.checkTrunkApi.CheckTrunkApiTask
import com.jetbrains.pluginverifier.tests.mocks.*
import org.hamcrest.Matchers.containsInAnyOrder
import org.hamcrest.Matchers.instanceOf
import org.junit.Assert.assertThat
import org.junit.Test
import java.io.Closeable
import java.io.File
import java.net.URL
import java.nio.file.Paths

/**
 * This test verifies that the following [plugin dependencies] [com.jetbrains.plugin.structure.intellij.plugin.PluginDependency]
 * during the 'check-trunk-api' task are resolved properly:
 * 1) Dependencies on plugins available locally
 * 2) Dependencies on modules contained in locally available plugins
 *
 * The following dependencies between plugins and modules are declared:
 *
 * plugin.to.check 1.0
 *  +--- org.jetbrains.plugin (this is the JetBrains plugin which version differs between RELEASE and TRUNK IDEs)
 *  \--- org.jetbrains.module (this module is declared in a plugin 'org.jetbrains.module.container')
 *
 *  'org.jetbrains.plugin' is to be found in the IDE/plugins directory (called the "local" repository).
 */
@Suppress("MemberVisibilityCanPrivate")
class CheckTrunkApiTaskDependenciesResolutionTest {

  val someJetBrainsPluginId = "org.jetbrains.plugin"

  val someJetBrainsModule = "org.jetbrains.module"

  val someJetBrainsPluginContainingModuleId = "org.jetbrains.module.container"

  val repositoryURL = URL("http://unnecessary.com")

  val releaseVersion = createIdeVersion("IU-173.1")

  val trunkVersion = createIdeVersion("IU-181.1")

  val releaseIde = MockIde(releaseVersion)

  val trunkIde = MockIde(trunkVersion)

  val someJetBrainsPluginBase = MockIdePlugin(
      pluginId = someJetBrainsPluginId,
      pluginName = "some jetbrains plugin",
      vendor = "JetBrains",
      definedModules = emptySet()
  )

  /**
   * This plugin will be available in the "local" plugin repository when
   * verifying the RELEASE or TRUNK IDE.
   */
  val releaseSomeJetBrainsMockPlugin = someJetBrainsPluginBase.copy(
      pluginVersion = "1.0",
      sinceBuild = releaseVersion,
      untilBuild = releaseVersion,
      originalFile = File("jetbrains.1.0")
  )

  /**
   * This is the TRUNK version of the dependent plugin 'org.jetbrains.plugin'
   * It will be available in the "local" plugin repository when verifying the TRUNK IDE.
   */
  val trunkSomeJetBrainsMockPlugin = someJetBrainsPluginBase.copy(
      pluginVersion = "2.0",
      sinceBuild = trunkVersion,
      untilBuild = trunkVersion,
      originalFile = File("jetbrains.2.0")
  )

  /**
   * This is the JetBrains plugin that contains the module 'org.jetbrains.module'.
   */
  val someJetBrainsMockPluginContainingModule = MockIdePlugin(
      pluginId = someJetBrainsPluginContainingModuleId,
      pluginName = "some jetbrains plugin containing module",
      pluginVersion = "1.0",
      definedModules = setOf(someJetBrainsModule),
      originalFile = File("jetbrains.module.container.1.0")
  )

  /**
   * This is the plugin to be verified in this test.
   */
  val pluginToCheck = MockIdePlugin(
      pluginId = "plugin.to.check",
      pluginVersion = "1.0",
      dependencies = listOf(
          //Dependency by id
          PluginDependencyImpl(someJetBrainsPluginId, false, false),

          //Dependency on module which is defined in [someJetBrainsPluginContainingModuleId]
          PluginDependencyImpl(someJetBrainsModule, false, true)
      ),
      originalFile = File("plugin.to.check.file")
  )

  @Test
  fun `local plugins are resolved both by ID and by module`() {
    val checkTrunkApiParams = createTrunkApiParamsForTest(releaseIde, trunkIde)
    PluginDetailsCache(10, createPluginDetailsProviderForTest()).use { pluginDetailsCache ->
      val checkTrunkApiTask = CheckTrunkApiTask(
          checkTrunkApiParams,
          EmptyPublicPluginRepository,
          pluginDetailsCache
      )
      val (_, releaseResults, _, trunkResults, _) = checkTrunkApiTask.execute(VerificationReportageImpl(EmptyReporterSetProvider))
      val releaseVerdict = releaseResults.single().verdict
      val trunkVerdict = trunkResults.single().verdict
      assertPluginsAreProperlyResolved(releaseVerdict, trunkVerdict)
    }
  }

  private fun assertPluginsAreProperlyResolved(releaseVerdict: Verdict, trunkVerdict: Verdict) {
    assertThat(trunkVerdict, instanceOf(Verdict.OK::class.java))
    assertThat(releaseVerdict, instanceOf(Verdict.OK::class.java))

    val trunkGraph = (trunkVerdict as Verdict.OK).dependenciesGraph
    val releaseGraph = (releaseVerdict as Verdict.OK).dependenciesGraph

    assertThat(trunkGraph.vertices.drop(1), containsInAnyOrder(
        DependencyNode(someJetBrainsPluginId, "2.0", emptyList()),
        DependencyNode(someJetBrainsPluginContainingModuleId, "1.0", emptyList())
    ))

    assertThat(releaseGraph.vertices.drop(1), containsInAnyOrder(
        DependencyNode(someJetBrainsPluginId, "1.0", emptyList()),
        DependencyNode(someJetBrainsPluginContainingModuleId, "1.0", emptyList())
    ))
  }

  private fun createPluginDetailsProviderForTest(): PluginDetailsProvider {
    val allPlugins = listOf(releaseSomeJetBrainsMockPlugin, trunkSomeJetBrainsMockPlugin, someJetBrainsMockPluginContainingModule, pluginToCheck)
    val infoToDetails = hashMapOf<PluginInfo, PluginDetails>()
    for (plugin in allPlugins) {
      val pluginInfo = LocalPluginRepository(repositoryURL).addLocalPlugin(plugin)
      infoToDetails[pluginInfo] = PluginDetails(
          pluginInfo,
          plugin,
          emptyList(),
          IdePluginClassesLocations(plugin, Closeable { }, emptyMap()), IdleFileLock(Paths.get("."))
      )
    }

    return object : PluginDetailsProvider {
      override fun providePluginDetails(pluginInfo: PluginInfo, pluginFileLock: FileLock): PluginDetailsProvider.Result {
        val details = infoToDetails[pluginInfo]
        if (details != null) {
          return PluginDetailsProvider.Result.Provided(details)
        }
        return PluginDetailsProvider.Result.InvalidPlugin(emptyList())
      }
    }
  }

  private fun createTrunkApiParamsForTest(releaseIde: MockIde, trunkIde: MockIde) = CheckTrunkApiParams(
      IdeDescriptor(trunkIde, EmptyResolver),
      IdeDescriptor(releaseIde, EmptyResolver),
      emptyList(),
      emptyList(),
      TestJdkDescriptorProvider.getJdkDescriptorForTests(),
      listOf(someJetBrainsPluginId),
      false,
      IdleFileLock(Paths.get("unnecessary")),
      createLocalRepository(releaseSomeJetBrainsMockPlugin, releaseVersion),
      createLocalRepository(trunkSomeJetBrainsMockPlugin, trunkVersion),
      listOf(LocalPluginRepository(repositoryURL).addLocalPlugin(pluginToCheck))
  )

  /**
   * Creates the [LocalPluginRepository] consisting of
   * 1) [jetBrainsPlugin] specified
   */
  private fun createLocalRepository(jetBrainsPlugin: MockIdePlugin,
                                    ideVersion: IdeVersion): LocalPluginRepository {
    val localPluginRepository = LocalPluginRepository(repositoryURL)
    val plugins = listOf(
        jetBrainsPlugin,
        someJetBrainsMockPluginContainingModule.copy(
            sinceBuild = ideVersion,
            untilBuild = ideVersion
        )
    )
    plugins.forEach { localPluginRepository.addLocalPlugin(it) }
    return localPluginRepository
  }


}