package io.vertx.ext.prometheus;

import java.util.function.Supplier;

import org.jetbrains.annotations.NotNull;

import io.prometheus.client.SimpleCollector;

public interface VertxCollectorRegistry {

	<T extends SimpleCollector<?>> T registerIfAbsent(@NotNull String collectorName, @NotNull Supplier<T> buildFunction);
	<T extends SimpleCollector<?>> T get(@NotNull String collectorName);
	void close();
	
}
