package io.vertx.ext.prometheus.metrics.counters;

import io.prometheus.client.Counter;
import io.prometheus.client.Counter.Child;
import io.vertx.ext.prometheus.metrics.PrometheusMetrics;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

public final class BytesCounter {
	private final @NotNull Counter counter;
	private final @NotNull Supplier<String> localAddress;
	private final @NotNull PrometheusMetrics metrics;
	private final @NotNull String collectorName;
	
	public BytesCounter(@NotNull PrometheusMetrics metrics, @NotNull String name, @NotNull String localAddress) {
		this(metrics, name, () -> localAddress);
	}
	
	public BytesCounter(@NotNull PrometheusMetrics metrics, @NotNull String name, @NotNull Supplier<String> localAddress) {
		this.metrics = metrics;
		this.localAddress = localAddress;
		this.collectorName = "vertx_" + name + "_bytes";
		counter = metrics.registerIfAbsent(collectorName, () -> Counter.build(collectorName, "Read/written bytes").labelNames("local_address", "type").create());
	}
	
	public void read(long bytes) {
		increment("read", bytes);
	}
	
	public void written(long bytes) {
		increment("written", bytes);
	}
	
	private void increment(@NotNull String operation, long bytes) {
		metrics.labels(collectorName, localAddress.get(), operation);
	}
}