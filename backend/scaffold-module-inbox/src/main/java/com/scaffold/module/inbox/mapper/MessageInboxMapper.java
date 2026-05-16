package com.scaffold.module.inbox.mapper;

import java.util.Date;
import java.util.List;
import org.apache.ibatis.annotations.Param;
import com.scaffold.module.inbox.domain.MessageInboxEntry;

/**
 * 离线消息盒 Mapper（位于 com.scaffold.**.mapper 包中以匹配全局 MapperScan）。
 *
 * @author scaffold
 */
public interface MessageInboxMapper
{
    int insert(MessageInboxEntry entry);

    List<MessageInboxEntry> selectUnreadByUser(@Param("username") String username,
            @Param("limit") int limit);

    int markRead(@Param("id") Long id, @Param("username") String username);

    int markAllReadByUser(@Param("username") String username);

    int countUnreadByUser(@Param("username") String username);

    int expireBeforeNow();

    int deleteExpired(@Param("days") int days);

    /**
     * 全页面分页查询：按用户范围 + 状态 + 类型 LIKE + 时间窗口过滤，倒序返回。
     * <p>
     * status 入参为 null 时默认包含 0 / 1，不含 2（已过期）；想看过期需显式传 statuses 列表。
     */
    List<MessageInboxEntry> selectPageByUser(@Param("username") String username,
            @Param("statuses") List<Integer> statuses,
            @Param("typeKeyword") String typeKeyword,
            @Param("fromTime") Date fromTime,
            @Param("toTime") Date toTime,
            @Param("offset") int offset,
            @Param("limit") int limit);

    int countPageByUser(@Param("username") String username,
            @Param("statuses") List<Integer> statuses,
            @Param("typeKeyword") String typeKeyword,
            @Param("fromTime") Date fromTime,
            @Param("toTime") Date toTime);

    /** 批量已读：仅命中本人未读消息（status=0）。 */
    int markBatchReadByIds(@Param("username") String username, @Param("ids") List<Long> ids);

    /** 批量删除：物理删除，仅命中本人记录。 */
    int deleteBatchByIds(@Param("username") String username, @Param("ids") List<Long> ids);

    /** 单条删除：物理删除，仅命中本人记录。 */
    int deleteByIdAndUser(@Param("id") Long id, @Param("username") String username);
}
