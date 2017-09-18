package io.vertx.ext.prometheus;

import java.util.function.Supplier;

import io.prometheus.client.Counter;
import io.prometheus.client.Gauge;
import io.prometheus.client.SimpleCollector;

public class NamedCollector<T extends SimpleCollector<?>> {

	public String name;
	public Supplier<T> builder;
	public T collector;
	
	public static NamedCollector<Gauge> gauge(String name, String decription, String... labelNames) {
		NamedCollector<Gauge> def = new NamedCollector<Gauge>();
		def.name = name;
		def.builder = () -> {
			Gauge.Builder builder = Gauge
					.build(name, decription);
			if(labelNames != null)
				builder.labelNames(labelNames);
			return builder.create();
		};
		return def;
	}
	
	public static NamedCollector<Counter> counter(String name, String decription, String... labelNames) {
		NamedCollector<Counter> def = new NamedCollector<Counter>();
		def.name = name;
		def.builder = () -> {
			Counter.Builder builder = Counter
					.build(name, decription);
			if(labelNames != null)
				builder.labelNames(labelNames);
			return builder.create();
		};
		return def;
	}
	
}
