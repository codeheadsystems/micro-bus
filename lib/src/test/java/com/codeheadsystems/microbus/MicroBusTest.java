package com.codeheadsystems.microbus;

import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.function.Function;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class MicroBusTest {

  private static final UUID UUID_1 = UUID.randomUUID();

  private ContextFactory contextFactory;
  private ExecutorService executorService;
  private MicroBus<String> bus;

  @BeforeEach
  void setUp() {
    contextFactory = new ContextFactory(() -> UUID_1);
    executorService = Executors.newSingleThreadExecutor();
    bus = new MicroBus<>(contextFactory, executorService);
  }

  @Test
  void defaultConstructor() {
    MicroBus<String> defaultBus = new MicroBus<>(contextFactory);
    assertNotNull(defaultBus);
  }

  @Test
  void nullContextFactory_throws() {
    assertThrows(NullPointerException.class, () -> new MicroBus<String>(null, executorService));
  }

  @Test
  void nullExecutorService_throws() {
    assertThrows(NullPointerException.class, () -> new MicroBus<String>(contextFactory, null));
  }

  @Test
  void publish_sync_deliversToSubscribers() {
    List<String> received = new ArrayList<>();
    bus.subscribe(msg -> received.add(msg.data()));

    Message<String> message = new Message<>("hello", false, UUID_1);
    bus.publish(message);

    assertEquals(List.of("hello"), received);
  }

  @Test
  void publish_sync_deliversToMultipleSubscribers() {
    List<String> first = new ArrayList<>();
    List<String> second = new ArrayList<>();
    bus.subscribe(msg -> first.add(msg.data()));
    bus.subscribe(msg -> second.add(msg.data()));

    bus.publish(new Message<>("data", false, UUID_1));

    assertEquals(List.of("data"), first);
    assertEquals(List.of("data"), second);
  }

  @Test
  void publish_sync_setsContextForSubscribers() {
    List<UUID> contextUuids = new ArrayList<>();
    bus.subscribe(msg -> contextUuids.add(contextFactory.contextUuid()));

    bus.publish(new Message<>("test", false, UUID_1));

    assertEquals(1, contextUuids.size());
    assertNotNull(contextUuids.get(0));
  }

  @Test
  void publish_sync_subscribersShareSameContext() {
    List<UUID> contextUuids = new ArrayList<>();
    bus.subscribe(msg -> contextUuids.add(contextFactory.contextUuid()));
    bus.subscribe(msg -> contextUuids.add(contextFactory.contextUuid()));

    bus.publish(new Message<>("test", false, UUID_1));

    assertEquals(2, contextUuids.size());
    assertEquals(contextUuids.get(0), contextUuids.get(1));
  }

  @Test
  void publish_sync_contextClearedAfterPublish() {
    bus.subscribe(msg -> {});
    bus.publish(new Message<>("test", false, UUID_1));

    assertNull(contextFactory.contextUuid());
  }

  @Test
  void publish_sync_subscriberException_doesNotStopOthers() {
    List<String> received = new ArrayList<>();
    bus.subscribe(msg -> { throw new RuntimeException("boom"); });
    bus.subscribe(msg -> received.add(msg.data()));

    bus.publish(new Message<>("hello", false, UUID_1));

    assertEquals(List.of("hello"), received);
  }

  @Test
  void publish_async_deliversToSubscribers() throws Exception {
    CountDownLatch latch = new CountDownLatch(1);
    List<String> received = new ArrayList<>();

    bus.subscribe(msg -> {
      received.add(msg.data());
      latch.countDown();
    });

    bus.publish(new Message<>("async-data", true, UUID_1));

    assertTrue(latch.await(5, TimeUnit.SECONDS));
    assertEquals(List.of("async-data"), received);
  }

  @Test
  void publish_async_setsContextForSubscribers() throws Exception {
    CountDownLatch latch = new CountDownLatch(1);
    List<UUID> contextUuids = new ArrayList<>();

    bus.subscribe(msg -> {
      contextUuids.add(contextFactory.contextUuid());
      latch.countDown();
    });

    bus.publish(new Message<>("test", true, UUID_1));

    assertTrue(latch.await(5, TimeUnit.SECONDS));
    assertEquals(1, contextUuids.size());
    assertNotNull(contextUuids.get(0));
  }

  @Test
  void publish_async_subscriberException_doesNotStopOthers() throws Exception {
    CountDownLatch latch = new CountDownLatch(1);
    List<String> received = new ArrayList<>();

    bus.subscribe(msg -> { throw new RuntimeException("boom"); });
    bus.subscribe(msg -> {
      received.add(msg.data());
      latch.countDown();
    });

    bus.publish(new Message<>("hello", true, UUID_1));

    assertTrue(latch.await(5, TimeUnit.SECONDS));
    assertEquals(List.of("hello"), received);
  }

  @Test
  void unsubscribe_removesSubscriber() {
    List<String> received = new ArrayList<>();
    Consumer<Message<String>> subscriber = msg -> received.add(msg.data());

    bus.subscribe(subscriber);
    bus.publish(new Message<>("first", false, UUID_1));
    bus.unsubscribe(subscriber);
    bus.publish(new Message<>("second", false, UUID_1));

    assertEquals(List.of("first"), received);
  }

  @Test
  void publish_noSubscribers_doesNotThrow() {
    assertDoesNotThrow(() -> bus.publish(new Message<>("orphan", false, UUID_1)));
  }

  @Test
  void subscribe_nullSubscriber_throws() {
    assertThrows(NullPointerException.class, () -> bus.subscribe(null));
  }

  @Test
  void registerContextFiller_nullFiller_throws() {
    assertThrows(NullPointerException.class, () -> bus.registerContextFiller(null));
  }

  @Test
  void registerContextFiller_fillerPopulatesContextBeforeSubscribers() {
    bus.registerContextFiller(data -> Map.of("key", data.toUpperCase()));

    List<Object> captured = new ArrayList<>();
    bus.subscribe(msg -> captured.add(contextFactory.currentContext().orElseThrow().properties().get("key")));

    bus.publish(new Message<>("hello", false, UUID_1));

    assertEquals(List.of("HELLO"), captured);
  }

  @Test
  void registerContextFiller_multipleFillers_allInvoked() {
    bus.registerContextFiller(data -> Map.of("a", "1"));
    bus.registerContextFiller(data -> Map.of("b", "2"));

    List<Object> capturedA = new ArrayList<>();
    List<Object> capturedB = new ArrayList<>();
    bus.subscribe(msg -> {
      Context ctx = contextFactory.currentContext().orElseThrow();
      capturedA.add(ctx.properties().get("a"));
      capturedB.add(ctx.properties().get("b"));
    });

    bus.publish(new Message<>("test", false, UUID_1));

    assertEquals(List.of("1"), capturedA);
    assertEquals(List.of("2"), capturedB);
  }

  @Test
  void registerContextFiller_fillerException_doesNotStopOtherFillersOrSubscribers() {
    bus.registerContextFiller(data -> { throw new RuntimeException("filler boom"); });
    bus.registerContextFiller(data -> Map.of("survived", true));

    List<Object> captured = new ArrayList<>();
    bus.subscribe(msg -> captured.add(contextFactory.currentContext().orElseThrow().properties().get("survived")));

    bus.publish(new Message<>("test", false, UUID_1));

    assertEquals(List.of(true), captured);
  }

  @Test
  void unregisterContextFiller_removesFiller() {
    Function<String, Map<String, Object>> filler = data -> Map.of("key", "value");
    bus.registerContextFiller(filler);

    List<Object> captured = new ArrayList<>();
    bus.subscribe(msg -> captured.add(contextFactory.currentContext().orElseThrow().properties().get("key")));

    bus.publish(new Message<>("first", false, UUID_1));
    bus.unregisterContextFiller(filler);
    bus.publish(new Message<>("second", false, UUID_1));

    assertEquals(2, captured.size());
    assertEquals("value", captured.get(0));
    assertNull(captured.get(1));
  }

  @Test
  void registerContextFiller_async_fillerPopulatesContext() throws Exception {
    CountDownLatch latch = new CountDownLatch(1);
    bus.registerContextFiller(data -> Map.of("async-key", data));

    List<Object> captured = new ArrayList<>();
    bus.subscribe(msg -> {
      captured.add(contextFactory.currentContext().orElseThrow().properties().get("async-key"));
      latch.countDown();
    });

    bus.publish(new Message<>("async-val", true, UUID_1));

    assertTrue(latch.await(5, TimeUnit.SECONDS));
    assertEquals(List.of("async-val"), captured);
  }

}
