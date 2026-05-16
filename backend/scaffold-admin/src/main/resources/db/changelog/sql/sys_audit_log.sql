-- 操作审计：结构化事件 + before/after JSON + RFC 6902 diff
CREATE TABLE IF NOT EXISTS sys_audit_log
(
    id            BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    trace_id      VARCHAR(64)  DEFAULT NULL COMMENT 'MDC traceId，便于跨日志系统串联',
    module        VARCHAR(64)  NOT NULL COMMENT '业务模块名 e.g. system.user / workflow.process',
    action        VARCHAR(32)  NOT NULL COMMENT '动作名 e.g. CREATE / UPDATE / DELETE / APPROVE',
    resource_type VARCHAR(64)  DEFAULT NULL COMMENT '资源类型 e.g. user / role / processInstance',
    resource_id   VARCHAR(64)  DEFAULT NULL COMMENT '资源 ID（字符串以兼容复合主键）',
    actor         VARCHAR(64)  DEFAULT NULL COMMENT '操作人用户名',
    actor_id      BIGINT       DEFAULT NULL COMMENT '操作人 user_id',
    actor_dept    VARCHAR(128) DEFAULT NULL COMMENT '操作人部门名（冗余加快检索）',
    client_ip     VARCHAR(64)  DEFAULT NULL,
    request_uri   VARCHAR(255) DEFAULT NULL,
    before_value  LONGTEXT     DEFAULT NULL COMMENT '变更前快照 JSON（敏感字段已抹）',
    after_value   LONGTEXT     DEFAULT NULL COMMENT '变更后快照 JSON',
    diff_value    LONGTEXT     DEFAULT NULL COMMENT 'RFC 6902 JSON Patch 数组',
    status        TINYINT(1)   NOT NULL DEFAULT 0 COMMENT '0=成功 1=失败',
    error_message VARCHAR(2000) DEFAULT NULL,
    comment       VARCHAR(255) DEFAULT NULL COMMENT '业务侧补充的人类可读说明',
    cost_ms       BIGINT       DEFAULT NULL,
    created_at    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_module_action_time (module, action, created_at),
    KEY idx_resource (resource_type, resource_id, created_at),
    KEY idx_actor_time (actor, created_at),
    KEY idx_trace (trace_id),
    KEY idx_created_at (created_at)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT '操作审计（结构化事件 + diff）';
