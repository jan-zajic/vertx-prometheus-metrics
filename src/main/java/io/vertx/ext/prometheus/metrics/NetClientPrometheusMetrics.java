package io.vertx.ext.prometheus.metrics;

import org.jetbrains.annotations.NotNull;

import io.vertx.ext.prometheus.VertxCollectorRegistry;

public final class NetClientPrometheusMetrics extends TCPPrometheusMetrics {

  public NetClientPrometheusMetrics(@NotNull VertxCollectorRegistry registry, @NotNull String localAddress) {
    super(registry, "netclient", localAddress);
  }
}
