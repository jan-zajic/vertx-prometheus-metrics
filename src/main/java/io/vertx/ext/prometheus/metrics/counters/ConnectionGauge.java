package io.vertx.ext.prometheus.metrics.counters;

import org.jetbrains.annotations.NotNull;

import io.prometheus.client.Gauge;
import io.vertx.ext.prometheus.metrics.PrometheusMetrics;

public final class ConnectionGauge {
  private final @NotNull Gauge gauge;
  private final @NotNull Gauge.Child connections;

  public ConnectionGauge(@NotNull PrometheusMetrics metrics, @NotNull String name, @NotNull String localAddress) {
  	String collectorName = "vertx_" + name + "_connections";
    gauge = metrics.registerIfAbsent(collectorName, () -> Gauge.build(collectorName, "Active connections number") .labelNames("local_address").create());
    connections = metrics.labels(collectorName, localAddress);
  }

  public void connected() {
    connections.inc();
  }

  public void disconnected() {
    connections.dec();
  }

}
