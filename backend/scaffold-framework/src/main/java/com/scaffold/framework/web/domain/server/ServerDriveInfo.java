package com.scaffold.framework.web.domain.server;

/**
 * 服务器磁盘分区信息（监控页面专用）。
 *
 * <p>历史命名 {@code SysFile} 与 M-6 文件中心 {@code com.scaffold.module.file.domain.SysFile}
 * 撞 mybatis typeAliasesPackage `com.scaffold.**.domain` 的 alias 简单类名 → 启动报
 * "alias 'SysFile' is already mapped"。重命名为 {@code ServerDriveInfo} 后两边解耦。
 *
 * <p>无 mybatis / DB 关系，纯 oshi 拿到的磁盘 mount info POJO（dirName / total / free / used / usage）。
 * 仅被 {@link com.scaffold.framework.web.domain.Server#getSysFiles()} 引用、序列化到
 * {@code /monitor/server} 的 JSON 响应（Server 的 getter 名 sysFiles 保持不变，JSON 字段无变化）。
 *
 * @author scaffold
 */
public class ServerDriveInfo
{
    /**
     * 盘符路径
     */
    private String dirName;

    /**
     * 盘符类型
     */
    private String sysTypeName;

    /**
     * 文件类型
     */
    private String typeName;

    /**
     * 总大小
     */
    private String total;

    /**
     * 剩余大小
     */
    private String free;

    /**
     * 已经使用量
     */
    private String used;

    /**
     * 资源的使用率
     */
    private double usage;

    public String getDirName()
    {
        return dirName;
    }

    public void setDirName(String dirName)
    {
        this.dirName = dirName;
    }

    public String getSysTypeName()
    {
        return sysTypeName;
    }

    public void setSysTypeName(String sysTypeName)
    {
        this.sysTypeName = sysTypeName;
    }

    public String getTypeName()
    {
        return typeName;
    }

    public void setTypeName(String typeName)
    {
        this.typeName = typeName;
    }

    public String getTotal()
    {
        return total;
    }

    public void setTotal(String total)
    {
        this.total = total;
    }

    public String getFree()
    {
        return free;
    }

    public void setFree(String free)
    {
        this.free = free;
    }

    public String getUsed()
    {
        return used;
    }

    public void setUsed(String used)
    {
        this.used = used;
    }

    public double getUsage()
    {
        return usage;
    }

    public void setUsage(double usage)
    {
        this.usage = usage;
    }
}
