package com.scaffold.system.domain;

import java.util.Date;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.scaffold.common.core.domain.BaseEntity;

/**
 * 操作审计记录（与 sys_audit_log 一一对应）。
 * <p>
 * 与传统 sys_oper_log 的差别：
 * <ul>
 *   <li>结构化资源标识：{@code module / action / resourceType / resourceId} → 可按资源检索</li>
 *   <li>记录 before/after 快照与 RFC 6902 JSON Patch diff，前端可直接渲染差异</li>
 *   <li>挂在 {@link com.scaffold.common.annotation.AuditLog} 注解上，由 AOP 自动写入</li>
 * </ul>
 * 两者互补共存：日常流水用 @Log/sys_oper_log；关键变更用 @AuditLog/sys_audit_log。
 * <p>
 * 继承 {@link BaseEntity} 仅为复用 {@code params} 透传机制——
 * {@link com.scaffold.framework.aspectj.DataScopeAspect} 会把生成的过滤 SQL 写到 {@code params.dataScope}，
 * mapper.xml 通过 {@code ${params.dataScope}} 占位拼接到查询末尾。
 *
 * @author scaffold
 */
public class SysAuditLog extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** 主键 */
    private Long id;
    /** 链路追踪 ID（来自 MDC traceId） */
    private String traceId;
    /** 业务模块 e.g. system.user / workflow.process */
    private String module;
    /** 动作 e.g. CREATE/UPDATE/DELETE/APPROVE */
    private String action;
    /** 资源类型 e.g. user / role / processInstance */
    private String resourceType;
    /** 资源 ID（字符串以兼容复合主键） */
    private String resourceId;
    /** 操作人用户名 */
    private String actor;
    /** 操作人 user_id */
    private Long actorId;
    /** 操作人所在部门 ID（用于 @DataScope 部门隔离） */
    private Long actorDeptId;
    /** 操作人部门名称（冗余，便于检索 / 显示） */
    private String actorDept;
    /** 时间区间检索：起始时间（仅查询用，不入库） */
    private Date fromTime;
    /** 时间区间检索：截止时间（仅查询用，不入库） */
    private Date toTime;
    /** 操作来源 IP */
    private String clientIp;
    /** 请求 URI */
    private String requestUri;
    /** 变更前快照（JSON 字符串，敏感字段已抹） */
    private String beforeValue;
    /** 变更后快照（JSON 字符串） */
    private String afterValue;
    /** RFC 6902 JSON Patch（数组形式） */
    private String diff;
    /** 0=成功 1=失败 */
    private Integer status;
    /** 失败时的错误信息 */
    private String errorMessage;
    /** 业务侧自定义说明（@AuditLog.comment 解析后） */
    private String comment;
    /** 耗时毫秒 */
    private Long costMs;
    /** 创建时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date createdAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getTraceId() { return traceId; }
    public void setTraceId(String traceId) { this.traceId = traceId; }
    public String getModule() { return module; }
    public void setModule(String module) { this.module = module; }
    public String getAction() { return action; }
    public void setAction(String action) { this.action = action; }
    public String getResourceType() { return resourceType; }
    public void setResourceType(String resourceType) { this.resourceType = resourceType; }
    public String getResourceId() { return resourceId; }
    public void setResourceId(String resourceId) { this.resourceId = resourceId; }
    public String getActor() { return actor; }
    public void setActor(String actor) { this.actor = actor; }
    public Long getActorId() { return actorId; }
    public void setActorId(Long actorId) { this.actorId = actorId; }
    public Long getActorDeptId() { return actorDeptId; }
    public void setActorDeptId(Long actorDeptId) { this.actorDeptId = actorDeptId; }
    public String getActorDept() { return actorDept; }
    public void setActorDept(String actorDept) { this.actorDept = actorDept; }
    public Date getFromTime() { return fromTime; }
    public void setFromTime(Date fromTime) { this.fromTime = fromTime; }
    public Date getToTime() { return toTime; }
    public void setToTime(Date toTime) { this.toTime = toTime; }
    public String getClientIp() { return clientIp; }
    public void setClientIp(String clientIp) { this.clientIp = clientIp; }
    public String getRequestUri() { return requestUri; }
    public void setRequestUri(String requestUri) { this.requestUri = requestUri; }
    public String getBeforeValue() { return beforeValue; }
    public void setBeforeValue(String beforeValue) { this.beforeValue = beforeValue; }
    public String getAfterValue() { return afterValue; }
    public void setAfterValue(String afterValue) { this.afterValue = afterValue; }
    public String getDiff() { return diff; }
    public void setDiff(String diff) { this.diff = diff; }
    public Integer getStatus() { return status; }
    public void setStatus(Integer status) { this.status = status; }
    public String getErrorMessage() { return errorMessage; }
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }
    public String getComment() { return comment; }
    public void setComment(String comment) { this.comment = comment; }
    public Long getCostMs() { return costMs; }
    public void setCostMs(Long costMs) { this.costMs = costMs; }
    public Date getCreatedAt() { return createdAt; }
    public void setCreatedAt(Date createdAt) { this.createdAt = createdAt; }
}
