package com.scaffold.module.file.service;

import java.util.Date;
import java.util.List;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.scaffold.common.exception.ServiceException;
import com.scaffold.common.utils.SecurityUtils;
import com.scaffold.module.file.domain.SysFile;
import com.scaffold.module.file.domain.SysFileShare;
import com.scaffold.module.file.dto.ShareCreateRequest;
import com.scaffold.module.file.mapper.SysFileMapper;
import com.scaffold.module.file.mapper.SysFileShareMapper;

/**
 * 文件中心：分享链接（带过期 / 一次性 / 可选密码）。
 *
 * <h3>访问校验</h3>
 * 顺序：token 命中 → status=0 → 未过期 → 一次性还有名额 → 密码匹配。任意一步失败抛
 * {@link ServiceException}。访问成功后 visits +1；一次性 token 在第二次访问时把 status 置 2（已用尽）。
 *
 * @author scaffold
 */
@Service
public class ShareService
{
    @Autowired private SysFileShareMapper shareMapper;
    @Autowired private SysFileMapper fileMapper;

    public List<SysFileShare> listMine()
    {
        return shareMapper.selectByCreator(SecurityUtils.getUsername());
    }

    @Transactional(rollbackFor = Exception.class)
    public SysFileShare create(ShareCreateRequest req)
    {
        if (req == null || req.getFileId() == null)
        {
            throw new ServiceException("文件 ID 必填");
        }
        SysFile f = fileMapper.selectById(req.getFileId());
        if (f == null || "2".equals(f.getDelFlag()))
        {
            throw new ServiceException("文件不存在或已删除: " + req.getFileId());
        }

        SysFileShare s = new SysFileShare();
        s.setFileId(req.getFileId());
        s.setToken(genToken());
        if (req.getExpireDays() != null && req.getExpireDays() > 0)
        {
            long ms = System.currentTimeMillis() + (long) req.getExpireDays() * 24L * 3600L * 1000L;
            s.setExpireAt(new Date(ms));
        }
        s.setOneTime("1".equals(req.getOneTime()) ? "1" : "0");
        s.setVisits(0);
        s.setStatus("0");
        if (req.getPassword() != null && !req.getPassword().isBlank())
        {
            s.setPasswordHash(com.scaffold.common.utils.SecurityUtils.encryptPassword(req.getPassword().trim()));
        }
        s.setCreateBy(SecurityUtils.getUsername());
        shareMapper.insert(s);
        return s;
    }

    /**
     * 校验访问。返回关联的 SysFile（成功）或抛异常（失败）。
     * 访问成功会 visits +1；一次性时把 status 置 2。
     *
     * @param token 分享 token
     * @param password 可选的访问密码（明文）
     */
    @Transactional(rollbackFor = Exception.class)
    public SysFile access(String token, String password)
    {
        if (token == null || token.isBlank()) throw new ServiceException("分享 token 必填");
        SysFileShare s = shareMapper.selectByToken(token);
        if (s == null) throw new ServiceException("分享不存在或已撤销");
        if (!"0".equals(s.getStatus())) throw new ServiceException("分享已停用或已用尽");
        if (s.getExpireAt() != null && s.getExpireAt().getTime() < System.currentTimeMillis())
        {
            throw new ServiceException("分享已过期");
        }
        if ("1".equals(s.getOneTime()) && s.getVisits() != null && s.getVisits() >= 1)
        {
            throw new ServiceException("一次性分享已使用");
        }
        if (s.getPasswordHash() != null && !s.getPasswordHash().isBlank())
        {
            if (password == null || !com.scaffold.common.utils.SecurityUtils.matchesPassword(password, s.getPasswordHash()))
            {
                throw new ServiceException("访问密码不正确");
            }
        }
        SysFile f = fileMapper.selectById(s.getFileId());
        if (f == null || "2".equals(f.getDelFlag()))
        {
            throw new ServiceException("分享指向的文件已删除");
        }
        shareMapper.incrVisits(s.getId());
        if ("1".equals(s.getOneTime()))
        {
            shareMapper.updateStatus(s.getId(), "2");
        }
        return f;
    }

    @Transactional(rollbackFor = Exception.class)
    public int disable(Long id)
    {
        return shareMapper.updateStatus(id, "1");
    }

    @Transactional(rollbackFor = Exception.class)
    public int remove(Long id)
    {
        return shareMapper.deleteById(id);
    }

    /** 22 位随机；URL 友好；冲突概率约 2^-130，无需重复检测 */
    private static String genToken()
    {
        return UUID.randomUUID().toString().replace("-", "").substring(0, 22);
    }
}
