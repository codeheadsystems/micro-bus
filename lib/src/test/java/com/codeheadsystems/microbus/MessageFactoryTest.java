package com.codeheadsystems.microbus;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.Deque;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;
import org.junit.jupiter.api.Test;

class MessageFactoryTest {

  @Test
  void defaultConstructorCreatesMessagesWithGeneratedContextAndMessageUuids() {
    MessageFactory factory = new MessageFactory();

    Message<String> message = factory.create("payload", false);

    assertEquals("payload", message.data());
    assertFalse(message.async());
    assertNotNull(message.contextUuid());
    assertNotNull(message.messageUuid());
    assertEquals(7, message.contextUuid().version());
    assertEquals(7, message.messageUuid().version());
    assertNotEquals(message.contextUuid(), message.messageUuid());
  }

  @Test
  void supplierConstructorRejectsNullSupplier() {
    assertThrows(NullPointerException.class, () -> new MessageFactory(null));
  }

  @Test
  void getContextUuidCachesValueUntilCleared() {
    UUID firstContext = uuid("00000000-0000-0000-0000-000000000001");
    UUID secondContext = uuid("00000000-0000-0000-0000-000000000002");
    MessageFactory factory = new MessageFactory(sequenceSupplier(firstContext, secondContext));

    assertEquals(firstContext, factory.getContextUUID());
    assertEquals(firstContext, factory.getContextUUID());

    factory.clearContextUUID();

    assertEquals(secondContext, factory.getContextUUID());
  }

  @Test
  void createUsesCurrentContextAndFreshMessageUuid() {
    UUID contextUuid = uuid("00000000-0000-0000-0000-000000000011");
    UUID messageUuid = uuid("00000000-0000-0000-0000-000000000012");
    MessageFactory factory = new MessageFactory(sequenceSupplier(contextUuid, messageUuid));

    Message<String> message = factory.create("payload", true);

    assertEquals("payload", message.data());
    assertTrue(message.async());
    assertEquals(contextUuid, message.contextUuid());
    assertEquals(messageUuid, message.messageUuid());
    assertSame(message.contextUuid(), factory.getContextUUID());
  }

  @Test
  void asyncCreatesAsyncMessage() {
    UUID contextUuid = uuid("00000000-0000-0000-0000-000000000021");
    UUID messageUuid = uuid("00000000-0000-0000-0000-000000000022");
    MessageFactory factory = new MessageFactory(sequenceSupplier(contextUuid, messageUuid));

    Message<String> message = factory.async("payload");

    assertTrue(message.async());
    assertEquals(contextUuid, message.contextUuid());
    assertEquals(messageUuid, message.messageUuid());
  }

  @Test
  void syncCreatesSyncMessage() {
    UUID contextUuid = uuid("00000000-0000-0000-0000-000000000031");
    UUID messageUuid = uuid("00000000-0000-0000-0000-000000000032");
    MessageFactory factory = new MessageFactory(sequenceSupplier(contextUuid, messageUuid));

    Message<String> message = factory.sync("payload");

    assertFalse(message.async());
    assertEquals(contextUuid, message.contextUuid());
    assertEquals(messageUuid, message.messageUuid());
  }

  @Test
  void doWithContextSupplierReturnsValueAndClearsContextAfterward() {
    UUID firstContext = uuid("00000000-0000-0000-0000-000000000041");
    UUID secondContext = uuid("00000000-0000-0000-0000-000000000042");
    MessageFactory factory = new MessageFactory(sequenceSupplier(firstContext, secondContext));
    AtomicReference<UUID> observedContext = new AtomicReference<>();

    String result = factory.doWithContext(() -> {
      observedContext.set(factory.getContextUUID());
      return "done";
    });

    assertEquals("done", result);
    assertEquals(firstContext, observedContext.get());
    assertEquals(secondContext, factory.getContextUUID());
  }

  @Test
  void doWithContextSupplierClearsContextWhenSupplierThrows() {
    UUID firstContext = uuid("00000000-0000-0000-0000-000000000051");
    UUID secondContext = uuid("00000000-0000-0000-0000-000000000052");
    MessageFactory factory = new MessageFactory(sequenceSupplier(firstContext, secondContext));

    IllegalStateException exception = assertThrows(IllegalStateException.class,
        () -> factory.doWithContext(() -> {
          assertEquals(firstContext, factory.getContextUUID());
          throw new IllegalStateException("boom");
        }));

    assertEquals("boom", exception.getMessage());
    assertEquals(secondContext, factory.getContextUUID());
  }

  @Test
  void doWithContextRunnableRunsAndClearsContextAfterward() {
    UUID firstContext = uuid("00000000-0000-0000-0000-000000000061");
    UUID secondContext = uuid("00000000-0000-0000-0000-000000000062");
    MessageFactory factory = new MessageFactory(sequenceSupplier(firstContext, secondContext));
    AtomicReference<UUID> observedContext = new AtomicReference<>();

    factory.doWithContext(() -> observedContext.set(factory.getContextUUID()));

    assertEquals(firstContext, observedContext.get());
    assertEquals(secondContext, factory.getContextUUID());
  }

  @Test
  void doWithContextRunnableClearsContextWhenRunnableThrows() {
    UUID firstContext = uuid("00000000-0000-0000-0000-000000000071");
    UUID secondContext = uuid("00000000-0000-0000-0000-000000000072");
    MessageFactory factory = new MessageFactory(sequenceSupplier(firstContext, secondContext));

    RuntimeException exception = assertThrows(RuntimeException.class,
        () -> factory.doWithContext(() -> {
          assertEquals(firstContext, factory.getContextUUID());
          throw new RuntimeException("boom");
        }));

    assertEquals("boom", exception.getMessage());
    assertEquals(secondContext, factory.getContextUUID());
  }

  private static Supplier<UUID> sequenceSupplier(UUID... uuids) {
    Deque<UUID> remaining = new ArrayDeque<>(Arrays.asList(uuids));
    return () -> {
      UUID next = remaining.pollFirst();
      if (next == null) {
        throw new AssertionError("UUID supplier exhausted");
      }
      return next;
    };
  }

  private static UUID uuid(String value) {
    return UUID.fromString(value);
  }
}

