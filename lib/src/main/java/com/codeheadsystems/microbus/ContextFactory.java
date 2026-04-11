package com.codeheadsystems.microbus;

import java.util.Objects;
import java.util.Optional;
import java.util.Stack;
import java.util.UUID;
import java.util.function.Supplier;
import javax.inject.Inject;
import javax.inject.Singleton;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Singleton
public class ContextFactory {

  private static final Logger LOGGER = LoggerFactory.getLogger(ContextFactory.class);

  private final ThreadLocal<Stack<Context>> threadLocalContext;
  private final Supplier<Context> contextSupplier;

  public ContextFactory() {
    this(new DefaultUUIDSupplier());
  }

  @Inject
  public ContextFactory(Supplier<UUID> uuidSupplier) {
    Objects.requireNonNull(uuidSupplier);
    this.contextSupplier = () -> new Context(uuidSupplier.get());
    this.threadLocalContext = ThreadLocal.withInitial(Stack::new);
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
    LOGGER.info("Clearing all contexts"); // warn folks because this means something is wonky, or about to be.
    threadLocalContext.remove();
  }

  public <T> T withSupplier(Supplier<T> supplier) {
    return withContextSet(supplier);
  }

  public void withRunnable(Runnable runnable) {
    withContextSet(() -> {
      runnable.run();
      return null;
    });
  }

  private <T> T withContextSet(Supplier<T> supplier) {
    final Context context = contextSupplier.get();
    final Stack<Context> stack = threadLocalContext.get();
    stack.push(context);
    try {
      return supplier.get();
    } finally {
      if (stack.isEmpty()) { // someone cleared the stack?
        LOGGER.warn("Context stack is not empty after supplier execution, possible leak: {}", stack);
      } else { // stack isn't empty
        final Context old = stack.pop();
        if (old != context) {  // Who's been messing with the stack?
          LOGGER.warn("Incorrect context removed desired:{} removed:{}", context, old);
        }
      }
    }
  }

}
