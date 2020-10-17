package com.alibaba.nacossync.util;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import lombok.experimental.UtilityClass;

import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

/**
 * Created by liaomengge on 2020/6/16.
 */
@UtilityClass
public class CaffeineUtil {

    private static final Cache<String, Object> cache = Caffeine.newBuilder()
            .initialCapacity(1).maximumSize(16)
            .expireAfterAccess(60, TimeUnit.MINUTES)
            .expireAfterWrite(60, TimeUnit.MINUTES)
            .build();

    public <V> V get(String key) {
        return (V) cache.getIfPresent(key);
    }

    public <V> V get(String key, Function<String, V> function) {
        return (V) cache.get(key, function);
    }

    public <V> void put(String key, V v) {
        cache.put(key, v);
    }

    public <V> void putAll(Map<String, V> map) {
        cache.putAll(map);
    }

    public void invalidate(String key) {
        cache.invalidate(key);
    }

    public void invalidateAll(Iterable<?> iterable) {
        cache.invalidateAll(iterable);
    }

    public void invalidateAll() {
        cache.invalidateAll();
    }

    public Cache<String, Object> getCache() {
        return cache;
    }
}
