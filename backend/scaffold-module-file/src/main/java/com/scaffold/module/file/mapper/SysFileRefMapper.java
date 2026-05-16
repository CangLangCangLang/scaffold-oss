package com.scaffold.module.file.mapper;

import java.util.List;
import org.apache.ibatis.annotations.Param;
import com.scaffold.module.file.domain.SysFileRef;

public interface SysFileRefMapper
{
    /** 幂等 INSERT IGNORE（建立一次引用） */
    int insertIgnore(SysFileRef r);

    /** 删除一条引用（解除关联） */
    int deleteOne(@Param("fileId") Long fileId,
                  @Param("module") String module,
                  @Param("type") String type,
                  @Param("id") String id);

    /** 列出所有引用某个文件的业务记录（用于"哪里在用我"） */
    List<SysFileRef> selectByFile(@Param("fileId") Long fileId);

    /** 删除所有 file_id=? 的引用（在文件硬删时清理） */
    int deleteByFileId(@Param("fileId") Long fileId);
}
