package com.scaffold.framework.lock;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.lang.reflect.Field;
import java.util.List;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.data.redis.core.script.RedisScript;
import com.scaffold.common.exception.ServiceException;

/**
 * Tests for {@link RedisLockTemplate}.
 */
class RedisLockTemplateTest
{
    private StringRedisTemplate redisTemplate;
    @SuppressWarnings("rawtypes")
    private ValueOperations valueOps;
    private RedisLockTemplate template;

    @BeforeEach
    void setUp() throws Exception
    {
        redisTemplate = mock(StringRedisTemplate.class);
        valueOps = mock(ValueOperations.class);
        when(redisTemplate.opsForValue()).thenReturn(valueOps);

        template = new RedisLockTemplate();
        Field field = RedisLockTemplate.class.getDeclaredField("stringRedisTemplate");
        field.setAccessible(true);
        field.set(template, redisTemplate);
    }

    @Test
    @SuppressWarnings("unchecked")
    void tryAcquireReturnsTokenWhenSetIfAbsentSucceeds()
    {
        when(valueOps.setIfAbsent(eq("k"), anyString(), eq(30L), eq(TimeUnit.SECONDS))).thenReturn(true);

        String token = template.tryAcquire("k", 30);

        assertThat(token).isNotBlank();
        verify(valueOps, times(1)).setIfAbsent(eq("k"), anyString(), eq(30L), eq(TimeUnit.SECONDS));
    }

    @Test
    @SuppressWarnings("unchecked")
    void tryAcquireReturnsNullWhenLockHeld()
    {
        when(valueOps.setIfAbsent(anyString(), anyString(), anyLong(), any())).thenReturn(false);

        assertThat(template.tryAcquire("k", 10)).isNull();
    }

    @Test
    @SuppressWarnings("unchecked")
    void releaseChecksTokenViaLuaScript()
    {
        when(redisTemplate.execute(any(RedisScript.class), any(List.class), any(Object[].class))).thenReturn(1L);

        boolean released = template.release("k", "tok");

        assertThat(released).isTrue();
    }

    @Test
    @SuppressWarnings("unchecked")
    void runWithLockThrowsServiceExceptionWhenAcquireFails()
    {
        when(valueOps.setIfAbsent(anyString(), anyString(), anyLong(), any())).thenReturn(false);

        assertThatThrownBy(() -> template.runWithLock("k", 5, () -> "ok"))
                .isInstanceOf(ServiceException.class);
    }

    @Test
    @SuppressWarnings("unchecked")
    void runWithLockExecutesAndReleases()
    {
        when(valueOps.setIfAbsent(anyString(), anyString(), anyLong(), any())).thenReturn(true);
        when(redisTemplate.execute(any(RedisScript.class), any(List.class), any(Object[].class))).thenReturn(1L);

        String result = template.runWithLock("k", 5, () -> "done");

        assertThat(result).isEqualTo("done");
        verify(redisTemplate).execute(any(RedisScript.class), any(List.class), any(Object[].class));
    }
}
