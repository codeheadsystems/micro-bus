package com.codeheadsystems.microbus;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;
import java.util.function.Function;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Basic implementation for a buss.
 *
 * @param <D> data used for the messages. This can be as simple or complex as you want.
 */
public class MicroBus<D> implements Bus<D> {

  private static final Logger LOGGER = LoggerFactory.getLogger(MicroBus.class);

  private final List<Consumer<Message<D>>> subscribers = new CopyOnWriteArrayList<>();
  private final List<Function<D, Map<String, Object>>> contextFillers = new CopyOnWriteArrayList<>();
  private final ContextFactory contextFactory;
  private final ExecutorService executorService;

  public MicroBus(ContextFactory contextFactory) {
    this(contextFactory, Executors.newCachedThreadPool());
  }

  public MicroBus(ContextFactory contextFactory,
                  ExecutorService executorService) {
    LOGGER.info("MicroBus()");
    this.contextFactory = Objects.requireNonNull(contextFactory);
    this.executorService = Objects.requireNonNull(executorService);
  }

  @Override
  public void registerContextFiller(Function<D, Map<String, Object>> filler) {
    LOGGER.debug("register({})", filler);
    contextFillers.add(Objects.requireNonNull(filler));
  }

  @Override
  public void unregisterContextFiller(Function<D, Map<String, Object>> filler) {
    LOGGER.debug("unregister({})", filler);
    contextFillers.remove(filler);
  }

  @Override
  public void publish(Message<D> message) {
    LOGGER.debug("publish({})", message.uuid());
    if (message.async()) {
      executorService.submit(() -> dispatch(message));
    } else {
      dispatch(message);
    }
  }

  @Override
  public void subscribe(Consumer<Message<D>> subscriber) {
    LOGGER.debug("subscribe({})", subscriber);
    subscribers.add(Objects.requireNonNull(subscriber));
  }

  @Override
  public void unsubscribe(Consumer<Message<D>> subscriber) {
    LOGGER.debug("unsubscribe({})", subscriber);
    subscribers.remove(subscriber);
  }

  private void dispatch(Message<D> message) {
    contextFactory.withRunnable(() -> {
      LOGGER.debug("[{}] dispatch({})", contextFactory.contextUuid(), message.uuid());
      // Fill up the context with any fillers first. Store the results we get.
      Map<String, Object> properties = contextFactory.currentContext()
          .map(ctx -> fillContext(message, ctx))
          .orElseThrow(); // should never happen.
      // Now call each subscriber with the message and note context.
      // Remember we have to copy the properties from the current context if we're doing async stuff.
      for (Consumer<Message<D>> subscriber : subscribers) {
        if (message.async()) {
          dispatchAsync(message, subscriber, properties);
        } else {
          dispatchSync(message, subscriber);
        }
      }
      LOGGER.debug("[{}] Finished dispatching message {}", contextFactory.contextUuid(), message.uuid());
    });
  }

  private void dispatchSync(final Message<D> message,
                            final Consumer<Message<D>> subscriber) {
    LOGGER.trace("[{}] dispatching message {} synchronously to {}", contextFactory.contextUuid(), message.uuid(), subscriber);
    callSubscriber(message, subscriber);
  }

  private void dispatchAsync(final Message<D> message,
                             final Consumer<Message<D>> subscriber,
                             final Map<String, Object> properties) {
    LOGGER.trace("[{}] dispatching message {} asynchronously to {}", contextFactory.contextUuid(), message, subscriber);
    executorService.submit(() -> {
      contextFactory.withRunnable(() -> {
        contextFactory.currentContext().ifPresent(ctx -> ctx.properties().putAll(properties));
        callSubscriber(message, subscriber);
      });
    });
  }

  private void callSubscriber(final Message<D> message,
                              final Consumer<Message<D>> subscriber) {
    try {
      subscriber.accept(message);
    } catch (Exception e) {
      LOGGER.error("[{}] Subscriber threw exception for message {}: {}", contextFactory.contextUuid(), message.uuid(), subscriber, e);
    }
  }

  private Map<String, Object> fillContext(final Message<D> message,
                                          final Context context) {
    LOGGER.debug("[{}] fillContext({})", contextFactory.contextUuid(), message.uuid());
    for (Function<D, Map<String, Object>> filler : contextFillers) {
      try {
        LOGGER.trace("[{}] filling context from {} for {}", contextFactory.contextUuid(), filler, message.uuid());
        context.properties().putAll(filler.apply(message.data()));
      } catch (Exception e) {
        LOGGER.error("ContextFiller threw exception for message {}: {}", message.uuid(), filler, e);
      }
    }
    return context.properties();
  }

}
