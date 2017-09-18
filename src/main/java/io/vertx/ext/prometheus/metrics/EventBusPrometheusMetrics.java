package io.vertx.ext.prometheus.metrics;

import java.util.Optional;
import java.util.regex.Pattern;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import io.prometheus.client.Counter;
import io.prometheus.client.Gauge;
import io.vertx.core.eventbus.ReplyFailure;
import io.vertx.core.spi.metrics.EventBusMetrics;
import io.vertx.ext.prometheus.NamedCollector;
import io.vertx.ext.prometheus.VertxCollectorRegistry;
import io.vertx.ext.prometheus.metrics.counters.Stopwatch;

public final class EventBusPrometheusMetrics extends PrometheusMetrics implements EventBusMetrics<EventBusPrometheusMetrics.Metric> {

  private static final @NotNull NamedCollector<Gauge> handlers = NamedCollector.gauge("vertx_eventbus_handlers", "Message handlers number");
  
  private static final @NotNull NamedCollector<Gauge> respondents = NamedCollector.gauge("vertx_eventbus_respondents", "Reply handlers number");
  
  private static final @NotNull NamedCollector<Gauge> messages = NamedCollector.gauge("vertx_eventbus_messages", "EventBus messages metrics", "range", "state", "address");
  
  private static final @NotNull NamedCollector<Counter> failures = NamedCollector.counter("vertx_eventbus_failures", "Message handling failures number", "address", "type", "reason");
  
  private static final @NotNull NamedCollector<Counter> time = NamedCollector.counter("vertx_eventbus_messages_time", "Total messages processing time (us)", "address");
  
  private static final @NotNull NamedCollector<Counter> bytes = NamedCollector.counter("vertx_eventbus_bytes", "Total read/written bytes", "address", "type");
  
  public EventBusPrometheusMetrics(@NotNull VertxCollectorRegistry registry) {
    super(registry);
    registerIfAbsent(handlers);
    registerIfAbsent(respondents);
    registerIfAbsent(messages);
    registerIfAbsent(failures);
    registerIfAbsent(time);
    registerIfAbsent(bytes);
  }

  @Override
  public @NotNull Metric handlerRegistered(@NotNull String address, @Nullable String repliedAddress) {
    handlers.collector.inc();
    final Optional<String> respondent = Optional.ofNullable(repliedAddress);
    respondent.ifPresent(r -> respondents.collector.inc());
    return new Metric(address, respondent);
  }

  @Override
  public void handlerUnregistered(@Nullable Metric metric) {
    handlers.collector.dec();
    if (metric != null) {
      metric.respondent.ifPresent(r -> respondents.collector.dec());
    }
  }

  @Override
  public void scheduleMessage(@Nullable Metric metric, boolean local) {
    messages(address(metric), local, "scheduled").inc();
  }

  @Override
  public void beginHandleMessage(@Nullable Metric metric, boolean local) {
    messages(address(metric), local, "pending").dec();
    messages(address(metric), local, "scheduled").dec();
    if (metric != null) {
      metric.stopwatch.reset();
    }
  }
  
  @Override
  public void endHandleMessage(@Nullable Metric metric, @Nullable Throwable failure) {
    if (metric != null) {
    	Counter.Child counterChild = labels(time, AddressResolver.instance.apply(metric.address));
    	counterChild.inc(metric.stopwatch.stop());
    }
    if (failure != null) {
    	Counter.Child counterChild = labels(failures, AddressResolver.instance.apply(address(metric)), "request", failure.getClass().getSimpleName());
    	counterChild.inc();
    }
  }

	@Override
  public void messageSent(@NotNull String address, boolean publish, boolean local, boolean remote) {
    messages(address, local, publish ? "publish" : "sent").inc();
  }

  @Override
  public void messageReceived(@NotNull String address, boolean publish, boolean local, int handlersNumber) {
    messages(address, local, "pending").inc(handlersNumber);
    messages(address, local, "received").inc();
    if (handlersNumber > 0) {
      messages(address, local, "delivered").inc();
    }
  }

  @Override
  public void messageWritten(@NotNull String address, int numberOfBytes) {
    bytes(address, "write").inc(numberOfBytes);
  }

  @Override
  public void messageRead(@NotNull String address, int numberOfBytes) {
    bytes(address, "read").inc(numberOfBytes);
  }

  @Override
  public void replyFailure(@NotNull String address, @NotNull ReplyFailure failure) {
  	Counter.Child counterChild = labels(failures, AddressResolver.instance.apply(address), "reply", failure.name());
  	counterChild.inc();
  }

  private @NotNull Counter.Child bytes(@NotNull String address, @NotNull String type) {
    return labels(bytes, AddressResolver.instance.apply(address), type);
  }

  private static @NotNull String address(@Nullable Metric metric) {
    return metric == null ? "unknown" : metric.address;
  }

  private @NotNull Gauge.Child messages(@NotNull String address, boolean local, @NotNull String state) {
    return labels(messages, local ? "local" : "remote", state, AddressResolver.instance.apply(address));
  }

  public static final class Metric {
    private final @NotNull String address;
    private final @NotNull Optional<String> respondent;
    private final @NotNull Stopwatch stopwatch = new Stopwatch();

    public Metric(@NotNull String address, @NotNull Optional<String> respondent) {
      this.address = address;
      this.respondent = respondent;
    }
  }

  private static final class AddressResolver {
    public static final @NotNull AddressResolver instance = new AddressResolver();

    private static final @NotNull Pattern NUMBER_PATTERN = Pattern.compile("^\\d+$");
    private static final @NotNull String GENERATED_ADDRESS = "vertx-generated-address";

    public @NotNull String apply(@NotNull String address) {
      if (NUMBER_PATTERN.matcher(address).matches()) {
        return GENERATED_ADDRESS;
      }
      if (address.split("-").length == 5) {
        return GENERATED_ADDRESS;
      }
      return address;
    }
  }
}