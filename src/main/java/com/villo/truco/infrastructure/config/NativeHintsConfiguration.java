package com.villo.truco.infrastructure.config;

import com.villo.truco.infrastructure.aot.PersistenceRuntimeHints;
import com.villo.truco.infrastructure.aot.WebSocketPayloadRuntimeHints;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.ImportRuntimeHints;

@Configuration(proxyBeanMethods = false)
@ImportRuntimeHints({WebSocketPayloadRuntimeHints.class, PersistenceRuntimeHints.class})
public class NativeHintsConfiguration {

}
