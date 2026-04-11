package com.codeheadsystems.microbus;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;
import javax.inject.Inject;
import javax.inject.Singleton;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Singleton
public class MicroBus<D> implements Bus<D> {

  private static final Logger LOGGER = LoggerFactory.getLogger(MicroBus.class);

  private final List<Consumer<Message<D>>> subscribers = new CopyOnWriteArrayList<>();
  private final List<ContextFiller<D>> contextFillers = new CopyOnWriteArrayList<>();
  private final ContextFactory contextFactory;
  private final ExecutorService executorService;

  @Inject
  public MicroBus(ContextFactory contextFactory) {
    this(contextFactory, Executors.newCachedThreadPool());
  }

  public MicroBus(ContextFactory contextFactory, ExecutorService executorService) {
    this.contextFactory = Objects.requireNonNull(contextFactory);
    this.executorService = Objects.requireNonNull(executorService);
  }

  @Override
  public void register(ContextFiller<D> filler) {
    contextFillers.add(Objects.requireNonNull(filler));
  }

  @Override
  public void unregister(ContextFiller<D> filler) {
    contextFillers.remove(filler);
  }

  @Override
  public void publish(Message<D> message) {
    if (message.async()) {
      executorService.submit(() -> dispatch(message));
    } else {
      dispatch(message);
    }
  }

  @Override
  public void subscribe(Consumer<Message<D>> subscriber) {
    subscribers.add(Objects.requireNonNull(subscriber));
  }

  @Override
  public void unsubscribe(Consumer<Message<D>> subscriber) {
    subscribers.remove(subscriber);
  }

  private void dispatch(Message<D> message) {
    contextFactory.withRunnable(() -> {
      LOGGER.debug("[{}] Starting dispatching message {}", contextFactory.contextUuid(), message);
      Context context = contextFactory.currentContext().orElseThrow();
      for (ContextFiller<D> filler : contextFillers) {
        try {
          LOGGER.debug("[{}] filling context from {}", contextFactory.contextUuid(), filler);
          filler.fillContext(message.data(), context);
        } catch (Exception e) {
          LOGGER.error("ContextFiller threw exception for message {}: {}", message.uuid(), e.getMessage(), e);
        }
      }
      for (Consumer<Message<D>> subscriber : subscribers) {
        try {
          LOGGER.debug("[{}] publishing to message {}", contextFactory.contextUuid(), subscriber);
          subscriber.accept(message);
        } catch (Exception e) {
          LOGGER.error("Subscriber threw exception for message {}: {}", message.uuid(), e.getMessage(), e);
        }
      }
      LOGGER.debug("[{}] Finished dispatching message {}", contextFactory.contextUuid(), message);
    });
  }

}
