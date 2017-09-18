package io.vertx.ext.prometheus.metrics;

import org.jetbrains.annotations.NotNull;

import io.vertx.core.net.SocketAddress;
import io.vertx.ext.prometheus.VertxCollectorRegistry;

public final class NetServerPrometheusMetrics extends TCPPrometheusMetrics {

  public NetServerPrometheusMetrics(@NotNull VertxCollectorRegistry registry, @NotNull SocketAddress localAddress) {
    super(registry, "netserver", localAddress.toString());
  }
}
