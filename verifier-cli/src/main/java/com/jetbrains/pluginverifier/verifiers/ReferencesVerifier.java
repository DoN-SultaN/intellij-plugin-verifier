package com.jetbrains.pluginverifier.verifiers;

import com.intellij.structure.domain.Plugin;
import com.intellij.structure.resolvers.Resolver;
import com.jetbrains.pluginverifier.VerificationContext;
import com.jetbrains.pluginverifier.Verifier;
import com.jetbrains.pluginverifier.error.VerificationError;
import com.jetbrains.pluginverifier.misc.DependenciesCache;
import com.jetbrains.pluginverifier.problems.FailedToReadClassProblem;
import com.jetbrains.pluginverifier.problems.MissingDependencyProblem;
import com.jetbrains.pluginverifier.results.ProblemLocation;
import com.jetbrains.pluginverifier.verifiers.clazz.ClassVerifier;
import com.jetbrains.pluginverifier.verifiers.field.FieldVerifier;
import com.jetbrains.pluginverifier.verifiers.instruction.InstructionVerifier;
import com.jetbrains.pluginverifier.verifiers.method.MethodVerifier;
import org.jetbrains.annotations.NotNull;
import org.objectweb.asm.tree.*;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * @author Dennis.Ushakov
 */
class ReferencesVerifier implements Verifier {

  @Override
  public void verify(@NotNull Plugin plugin, @NotNull VerificationContext ctx) throws VerificationError {
    final Resolver pluginPool = plugin.getPluginClassPool();

    DependenciesCache.PluginDependenciesDescriptor descriptor = DependenciesCache.getInstance().getResolver(plugin, ctx.getIde(), ctx.getJdk(), ctx.getExternalClassPath());
    Resolver cacheResolver = Resolver.createCacheResolver(descriptor.getResolver());

    processMissingDependencies(descriptor, ctx);

    final Collection<String> classes = pluginPool.getAllClasses();
    for (String className : classes) {
      final ClassNode node = pluginPool.findClass(className);

      if (node == null) {
        ctx.registerProblem(new FailedToReadClassProblem(className), ProblemLocation.fromClass(className));
        continue;
      }

      verifyClass(cacheResolver, node, ctx);
    }
  }

  private void processMissingDependencies(@NotNull DependenciesCache.PluginDependenciesDescriptor descriptor, @NotNull VerificationContext ctx) {
    String pluginName = descriptor.getPluginName();
    Map<String, String> missingDependencies = descriptor.getMissingDependencies().get(pluginName);
    if (missingDependencies != null) {
      for (Map.Entry<String, String> entry : missingDependencies.entrySet()) {
        ctx.registerProblem(new MissingDependencyProblem(entry.getKey(), entry.getValue()), ProblemLocation.fromPlugin(pluginName));
      }
    }


  }

  @SuppressWarnings("unchecked")
  private void verifyClass(@NotNull Resolver resolver, @NotNull ClassNode node, @NotNull VerificationContext ctx) {
    for (ClassVerifier verifier : Verifiers.getClassVerifiers()) {
      verifier.verify(node, resolver, ctx);
    }

    List<MethodNode> methods = (List<MethodNode>) node.methods;
    for (MethodNode method : methods) {
      for (MethodVerifier verifier : Verifiers.getMemberVerifiers()) {
        verifier.verify(node, method, resolver, ctx);
      }

      final InsnList instructions = method.instructions;
      for (Iterator<AbstractInsnNode> i = instructions.iterator(); i.hasNext(); ) {
        AbstractInsnNode instruction = i.next();
        for (InstructionVerifier verifier : Verifiers.getInstructionVerifiers()) {
          verifier.verify(node, method, instruction, resolver, ctx);
        }
      }
    }

    List<FieldNode> fields = (List<FieldNode>) node.fields;
    for (FieldNode field : fields) {
      for (FieldNode method : fields) {
        for (FieldVerifier verifier : Verifiers.getFieldVerifiers()) {
          verifier.verify(node, field, resolver, ctx);
        }
      }
    }
  }
}
