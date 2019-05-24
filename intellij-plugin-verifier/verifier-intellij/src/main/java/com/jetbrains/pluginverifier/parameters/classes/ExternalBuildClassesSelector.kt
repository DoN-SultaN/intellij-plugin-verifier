package com.jetbrains.pluginverifier.parameters.classes

import com.jetbrains.plugin.structure.classes.resolvers.EmptyResolver
import com.jetbrains.plugin.structure.classes.resolvers.JarFileResolver
import com.jetbrains.plugin.structure.classes.resolvers.Resolver
import com.jetbrains.plugin.structure.classes.resolvers.UnionResolver
import com.jetbrains.plugin.structure.intellij.classes.locator.CompileServerExtensionKey
import com.jetbrains.plugin.structure.intellij.classes.plugin.IdePluginClassesLocations

/**
 * [ClassesSelector] that selects classes used for the external build processes,
 * such as JPS classes bundled into the Kotlin plugin (`/lib/jps`).
 */
class ExternalBuildClassesSelector : ClassesSelector {
  override fun getClassLoader(classesLocations: IdePluginClassesLocations): Resolver =
      classesLocations.getResolver(CompileServerExtensionKey) ?: EmptyResolver

  override fun getClassesForCheck(classesLocations: IdePluginClassesLocations): Set<String> {
    val compileServerResolver = classesLocations.getResolver(CompileServerExtensionKey) ?: return emptySet()

    val jarFileResolvers = compileServerResolver.finalResolvers.filterIsInstance<JarFileResolver>()
    val allServiceImplementations = hashSetOf<String>()
    for (jarFileResolver in jarFileResolvers) {
      jarFileResolver.implementedServiceProviders
          .filter { isJetbrainsServiceProvider(it) }
          .flatMapTo(allServiceImplementations) { jarFileResolver.readServiceImplementationNames(it) }
    }

    val serviceProviderContainingJars = allServiceImplementations.mapNotNullTo(hashSetOf()) {
      compileServerResolver.getClassLocation(it.replace('.', '/'))
    }
    return UnionResolver.create(serviceProviderContainingJars).allClasses
  }

  private fun isJetbrainsServiceProvider(serviceProvider: String): Boolean =
      serviceProvider.startsWith("org.jetbrains.")
}