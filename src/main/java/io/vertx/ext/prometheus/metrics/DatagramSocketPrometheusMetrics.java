package io.vertx.ext.prometheus.metrics;

import java.util.function.Supplier;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import io.vertx.core.net.SocketAddress;
import io.vertx.core.net.impl.SocketAddressImpl;
import io.vertx.core.spi.metrics.DatagramSocketMetrics;
import io.vertx.ext.prometheus.VertxCollectorRegistry;
import io.vertx.ext.prometheus.metrics.counters.BytesCounter;
import io.vertx.ext.prometheus.metrics.counters.ErrorCounter;

public final class DatagramSocketPrometheusMetrics extends PrometheusMetrics implements DatagramSocketMetrics {
  private static final @NotNull String NAME = "datagram_socket";

  private final @NotNull BytesCounter bytes;
  private final @NotNull ErrorCounter errors;

  private volatile @Nullable SocketAddress namedLocalAddress;

  public DatagramSocketPrometheusMetrics(@NotNull VertxCollectorRegistry registry) {
    super(registry);
    final Supplier<String> localAddress = () -> String.valueOf(namedLocalAddress);
    bytes = new BytesCounter(this, NAME, localAddress);
    errors = new ErrorCounter(this, NAME, localAddress);
  }

  @Override
  public void listening(@NotNull String localName, @NotNull SocketAddress localAddress) {
    this.namedLocalAddress = new SocketAddressImpl(localAddress.port(), localName);
  }

  @Override
  public void bytesRead(@Nullable Void socketMetric, @NotNull SocketAddress remoteAddress, long numberOfBytes) {
    bytes.read(numberOfBytes);
  }

  @Override
  public void bytesWritten(@Nullable Void socketMetric, @NotNull SocketAddress remoteAddress, long numberOfBytes) {
    bytes.read(numberOfBytes);
  }

  @Override
  public void exceptionOccurred(@Nullable Void socketMetric, @NotNull SocketAddress remoteAddress, @NotNull Throwable throwable) {
    errors.increment(throwable);
  }
}
