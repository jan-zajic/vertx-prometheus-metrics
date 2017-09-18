package io.vertx.ext.prometheus.metrics.counters;

import io.prometheus.client.Gauge;
import org.jetbrains.annotations.NotNull;
import io.vertx.ext.prometheus.metrics.PrometheusMetrics;

public final class EndpointMetrics {
  private final @NotNull Gauge gauge;
  private final @NotNull TimeCounter queueTime;
  private final @NotNull String localAddress;
  
  private final @NotNull PrometheusMetrics metrics;
  private final @NotNull String collectorName;

  public EndpointMetrics(@NotNull PrometheusMetrics metrics, @NotNull String name, @NotNull String localAddress) {
    this.localAddress = localAddress;
    this.metrics = metrics;
    this.collectorName = "vertx_" + name + "_endpoints";
		gauge = metrics.registerIfAbsent(collectorName, () -> Gauge.build(collectorName, "Endpoints number")
    		.labelNames("local_address", "state").create());        
    queueTime = new TimeCounter(metrics, name + "_endpoints_queue", localAddress);
  }

  public void increment() {
    gauge("established").inc();
  }

  public void decrement() {
    gauge("established").dec();
  }

  public @NotNull Stopwatch enqueue() {
    gauge("queued").inc();
    return new Stopwatch();
  }

  public void dequeue(@NotNull Stopwatch stopwatch) {
    gauge("queued").dec();
    queueTime.apply(stopwatch);
  }

  private @NotNull Gauge.Child gauge(@NotNull String name) {
    return metrics.labels(collectorName, localAddress, name);
  }
}
