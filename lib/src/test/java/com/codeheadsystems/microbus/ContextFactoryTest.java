package com.codeheadsystems.microbus;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ContextFactoryTest {

  private static final UUID UUID_1 = UUID.randomUUID();
  private static final UUID UUID_2 = UUID.randomUUID();

  private ContextFactory contextFactory;

  @BeforeEach
  void setUp() {
    Supplier<UUID> uuidSupplier = new Supplier<>() {
      private int count = 0;
      @Override
      public UUID get() {
        return (count++ % 2 == 0) ? UUID_1 : UUID_2;
      }
    };
    contextFactory = new ContextFactory(uuidSupplier);
  }

  @Test
  void defaultConstructor() {
    ContextFactory factory = new ContextFactory();
    assertNotNull(factory);
    assertEquals(Optional.empty(), factory.currentContext());
  }

  @Test
  void nullUuidSupplier_throws() {
    assertThrows(NullPointerException.class, () -> new ContextFactory(null));
  }

  @Test
  void currentContext_empty_whenNoContext() {
    assertEquals(Optional.empty(), contextFactory.currentContext());
  }

  @Test
  void contextUuid_null_whenNoContext() {
    assertNull(contextFactory.contextUuid());
  }

  @Test
  void withContext_supplier_setsAndClearsSupplier() {
    String result = contextFactory.withSupplier(() -> {
      Optional<Context> ctx = contextFactory.currentContext();
      assertTrue(ctx.isPresent());
      assertEquals(UUID_1, ctx.get().uuid());
      assertNotNull(ctx.get().properties());
      assertEquals(UUID_1, contextFactory.contextUuid());
      return "done";
    });

    assertEquals("done", result);
    assertEquals(Optional.empty(), contextFactory.currentContext());
  }

  @Test
  void withContext_runnable_setsAndClearsRunnable() {
    contextFactory.withRunnable(() -> {
      Optional<Context> ctx = contextFactory.currentContext();
      assertTrue(ctx.isPresent());
      assertEquals(UUID_1, ctx.get().uuid());
      assertEquals(UUID_1, contextFactory.contextUuid());
    });

    assertEquals(Optional.empty(), contextFactory.currentContext());
  }

  @Test
  void withContext_supplier_clearsSupplier_onException() {
    assertThrows(RuntimeException.class, () ->
        contextFactory.withSupplier((Supplier<Object>) () -> {
          throw new RuntimeException("boom");
        })
    );

    assertEquals(Optional.empty(), contextFactory.currentContext());
  }

  @Test
  void withContext_runnable_clearsRunnable_onException() {
    assertThrows(RuntimeException.class, () ->
        contextFactory.withRunnable((Runnable) () -> {
          throw new RuntimeException("boom");
        })
    );

    assertEquals(Optional.empty(), contextFactory.currentContext());
  }

  @Test
  void withSupplier_nested_pushesAndPops() {
    contextFactory.withRunnable(() -> {
      assertEquals(UUID_1, contextFactory.contextUuid());

      contextFactory.withRunnable(() -> {
        assertEquals(UUID_2, contextFactory.contextUuid());
      });

      assertEquals(UUID_1, contextFactory.contextUuid());
    });

    assertNull(contextFactory.contextUuid());
  }

  @Test
  void clearAllContexts_removesEntireStack() {
    // Manually push contexts via doWithContext, then clear from outside
    contextFactory.withRunnable(() -> {
      assertNotNull(contextFactory.contextUuid());
    });
    // After doWithContext completes, stack is empty. Verify clearAllContexts works on a fresh stack.
    contextFactory.clearAllContexts();
    assertEquals(Optional.empty(), contextFactory.currentContext());
  }

  @Test
  void clearAllContexts_insideWithRunnable_finallySurvivesEmptyStack() {
    contextFactory.withRunnable(() -> {
      contextFactory.clearAllContexts();
      assertEquals(Optional.empty(), contextFactory.currentContext());
    });
    // The finally block in withRunnable encounters an empty stack via ifNotEmpty — no exception thrown.
    assertEquals(Optional.empty(), contextFactory.currentContext());
  }

  @Test
  void clearAllContexts_insideWithSupplier_finallySurvivesEmptyStack() {
    String result = contextFactory.withSupplier(() -> {
      contextFactory.clearAllContexts();
      assertEquals(Optional.empty(), contextFactory.currentContext());
      return "ok";
    });
    assertEquals("ok", result);
    assertEquals(Optional.empty(), contextFactory.currentContext());
  }

}
