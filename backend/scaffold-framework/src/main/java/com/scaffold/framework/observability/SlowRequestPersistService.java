package com.scaffold.framework.observability;

import java.util.concurrent.atomic.AtomicLong;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import com.scaffold.framework.observability.domain.SlowRequest;
import com.scaffold.framework.observability.mapper.SlowRequestMapper;

/**
 * sys_slow_request 异步落库——独立 bean 让 {@code @Async} 走代理（同类内 self-invocation 不生效）。
 *
 * <p>插入失败按指数节流打 ERROR：第 1、2、4、8、…、第 N 条失败才打一行；防止 DB 故障打爆日志。
 */
@Service
public class SlowRequestPersistService
{
    private static final Logger log = LoggerFactory.getLogger(SlowRequestPersistService.class);

    private final SlowRequestMapper mapper;
    private final AtomicLong insertFailures = new AtomicLong();

    public SlowRequestPersistService(SlowRequestMapper mapper)
    {
        this.mapper = mapper;
    }

    @Async
    public void asyncSave(SlowRequest record)
    {
        try
        {
            mapper.insert(record);
        }
        catch (Exception e)
        {
            long n = insertFailures.incrementAndGet();
            // 节流：1, 2, 4, 8, 16, 32... 才打日志（避免 DB 不可用时把 ERROR 打爆）
            if ((n & (n - 1)) == 0)
            {
                log.error("[Observability] 写 sys_slow_request 第 {} 次失败 / uri={} reason={} : {}",
                        n, record.getRequestUri(), record.getReason(), e.getMessage());
            }
        }
    }

    /** 测试 / 调试用：累计失败次数 */
    public long getInsertFailureCount()
    {
        return insertFailures.get();
    }
}
