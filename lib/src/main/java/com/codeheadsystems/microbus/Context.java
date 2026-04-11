package com.codeheadsystems.microbus;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public record Context(UUID uuid, Map<String, Object> properties) {

  public Context(UUID uuid) {
    this(uuid, new HashMap<>());
  }

}
