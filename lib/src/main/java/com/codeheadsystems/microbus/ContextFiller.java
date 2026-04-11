package com.codeheadsystems.microbus;

/**
 * Updates the context for the given message.
 *
 * @param <D> datatype.
 */
@FunctionalInterface
public interface ContextFiller<D> {

  void fillContext(D data, Context context);

}
