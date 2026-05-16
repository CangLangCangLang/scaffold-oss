package com.scaffold.module.file.service;

import java.util.Date;
import java.util.List;
import java.util.Locale;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import com.scaffold.common.core.domain.entity.SysUser;
import com.scaffold.common.core.domain.model.LoginUser;
import com.scaffold.common.core.page.PageDomain;
import com.scaffold.common.core.page.TableSupport;
import com.scaffold.common.core.storage.FileStorageService;
import com.scaffold.common.exception.ServiceException;
import com.scaffold.common.utils.SecurityUtils;
import com.scaffold.framework.web.storage.UploadStorageService;
import com.scaffold.module.file.domain.SysFile;
import com.scaffold.module.file.dto.FileEditRequest;
import com.scaffold.module.file.dto.FileQuery;
import com.scaffold.module.file.mapper.SysFileMapper;
import com.scaffold.module.file.mapper.SysFileRefMapper;

/**
 * 文件中心：文件 CRUD + 引用计数 + 软删 / 硬删 + 鉴权读盘。
 *
 * <h3>上传链路</h3>
 * Controller 接收 MultipartFile → 调本 service.upload(file, bucket, folderId)；本 service 委托
 * framework {@link UploadStorageService} 落盘（带白名单 / 大小校验 / objectKey 生成），收到对外 URL 后
 * 反推 objectKey 落 sys_file 一行。
 *
 * <h3>软删 / 硬删 / 30 天清理</h3>
 * 默认走 {@link #softRemove(Long)} → 置 del_flag=2 + delete_time=now()；
 * {@code ref_count>0} 时拒删（保护 CMS / Form 引用）；
 * 30 天后由 {@link com.scaffold.module.file.job.FileCleanupJob#purge()} 调
 * {@link #purgeExpired(int)} 真正删盘 + 删 DB。
 *
 * <h3>读盘</h3>
 * 鉴权下载 controller 通过 {@link #resolveObjectKey(SysFile)} 反推 objectKey 后用
 * {@code Files.newInputStream} 读取（仅 local 实现；S3 时走 redirect）。
 *
 * @author scaffold
 */
@Service
public class FileService
{
    private static final Logger log = LoggerFactory.getLogger(FileService.class);
    /** 默认软删保留 30 天后由 quartz 物理清理。 */
    public static final int DEFAULT_RETAIN_DAYS = 30;

    @Autowired private SysFileMapper sysFileMapper;
    @Autowired private SysFileRefMapper sysFileRefMapper;
    @Autowired private UploadStorageService uploadStorageService;
    @Autowired private FileStorageService fileStorageService;

    /** {@code file.storage.url-prefix}，默认 /profile；从 URL 反推 objectKey 用 */
    @Value("${file.storage.url-prefix:/profile}")
    private String urlPrefix;

    // ---------- 上传 ----------

    /**
     * 业务上传入口。bucket=null/空 时走 "common"，落 /profile/common/yyyyMM/uuid.ext。
     */
    @Transactional(rollbackFor = Exception.class)
    public SysFile upload(MultipartFile file, String bucket, Long folderId)
    {
        String safeBucket = (bucket == null || bucket.isBlank()) ? "common" : bucket.trim();

        String url = uploadStorageService.save(file, safeBucket);

        SysFile entity = new SysFile();
        entity.setBucket(safeBucket);
        entity.setFolderId(folderId);
        String original = file.getOriginalFilename() == null ? "(unnamed)" : file.getOriginalFilename();
        entity.setName(original);
        entity.setOriginalName(original);
        entity.setExt(extOf(original));
        entity.setMime(file.getContentType());
        entity.setSizeBytes(file.getSize());
        entity.setStoragePath(url);
        entity.setRefCount(0);
        entity.setDelFlag("0");
        SysUser u = currentUser();
        entity.setCreateBy(u.getUserName());
        entity.setCreateByName(u.getNickName());
        entity.setUpdateBy(u.getUserName());

        sysFileMapper.insert(entity);
        log.info("file uploaded id={} bucket={} url={} size={}KB by={}",
                entity.getId(), safeBucket, url, file.getSize() / 1024, u.getUserName());
        return entity;
    }

    // ---------- CRUD ----------

    public java.util.Map<String, Object> page(FileQuery q)
    {
        PageDomain p = TableSupport.buildPageRequest();
        int pageNum = p.getPageNum() == null ? 1 : p.getPageNum();
        int pageSize = p.getPageSize() == null ? 10 : p.getPageSize();
        int offset = (pageNum - 1) * pageSize;

        List<SysFile> rows = sysFileMapper.selectPage(q, offset, pageSize);
        long total = sysFileMapper.count(q);

        java.util.Map<String, Object> rsp = new java.util.LinkedHashMap<>();
        rsp.put("code", 200);
        rsp.put("msg", "OK");
        rsp.put("rows", rows);
        rsp.put("total", total);
        return rsp;
    }

    public SysFile detail(Long id)
    {
        SysFile f = sysFileMapper.selectById(id);
        if (f == null)
        {
            throw new ServiceException("文件不存在: " + id);
        }
        return f;
    }

    @Transactional(rollbackFor = Exception.class)
    public int edit(FileEditRequest req)
    {
        if (req == null || req.getId() == null)
        {
            throw new ServiceException("文件 ID 必填");
        }
        SysFile current = sysFileMapper.selectById(req.getId());
        if (current == null || "2".equals(current.getDelFlag()))
        {
            throw new ServiceException("文件不存在或已删除: " + req.getId());
        }
        SysFile patch = new SysFile();
        patch.setId(req.getId());
        patch.setName(req.getName());
        patch.setFolderId(req.getFolderId());
        patch.setCategory(req.getCategory());
        patch.setTags(req.getTags());
        patch.setRemark(req.getRemark());
        patch.setUpdateBy(SecurityUtils.getUsername());
        return sysFileMapper.updateById(patch);
    }

    @Transactional(rollbackFor = Exception.class)
    public int softRemove(Long id)
    {
        SysFile f = sysFileMapper.selectById(id);
        if (f == null)
        {
            throw new ServiceException("文件不存在: " + id);
        }
        if (f.getRefCount() != null && f.getRefCount() > 0)
        {
            throw new ServiceException("文件被 " + f.getRefCount() + " 处引用，禁止删除（请先在引用方解除）");
        }
        return sysFileMapper.softDeleteById(id, SecurityUtils.getUsername());
    }

    @Transactional(rollbackFor = Exception.class)
    public int batchSoftRemove(List<Long> ids)
    {
        if (ids == null || ids.isEmpty()) return 0;
        int total = 0;
        for (Long id : ids)
        {
            try { total += softRemove(id); }
            catch (ServiceException e)
            {
                log.warn("批量软删跳过 id={}: {}", id, e.getMessage());
            }
        }
        return total;
    }

    /** 管理员立即清回收站（不等 30 天） */
    @Transactional(rollbackFor = Exception.class)
    public int hardRemove(Long id)
    {
        SysFile f = sysFileMapper.selectById(id);
        if (f == null) return 0;
        if (f.getRefCount() != null && f.getRefCount() > 0)
        {
            throw new ServiceException("文件被引用，禁止物理删除");
        }
        // 删盘 + 删 ref + 删主记录
        deletePhysical(f);
        sysFileRefMapper.deleteByFileId(id);
        return sysFileMapper.hardDeleteById(id);
    }

    /**
     * quartz 调用：扫所有 del_flag=2 + delete_time <= now() - retainDays + ref_count=0 的记录，
     * 删盘 + 清 DB。每次最多 500 条（mapper LIMIT），分批跑避免单次卡死。
     */
    @Transactional(rollbackFor = Exception.class)
    public int purgeExpired(int retainDays)
    {
        int days = retainDays > 0 ? retainDays : DEFAULT_RETAIN_DAYS;
        long thresholdMs = System.currentTimeMillis() - (long) days * 24L * 3600L * 1000L;
        Date threshold = new Date(thresholdMs);
        List<SysFile> expired = sysFileMapper.selectExpiredSoftDeleted(threshold);
        if (expired.isEmpty())
        {
            log.debug("purgeExpired: nothing to clean (retain={}d)", days);
            return 0;
        }
        int n = 0;
        for (SysFile f : expired)
        {
            try
            {
                deletePhysical(f);
                sysFileRefMapper.deleteByFileId(f.getId());
                n += sysFileMapper.hardDeleteById(f.getId());
            }
            catch (Exception e)
            {
                log.warn("purgeExpired skip id={} reason={}", f.getId(), e.getMessage());
            }
        }
        log.info("purgeExpired done retain={}d candidates={} purged={}", days, expired.size(), n);
        return n;
    }

    // ---------- 读盘（鉴权下载） ----------

    /**
     * 把 sys_file.storage_path（"/profile/cms/image/202605/abc.jpg"）反推回 objectKey
     * （"cms/image/202605/abc.jpg"），只在 local 介质下有意义；S3 模式下 controller 应直接 302 redirect。
     */
    public String resolveObjectKey(SysFile f)
    {
        if (f == null || f.getStoragePath() == null) return null;
        String url = f.getStoragePath();
        String prefix = (urlPrefix == null || urlPrefix.isBlank()) ? "/profile" : urlPrefix;
        if (!prefix.startsWith("/")) prefix = "/" + prefix;
        if (url.startsWith(prefix + "/"))
        {
            return url.substring(prefix.length() + 1);
        }
        if (url.startsWith(prefix))
        {
            return url.substring(prefix.length());
        }
        // S3 / 远程 URL：返 null，让 controller 走 302 redirect
        return null;
    }

    /** 当前底层存储类型 — controller 用以决定走 304 redirect 还是本地读盘 */
    public String storageType()
    {
        return fileStorageService.type();
    }

    private void deletePhysical(SysFile f)
    {
        String key = resolveObjectKey(f);
        if (key == null) return; // S3 单独清理
        boolean ok = fileStorageService.delete(key);
        if (!ok)
        {
            log.debug("deletePhysical miss key={} (already gone or s3-redirect)", key);
        }
    }

    private static String extOf(String name)
    {
        if (name == null) return null;
        int idx = name.lastIndexOf('.');
        if (idx < 0 || idx == name.length() - 1) return null;
        return name.substring(idx + 1).toLowerCase(Locale.ROOT);
    }

    private SysUser currentUser()
    {
        try
        {
            LoginUser lu = SecurityUtils.getLoginUser();
            if (lu != null && lu.getUser() != null) return lu.getUser();
        }
        catch (Exception ignore) { /* not in security ctx — fall through */ }
        SysUser u = new SysUser();
        u.setUserName("anonymous");
        u.setNickName("anonymous");
        return u;
    }
}
