package io.pivotal.geode.app;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Optional;
import java.util.function.Supplier;

import org.junit.Test;

import org.apache.geode.cache.CacheFactory;
import org.apache.geode.cache.GemFireCache;
import org.apache.geode.cache.Region;
import org.apache.geode.cache.client.ClientCacheFactory;

/**
 * Integration Tests for {@link GeodeApplication}.
 *
 * @author John Blum
 * @see org.junit.Test
 * @see org.apache.geode.cache.GemFireCache
 * @see org.apache.geode.cache.Region
 * @since 1.0.0
 */
public class GeodeApplicationIntegrationTests {

	private GemFireCache resolveCache() {
		return Optional.ofNullable(resolveCache(ClientCacheFactory::getAnyInstance))
			.orElseGet(() -> resolveCache(CacheFactory::getAnyInstance));
	}

	private GemFireCache resolveCache(Supplier<GemFireCache> cacheResolver) {

		try {
			return cacheResolver.get();
		}
		catch (Throwable cause) {
			return null;
		}
	}

	@Test
	public void geodeApplicationRunsSuccessfully() {

		new GeodeApplication(new String[0]).run();

		GemFireCache cache = resolveCache();

		assertThat(cache).isNotNull();
		assertThat(cache.getName()).isEqualTo(GeodeApplication.GEODE_CACHE_NAME);

		Region<?, ?> region = cache.getRegion(GeodeApplication.GEODE_REGION_NAME);

		assertThat(region).isNotNull();
		assertThat(region.getName()).isEqualTo(GeodeApplication.GEODE_REGION_NAME);
		assertThat(region.get(1)).isEqualTo("TEST");
	}
}
