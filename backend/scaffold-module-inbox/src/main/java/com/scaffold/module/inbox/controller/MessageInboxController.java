package com.scaffold.module.inbox.controller;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import com.scaffold.common.core.domain.AjaxResult;
import com.scaffold.common.core.page.TableDataInfo;
import com.scaffold.common.utils.SecurityUtils;
import com.scaffold.module.inbox.domain.MessageInboxEntry;
import com.scaffold.module.inbox.dto.InboxQueryRequest;
import com.scaffold.module.inbox.service.MessageInboxService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * 离线消息盒 REST 接口：
 * <ul>
 *   <li>GET /system/inbox/unread?limit= → 当前用户未读列表（顶部铃铛 popover 用）</li>
 *   <li>GET /system/inbox/unread-count → 未读数</li>
 *   <li>POST /system/inbox/{id}/ack    → 单条已读</li>
 *   <li>POST /system/inbox/ack-all     → 全部已读</li>
 *   <li>GET /system/inbox/page         → 全页面分页查询（status / type / 时间窗口过滤）</li>
 *   <li>POST /system/inbox/ack-batch   → 批量已读</li>
 *   <li>DELETE /system/inbox/batch     → 批量物理删除（仅本人）</li>
 *   <li>DELETE /system/inbox/{id}      → 单条物理删除（仅本人）</li>
 * </ul>
 * 跨用户隔离：username 一律由 SecurityUtils 取，不接受请求体 / 参数中的 target 字段。
 *
 * @author scaffold
 */
@Tag(name = "Inbox 收件箱", description = "用户消息收件箱：未读 popover、全页面分页、批量已读、批量删除")
@RestController
@RequestMapping("/system/inbox")
public class MessageInboxController
{
    @Autowired
    private MessageInboxService inboxService;

    @Operation(summary = "未读消息列表（顶部铃铛 popover 用）")
    @GetMapping("/unread")
    public AjaxResult listUnread(
            @Parameter(description = "上限条数，默认 50") @RequestParam(defaultValue = "50") int limit)
    {
        String username = SecurityUtils.getUsername();
        List<MessageInboxEntry> list = inboxService.fetchUnread(username, limit);
        return AjaxResult.success(list);
    }

    @Operation(summary = "未读消息数（顶部铃铛红点用）")
    @GetMapping("/unread-count")
    public AjaxResult countUnread()
    {
        String username = SecurityUtils.getUsername();
        Map<String, Object> data = new HashMap<>();
        data.put("count", inboxService.countUnread(username));
        return AjaxResult.success(data);
    }

    @Operation(summary = "标记单条消息为已读")
    @PostMapping("/{id}/ack")
    public AjaxResult ack(@Parameter(description = "inbox 记录主键") @PathVariable Long id)
    {
        boolean ok = inboxService.ack(id, SecurityUtils.getUsername());
        return ok ? AjaxResult.success() : AjaxResult.error("消息不存在或已读");
    }

    @Operation(summary = "全部标记已读")
    @PostMapping("/ack-all")
    public AjaxResult ackAll()
    {
        int n = inboxService.ackAll(SecurityUtils.getUsername());
        Map<String, Object> data = new HashMap<>();
        data.put("count", n);
        return AjaxResult.success(data);
    }

    @Operation(summary = "分页查询（全页面用）",
            description = "按状态 / 类型 LIKE / 时间窗口过滤；statuses 为空时默认 [0,1]，不含过期。")
    @GetMapping("/page")
    public TableDataInfo page(@ModelAttribute InboxQueryRequest req)
    {
        String username = SecurityUtils.getUsername();
        MessageInboxService.PageResult<MessageInboxEntry> result = inboxService.page(username, req);
        TableDataInfo info = new TableDataInfo(result.getRows(), result.getTotal());
        info.setCode(200);
        return info;
    }

    @Operation(summary = "批量标记已读",
            description = "仅命中本人未读消息（status=0 + target=current_user）")
    @PostMapping("/ack-batch")
    public AjaxResult ackBatch(@RequestBody List<Long> ids)
    {
        int n = inboxService.ackBatch(SecurityUtils.getUsername(), ids);
        Map<String, Object> data = new HashMap<>();
        data.put("count", n);
        return AjaxResult.success(data);
    }

    @Operation(summary = "批量删除（物理）", description = "仅命中本人记录")
    @DeleteMapping("/batch")
    public AjaxResult removeBatch(@RequestBody List<Long> ids)
    {
        int n = inboxService.removeBatch(SecurityUtils.getUsername(), ids);
        Map<String, Object> data = new HashMap<>();
        data.put("count", n);
        return AjaxResult.success(data);
    }

    @Operation(summary = "单条删除（物理）", description = "仅本人")
    @DeleteMapping("/{id}")
    public AjaxResult removeOne(@Parameter(description = "inbox 记录主键") @PathVariable Long id)
    {
        boolean ok = inboxService.removeOne(SecurityUtils.getUsername(), id);
        return ok ? AjaxResult.success() : AjaxResult.error("消息不存在");
    }
}
