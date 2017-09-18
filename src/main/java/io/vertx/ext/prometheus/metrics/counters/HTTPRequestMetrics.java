package io.vertx.ext.prometheus.metrics.counters;

import io.prometheus.client.Counter;
import io.prometheus.client.Gauge;
import io.vertx.core.http.HttpMethod;
import io.vertx.ext.prometheus.metrics.PrometheusMetrics;
import org.jetbrains.annotations.NotNull;

public final class HTTPRequestMetrics {
  private final @NotNull Gauge requests;
  private final @NotNull Counter responses;
  private final @NotNull TimeCounter proocessTime;
  private final @NotNull String localAddress;
  private final @NotNull PrometheusMetrics metrics;
	private final @NotNull String requestsCollectorName;
	private final @NotNull String responsesCollectorName;

  public HTTPRequestMetrics(@NotNull PrometheusMetrics metrics, @NotNull String name, @NotNull String localAddress) {
    this.localAddress = localAddress;
    this.metrics = metrics;
    this.requestsCollectorName = "vertx_" + name + "_requests";
    requests = metrics.registerIfAbsent(requestsCollectorName, () -> Gauge.build(requestsCollectorName, "HTTP requests number").labelNames("local_address", "method", "path", "state").create());
    this.responsesCollectorName = "vertx_" + name + "_responses";
		responses = metrics.registerIfAbsent(responsesCollectorName, () -> Counter.build(responsesCollectorName, "HTTP responses number").labelNames("local_address", "code").create());
    proocessTime = new TimeCounter(metrics, name + "_requests", localAddress);
  }

  public @NotNull Metric begin(@NotNull HttpMethod method, @NotNull String path) {
    requests(method.name(), path, "active").inc();
    requests(method.name(), path, "total").inc();
    return new Metric(method, path);
  }

  public void reset(@NotNull Metric metric) {
    proocessTime.apply(metric.stopwatch);
    requests(metric, "reset").inc();
    requests(metric, "processed").inc();
    requests(metric, "active").dec();
  }

  public void responseEnd(@NotNull Metric metric, int responseStatusCode) {
    proocessTime.apply(metric.stopwatch);
    requests(metric, "active").dec();
    requests(metric, "processed").inc();
    responses(responseStatusCode).inc();
  }

  public void requestEnd(@NotNull Metric metric) {
    proocessTime.apply(metric.stopwatch);
  }

  public void upgrade(@NotNull Metric metric) {
    requests(metric, "upgraded").inc();
  }

  private @NotNull Counter.Child responses(int responseStatusCode) {
    return metrics.labels(this.responsesCollectorName, localAddress, Integer.toString(responseStatusCode));
  }

  private @NotNull Gauge.Child requests(@NotNull HTTPRequestMetrics.@NotNull Metric metric, @NotNull String state) {
    return requests(metric.method.name(), metric.path, state);
  }

  private @NotNull Gauge.Child requests(@NotNull String method, @NotNull String path, @NotNull String state) {
    return metrics.labels(this.requestsCollectorName, localAddress, method, path, state);
  }

  public static final class Metric {
    private final @NotNull HttpMethod method;
    private final @NotNull String path;
    private final @NotNull Stopwatch stopwatch = new Stopwatch();

    public Metric(@NotNull HttpMethod method, @NotNull String path) {
      this.method = method;
      this.path = path;
    }
  }
}
