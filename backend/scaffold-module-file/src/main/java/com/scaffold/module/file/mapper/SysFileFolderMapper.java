package com.scaffold.module.file.mapper;

import java.util.List;
import org.apache.ibatis.annotations.Param;
import com.scaffold.module.file.domain.SysFileFolder;

public interface SysFileFolderMapper
{
    /** 一个 owner 下全部未软删文件夹（前端自行拼树） */
    List<SysFileFolder> selectByOwner(@Param("owner") String owner);

    SysFileFolder selectById(@Param("id") Long id);

    /** path 唯一约束，给重命名 / 创建做去重检查 */
    SysFileFolder selectByOwnerAndPath(@Param("owner") String owner, @Param("path") String path);

    int insert(SysFileFolder f);

    int updateById(SysFileFolder f);

    /** 软删（递归 path 前缀匹配子级一起标） */
    int softDeleteSubtree(@Param("owner") String owner, @Param("pathPrefix") String pathPrefix);
}
