package io.vertx.ext.prometheus;

import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

import org.jetbrains.annotations.NotNull;

import io.prometheus.client.Collector;
import io.prometheus.client.CollectorRegistry;
import io.prometheus.client.SimpleCollector;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.ext.prometheus.metrics.PrometheusMetrics;

public class VertxCollectorRegistryImpl implements VertxCollectorRegistry {

	private Logger logger = LoggerFactory.getLogger(PrometheusMetrics.class);
	
	private final @NotNull CollectorRegistry registry;
  @SuppressWarnings("rawtypes")
	private final @NotNull ConcurrentHashMap<String,SimpleCollector> collectors = new ConcurrentHashMap<>();
  
  public VertxCollectorRegistryImpl(@NotNull CollectorRegistry registry) {
		super();
		this.registry = registry;
	}

	private void register(@NotNull Collector collector) {
    try {
      registry.register(collector);
    } catch (IllegalArgumentException e) {
    	logger.error("Cannot register collector: "+collector, e);
    }
  }
  
	@Override
	@SuppressWarnings("unchecked")	
	public @NotNull <T extends SimpleCollector<?>> T  registerIfAbsent(@NotNull String collectorName, @NotNull Supplier<T> buildFunction) {
		return (T) collectors.computeIfAbsent(collectorName, key -> {
			T collector = buildFunction.get();			
			register(collector);
			return collector;
		});
	}

	@Override
	public void close() {
		 collectors.values().forEach(registry::unregister);
	   collectors.clear();
	}
	
	@Override
	@SuppressWarnings("unchecked")
	public <T extends SimpleCollector<?>> T get(String collectorName) {
		return (T) collectors.get(collectorName);
	}
	
}
