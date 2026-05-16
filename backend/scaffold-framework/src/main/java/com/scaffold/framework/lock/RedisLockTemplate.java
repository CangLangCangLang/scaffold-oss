package com.scaffold.framework.lock;

import java.util.Collections;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Component;
import com.scaffold.common.exception.ServiceException;

/**
 * 基于 Redis 的轻量分布式锁工具。
 * <p>
 * acquire 使用 {@code SET key value NX EX expire}，release 使用 Lua 脚本比对 token，
 * 避免误删别人的锁。适合短时业务互斥场景；高并发下建议直接使用 Redisson。
 *
 * @author scaffold
 */
@Component
public class RedisLockTemplate
{
    private static final Logger log = LoggerFactory.getLogger(RedisLockTemplate.class);

    private static final String UNLOCK_SCRIPT =
            "if redis.call('get', KEYS[1]) == ARGV[1] then "
            + "return redis.call('del', KEYS[1]) else return 0 end";

    private static final RedisScript<Long> UNLOCK = new DefaultRedisScript<>(UNLOCK_SCRIPT, Long.class);

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    /**
     * 尝试获取锁。
     *
     * @param lockKey         锁 key
     * @param leaseSeconds    自动过期时间（秒）
     * @return 锁 token，失败返回 null
     */
    public String tryAcquire(String lockKey, int leaseSeconds)
    {
        String token = UUID.randomUUID().toString().replace("-", "");
        Boolean ok = stringRedisTemplate.opsForValue()
                .setIfAbsent(lockKey, token, leaseSeconds, TimeUnit.SECONDS);
        return Boolean.TRUE.equals(ok) ? token : null;
    }

    /**
     * 释放锁；只有持有方（token 匹配）才能释放。
     */
    public boolean release(String lockKey, String token)
    {
        if (lockKey == null || token == null)
        {
            return false;
        }
        Long result = stringRedisTemplate.execute(UNLOCK, Collections.singletonList(lockKey), token);
        return result != null && result > 0;
    }

    /**
     * 在锁保护下执行业务逻辑；获取失败将抛出 {@link ServiceException}。
     *
     * @param lockKey      锁 key
     * @param leaseSeconds 自动过期时间（秒）
     * @param supplier     业务逻辑
     */
    public <T> T runWithLock(String lockKey, int leaseSeconds, Supplier<T> supplier)
    {
        String token = tryAcquire(lockKey, leaseSeconds);
        if (token == null)
        {
            throw new ServiceException("当前操作正在被其他请求处理，请稍候再试");
        }
        try
        {
            return supplier.get();
        }
        finally
        {
            try
            {
                release(lockKey, token);
            }
            catch (Exception e)
            {
                log.warn("释放锁失败 key={} token={} reason={}", lockKey, token, e.getMessage());
            }
        }
    }
}
