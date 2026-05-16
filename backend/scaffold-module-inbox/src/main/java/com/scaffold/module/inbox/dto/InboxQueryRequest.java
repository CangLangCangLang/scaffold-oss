package com.scaffold.module.inbox.dto;

import java.util.Date;
import java.util.List;
import org.springframework.format.annotation.DateTimeFormat;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 全页面分页查询参数。
 * <ul>
 *   <li>不暴露 username / target —— 服务层强制注入当前登录用户，跨用户隔离不可绕过。</li>
 *   <li>statuses 为空时默认 [0, 1]，即未读 + 已读（不含过期 status=2）。</li>
 * </ul>
 *
 * @author scaffold
 */
@Schema(description = "收件箱分页查询参数")
public class InboxQueryRequest
{
    @Schema(description = "页码（1 起）", example = "1", defaultValue = "1")
    private Integer pageNum = 1;

    @Schema(description = "每页条数", example = "10", defaultValue = "10")
    private Integer pageSize = 10;

    @Schema(description = "状态过滤；为空则默认 [0,1]（未读+已读，不含过期）。0=未读 1=已读 2=已过期",
            allowableValues = { "0", "1", "2" })
    private List<Integer> statuses;

    @Schema(description = "type 业务类型 LIKE，例如 cms.article 或 workflow.task", example = "cms.article")
    private String typeKeyword;

    @Schema(description = "起始时间（含），ISO 8601", example = "2026-05-01T00:00:00")
    @DateTimeFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private Date fromTime;

    @Schema(description = "截止时间（含），ISO 8601", example = "2026-05-31T23:59:59")
    @DateTimeFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private Date toTime;

    public Integer getPageNum() { return pageNum == null || pageNum < 1 ? 1 : pageNum; }
    public void setPageNum(Integer pageNum) { this.pageNum = pageNum; }

    public Integer getPageSize()
    {
        if (pageSize == null || pageSize < 1) return 10;
        return Math.min(pageSize, 100);
    }
    public void setPageSize(Integer pageSize) { this.pageSize = pageSize; }

    public List<Integer> getStatuses() { return statuses; }
    public void setStatuses(List<Integer> statuses) { this.statuses = statuses; }

    public String getTypeKeyword()
    {
        return typeKeyword == null ? null : (typeKeyword.isBlank() ? null : typeKeyword.trim());
    }
    public void setTypeKeyword(String typeKeyword) { this.typeKeyword = typeKeyword; }

    public Date getFromTime() { return fromTime; }
    public void setFromTime(Date fromTime) { this.fromTime = fromTime; }

    public Date getToTime() { return toTime; }
    public void setToTime(Date toTime) { this.toTime = toTime; }
}
