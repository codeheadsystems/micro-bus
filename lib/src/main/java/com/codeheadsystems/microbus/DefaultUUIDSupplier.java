package com.codeheadsystems.microbus;

import com.fasterxml.uuid.Generators;
import com.fasterxml.uuid.impl.TimeBasedEpochGenerator;
import java.util.UUID;
import java.util.function.Supplier;

public class DefaultUUIDSupplier implements Supplier<UUID> {

  private final TimeBasedEpochGenerator generator;

  public DefaultUUIDSupplier() {
    this.generator = Generators.timeBasedEpochGenerator();
  }

  @Override
  public UUID get() {
    return generator.generate();
  }

}
