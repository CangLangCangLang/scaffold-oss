package com.scaffold.module.file.service;

import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.scaffold.common.exception.ServiceException;
import com.scaffold.common.utils.SecurityUtils;
import com.scaffold.module.file.domain.SysFileFolder;
import com.scaffold.module.file.dto.FolderRequest;
import com.scaffold.module.file.mapper.SysFileFolderMapper;

/**
 * 文件中心：文件夹（owner 隔离 / path 拼接 / 软删递归）。
 *
 * <p>结构：每个用户独立看见自己的文件夹；admin 通过 controller 层走 owner 参数可看其它人。
 *
 * @author scaffold
 */
@Service
public class FolderService
{
    @Autowired private SysFileFolderMapper folderMapper;

    /** 列表（当前用户隔离）— 树结构由前端拼。 */
    public List<SysFileFolder> listMine()
    {
        return folderMapper.selectByOwner(SecurityUtils.getUsername());
    }

    public List<SysFileFolder> listByOwner(String owner)
    {
        return folderMapper.selectByOwner(owner);
    }

    @Transactional(rollbackFor = Exception.class)
    public SysFileFolder create(FolderRequest req)
    {
        if (req == null || req.getName() == null || req.getName().isBlank())
        {
            throw new ServiceException("文件夹名必填");
        }
        Long parentId = req.getParentId() == null ? 0L : req.getParentId();
        String owner = SecurityUtils.getUsername();
        String path = buildPath(owner, parentId, req.getName().trim());

        SysFileFolder dup = folderMapper.selectByOwnerAndPath(owner, path);
        if (dup != null)
        {
            throw new ServiceException("文件夹已存在: " + path);
        }
        SysFileFolder f = new SysFileFolder();
        f.setOwner(owner);
        f.setParentId(parentId);
        f.setName(req.getName().trim());
        f.setPath(path);
        f.setDelFlag("0");
        f.setCreateBy(owner);
        f.setUpdateBy(owner);
        folderMapper.insert(f);
        return f;
    }

    @Transactional(rollbackFor = Exception.class)
    public int rename(FolderRequest req)
    {
        if (req == null || req.getId() == null || req.getName() == null || req.getName().isBlank())
        {
            throw new ServiceException("ID 与新名必填");
        }
        SysFileFolder current = folderMapper.selectById(req.getId());
        if (current == null || "2".equals(current.getDelFlag()))
        {
            throw new ServiceException("文件夹不存在: " + req.getId());
        }
        if (!current.getOwner().equals(SecurityUtils.getUsername()))
        {
            throw new ServiceException("没有权限改他人文件夹");
        }

        String newName = req.getName().trim();
        String newPath = buildPath(current.getOwner(), current.getParentId(), newName);

        SysFileFolder dup = folderMapper.selectByOwnerAndPath(current.getOwner(), newPath);
        if (dup != null && !dup.getId().equals(current.getId()))
        {
            throw new ServiceException("同级已有同名: " + newPath);
        }

        SysFileFolder patch = new SysFileFolder();
        patch.setId(current.getId());
        patch.setName(newName);
        patch.setPath(newPath);
        patch.setUpdateBy(SecurityUtils.getUsername());
        return folderMapper.updateById(patch);
    }

    /** 软删（递归子级） */
    @Transactional(rollbackFor = Exception.class)
    public int remove(Long id)
    {
        SysFileFolder f = folderMapper.selectById(id);
        if (f == null) throw new ServiceException("文件夹不存在: " + id);
        if (!f.getOwner().equals(SecurityUtils.getUsername()))
        {
            throw new ServiceException("没有权限删他人文件夹");
        }
        return folderMapper.softDeleteSubtree(f.getOwner(), f.getPath());
    }

    private String buildPath(String owner, Long parentId, String name)
    {
        if (parentId == null || parentId == 0L)
        {
            return "/" + name;
        }
        SysFileFolder parent = folderMapper.selectById(parentId);
        if (parent == null || !parent.getOwner().equals(owner))
        {
            throw new ServiceException("父级文件夹不存在或不属于你: " + parentId);
        }
        return parent.getPath() + "/" + name;
    }
}
