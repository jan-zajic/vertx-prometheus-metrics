package io.vertx.ext.prometheus.metrics.counters;

import java.util.function.Supplier;

import org.jetbrains.annotations.NotNull;

import io.prometheus.client.Counter;
import io.vertx.ext.prometheus.metrics.PrometheusMetrics;

public final class BytesCounter {
	private final @NotNull Counter counter;
	private final @NotNull Supplier<String> localAddress;
	private final @NotNull PrometheusMetrics metrics;
	private final @NotNull String collectorName;
	
	private final @NotNull Counter.Child read;
	private final @NotNull Counter.Child write;
	
	public BytesCounter(@NotNull PrometheusMetrics metrics, @NotNull String name, @NotNull String localAddress) {
		this(metrics, name, () -> localAddress);
	}
	
	public BytesCounter(@NotNull PrometheusMetrics metrics, @NotNull String name, @NotNull Supplier<String> localAddress) {
		this.metrics = metrics;
		this.localAddress = localAddress;
		this.collectorName = "vertx_" + name + "_bytes";
		this.counter = metrics.registerIfAbsent(collectorName, () -> Counter.build(collectorName, "Read/written bytes").labelNames("local_address", "type").create());
		this.read = metrics.labels(collectorName, localAddress.get(), "read");
		this.write = metrics.labels(collectorName, localAddress.get(), "write");		
	}
	
	public void read(long bytes) {
		this.read.inc(bytes);
	}
	
	public void written(long bytes) {
		this.write.inc(bytes);
	}

}