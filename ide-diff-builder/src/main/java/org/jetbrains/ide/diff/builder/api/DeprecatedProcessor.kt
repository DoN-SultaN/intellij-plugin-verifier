/*
 * Copyright 2000-2020 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.ide.diff.builder.api

import com.jetbrains.plugin.structure.classes.resolvers.Resolver
import com.jetbrains.pluginverifier.usages.deprecated.deprecationInfo
import com.jetbrains.pluginverifier.verifiers.resolution.ClassFile
import com.jetbrains.pluginverifier.verifiers.resolution.ClassFileMember

class DeprecatedProcessor : ApiDiffProcessor {

  data class MarkedDeprecated(val member: ClassFileMember, val forRemoval: Boolean, val inVersion: String?)

  val markedDeprecated: MutableList<MarkedDeprecated> = arrayListOf()

  val unmarkedDeprecated: MutableList<ClassFileMember> = arrayListOf()

  override fun process(
    oldClass: ClassFile?,
    oldMember: ClassFileMember?,
    newClass: ClassFile?,
    newMember: ClassFileMember?,
    oldResolver: Resolver,
    newResolver: Resolver
  ) {
    val oldDeprecation = oldMember?.deprecationInfo
    val newDeprecation = newMember?.deprecationInfo

    when {
      oldDeprecation != null && newDeprecation != null && oldDeprecation != newDeprecation && oldMember.isAccessible -> {
        //API deprecation status has been updated (either 'forRemoval' or 'version' has been updated).
        unmarkedDeprecated += oldMember
        markedDeprecated += MarkedDeprecated(newMember, newDeprecation.forRemoval, newDeprecation.untilVersion)
      }
      oldDeprecation == null && newDeprecation != null && newMember.isAccessible -> {
        markedDeprecated += MarkedDeprecated(newMember, newDeprecation.forRemoval, newDeprecation.untilVersion)
      }
      oldDeprecation != null && newDeprecation == null && oldMember.isAccessible -> {
        unmarkedDeprecated += oldMember
      }
    }
  }

}