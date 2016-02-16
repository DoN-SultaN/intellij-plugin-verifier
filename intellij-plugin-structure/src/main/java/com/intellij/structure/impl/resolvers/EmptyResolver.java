package com.intellij.structure.impl.resolvers;

import com.intellij.structure.resolvers.Resolver;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.objectweb.asm.tree.ClassNode;

import java.util.Collection;
import java.util.Collections;

/**
 * @author Sergey Patrikeev
 */
public class EmptyResolver extends Resolver {

  public static final Resolver INSTANCE = new EmptyResolver();

  @Nullable
  @Override
  public ClassNode findClass(@NotNull String className) {
    return null;
  }

  @Nullable
  @Override
  public Resolver getClassLocation(@NotNull String className) {
    return null;
  }

  @Override
  @NotNull
  public Collection<String> getAllClasses() {
    return Collections.emptySet();
  }

  @Override
  public boolean isEmpty() {
    return true;
  }

  @Override
  public String toString() {
    return "EmptyResolver";
  }
}
