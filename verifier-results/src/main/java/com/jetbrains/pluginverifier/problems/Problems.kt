package com.jetbrains.pluginverifier.problems

import com.google.gson.annotations.SerializedName
import com.jetbrains.pluginverifier.location.*
import com.jetbrains.pluginverifier.reference.ClassReference
import com.jetbrains.pluginverifier.reference.FieldReference
import com.jetbrains.pluginverifier.reference.MethodReference

/**
 * @author Sergey Patrikeev
 */
interface Problem {
  fun getDescription(): String

  fun getEffect(): String
}

private fun AccessFlags.classOrInterface(): String = if (this.contains(AccessFlags.Flag.INTERFACE)) "interface" else "class"

data class MultipleDefaultImplementationsProblem(@SerializedName("caller") val caller: MethodLocation,
                                                 @SerializedName("methodReference") val methodReference: MethodReference,
                                                 @SerializedName("instruction") val instruction: Instruction,
                                                 @SerializedName("implementation1") val implementation1: MethodLocation,
                                                 @SerializedName("implementation2") val implementation2: MethodLocation) : Problem {
  override fun getDescription(): String = "multiple default implementations of method $caller"

  override fun getEffect(): String = "Method $caller contains an *$instruction* instruction referencing a method reference $methodReference which has several default implementations: $implementation1 and $implementation2. This can lead to **IncompatibleClassChangeError** exception at runtime."
}

data class IllegalClassAccessProblem(@SerializedName("unavailableClass") val unavailableClass: ClassLocation,
                                     @SerializedName("access") val access: AccessType,
                                     @SerializedName("usage") val usage: ProblemLocation) : Problem {
  override fun getDescription(): String = "illegal access to $access class $unavailableClass"

  override fun getEffect(): String {
    val type = unavailableClass.accessFlags.classOrInterface()
    return "${access.toString().capitalize()} $type $unavailableClass is not available at $usage"
  }
}

data class AbstractClassInstantiationProblem(@SerializedName("abstractClass") val abstractClass: ClassLocation,
                                             @SerializedName("creator") val creator: MethodLocation) : Problem {
  override fun getDescription(): String = "instantiation of an abstract class $abstractClass"

  override fun getEffect(): String = "Method $creator has instantiation *new* instruction referencing an abstract class $abstractClass. This can lead to **InstantiationError** exception at runtime."
}

data class ClassNotFoundProblem(@SerializedName("class") val unknownClass: ClassReference,
                                @SerializedName("usage") val usage: ProblemLocation) : Problem {
  override fun getDescription(): String = "accessing to unknown class $unknownClass"

  override fun getEffect(): String {
    val type: String = when (usage) {
      is ClassLocation -> "Class"
      is MethodLocation -> "Method"
      is FieldLocation -> "Field"
      else -> throw IllegalArgumentException()
    }
    return "$type $usage references an unresolved class $unknownClass. This can lead to **NoSuchClassError** exception at runtime."
  }
}

data class SuperClassBecameInterfaceProblem(@SerializedName("child") val child: ClassLocation,
                                            @SerializedName("interface") val interfaze: ClassLocation) : Problem {
  override fun getDescription(): String = "incompatible change of super class $interfaze to interface"

  override fun getEffect(): String = "Class $child has a *super class* $interfaze which is actually an *interface*. This can lead to **IncompatibleClassChangeError** at runtime."
}

data class InvokeClassMethodOnInterfaceProblem(@SerializedName("methodReference") val methodReference: MethodReference,
                                               @SerializedName("caller") val caller: MethodLocation,
                                               @SerializedName("instruction") val instruction: Instruction) : Problem {
  override fun getDescription(): String = "incompatible change of class ${methodReference.hostClass} to interface"

  override fun getEffect(): String = "Method $caller has invocation *$instruction* instruction referencing a *class* method $methodReference, but the method's host ${methodReference.hostClass} is an *interface*. This can lead to **IncompatibleClassChangeError** at runtime."
}

data class SuperInterfaceBecameClassProblem(@SerializedName("child") val child: ClassLocation,
                                            @SerializedName("class") val clazz: ClassLocation) : Problem {
  override fun getDescription(): String = "incompatible change of super interface $clazz to class"

  override fun getEffect(): String {
    val type = if (child.accessFlags.contains(AccessFlags.Flag.INTERFACE)) "Interface" else "Class"
    return "$type $child has a *super interface* $clazz which is actually a *class*. This can lead to **IncompatibleClassChangeError** exception at runtime."
  }
}

data class InvokeInterfaceMethodOnClassProblem(@SerializedName("methodReference") val methodReference: MethodReference,
                                               @SerializedName("caller") val caller: MethodLocation,
                                               @SerializedName("instruction") val instruction: Instruction) : Problem {
  override fun getDescription(): String = "incompatible change of interface ${methodReference.hostClass} to class"

  override fun getEffect(): String = "Method $caller has invocation *$instruction* instruction referencing an *interface* method $methodReference, but the method's host ${methodReference.hostClass} is a *class*. This can lead to **IncompatibleClassChangeError** at runtime."
}

data class InheritFromFinalClassProblem(@SerializedName("child") val child: ClassLocation,
                                        @SerializedName("finalClass") val finalClass: ClassLocation) : Problem {
  override fun getDescription(): String = "inheritance from a final class $finalClass"

  override fun getEffect(): String {
    val type = if (child.accessFlags.contains(AccessFlags.Flag.INTERFACE)) "Interface" else "Class"
    return "$type $child inherits from a final class $finalClass. This can lead to **VerifyError** exception at runtime."
  }
}

data class InterfaceInstantiationProblem(@SerializedName("interface") val interfaze: ClassLocation,
                                         @SerializedName("creator") val creator: MethodLocation) : Problem {
  override fun getDescription(): String = "instantiation an interface $interfaze"

  override fun getEffect(): String = "Method $creator has instantiation *new* instruction referencing an interface $interfaze. This can lead to **InstantiationError** exception at runtime."
}

data class ChangeFinalFieldProblem(@SerializedName("field") val field: FieldLocation,
                                   @SerializedName("accessor") val accessor: MethodLocation,
                                   @SerializedName("instruction") val instruction: Instruction) : Problem {
  override fun getDescription(): String = "attempt to change a final field $field"

  override fun getEffect(): String = "Method $accessor has modifying instruction *$instruction* referencing a final field $field. This can lead to **IllegalAccessError** exception at runtime."
}

data class FieldNotFoundProblem(@SerializedName("field") val field: FieldReference,
                                @SerializedName("accessor") val accessor: MethodLocation,
                                @SerializedName("instruction") val instruction: Instruction) : Problem {
  override fun getDescription(): String = "accessing to unknown field $field"

  override fun getEffect(): String = "Method $accessor contains a *$instruction* instruction referencing an unresolved field $field. This can lead to **NoSuchFieldError** exception at runtime."
}

data class IllegalFieldAccessProblem(@SerializedName("field") val field: FieldLocation,
                                     @SerializedName("accessor") val accessor: MethodLocation,
                                     @SerializedName("instruction") val instruction: Instruction,
                                     @SerializedName("access") val fieldAccess: AccessType) : Problem {
  override fun getDescription(): String = "illegal access of $fieldAccess field $field"

  override fun getEffect(): String = "Method $accessor contains a *$instruction* instruction referencing a $fieldAccess field $field that a class ${accessor.hostClass} doesn't have access to. This can lead to **IllegalAccessError** exception at runtime."
}

data class IllegalMethodAccessProblem(@SerializedName("method") val method: MethodLocation,
                                      @SerializedName("caller") val caller: MethodLocation,
                                      @SerializedName("instruction") val instruction: Instruction,
                                      @SerializedName("access") val methodAccess: AccessType) : Problem {
  override fun getDescription(): String = "illegal invocation of $methodAccess method $method"

  override fun getEffect(): String = "Method $caller contains an *$instruction* instruction referencing a $methodAccess method $method that a class ${caller.hostClass} doesn't have access to. This can lead to **IllegalAccessError** exception at runtime."
}

data class InvokeInterfaceOnPrivateMethodProblem(@SerializedName("resolvedMethod") val resolvedMethod: MethodLocation,
                                                 @SerializedName("caller") val caller: MethodLocation) : Problem {
  override fun getDescription(): String = "attempt to perform 'invokeinterface' on private method $resolvedMethod"

  override fun getEffect(): String = "Method $caller contains an *invokeinterface* instruction referencing a private method $resolvedMethod. This can lead to **IncompatibleClassChangeError** exception at runtime."
}

data class MethodNotFoundProblem(@SerializedName("method") val method: MethodReference,
                                 @SerializedName("caller") val caller: MethodLocation,
                                 @SerializedName("instruction") val instruction: Instruction) : Problem {
  override fun getDescription(): String = "invoking unknown method $method"

  override fun getEffect(): String = "Method $caller contains an *$instruction* instruction referencing an unresolved method $method. This can lead to **NoSuchMethodError** exception at runtime."
}

data class MethodNotImplementedProblem(@SerializedName("method") val method: MethodLocation,
                                       @SerializedName("incompleteClass") val incompleteClass: ClassLocation) : Problem {
  override fun getDescription(): String = "method isn't implemented $method"

  override fun getEffect() = "Non-abstract class $incompleteClass inherits from ${method.hostClass} but doesn't implement the abstract method ${method.methodNameAndParameters()}. This can lead to **AbstractMethodError** exception at runtime."
}

data class AbstractMethodInvocationProblem(@SerializedName("method") val method: MethodLocation,
                                           @SerializedName("caller") val caller: MethodLocation,
                                           @SerializedName("instruction") val instruction: Instruction) : Problem {
  override fun getDescription(): String = "attempt to invoke an abstract method $method"

  override fun getEffect(): String = "Method $caller contains an *$instruction* instruction referencing a method $method which doesn't have a non-abstract implementation. This can lead to **AbstractMethodError** exception at runtime."
}

data class OverridingFinalMethodProblem(@SerializedName("method") val method: MethodLocation,
                                        @SerializedName("invalidClass") val invalidClass: ClassLocation) : Problem {
  override fun getDescription(): String = "overriding final method $method"

  override fun getEffect() = "Class $invalidClass overrides the final method $method. This can lead to **VerifyError** exception at runtime."
}

data class NonStaticAccessOfStaticFieldProblem(@SerializedName("field") val field: FieldLocation,
                                               @SerializedName("accessor") val accessor: MethodLocation,
                                               @SerializedName("instruction") val instruction: Instruction) : Problem {
  override fun getDescription(): String = "attempt to perform instance access on a static field $field"

  override fun getEffect(): String = "Method $accessor has non-static access instruction *$instruction* referencing a static field $field. This can lead to **IncompatibleClassChangeError** exception at runtime."
}

data class InvokeStaticOnNonStaticMethodProblem(@SerializedName("resolvedMethod") val resolvedMethod: MethodLocation,
                                                @SerializedName("caller") val caller: MethodLocation) : Problem {
  override fun getDescription(): String = "attempt to perform 'invokestatic' on a non-static method $caller"

  override fun getEffect(): String = "Method $caller contains an *invokestatic* instruction referencing a non-static method $resolvedMethod. This can lead to **IncompatibleClassChangeError** exception at runtime."
}

data class InvokeNonStaticInstructionOnStaticMethodProblem(@SerializedName("resolvedMethod") val resolvedMethod: MethodLocation,
                                                           @SerializedName("caller") val caller: MethodLocation,
                                                           @SerializedName("instruction") val instruction: Instruction) : Problem {
  override fun getDescription(): String = "attempt to perform '$instruction' on a static method $resolvedMethod"

  override fun getEffect(): String = "Method $caller contains an *$instruction* instruction referencing a static method $resolvedMethod. This can lead to **IncompatibleClassChangeError** exception at runtime."
}

data class StaticAccessOfNonStaticFieldProblem(@SerializedName("field") val field: FieldLocation,
                                               @SerializedName("accessor") val accessor: MethodLocation,
                                               @SerializedName("instruction") val instruction: Instruction) : Problem {
  override fun getDescription(): String = "attempt to perform static access on an instance field $field"

  override fun getEffect(): String = "Method $accessor has static access instruction *$instruction* referencing a non-static field $field. This can lead to **IncompatibleClassChangeError** exception at runtime."
}

enum class Instruction(private val type: String) {
  GET_STATIC("getstatic"),
  PUT_STATIC("putstatic"),
  PUT_FIELD("putfield"),
  GET_FIELD("getfield"),
  INVOKE_VIRTUAL("invokevirtual"),
  INVOKE_INTERFACE("invokeinterface"),
  INVOKE_STATIC("invokestatic"),
  INVOKE_SPECIAL("invokespecial");

  override fun toString(): String = type
}

enum class AccessType(private val type: String) {
  PUBLIC("public"),
  PROTECTED("protected"),
  PACKAGE_PRIVATE("package-private"),
  PRIVATE("private");

  override fun toString(): String = type
}