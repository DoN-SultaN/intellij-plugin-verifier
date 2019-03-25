package com.jetbrains.pluginverifier.verifiers.method

import com.jetbrains.pluginverifier.verifiers.VerificationContext
import com.jetbrains.pluginverifier.verifiers.createMethodLocation
import com.jetbrains.pluginverifier.verifiers.extractClassNameFromDescr
import com.jetbrains.pluginverifier.verifiers.resolveClassOrProblem
import org.objectweb.asm.Type
import org.objectweb.asm.tree.ClassNode
import org.objectweb.asm.tree.MethodNode

class MethodReturnTypeVerifier : MethodVerifier {
  override fun verify(clazz: ClassNode, method: MethodNode, ctx: VerificationContext) {
    val methodType = Type.getType(method.desc)
    val returnType = methodType.returnType

    val descriptor = returnType.descriptor
    if ("V" == descriptor) return  //void return type

    val returnTypeDesc = descriptor.extractClassNameFromDescr() ?: return

    ctx.resolveClassOrProblem(returnTypeDesc, clazz) { createMethodLocation(clazz, method) }
  }
}
