package io.vertx.ext.prometheus;

import static io.vertx.ext.prometheus.MetricsType.*;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import io.prometheus.client.Gauge;
import io.vertx.core.Closeable;
import io.vertx.core.Verticle;
import io.vertx.core.Vertx;
import io.vertx.core.datagram.DatagramSocket;
import io.vertx.core.datagram.DatagramSocketOptions;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.http.HttpClient;
import io.vertx.core.http.HttpClientOptions;
import io.vertx.core.http.HttpServer;
import io.vertx.core.http.HttpServerOptions;
import io.vertx.core.metrics.impl.DummyVertxMetrics;
import io.vertx.core.net.NetClientOptions;
import io.vertx.core.net.NetServerOptions;
import io.vertx.core.net.SocketAddress;
import io.vertx.core.spi.metrics.DatagramSocketMetrics;
import io.vertx.core.spi.metrics.EventBusMetrics;
import io.vertx.core.spi.metrics.HttpClientMetrics;
import io.vertx.core.spi.metrics.HttpServerMetrics;
import io.vertx.core.spi.metrics.PoolMetrics;
import io.vertx.core.spi.metrics.TCPMetrics;
import io.vertx.ext.prometheus.metrics.DatagramSocketPrometheusMetrics;
import io.vertx.ext.prometheus.metrics.EventBusPrometheusMetrics;
import io.vertx.ext.prometheus.metrics.HTTPClientPrometheusMetrics;
import io.vertx.ext.prometheus.metrics.HTTPServerPrometheusMetrics;
import io.vertx.ext.prometheus.metrics.NetClientPrometheusMetrics;
import io.vertx.ext.prometheus.metrics.NetServerPrometheusMetrics;
import io.vertx.ext.prometheus.metrics.PoolPrometheusMetrics;
import io.vertx.ext.prometheus.metrics.PrometheusMetrics;
import io.vertx.ext.prometheus.server.MetricsServer;

public final class VertxPrometheusMetrics extends DummyVertxMetrics {
  private final @NotNull Vertx vertx;
  private final @NotNull VertxPrometheusOptions options;
  private final @NotNull VerticleMetrics verticleMetrics;
  private final @NotNull TimerMetrics timerMetrics;

  private @Nullable Closeable server;
  private VertxCollectorRegistry vertxCollectorRegistry;
  
  public VertxPrometheusMetrics(@NotNull Vertx vertx, @NotNull VertxPrometheusOptions options) {
    this.vertx = vertx;
    this.options = options;
    this.vertxCollectorRegistry = new VertxCollectorRegistryImpl(options.getRegistry());
    this.verticleMetrics = options.isEnabled(Verticles) ? new VerticlePrometheusMetrics(this.vertxCollectorRegistry) : new VerticleDummyMetrics();
    this.timerMetrics = options.isEnabled(Timers) ? new TimerPrometheusMetrics(this.vertxCollectorRegistry) : new TimerDummyMetrics();    
  }

  @Override
  public void eventBusInitialized(@NotNull EventBus bus) {
    if (options.isEmbeddedServerEnabled()) {
      server = MetricsServer.create(vertx).apply(options.getRegistry()).apply(options.getAddress());
    }
  }

  @Override
  public void verticleDeployed(@NotNull Verticle verticle) {
    verticleMetrics.deployed(verticle);
  }

  @Override
  public void verticleUndeployed(@NotNull Verticle verticle) {
    verticleMetrics.undeployed(verticle);
  }

  @Override
  public void timerCreated(long id) {
    timerMetrics.created(id);
  }

  @Override
  public void timerEnded(long id, boolean cancelled) {
    timerMetrics.ended(id, cancelled);
  }

  @Override
  public @NotNull EventBusMetrics<?> createMetrics(@NotNull EventBus eventBus) {
    return options.isEnabled(EventBus)
        ? new EventBusPrometheusMetrics(vertxCollectorRegistry)
        : super.createMetrics(eventBus);
  }

  @Override
  public @NotNull HttpServerMetrics<?, ?, ?> createMetrics(@NotNull HttpServer httpServer, @NotNull SocketAddress localAddress, @NotNull HttpServerOptions httpServerOptions) {
    return options.isEnabled(HTTPServer)
        ? new HTTPServerPrometheusMetrics(vertxCollectorRegistry, localAddress)
        : super.createMetrics(httpServer, localAddress, httpServerOptions);
  }

  @Override
  public @NotNull HttpClientMetrics<?, ?, ?, ?, ?> createMetrics(@NotNull HttpClient client, @NotNull HttpClientOptions httpClientOptions) {
    return options.isEnabled(HTTPClient)
        ? new HTTPClientPrometheusMetrics(vertxCollectorRegistry, getLocalAddress(httpClientOptions.getLocalAddress()))
        : super.createMetrics(client, httpClientOptions);
  }

  @Override
  public @NotNull TCPMetrics<?> createMetrics(@NotNull SocketAddress localAddress, @NotNull NetServerOptions netServerOptions) {
    return options.isEnabled(NetServer)
        ? new NetServerPrometheusMetrics(vertxCollectorRegistry, localAddress)
        : super.createMetrics(localAddress, netServerOptions);
  }

  @Override
  public @NotNull TCPMetrics<?> createMetrics(@NotNull NetClientOptions netClientOptions) {
    return options.isEnabled(NetClient)
        ? new NetClientPrometheusMetrics(vertxCollectorRegistry, getLocalAddress(netClientOptions.getLocalAddress()))
        : super.createMetrics(netClientOptions);
  }

  @Override
  public @NotNull DatagramSocketMetrics createMetrics(@NotNull DatagramSocket socket, @NotNull DatagramSocketOptions datagramSocketOptions) {
    return options.isEnabled(DatagramSocket)
        ? new DatagramSocketPrometheusMetrics(vertxCollectorRegistry)
        : super.createMetrics(socket, datagramSocketOptions);
  }

  @Override
  public @NotNull <P> PoolMetrics<?> createMetrics(@NotNull P pool, @NotNull String poolType, @NotNull String poolName, int maxPoolSize) {
    return options.isEnabled(Pools)
        ? new PoolPrometheusMetrics(vertxCollectorRegistry, poolType, poolName, maxPoolSize)
        : super.createMetrics(pool, poolType, poolName, maxPoolSize);
  }

  public PrometheusMetrics createMetrics() {
  	return new PrometheusMetrics(vertxCollectorRegistry);
  }
  
  @Override
  public boolean isMetricsEnabled() {
    return true;
  }

  @Override
  public boolean isEnabled() {
    return true;
  }
  
  @Override
  public void close() {
    if (server != null) {
      server.close(event -> { vertxCollectorRegistry.close(); });
    } else {
    	vertxCollectorRegistry.close();
    }
  }

  private static @NotNull String getLocalAddress(@Nullable String address) {
    return address == null ? "unknown" : address;
  }

  private interface VerticleMetrics {
    void deployed(@NotNull Verticle verticle);

    void undeployed(@NotNull Verticle verticle);
  }

  private interface TimerMetrics {
    void created(long id);

    void ended(long id, boolean cancelled);
  }

  private static final class VerticlePrometheusMetrics extends PrometheusMetrics implements VerticleMetrics {
    private static final @NotNull NamedCollector<Gauge> collector =
    		NamedCollector.gauge("vertx_verticle_number", "Deployed verticles number", "class");
    
    public VerticlePrometheusMetrics(@NotNull VertxCollectorRegistry registry) {
      super(registry);
      registerIfAbsent(collector);
    }

    @Override
    public void deployed(@NotNull Verticle verticle) {
    	Gauge.Child child = labels(collector, verticle.getClass().getName());
    	child.inc();
    }

    @Override
    public void undeployed(@NotNull Verticle verticle) {
    	Gauge.Child child = labels(collector, verticle.getClass().getName());
      child.dec();
    }
  }

  private static final class TimerPrometheusMetrics extends PrometheusMetrics implements TimerMetrics {
    private static final @NotNull NamedCollector<Gauge> collector =
    		NamedCollector.gauge("vertx_timers_number", "Timers number", "state");

    public TimerPrometheusMetrics(@NotNull VertxCollectorRegistry registry) {
      super(registry);
      registerIfAbsent(collector);
    }

    @Override
    public void created(long id) {
    	Gauge.Child created = labels(collector, "created");
    	created.inc();
    	Gauge.Child active = labels(collector, "active");
    	active.inc();
    }

    @Override
    public void ended(long id, boolean cancelled) {
      if (cancelled) {
      	Gauge.Child cancelledChild = labels(collector, "cancelled");
      	cancelledChild.inc();
      }
      Gauge.Child destroyed = labels(collector, "destroyed");
      destroyed.inc();
      
      Gauge.Child active = labels(collector, "active");
      active.dec();
    }
  }

  private final class VerticleDummyMetrics implements @NotNull VerticleMetrics {

    @Override
    public void deployed(@NotNull Verticle verticle) {
      VertxPrometheusMetrics.super.verticleDeployed(verticle);
    }

    @Override
    public void undeployed(@NotNull Verticle verticle) {
      VertxPrometheusMetrics.super.verticleUndeployed(verticle);
    }
  }

  private final class TimerDummyMetrics implements @NotNull TimerMetrics {

    @Override
    public void created(long id) {
      VertxPrometheusMetrics.super.timerCreated(id);
    }

    @Override
    public void ended(long id, boolean cancelled) {
      VertxPrometheusMetrics.super.timerEnded(id, cancelled);
    }
  }
}
