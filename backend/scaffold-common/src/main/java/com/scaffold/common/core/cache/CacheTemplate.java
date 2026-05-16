package com.scaffold.common.core.cache;

import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

/**
 * 通用缓存模板：
 * <ul>
 *   <li>get-or-load 语义：缓存命中返回，否则执行 loader 并回写</li>
 *   <li>缓存击穿：通过 Redis SET NX EX 串行化同一 key 的回源请求，等待锁的请求会再读一次缓存</li>
 *   <li>缓存穿透：loader 返回 null 时也会写入空值占位（{@link #NULL_PLACEHOLDER}），TTL 短一些</li>
 * </ul>
 *
 * <p>未引入 Redisson 等重型依赖；高并发场景如果想要可重入 / 自动续期，再切到 Redisson。
 *
 * @author scaffold
 */
@Component
public class CacheTemplate
{
    private static final Logger log = LoggerFactory.getLogger(CacheTemplate.class);

    /** 用于缓存穿透防御：loader 返回 null 时写入此对象 */
    public static final Object NULL_PLACEHOLDER = new Object();

    private static final String LOCK_PREFIX = "cache:lock:";

    private static final long DEFAULT_NULL_TTL_SECONDS = 30;

    private static final int DEFAULT_LOCK_TTL_SECONDS = 5;

    private static final long DEFAULT_WAIT_INTERVAL_MS = 50;

    private final RedisTemplate<String, Object> redisTemplate;

    @SuppressWarnings({ "unchecked", "rawtypes" })
    public CacheTemplate(RedisTemplate redisTemplate)
    {
        this.redisTemplate = (RedisTemplate<String, Object>) redisTemplate;
    }

    /**
     * 普通 get-or-load。
     *
     * @param key            缓存 key
     * @param ttl            过期时长
     * @param unit           过期时长单位
     * @param loader         未命中时的回源逻辑
     * @return 命中或回源后的值；如果 loader 返回 null 也会写入 {@link #NULL_PLACEHOLDER}，调用方拿到的是 null
     */
    public <T> T getOrLoad(String key, long ttl, TimeUnit unit, Callable<T> loader)
    {
        return getOrLoad(key, ttl, unit, DEFAULT_LOCK_TTL_SECONDS, loader);
    }

    /**
     * @param lockTtlSeconds 防击穿锁的最长持有时间，必须大于 loader 的 P99 时长
     */
    @SuppressWarnings("unchecked")
    public <T> T getOrLoad(String key, long ttl, TimeUnit unit, int lockTtlSeconds, Callable<T> loader)
    {
        Object cached = redisTemplate.opsForValue().get(key);
        if (cached != null)
        {
            return cached == NULL_PLACEHOLDER ? null : (T) cached;
        }

        String lockKey = LOCK_PREFIX + key;
        String token = Long.toString(System.nanoTime());
        Boolean ok = redisTemplate.opsForValue().setIfAbsent(lockKey, token, lockTtlSeconds, TimeUnit.SECONDS);

        if (Boolean.TRUE.equals(ok))
        {
            try
            {
                cached = redisTemplate.opsForValue().get(key);
                if (cached != null)
                {
                    return cached == NULL_PLACEHOLDER ? null : (T) cached;
                }
                T fresh = invokeLoader(loader);
                if (fresh == null)
                {
                    redisTemplate.opsForValue().set(key, NULL_PLACEHOLDER, DEFAULT_NULL_TTL_SECONDS, TimeUnit.SECONDS);
                    return null;
                }
                redisTemplate.opsForValue().set(key, fresh, ttl, unit);
                return fresh;
            }
            finally
            {
                releaseLock(lockKey, token);
            }
        }

        // 没拿到锁：自旋等待持锁线程把缓存写好；超过锁 TTL 仍读不到则降级直接回源
        long deadline = System.currentTimeMillis() + lockTtlSeconds * 1000L;
        while (System.currentTimeMillis() < deadline)
        {
            try
            {
                Thread.sleep(DEFAULT_WAIT_INTERVAL_MS);
            }
            catch (InterruptedException ie)
            {
                Thread.currentThread().interrupt();
                break;
            }
            cached = redisTemplate.opsForValue().get(key);
            if (cached != null)
            {
                return cached == NULL_PLACEHOLDER ? null : (T) cached;
            }
        }
        log.warn("缓存等待持锁线程超时，降级直接回源 key={}", key);
        return invokeLoader(loader);
    }

    /**
     * 主动失效。
     */
    public void evict(String key)
    {
        redisTemplate.delete(key);
    }

    private <T> T invokeLoader(Callable<T> loader)
    {
        try
        {
            return loader.call();
        }
        catch (RuntimeException e)
        {
            throw e;
        }
        catch (Exception e)
        {
            throw new RuntimeException(e);
        }
    }

    private void releaseLock(String lockKey, String token)
    {
        try
        {
            Object current = redisTemplate.opsForValue().get(lockKey);
            if (current != null && current.toString().equals(token))
            {
                redisTemplate.delete(lockKey);
            }
        }
        catch (Exception e)
        {
            log.warn("释放缓存锁失败 key={} reason={}", lockKey, e.getMessage());
        }
    }
}
