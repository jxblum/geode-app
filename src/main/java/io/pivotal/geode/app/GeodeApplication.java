package io.pivotal.geode.app;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Properties;

import org.apache.geode.cache.Cache;
import org.apache.geode.cache.CacheFactory;
import org.apache.geode.cache.GemFireCache;
import org.apache.geode.cache.Region;
import org.apache.geode.cache.RegionShortcut;
import org.apache.geode.cache.client.ClientCache;
import org.apache.geode.cache.client.ClientCacheFactory;
import org.apache.geode.cache.client.ClientRegionShortcut;
import org.apache.geode.internal.cache.GemFireCacheImpl;

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
public class GeodeApplication implements Runnable {

	static final CacheType GEODE_CACHE_TYPE = CacheType.CLIENT;

	static final ClientRegionShortcut CLIENT_REGION_SHORTCUT = ClientRegionShortcut.LOCAL;

	final RegionShortcut PEER_REGION_SHORTCUT = RegionShortcut.PARTITION;

	static final String GEODE_CACHE_NAME = GeodeApplication.class.getSimpleName();
	static final String GEODE_LOCATORS = "";
	static final String GEODE_LOG_LEVEL = "config";
	static final String GEODE_REGION_NAME = "Example";

	public static void main(String[] args) {
		new GeodeApplication(args).run();
	}

	private final String[] arguments;

	public GeodeApplication(String[] arguments) {
		assertThat(arguments).isNotNull();
		this.arguments = arguments;
	}

	protected String[] getArguments() {
		return this.arguments;
	}

	protected void log(String message, Object... args) {

		message = String.valueOf(message);
		message = message.endsWith("%n") ? message : message + "%n";

		System.err.printf(message, args);
	}

	public void run() {
		run(getArguments());
	}

	protected void run(String[] arguments) {

		Object key = 1;

		Region<Object, Object> example = newRegion(newCache(), GEODE_REGION_NAME);

		assertThat(example).isNotNull();
		assertThat(example.getName()).isEqualTo(GEODE_REGION_NAME);
		assertThat(example.get(key)).isNull();
		assertThat(example.put(key, "TEST")).isNull();

		Object value = example.get(key);

		assertThat(value).isEqualTo("TEST");

		log("Value for Key [%s] is: %s%n", key, value);
		log("SUCCESS!");
	}

	protected Properties gemfireProperties() {

		Properties gemfireProperties = new Properties();

		gemfireProperties.setProperty("name", GEODE_CACHE_NAME);
		gemfireProperties.setProperty("log-level", GEODE_LOG_LEVEL);

		return gemfireProperties;
	}

	protected GemFireCache newCache() {

		return CacheType.CLIENT.equals(GeodeApplication.GEODE_CACHE_TYPE)
			? newClientCache(gemfireProperties())
			: newPeerCache(gemfireProperties());
	}

	protected ClientCache newClientCache(Properties gemfireProperties) {
		log("CLIENT CACHE");
		return new ClientCacheFactory(gemfireProperties).create();
	}

	protected Cache newPeerCache(Properties gemfireProperties) {

		log("PEER CACHE");

		return new CacheFactory(gemfireProperties)
			.set("locators", GEODE_LOCATORS)
			.create();
	}

	private boolean isClient(GemFireCache cache) {

		return cache instanceof GemFireCacheImpl
			? ((GemFireCacheImpl) cache).isClient()
			: CacheType.CLIENT.equals(GEODE_CACHE_TYPE);
	}

	protected <K, V> Region<K, V> newRegion(GemFireCache cache, String name) {

		return isClient(cache)
			? this.<K, V>newClientRegion(((ClientCache) cache), name)
			: this.<K, V>newPeerRegion(((Cache) cache), name);
	}

	protected <K, V> Region<K, V> newClientRegion(ClientCache clientCache, String name) {
		log("CLIENT LOCAL REGION");
		return clientCache.<K, V>createClientRegionFactory(CLIENT_REGION_SHORTCUT).create(name);
	}

	protected <K, V> Region<K, V> newPeerRegion(Cache peerCache, String name) {
		log("SERVER PARTITION REGION");
		return peerCache.<K, V>createRegionFactory(PEER_REGION_SHORTCUT).create(name);
	}

	protected enum CacheType {
		CLIENT, PEER
	}
}
