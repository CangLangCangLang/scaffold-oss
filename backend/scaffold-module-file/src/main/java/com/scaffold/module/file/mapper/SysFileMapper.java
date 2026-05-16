package com.scaffold.module.file.mapper;

import java.util.Date;
import java.util.List;
import org.apache.ibatis.annotations.Param;
import com.scaffold.module.file.domain.SysFile;
import com.scaffold.module.file.dto.FileQuery;

public interface SysFileMapper
{
    /** 列表分页（多过滤维度） */
    List<SysFile> selectPage(@Param("q") FileQuery q,
                             @Param("offset") int offset,
                             @Param("limit") int limit);

    /** 与 selectPage 同 where 的总数 */
    long count(@Param("q") FileQuery q);

    SysFile selectById(@Param("id") Long id);

    int insert(SysFile f);

    int updateById(SysFile f);

    /** 软删；置 del_flag=2 + delete_time=now()；ref_count>0 时返 0 */
    int softDeleteById(@Param("id") Long id, @Param("operator") String operator);

    /** 硬删（quartz 清理 / 管理员立即清回收站）；返回受影响行数 */
    int hardDeleteById(@Param("id") Long id);

    /** 引用计数原子 +1；ref_count>=0 守卫 */
    int incrRefCount(@Param("id") Long id);

    /** 引用计数原子 -1；ref_count>0 守卫，避免变负数 */
    int decrRefCount(@Param("id") Long id);

    /** 找软删超过 retainDays 天且 ref_count=0 的待清理记录（quartz 调用） */
    List<SysFile> selectExpiredSoftDeleted(@Param("threshold") Date threshold);
}
