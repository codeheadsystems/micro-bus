package com.codeheadsystems.microbus;

import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;

public interface Bus<D> {

  /**
   * Context filler work 'per message', and provide some default behavior you need at a per-message level that the
   * publisher may not be aware of. All subscribers get the same context that has been filled.
   *
   * @param filler for a context.
   */
  void registerContextFiller(Function<D, Map<String, Object>> filler);

  void unregisterContextFiller(Function<D, Map<String, Object>> filler);

  void publish(Message<D> message);

  void subscribe(Consumer<Message<D>> subscriber);

  void unsubscribe(Consumer<Message<D>> subscriber);

}
