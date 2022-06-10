package io.pivotal.geode.app.support;

import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Objects;
import java.util.Optional;
import java.util.Properties;
import java.util.function.Function;
import java.util.function.Predicate;

import org.apache.geode.cache.RegionShortcut;
import org.apache.geode.cache.client.ClientRegionShortcut;
import org.apache.geode.cache.server.CacheServer;
import org.apache.geode.internal.DistributionLocator;

/**
 * Support class for building Apache Geode applications.
 *
 * @author John Blum
 * @since 1.0.0
 */
public abstract class GeodeApplicationSupport {

	private static final Predicate<String> STRING_HAS_TEXT = value -> !(value == null || value.isBlank());

	protected static final CacheType GEODE_CACHE_TYPE = resolveCacheType(CacheType.CLIENT);

	protected static final DateTimeFormatter TIMESTAMP_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd-hh-mm-ss");

	private static final Function<Properties, Properties> jmxManagerFunction = properties -> GEODE_CACHE_TYPE.isServer()
		? PropertiesBuilder.from(properties)
			.set("jmx-manager", Boolean.TRUE.toString())
			.set("jmx-manager-start", Boolean.TRUE.toString())
			.build()
		: properties;

	private static final Function<Properties, Properties> locatorsFunction = properties -> GEODE_CACHE_TYPE.isServerSide()
		? PropertiesBuilder.from(properties)
			.set("locators", String.format("localhost[%d]", resolveGeodeLocatorPort()))
			.build()
		: properties;

	private static final Function<Properties, Properties> startLocatorFunction = properties -> GEODE_CACHE_TYPE.isServer()
		? PropertiesBuilder.from(properties)
			.set("start-locator", String.format("localhost[%d]", resolveGeodeLocatorPort()))
			.build()
		: properties;

	private final Function<Properties, Properties> gemfirePropertiesFunction = jmxManagerFunction
		.andThen(locatorsFunction)
		.andThen(startLocatorFunction);

	protected static final String DEFAULT_GEODE_LOG_LEVEL = "config";
	protected static final String GEODE_CACHE_TYPE_PROPERTY = "geode.cache.type";
	protected static final String GEODE_CACHE_SERVER_PORT_PROPERTY = "geode.cache.server.port";
	protected static final String GEODE_CLIENT_CACHE_REGION_TYPE_PROPERTY = "geode.cache.client.region.type";
	protected static final String GEODE_LOCATOR_PORT_PROPERTY = "geode.locator.port";
	protected static final String GEODE_LOG_LEVEL_PROPERTY = "geode.log.level";
	protected static final String GEODE_PEER_CACHE_REGION_TYPE_PROPERTY = "geode.cache.peer.region.type";

	@SuppressWarnings("all")
	protected static CacheType resolveCacheType(CacheType defaultCacheType) {

		return Optional.ofNullable(System.getProperty(GEODE_CACHE_TYPE_PROPERTY, defaultCacheType.name()))
			.filter(STRING_HAS_TEXT)
			.map(CacheType::valueOf)
			.orElse(defaultCacheType);
	}

	protected static int resolveCacheServerPort() {
		return resolveCacheServerPort(CacheServer.DEFAULT_PORT);
	}

	@SuppressWarnings("all")
	protected static int resolveCacheServerPort(int defaultCacheServerPort) {
		return Integer.getInteger(GEODE_CACHE_SERVER_PORT_PROPERTY, defaultCacheServerPort);
	}

	@SuppressWarnings("all")
	protected static ClientRegionShortcut resolveClientRegionType(ClientRegionShortcut defaultClientRegionType) {

		return Optional.ofNullable(System.getProperty(GEODE_CLIENT_CACHE_REGION_TYPE_PROPERTY, defaultClientRegionType.name()))
			.filter(STRING_HAS_TEXT)
			.map(ClientRegionShortcut::valueOf)
			.orElse(defaultClientRegionType);
	}

	protected static int resolveGeodeLocatorPort() {
		return resolveGeodeLocatorPort(DistributionLocator.DEFAULT_LOCATOR_PORT);
	}

	@SuppressWarnings("all")
	protected static int resolveGeodeLocatorPort(int defaultLocatorPort) {
		return Integer.getInteger(GEODE_LOCATOR_PORT_PROPERTY, defaultLocatorPort);
	}

	protected static String resolveGeodeLogLevel() {
		return resolveGeodeLogLevel(DEFAULT_GEODE_LOG_LEVEL);
	}

	@SuppressWarnings("all")
	protected static String resolveGeodeLogLevel(String defaultGeodeLogLevel) {

		return Optional.ofNullable(System.getProperty(GEODE_LOG_LEVEL_PROPERTY, defaultGeodeLogLevel))
			.filter(STRING_HAS_TEXT)
			.orElse(defaultGeodeLogLevel);
	}

	@SuppressWarnings("all")
	protected static RegionShortcut resolvePeerRegionType(RegionShortcut defaultPeerRegionType) {

		return Optional.ofNullable(System.getProperty(GEODE_PEER_CACHE_REGION_TYPE_PROPERTY, defaultPeerRegionType.name()))
			.filter(STRING_HAS_TEXT)
			.map(RegionShortcut::valueOf)
			.orElse(defaultPeerRegionType);
	}

	protected Function<Properties, Properties> gemfirePropertiesFunction() {
		return this.gemfirePropertiesFunction;
	}

	protected void log(String message, Object... args) {

		message = String.valueOf(message);
		message = message.endsWith("%n") ? message : message + "%n";

		System.err.printf(message, args);
		System.err.flush();
	}

	@SuppressWarnings("all")
	protected <T> T runSafely(ThrowableOperation<T> operation) {

		try {
			return operation.run();
		}
		catch (Throwable cause) {
			String message = String.format("Failed to run operation [%s]", operation.getClass().getName());
			throw new RuntimeException(message, cause);
		}
	}

	protected enum CacheType {

		CLIENT, PEER, SERVER;

		public boolean isClient() {
			return CLIENT.equals(this);
		}

		public boolean isServer() {
			return SERVER.equals(this);
		}

		public boolean isServerSide() {
			return Arrays.asList(PEER, SERVER).contains(this);
		}
	}

	@SuppressWarnings("all")
	protected static class PropertiesBuilder {

		private static Properties copy(Properties properties) {

			Properties propertiesCopy = new Properties();

			propertiesCopy.putAll(properties);

			return propertiesCopy;
		}

		protected static PropertiesBuilder create() {
			return from(new Properties());
		}

		protected static PropertiesBuilder from(Properties properties) {
			return new PropertiesBuilder(copy(properties));
		}

		private final Properties properties;

		private PropertiesBuilder(Properties properties) {
			this.properties = Objects.requireNonNull(properties);
		}

		protected PropertiesBuilder set(String propertyName, String propertyValue) {
			this.properties.setProperty(propertyName, propertyValue);
			return this;
		}

		protected Properties build() {
			return this.properties;
		}
	}

	@FunctionalInterface
	protected interface ThrowableOperation<T> {
		T run(Object... args) throws Throwable;
	}
}
