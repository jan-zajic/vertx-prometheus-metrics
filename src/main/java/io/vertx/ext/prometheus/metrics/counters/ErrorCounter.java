package io.vertx.ext.prometheus.metrics.counters;

import io.prometheus.client.Counter;
import org.jetbrains.annotations.NotNull;
import io.vertx.ext.prometheus.metrics.PrometheusMetrics;

import java.util.function.Supplier;

public final class ErrorCounter {
  private final @NotNull Counter counter;
  private final @NotNull Supplier<String> localAddress;
  private final @NotNull PrometheusMetrics metrics;
	private final @NotNull String collectorName;

  public ErrorCounter(@NotNull PrometheusMetrics metrics, @NotNull String name, @NotNull String localAddress) {
    this(metrics, name, () -> localAddress);
  }

  public ErrorCounter(@NotNull PrometheusMetrics metrics, @NotNull String name, @NotNull Supplier<String> localAddress) {
    this.localAddress = localAddress;
    this.metrics = metrics;
    this.collectorName = "vertx_" + name + "_errors";
		counter = metrics.registerIfAbsent(collectorName, () -> Counter.build(collectorName, "Errors number").labelNames("local_address", "class").create());
  }

  public void increment(@NotNull Throwable throwable) {
    counter.labels(localAddress.get(), throwable.getClass().getSimpleName()).inc();
  }
  
}