package com.codeheadsystems.microbus;

import java.util.function.Consumer;

public interface Bus<D> {

  void register(ContextFiller<D> filler);

  void unregister(ContextFiller<D> filler);

  void publish(Message<D> message);

  void subscribe(Consumer<Message<D>> subscriber);

  void unsubscribe(Consumer<Message<D>> subscriber);

}
