package com.scaffold.web.controller.system;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import com.scaffold.common.annotation.AuditLog;
import com.scaffold.common.core.controller.BaseController;
import com.scaffold.common.core.domain.AjaxResult;
import com.scaffold.common.core.page.TableDataInfo;
import com.scaffold.system.domain.SysAuditLog;
import com.scaffold.system.service.ISysAuditLogService;

/**
 * 操作审计日志（结构化事件 + diff），与 {@code /monitor/operlog} 互补：
 * <ul>
 *   <li>{@code /monitor/operlog}：日常操作流水，按 URL/IP/参数检索</li>
 *   <li>{@code /system/audit/log}：关键变更事件，按 module/resource/actor 检索 + 看 diff</li>
 * </ul>
 * <p>
 * 数据级权限：list 走 {@link com.scaffold.common.annotation.DataScope}（在 service 层），
 * 按当前登录人的角色 dataScope 自动按 {@code actor_dept_id} / {@code actor_id} 隔离。
 * 想改成"全部数据"，给当前角色挂"全部数据权限"即可。
 *
 * @author scaffold
 */
@RestController
@RequestMapping("/system/audit/log")
public class SysAuditLogController extends BaseController
{
    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    @Autowired
    private ISysAuditLogService auditLogService;

    /** 多条件检索 + 分页 + 数据级权限隔离 */
    @PreAuthorize("@ss.hasPermi('system:audit:list')")
    @GetMapping("/list")
    public TableDataInfo list(@RequestParam(required = false) String module,
                              @RequestParam(required = false) String action,
                              @RequestParam(required = false) String resourceType,
                              @RequestParam(required = false) String resourceId,
                              @RequestParam(required = false) String actor,
                              @RequestParam(required = false) Integer status,
                              @RequestParam(required = false) String fromTime,
                              @RequestParam(required = false) String toTime)
    {
        SysAuditLog query = new SysAuditLog();
        query.setModule(module);
        query.setAction(action);
        query.setResourceType(resourceType);
        query.setResourceId(resourceId);
        query.setActor(actor);
        query.setStatus(status);
        query.setFromTime(parseDate(fromTime));
        query.setToTime(parseDate(toTime));
        startPage();
        List<SysAuditLog> list = auditLogService.selectList(query);
        return getDataTable(list);
    }

    @PreAuthorize("@ss.hasPermi('system:audit:list')")
    @GetMapping("/{id}")
    public AjaxResult detail(@PathVariable Long id)
    {
        return AjaxResult.success(auditLogService.selectById(id));
    }

    /**
     * 清理 N 天前的审计记录（默认保留 180 天）。审计敏感，需高权限。
     */
    @AuditLog(module = "system.audit", action = "PURGE",
            comment = "'清理保留 ' + #retainDays + ' 天前的审计日志'", recordReturn = false)
    @PreAuthorize("@ss.hasPermi('system:audit:clean')")
    @DeleteMapping("/older")
    public AjaxResult deleteOlder(@RequestParam(defaultValue = "180") int retainDays)
    {
        int affected = auditLogService.deleteOlderThan(retainDays);
        Map<String, Object> data = new HashMap<>();
        data.put("affected", affected);
        return AjaxResult.success(data);
    }

    private static Date parseDate(String text)
    {
        if (text == null || text.isEmpty()) return null;
        try { return DATE_FORMAT.parse(text); }
        catch (ParseException e) { return null; }
    }
}
