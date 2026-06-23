package com.yanxiaomap.util;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
public class RedisUtil {

    @Autowired(required = false)
    private RedisTemplate<String, Object> redisTemplate;

    private final Map<String, CacheEntry> localCache = new ConcurrentHashMap<>();
    private boolean useLocalCache = false;

    @PostConstruct
    public void init() {
        if (redisTemplate == null) {
            useLocalCache = true;
            log.warn("RedisTemplate不可用，将使用本地内存缓存（仅适用于开发环境）");
        } else {
            log.info("RedisTemplate已注入，使用Redis缓存");
        }
    }

    // ============================== common ==============================

    public boolean expire(String key, long time) {
        if (useLocalCache) {
            return localExpire(key, time);
        }
        try {
            if (time > 0) {
                redisTemplate.expire(key, time, TimeUnit.SECONDS);
            }
            return true;
        } catch (Exception e) {
            log.error("设置缓存失效时间失败: key={}, time={}", key, time, e);
            return false;
        }
    }

    public Long getExpire(String key) {
        if (useLocalCache) {
            CacheEntry entry = localCache.get(key);
            if (entry == null) return -2L;
            if (entry.expireAt == null) return -1L;
            long remain = entry.expireAt - System.currentTimeMillis();
            return remain > 0 ? remain / 1000 : -2L;
        }
        try {
            return redisTemplate.getExpire(key, TimeUnit.SECONDS);
        } catch (Exception e) {
            log.error("获取过期时间失败: key={}", key, e);
            return -1L;
        }
    }

    public Boolean hasKey(String key) {
        if (useLocalCache) {
            return localHasKey(key);
        }
        try {
            return redisTemplate.hasKey(key);
        } catch (Exception e) {
            log.error("判断key是否存在失败: key={}", key, e);
            return false;
        }
    }

    @SuppressWarnings("unchecked")
    public void delete(String... keys) {
        if (useLocalCache) {
            for (String key : keys) {
                localCache.remove(key);
            }
            return;
        }
        if (keys != null && keys.length > 0) {
            if (keys.length == 1) {
                redisTemplate.delete(keys[0]);
            } else {
                redisTemplate.delete((Collection<String>) CollectionUtils.arrayToList(keys));
            }
        }
    }

    public void delete(Collection<String> keys) {
        if (useLocalCache) {
            keys.forEach(localCache::remove);
            return;
        }
        redisTemplate.delete(keys);
    }

    // ============================== String ==============================

    public Object get(String key) {
        if (useLocalCache) {
            return localGet(key);
        }
        return key == null ? null : redisTemplate.opsForValue().get(key);
    }

    public boolean set(String key, Object value) {
        if (useLocalCache) {
            return localSet(key, value, null);
        }
        try {
            redisTemplate.opsForValue().set(key, value);
            return true;
        } catch (Exception e) {
            log.error("设置缓存失败: key={}", key, e);
            return false;
        }
    }

    public boolean set(String key, Object value, long time) {
        if (useLocalCache) {
            return localSet(key, value, time > 0 ? time : null);
        }
        try {
            if (time > 0) {
                redisTemplate.opsForValue().set(key, value, time, TimeUnit.SECONDS);
            } else {
                set(key, value);
            }
            return true;
        } catch (Exception e) {
            log.error("设置缓存失败: key={}, time={}", key, time, e);
            return false;
        }
    }

    public Long increment(String key, long delta) {
        if (useLocalCache) {
            return localIncr(key, delta);
        }
        return redisTemplate.opsForValue().increment(key, delta);
    }

    public Long incr(String key, long delta) {
        return increment(key, delta);
    }

    public Long decrement(String key, long delta) {
        if (useLocalCache) {
            return localIncr(key, -delta);
        }
        return redisTemplate.opsForValue().decrement(key, -delta);
    }

    // ============================== Hash ==============================

    @SuppressWarnings("unchecked")
    public Object hget(String key, String hashKey) {
        if (useLocalCache) {
            CacheEntry entry = localCache.get(key);
            if (entry == null || entry.expired()) return null;
            if (entry.value instanceof Map) {
                return ((Map<String, Object>) entry.value).get(hashKey);
            }
            return null;
        }
        return redisTemplate.opsForHash().get(key, hashKey);
    }

    @SuppressWarnings("unchecked")
    public Map<Object, Object> hmget(String key) {
        if (useLocalCache) {
            CacheEntry entry = localCache.get(key);
            if (entry == null || entry.expired()) return new HashMap<>();
            if (entry.value instanceof Map) {
                return new HashMap<>((Map<Object, Object>) entry.value);
            }
            return new HashMap<>();
        }
        return redisTemplate.opsForHash().entries(key);
    }

    @SuppressWarnings("unchecked")
    public boolean hset(String key, String hashKey, Object value) {
        if (useLocalCache) {
            return localHSet(key, hashKey, value, null);
        }
        try {
            redisTemplate.opsForHash().put(key, hashKey, value);
            return true;
        } catch (Exception e) {
            log.error("设置Hash缓存失败: key={}, hashKey={}", key, hashKey, e);
            return false;
        }
    }

    @SuppressWarnings("unchecked")
    public boolean hset(String key, String hashKey, Object value, long time) {
        if (useLocalCache) {
            return localHSet(key, hashKey, value, time > 0 ? time : null);
        }
        try {
            redisTemplate.opsForHash().put(key, hashKey, value);
            if (time > 0) {
                expire(key, time);
            }
            return true;
        } catch (Exception e) {
            log.error("设置Hash缓存失败: key={}, hashKey={}, time={}", key, hashKey, time, e);
            return false;
        }
    }

    @SuppressWarnings("unchecked")
    public boolean hmset(String key, Map<String, Object> map) {
        if (useLocalCache) {
            CacheEntry entry = new CacheEntry(new ConcurrentHashMap<>(map), null);
            localCache.put(key, entry);
            return true;
        }
        try {
            redisTemplate.opsForHash().putAll(key, map);
            return true;
        } catch (Exception e) {
            log.error("设置Hash缓存失败: key={}", key, e);
            return false;
        }
    }

    public void hdelete(String key, Object... hashKeys) {
        if (useLocalCache) {
            CacheEntry entry = localCache.get(key);
            if (entry != null && entry.value instanceof Map) {
                @SuppressWarnings("unchecked")
                Map<Object, Object> map = (Map<Object, Object>) entry.value;
                for (Object hk : hashKeys) {
                    map.remove(hk);
                }
            }
            return;
        }
        redisTemplate.opsForHash().delete(key, hashKeys);
    }

    public boolean hHasKey(String key, String hashKey) {
        if (useLocalCache) {
            CacheEntry entry = localCache.get(key);
            if (entry == null || entry.expired()) return false;
            if (entry.value instanceof Map) {
                @SuppressWarnings("unchecked")
                Map<String, Object> map = (Map<String, Object>) entry.value;
                return map.containsKey(hashKey);
            }
            return false;
        }
        return redisTemplate.opsForHash().hasKey(key, hashKey);
    }

    // ============================== Set ==============================

    @SuppressWarnings("unchecked")
    public Set<Object> sget(String key) {
        if (useLocalCache) {
            CacheEntry entry = localCache.get(key);
            if (entry == null || entry.expired()) return new HashSet<>();
            if (entry.value instanceof Set) {
                return new HashSet<>((Set<Object>) entry.value);
            }
            return new HashSet<>();
        }
        return redisTemplate.opsForSet().members(key);
    }

    @SuppressWarnings("unchecked")
    public Long sset(String key, Object... values) {
        if (useLocalCache) {
            CacheEntry entry = localCache.computeIfAbsent(key, k -> new CacheEntry(new HashSet<>(), null));
            if (!(entry.value instanceof Set)) {
                entry.value = new HashSet<>();
            }
            Set<Object> set = (Set<Object>) entry.value;
            long count = 0;
            for (Object v : values) {
                if (set.add(v)) count++;
            }
            return count;
        }
        try {
            return redisTemplate.opsForSet().add(key, values);
        } catch (Exception e) {
            log.error("设置Set缓存失败: key={}", key, e);
            return 0L;
        }
    }

    public Boolean sHasKey(String key, Object value) {
        if (useLocalCache) {
            CacheEntry entry = localCache.get(key);
            if (entry == null || entry.expired()) return false;
            if (entry.value instanceof Set) {
                @SuppressWarnings("unchecked")
                Set<Object> set = (Set<Object>) entry.value;
                return set.contains(value);
            }
            return false;
        }
        try {
            return redisTemplate.opsForSet().isMember(key, value);
        } catch (Exception e) {
            log.error("判断Set中是否存在值失败: key={}", key, e);
            return false;
        }
    }

    // ============================== List ==============================

    public List<Object> lget(String key, long start, long end) {
        if (useLocalCache) {
            CacheEntry entry = localCache.get(key);
            if (entry == null || entry.expired()) return null;
            if (entry.value instanceof List) {
                @SuppressWarnings("unchecked")
                List<Object> list = (List<Object>) entry.value;
                long from = Math.max(0, start);
                long to = end < 0 ? list.size() - 1 : Math.min(end, list.size() - 1);
                if (from > to) return new ArrayList<>();
                return new ArrayList<>(list.subList((int) from, (int) to + 1));
            }
            return null;
        }
        try {
            return redisTemplate.opsForList().range(key, start, end);
        } catch (Exception e) {
            log.error("获取List缓存失败: key={}", key, e);
            return null;
        }
    }

    public boolean lset(String key, List<Object> value) {
        if (useLocalCache) {
            localCache.put(key, new CacheEntry(new ArrayList<>(value), null));
            return true;
        }
        try {
            redisTemplate.opsForList().rightPushAll(key, value);
            return true;
        } catch (Exception e) {
            log.error("设置List缓存失败: key={}", key, e);
            return false;
        }
    }

    // ============================== Token Blacklist ==============================

    public boolean isTokenBlacklisted(String token) {
        String key = "token:blacklist:" + token;
        if (useLocalCache) {
            return localHasKey(key);
        }
        try {
            return redisTemplate.hasKey(key);
        } catch (Exception e) {
            log.error("检查令牌黑名单失败", e);
            return false;
        }
    }

    public boolean addTokenToBlacklist(String token, long expireSeconds) {
        String key = "token:blacklist:" + token;
        if (useLocalCache) {
            return localSet(key, "blacklisted", expireSeconds > 0 ? expireSeconds : null);
        }
        try {
            redisTemplate.opsForValue().set(key, "blacklisted", expireSeconds, TimeUnit.SECONDS);
            return true;
        } catch (Exception e) {
            log.error("添加令牌到黑名单失败", e);
            return false;
        }
    }

    public boolean removeTokenFromBlacklist(String token) {
        String key = "token:blacklist:" + token;
        if (useLocalCache) {
            localCache.remove(key);
            return true;
        }
        try {
            redisTemplate.delete(key);
            return true;
        } catch (Exception e) {
            log.error("从黑名单移除令牌失败", e);
            return false;
        }
    }

    // ============================== Local Cache Methods ==============================

    private boolean localHasKey(String key) {
        CacheEntry entry = localCache.get(key);
        return entry != null && !entry.expired();
    }

    private Object localGet(String key) {
        CacheEntry entry = localCache.get(key);
        if (entry == null || entry.expired()) {
            if (entry != null && entry.expired()) {
                localCache.remove(key);
            }
            return null;
        }
        return entry.value;
    }

    private boolean localSet(String key, Object value, Long expireSeconds) {
        Long expireAt = expireSeconds != null ? System.currentTimeMillis() + expireSeconds * 1000 : null;
        localCache.put(key, new CacheEntry(value, expireAt));
        return true;
    }

    private boolean localExpire(String key, long time) {
        CacheEntry entry = localCache.get(key);
        if (entry != null) {
            entry.expireAt = System.currentTimeMillis() + time * 1000;
            return true;
        }
        return false;
    }

    private Long localIncr(String key, long delta) {
        CacheEntry entry = localCache.computeIfAbsent(key, k -> new CacheEntry(0L, null));
        if (!(entry.value instanceof Number)) {
            entry.value = 0L;
        }
        long newVal = ((Number) entry.value).longValue() + delta;
        entry.value = newVal;
        return newVal;
    }

    @SuppressWarnings("unchecked")
    private boolean localHSet(String key, String hashKey, Object value, Long expireSeconds) {
        CacheEntry entry = localCache.computeIfAbsent(key, k -> {
            Long expireAt = expireSeconds != null ? System.currentTimeMillis() + expireSeconds * 1000 : null;
            return new CacheEntry(new ConcurrentHashMap<String, Object>(), expireAt);
        });
        if (!(entry.value instanceof Map)) {
            entry.value = new ConcurrentHashMap<String, Object>();
        }
        ((Map<String, Object>) entry.value).put(hashKey, value);
        return true;
    }

    // ============================== Inner Class ==============================

    private static class CacheEntry {
        Object value;
        Long expireAt;

        CacheEntry(Object value, Long expireAt) {
            this.value = value;
            this.expireAt = expireAt;
        }

        boolean expired() {
            return expireAt != null && System.currentTimeMillis() > expireAt;
        }
    }
}
