package com.codeheadsystems.microbus;

import java.util.Objects;
import java.util.Optional;
import java.util.Stack;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.function.Supplier;
import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class ContextFactory {

  private final ThreadLocal<Stack<Context>> threadLocalContext;
  private final Supplier<UUID> uuidSupplier;

  public ContextFactory() {
    this(new DefaultUUIDSupplier());
  }

  @Inject
  public ContextFactory(Supplier<UUID> uuidSupplier) {
    this.uuidSupplier = Objects.requireNonNull(uuidSupplier);
    threadLocalContext = ThreadLocal.withInitial(Stack::new);
  }

  /**
   * Returns the current context, if any is set.
   *
   * @return an optional if a context is set.
   */
  public Optional<Context> currentContext() {
    Stack<Context> stack = threadLocalContext.get();
    if (stack.isEmpty()) {
      return Optional.empty();
    } else {
      return Optional.of(stack.peek());
    }
  }

  /**
   * Returns the current context UUID if set. May be null.
   *
   * @return UUID or null
   */
  public UUID contextUuid() {
    return currentContext()
        .map(Context::uuid)
        .orElse(null);
  }

  /**
   * Removes the full stack of context.
   */
  public void clearAllContexts() {
    threadLocalContext.remove();
  }

  public <T> T withSupplier(Supplier<T> supplier) {
    try {
      threadLocalContext.get().push(generateContext());
      return supplier.get();
    } finally {
      ifNotEmpty(Stack::pop);
    }
  }

  public void withRunnable(Runnable runnable) {
    try {
      threadLocalContext.get().push(generateContext());
      runnable.run();
    } finally {
      ifNotEmpty(Stack::pop);
    }
  }

  private Context generateContext() {
    return new Context(uuidSupplier.get());
  }

  private void ifNotEmpty(Consumer<Stack<Context>> consumer) {
    Stack<Context> stack = threadLocalContext.get();
    if (!stack.isEmpty()) {
      consumer.accept(stack);
    }
  }
}
