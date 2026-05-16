package com.scaffold.framework.aspectj;

import java.util.Arrays;
import java.util.Collections;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Component;
import com.scaffold.common.annotation.RateLimit;
import com.scaffold.common.core.domain.model.LoginUser;
import com.scaffold.common.exception.ServiceException;
import com.scaffold.common.utils.SecurityUtils;
import com.scaffold.common.utils.StringUtils;
import com.scaffold.common.utils.ip.IpUtils;

/**
 * 基于 Redis Lua 的限流切面，单脚本同时完成「INCR + EXPIRE 首次设置 + 阈值判断」，避免并发漂移。
 *
 * @author scaffold
 */
@Aspect
@Component
public class RateLimitAspect
{
    private static final Logger log = LoggerFactory.getLogger(RateLimitAspect.class);

    /**
     * Lua 脚本：原子计数 + 首次设置过期时间。
     * <pre>
     * local cnt = redis.call('INCR', KEYS[1])
     * if cnt == 1 then redis.call('EXPIRE', KEYS[1], ARGV[2]) end
     * if cnt > tonumber(ARGV[1]) then return -1 end
     * return cnt
     * </pre>
     */
    private static final String LIMIT_SCRIPT =
            "local cnt = redis.call('INCR', KEYS[1]) "
            + "if cnt == 1 then redis.call('EXPIRE', KEYS[1], ARGV[2]) end "
            + "if cnt > tonumber(ARGV[1]) then return -1 end "
            + "return cnt";

    private static final RedisScript<Long> SCRIPT = new DefaultRedisScript<>(LIMIT_SCRIPT, Long.class);

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @Around("@annotation(rateLimit)")
    public Object around(ProceedingJoinPoint point, RateLimit rateLimit) throws Throwable
    {
        String key = buildKey(point, rateLimit);
        Long current;
        try
        {
            current = stringRedisTemplate.execute(SCRIPT, Collections.singletonList(key),
                    String.valueOf(rateLimit.count()), String.valueOf(rateLimit.period()));
        }
        catch (Exception e)
        {
            log.warn("限流脚本执行失败，放行本次请求 key={} reason={}", key, e.getMessage());
            return point.proceed();
        }
        if (current == null || current < 0)
        {
            log.warn("限流命中 key={} count={} period={}s", key, rateLimit.count(), rateLimit.period());
            throw new ServiceException(rateLimit.message());
        }
        return point.proceed();
    }

    private String buildKey(ProceedingJoinPoint point, RateLimit rateLimit)
    {
        String dimension;
        switch (rateLimit.limitType())
        {
            case IP:
                dimension = safeIp();
                break;
            case USER:
                dimension = currentUser();
                break;
            default:
                dimension = "global";
        }
        String method = point.getSignature().getDeclaringTypeName() + "." + point.getSignature().getName();
        int hash = Arrays.deepHashCode(point.getArgs());
        return "rate:" + rateLimit.key() + ":" + dimension + ":" + method + ":" + hash;
    }

    private String safeIp()
    {
        try
        {
            return IpUtils.getIpAddr();
        }
        catch (Exception e)
        {
            return "unknown";
        }
    }

    private String currentUser()
    {
        try
        {
            LoginUser user = SecurityUtils.getLoginUser();
            if (user != null && StringUtils.isNotEmpty(user.getUsername()))
            {
                return user.getUsername();
            }
        }
        catch (Exception ignored)
        {
        }
        return safeIp();
    }
}
