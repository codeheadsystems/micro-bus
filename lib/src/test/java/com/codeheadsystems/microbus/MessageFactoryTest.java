package com.codeheadsystems.microbus;

import static org.junit.jupiter.api.Assertions.*;

import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class MessageFactoryTest {

  private static final UUID UUID_1 = UUID.randomUUID();

  private MessageFactory messageFactory;

  @BeforeEach
  void setUp() {
    messageFactory = new MessageFactory(() -> UUID_1);
  }

  @Test
  void defaultConstructor() {
    MessageFactory factory = new MessageFactory();
    assertNotNull(factory);
  }

  @Test
  void nullUuidSupplier_throws() {
    assertThrows(NullPointerException.class, () -> new MessageFactory(null));
  }

  @Test
  void create_async() {
    Message<String> message = messageFactory.create("hello", true);

    assertEquals("hello", message.data());
    assertTrue(message.async());
    assertEquals(UUID_1, message.uuid());
  }

  @Test
  void create_sync() {
    Message<String> message = messageFactory.create("hello", false);

    assertEquals("hello", message.data());
    assertFalse(message.async());
    assertEquals(UUID_1, message.uuid());
  }

  @Test
  void async_createsAsyncMessage() {
    Message<Integer> message = messageFactory.async(42);

    assertEquals(42, message.data());
    assertTrue(message.async());
    assertEquals(UUID_1, message.uuid());
  }

  @Test
  void sync_createsSyncMessage() {
    Message<Integer> message = messageFactory.sync(42);

    assertEquals(42, message.data());
    assertFalse(message.async());
    assertEquals(UUID_1, message.uuid());
  }

}
