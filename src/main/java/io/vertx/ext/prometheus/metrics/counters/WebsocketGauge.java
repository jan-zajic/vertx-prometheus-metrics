package io.vertx.ext.prometheus.metrics.counters;

import io.prometheus.client.Gauge;
import io.vertx.ext.prometheus.metrics.PrometheusMetrics;
import org.jetbrains.annotations.NotNull;

public final class WebsocketGauge {
  private final @NotNull Gauge gauge;
  private final @NotNull Gauge.Child websockets;  
  private final String collectorName;
  
  public WebsocketGauge(@NotNull PrometheusMetrics metrics, @NotNull String name, @NotNull String localAddress) {
  	collectorName = "vertx_" + name + "_websockets";
    gauge = metrics.registerIfAbsent(collectorName, () -> Gauge.build(collectorName, "Websockets number").labelNames("local_address").create());
    websockets = metrics.labels(collectorName, localAddress);
  }

  public void increment() {
    websockets.inc();
  }

  public void decrement() {
    websockets.dec();
  }

}