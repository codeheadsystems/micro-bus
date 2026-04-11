package com.codeheadsystems.microbus;

import java.util.UUID;

public record Message<DATA>(DATA data, boolean async, UUID uuid){

}
