package io.vertx.ext.prometheus.metrics;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

import org.jetbrains.annotations.NotNull;

import io.prometheus.client.Counter;
import io.prometheus.client.Gauge;
import io.prometheus.client.SimpleCollector;
import io.vertx.core.spi.metrics.Metrics;
import io.vertx.ext.prometheus.NamedCollector;
import io.vertx.ext.prometheus.VertxCollectorRegistry;

public abstract class PrometheusMetrics implements Metrics {
	
	private final VertxCollectorRegistry registry;
	private final Map<List<String>, String> childrens = new ConcurrentHashMap<List<String>, String>();

  protected PrometheusMetrics(@NotNull VertxCollectorRegistry registry) {
    this.registry = registry;
  }

  @Override
  public final boolean isEnabled() {
    return !childrens.isEmpty();
  }
  
  @Override
  public final void close() {
  	for (Entry<List<String>, String> entry : childrens.entrySet()) {
  		List<String> labels = entry.getKey();
  		String parentCollectorName = entry.getValue();
  		SimpleCollector<?> collector = registry.get(parentCollectorName);
  		collector.remove(labels.toArray(new String[]{}));
		}
    childrens.clear();
  }
  
  public <T extends SimpleCollector<?>> T registerIfAbsent(@NotNull String collectorName, @NotNull Supplier<T> buildFunction) {
  	return registry.registerIfAbsent(collectorName, buildFunction);
  }
  
  public <T extends SimpleCollector<?>> T registerIfAbsent(NamedCollector<T> collectorDefinition) {
  	T collector = registry.registerIfAbsent(collectorDefinition.name, collectorDefinition.builder);
  	collectorDefinition.collector = collector;
  	return collector;
  }
   
	public <CHILD> CHILD labels(String parentCollectorName, String... labels) {
		SimpleCollector<CHILD> collector = registry.get(parentCollectorName);
		CHILD child = collector.labels(labels);
		childrens.putIfAbsent(Arrays.asList(labels), parentCollectorName);
		return child;
	}
  
	public <CHILD> CHILD  labels(@NotNull NamedCollector<?> collectorDefinition, @NotNull String... labels) {
		return labels(collectorDefinition.name, labels);
	}
	
}
