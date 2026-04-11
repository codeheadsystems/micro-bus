package com.codeheadsystems.microbus;

import java.util.Objects;
import java.util.UUID;
import java.util.function.Supplier;
import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class MessageFactory {

  private final Supplier<UUID> uuidSupplier;

  public MessageFactory() {
    this(new DefaultUUIDSupplier());
  }

  @Inject
  public MessageFactory(Supplier<UUID> uuidSupplier) {
    this.uuidSupplier = Objects.requireNonNull(uuidSupplier);
  }

  public <D> Message<D> create(D data, boolean async) {
    return new Message<>(data, async, uuidSupplier.get());
  }

  public <D> Message<D> async(D data) {
    return create(data, true);
  }

  public <D> Message<D> sync(D data) {
    return create(data, false);
  }


}
