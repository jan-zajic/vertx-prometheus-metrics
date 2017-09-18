package io.vertx.ext.prometheus.metrics;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import io.vertx.core.http.HttpMethod;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.http.ServerWebSocket;
import io.vertx.core.net.SocketAddress;
import io.vertx.core.spi.metrics.HttpServerMetrics;
import io.vertx.ext.prometheus.VertxCollectorRegistry;
import io.vertx.ext.prometheus.metrics.counters.HTTPRequestMetrics;
import io.vertx.ext.prometheus.metrics.counters.WebsocketGauge;

public final class HTTPServerPrometheusMetrics extends TCPPrometheusMetrics implements HttpServerMetrics<HTTPRequestMetrics.Metric, Void, Void> {
  private static final @NotNull String NAME = "httpserver";

  private final @NotNull HTTPRequestMetrics requests;
  private final @NotNull WebsocketGauge websockets;

  public HTTPServerPrometheusMetrics(@NotNull VertxCollectorRegistry registry, @NotNull SocketAddress localAddress) {
    super(registry, NAME, localAddress.toString());
    websockets = new WebsocketGauge(this, NAME, localAddress.toString());
    requests = new HTTPRequestMetrics(this, NAME, localAddress.toString());
  }

  @Override
  public @NotNull HTTPRequestMetrics.Metric requestBegin(@Nullable Void metric, @NotNull HttpServerRequest request) {
    return requests.begin(request.method(), request.path());
  }

  @Override
  public void requestReset(@NotNull HTTPRequestMetrics.Metric metric) {
    requests.reset(metric);
  }

  @Override
  public @NotNull HTTPRequestMetrics.Metric responsePushed(@Nullable Void metric, @NotNull HttpMethod method, @NotNull String uri, @NotNull HttpServerResponse response) {
    return requests.begin(method, uri);
  }

  @Override
  public void responseEnd(@NotNull HTTPRequestMetrics.Metric metric, @NotNull HttpServerResponse response) {
    requests.responseEnd(metric, response.getStatusCode());
  }

  @Override
  public @Nullable Void upgrade(@NotNull HTTPRequestMetrics.Metric metric, @NotNull ServerWebSocket serverWebSocket) {
    requests.upgrade(metric);
    return null;
  }

  @Override
  public @Nullable Void connected(@Nullable Void metric, @NotNull ServerWebSocket serverWebSocket) {
    websockets.increment();
    return metric;
  }

  @Override
  public void disconnected(@Nullable Void metric) {
    websockets.decrement();
  }
}