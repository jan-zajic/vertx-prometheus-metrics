package io.vertx.ext.prometheus.metrics.counters;

import io.prometheus.client.Counter;
import io.vertx.ext.prometheus.metrics.PrometheusMetrics;
import org.jetbrains.annotations.NotNull;

public final class TimeCounter {
  private final @NotNull Counter counter;
  private final @NotNull Counter.Child time;

  public TimeCounter(@NotNull PrometheusMetrics metrics, @NotNull String name, @NotNull String localAddress) {
  	String collectorName = "vertx_" + name + "_time";
    counter = metrics.registerIfAbsent(collectorName, () ->  Counter.build(collectorName, "Processing time (us)").labelNames("local_address").create());
    time = metrics.labels(collectorName, localAddress);
  }

  public void apply(@NotNull Stopwatch stopwatch) {
    time.inc(stopwatch.stop());
  }

}
