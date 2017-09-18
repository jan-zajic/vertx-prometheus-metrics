package io.vertx.ext.prometheus.metrics;

import org.jetbrains.annotations.NotNull;

import io.prometheus.client.Counter;
import io.prometheus.client.Gauge;
import io.vertx.core.spi.metrics.PoolMetrics;
import io.vertx.ext.prometheus.NamedCollector;
import io.vertx.ext.prometheus.VertxCollectorRegistry;
import io.vertx.ext.prometheus.metrics.counters.Stopwatch;

public final class PoolPrometheusMetrics extends PrometheusMetrics implements PoolMetrics<Stopwatch> {
  private final @NotNull TaskMetrics tasks;
  private final @NotNull TimeMetrics time;
  
  public PoolPrometheusMetrics(@NotNull VertxCollectorRegistry registry, @NotNull String type, @NotNull String name, int maxSize) {
    super(registry);
    registerIfAbsent(TaskMetrics.gauge);
    registerIfAbsent(TimeMetrics.counter);
    tasks = new TaskMetrics(this, type, name);
    time = new TimeMetrics(this, type, name);
    Gauge.Child gaugeChild = labels(TaskMetrics.gauge, type, name, "max_size");
    gaugeChild.set(maxSize);
  }
  
  @Override
  public @NotNull Stopwatch submitted() {
    tasks.queued.inc();
    return new Stopwatch();
  }

  @Override
  public void rejected(@NotNull Stopwatch submittedStopwatch) {
    tasks.queued.dec();
  }

  @Override
  public @NotNull Stopwatch begin(@NotNull Stopwatch submittedStopwatch) {
    tasks.queued.dec();
    tasks.used.inc();
    time.delay.inc(submittedStopwatch.stop());
    return submittedStopwatch;
  }

  @Override
  public void end(@NotNull Stopwatch beginStopwatch, boolean succeeded) {
    time.process.inc(beginStopwatch.stop());
    tasks.used.dec();
  }

  private static final class TaskMetrics {
  	
    private static final @NotNull NamedCollector<Gauge> gauge = NamedCollector.gauge("vertx_pool_tasks", "Pool queue metrics", "type", "name", "state");

    private final @NotNull Gauge.Child queued;
    private final @NotNull Gauge.Child used;
    
    public TaskMetrics(PoolPrometheusMetrics metrics, @NotNull String type, @NotNull String name) {
      queued = metrics.labels(gauge, type, name, "queued");
      used = metrics.labels(gauge, type, name, "used");
    }
  }

  private static final class TimeMetrics {
    private static final @NotNull NamedCollector<Counter> counter = NamedCollector.counter("vertx_pool_time", "Pool time metrics (us)", "type", "name", "state");

    private final @NotNull Counter.Child delay;
    private final @NotNull Counter.Child process;

    public TimeMetrics(PoolPrometheusMetrics metrics, @NotNull String type, @NotNull String name) {
      delay = metrics.labels(counter, type, name, "delay");
      process = metrics.labels(counter, type, name, "process");
    }
  }
}
