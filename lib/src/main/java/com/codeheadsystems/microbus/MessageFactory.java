package com.codeheadsystems.microbus;

import java.util.Objects;
import java.util.UUID;
import java.util.function.Supplier;
import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class MessageFactory {

  private final Supplier<UUID> uuidSupplier;
  private final ThreadLocal<UUID> threadLocalContextUUID;

  public MessageFactory() {
    this(new DefaultUUIDSupplier());
  }

  @Inject
  public MessageFactory(Supplier<UUID> uuidSupplier) {
    this.uuidSupplier = Objects.requireNonNull(uuidSupplier);
    this.threadLocalContextUUID = ThreadLocal.withInitial(this.uuidSupplier);
  }

  public UUID getContextUUID() {
    return threadLocalContextUUID.get();
  }

  public void clearContextUUID() {
    threadLocalContextUUID.remove();
  }

  public <T> T doWithContext(Supplier<T> supplier) {
    try {
      getContextUUID();
      return supplier.get();
    } finally {
      clearContextUUID();
    }
  }

  public void doWithContext(Runnable runnable) {
    try {
      getContextUUID();
      runnable.run();
    } finally {
      clearContextUUID();
    }
  }

  public <D> Message<D> create(D data, boolean async) {
    return new Message<>(data, async, getContextUUID(), uuidSupplier.get());
  }

  public <D> Message<D> async(D data) {
    return create(data, true);
  }

  public <D> Message<D> sync(D data) {
    return create(data, false);
  }


}
