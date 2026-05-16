package com.scaffold.module.report.runtime;

import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;
import javax.sql.DataSource;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import com.alibaba.druid.pool.DruidDataSource;
import com.scaffold.common.exception.ServiceException;
import com.scaffold.common.utils.Aes256Util;
import com.scaffold.module.report.domain.SysReportDataSource;
import com.scaffold.module.report.mapper.SysReportDataSourceMapper;

/**
 * 数据源管理：
 *
 * <ul>
 *   <li>id=0 → Spring 注入的主库 {@link DataSource}（不归本类管，由 Druid 主库托管）</li>
 *   <li>id&gt;0 → 按需建 {@link DruidDataSource}，惰性 / 缓存（避免每次执行都连数据库）</li>
 * </ul>
 *
 * <h3>密码安全</h3>
 * 落库为 Aes256Util ENC(...) 密文；本类只负责装载时解密一次性塞 Druid，不在内存外暴露明文。
 *
 * @author scaffold
 */
@Component
public class ReportDataSourceManager implements DisposableBean
{
    private final SysReportDataSourceMapper datasourceMapper;
    private final DataSource masterDataSource;
    private final String aesMasterKey;

    /** 缓存：id → 已建好的 DruidDataSource。修改 / 删除时需调 {@link #invalidate(Long)} */
    private final Map<Long, DruidDataSource> pool = new ConcurrentHashMap<>();

    public ReportDataSourceManager(SysReportDataSourceMapper datasourceMapper,
                                   DataSource masterDataSource,
                                   @Value("${app.module.report.aes-key:scaffold-report-default-key}") String aesMasterKey)
    {
        this.datasourceMapper = datasourceMapper;
        this.masterDataSource = masterDataSource;
        this.aesMasterKey = aesMasterKey;
    }

    /**
     * 解析 id 对应的 DataSource。
     *
     * @param id 0 = 主库；&gt;0 = sys_report_datasource.id
     */
    public DataSource resolve(Long id)
    {
        if (id == null || id <= 0)
        {
            return masterDataSource;
        }
        DruidDataSource cached = pool.get(id);
        if (cached != null)
        {
            return cached;
        }
        return pool.computeIfAbsent(id, this::build);
    }

    /** 失效缓存（编辑 / 删除数据源时调用） */
    public void invalidate(Long id)
    {
        if (id == null)
        {
            return;
        }
        DruidDataSource old = pool.remove(id);
        closeQuietly(old);
    }

    /** 全部失效（重启 / 测试用） */
    public void invalidateAll()
    {
        pool.values().forEach(this::closeQuietly);
        pool.clear();
    }

    /** AES 加密一段明文（service 层落库时使用） */
    public String encrypt(String plain)
    {
        return Aes256Util.encrypt(aesMasterKey, plain);
    }

    /**
     * 试连：从 cfg 临时 build 一个连接，open / close 后立刻释放，不入池。
     * 用于 /report/datasource/test 端点；密码若以 ENC(...) 起头则解密，否则视为明文。
     */
    public void test(SysReportDataSource cfg)
    {
        DruidDataSource ds = newDruid(cfg);
        try
        {
            ds.getConnection().close();
        }
        catch (Exception e)
        {
            throw new ServiceException("数据源连接失败：" + e.getMessage());
        }
        finally
        {
            closeQuietly(ds);
        }
    }

    @Override
    public void destroy()
    {
        invalidateAll();
    }

    /** 内部：从 mapper 拉记录，按 cfg 构建并缓存 */
    private DruidDataSource build(Long id)
    {
        SysReportDataSource cfg = datasourceMapper.selectById(id);
        if (cfg == null)
        {
            throw new ServiceException("数据源不存在：id=" + id);
        }
        if (!"0".equals(cfg.getStatus()))
        {
            throw new ServiceException("数据源已停用：" + cfg.getCode());
        }
        return newDruid(cfg);
    }

    /** 不入池的临时构造（test 用） */
    private DruidDataSource newDruid(SysReportDataSource cfg)
    {
        DruidDataSource ds = new DruidDataSource();
        ds.setUrl(cfg.getJdbcUrl());
        ds.setUsername(cfg.getUsername());
        String enc = cfg.getPasswordEnc();
        if (enc != null && !enc.isEmpty())
        {
            ds.setPassword(Aes256Util.decrypt(aesMasterKey, enc));
        }
        if (cfg.getDriverClass() != null && !cfg.getDriverClass().isEmpty())
        {
            ds.setDriverClassName(cfg.getDriverClass());
        }
        ds.setInitialSize(0);
        ds.setMaxActive(8);
        ds.setMinIdle(0);
        ds.setMaxWait(5000);
        ds.setValidationQuery(defaultValidation(cfg.getType()));
        ds.setTestOnBorrow(false);
        ds.setTestWhileIdle(true);
        ds.setConnectProperties(new Properties());
        ds.setName("report-" + cfg.getCode());
        try
        {
            ds.init();
        }
        catch (Exception e)
        {
            closeQuietly(ds);
            throw new ServiceException("数据源初始化失败：" + e.getMessage());
        }
        return ds;
    }

    private static String defaultValidation(String type)
    {
        if (type == null)
        {
            return "SELECT 1";
        }
        switch (type.toLowerCase())
        {
            case "oracle": return "SELECT 1 FROM DUAL";
            default: return "SELECT 1";
        }
    }

    private void closeQuietly(DruidDataSource ds)
    {
        if (ds == null)
        {
            return;
        }
        try { ds.close(); } catch (Exception ignore) { }
    }
}
