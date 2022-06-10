package io.pivotal.geode.app;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;
import java.util.Properties;
import java.util.function.Function;

import org.apache.geode.cache.Cache;
import org.apache.geode.cache.CacheFactory;
import org.apache.geode.cache.GemFireCache;
import org.apache.geode.cache.Region;
import org.apache.geode.cache.RegionShortcut;
import org.apache.geode.cache.client.ClientCache;
import org.apache.geode.cache.client.ClientCacheFactory;
import org.apache.geode.cache.client.ClientRegionShortcut;
import org.apache.geode.cache.server.CacheServer;
import org.apache.geode.internal.cache.GemFireCacheImpl;

import io.pivotal.geode.app.support.GeodeApplicationSupport;

/**
 * Example/Test Apache Geode application that creates and initialize an Apache Geode cache instance
 * and then simply puts data into a cache Region and gets the data back out.
 *
 * @author John Blum
 * @see java.util.Properties
 * @see org.apache.geode.cache.Cache
 * @see org.apache.geode.cache.Region
 * @see org.apache.geode.cache.client.ClientCache
 * @since 1.0.0
 */
@SuppressWarnings("unused")
public class GeodeApplication extends GeodeApplicationSupport implements Runnable {

	private static final ClientRegionShortcut CLIENT_REGION_TYPE = resolveClientRegionType(ClientRegionShortcut.LOCAL);

	private static final RegionShortcut PEER_REGION_SHORTCUT = resolvePeerRegionType(RegionShortcut.PARTITION);

	static final String REGION_NAME = "Example";

	public static void main(String[] args) {
		new GeodeApplication(args).run();
	}

	private final String[] arguments;

	public GeodeApplication(String[] arguments) {

		assertThat(arguments)
			.describedAs("Program arguments are required")
			.isNotNull();

		this.arguments = arguments;
	}

	protected String[] getArguments() {
		return this.arguments;
	}

	public void run() {
		run(getArguments());
	}

	protected void run(String[] arguments) {

		Object key = 1;

		GemFireCache cache = newCache();

		Region<Object, Object> example = newRegion(cache, REGION_NAME);

		assertThat(example).isNotNull();
		assertThat(example.getName()).isEqualTo(REGION_NAME);
		assertThat(example.get(key)).isNull();
		assertThat(example.put(key, "TEST")).isNull();

		Object value = example.get(key);

		assertThat(value).isEqualTo("TEST");

		log("Value for Key [%s] is: %s%n", key, value);
		log("SUCCESS!");
	}

	protected Properties gemfireProperties() {
		return gemfireProperties(Function.identity());
	}

	protected Properties gemfireProperties(Function<Properties, Properties> gemfirePropertiesFunction) {

		Properties gemfireProperties = new Properties();

		gemfireProperties.setProperty("name", resolveGeodeMemberName());
		gemfireProperties.setProperty("log-level", resolveGeodeLogLevel());

		return gemfirePropertiesFunction.apply(gemfireProperties);
	}

	protected String resolveGeodeMemberName() {
		return String.format("%s-%s-%s", getClass().getSimpleName(), GEODE_CACHE_TYPE.name(),
			LocalDateTime.now().format(TIMESTAMP_FORMATTER));
	}

	protected GemFireCache newCache() {

		log("%s CACHE", GEODE_CACHE_TYPE);

		return GEODE_CACHE_TYPE.isClient()
			? newClientCache(gemfireProperties(gemfirePropertiesFunction()))
			: newPeerCache(gemfireProperties(gemfirePropertiesFunction()));
	}

	protected ClientCache newClientCache(Properties gemfireProperties) {
		return new ClientCacheFactory(gemfireProperties).create();
	}

	protected Cache newPeerCache(Properties gemfireProperties) {
		return addCacheServer(new CacheFactory(gemfireProperties).create());
	}

	protected Cache addCacheServer(Cache cache) {

		CacheServer cacheServer = cache.addCacheServer();

		cacheServer.setHostnameForClients("localhost");
		cacheServer.setPort(resolveCacheServerPort());
		runSafely(args -> { cacheServer.start(); return null; });

		return cache;
	}

	private boolean isClient(GemFireCache cache) {

		return cache instanceof GemFireCacheImpl
			? ((GemFireCacheImpl) cache).isClient()
			: GEODE_CACHE_TYPE.isClient();
	}

	@SuppressWarnings("all")
	protected <K, V> Region<K, V> newRegion(GemFireCache cache, String name) {

		return isClient(cache)
			? this.newClientRegion(((ClientCache) cache), name)
			: this.newPeerRegion(((Cache) cache), name);
	}

	protected <K, V> Region<K, V> newClientRegion(ClientCache clientCache, String name) {
		log("CLIENT LOCAL REGION");
		return clientCache.<K, V>createClientRegionFactory(CLIENT_REGION_TYPE).create(name);
	}

	protected <K, V> Region<K, V> newPeerRegion(Cache peerCache, String name) {
		log("PEER PARTITION REGION");
		return peerCache.<K, V>createRegionFactory(PEER_REGION_SHORTCUT).create(name);
	}
}
