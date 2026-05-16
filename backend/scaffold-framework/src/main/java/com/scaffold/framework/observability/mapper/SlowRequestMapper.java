package com.scaffold.framework.observability.mapper;

import java.util.Date;
import java.util.List;
import org.apache.ibatis.annotations.Param;
import com.scaffold.framework.observability.domain.SlowRequest;

/**
 * sys_slow_request Mapper。
 *
 * <p>在 {@code com.scaffold.**.mapper} 包内，自动被 framework 主 {@code @MapperScan} 注册。
 */
public interface SlowRequestMapper
{
    /** 异步插入：HttpRequestRecorder 用 ; cost_ms 已识别为慢 / 5xx 才会进来 */
    int insert(SlowRequest record);

    /** 按 alerted=0 + create_time >= since，列待告警的记录 */
    List<SlowRequest> selectPendingAlerts(@Param("since") Date since);

    /** 标记为已告警，避免重发 */
    int markAlerted(@Param("ids") List<Long> ids);

    /** 列表查询（管理控制台） */
    List<SlowRequest> selectList(
            @Param("reason") String reason,
            @Param("requestUri") String requestUri,
            @Param("beginTime") Date beginTime,
            @Param("endTime") Date endTime);

    /** 清理 N 天前 */
    int deleteOlderThan(@Param("days") int days);

    /** 单条删除 */
    int deleteById(@Param("id") Long id);

    /** 当前未告警条数（暴露 Gauge 指标） */
    long countPending();
}
