package com.codeheadsystems.microbus;

import java.util.function.Consumer;

public interface Bus<D> {

  /**
   * Context filler work 'per message', and provide some default behavior you need at a per-message level that the
   * publisher may not be aware of. All subscribers get the same context that has been filled.
   *
   * @param filler for a context.
   */
  void register(ContextFiller<D> filler);

  void unregister(ContextFiller<D> filler);

  void publish(Message<D> message);

  void subscribe(Consumer<Message<D>> subscriber);

  void unsubscribe(Consumer<Message<D>> subscriber);

}
