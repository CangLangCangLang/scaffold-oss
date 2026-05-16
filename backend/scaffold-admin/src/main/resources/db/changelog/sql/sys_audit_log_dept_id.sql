-- 数据级权限示例：sys_audit_log 增加 actor_dept_id，让审计列表能按部门隔离
-- changeset 20260506-sys-audit-log-dept-id
ALTER TABLE sys_audit_log
    ADD COLUMN actor_dept_id BIGINT DEFAULT NULL COMMENT '操作人所在部门 ID（用于 @DataScope 部门隔离）';
ALTER TABLE sys_audit_log
    ADD INDEX idx_actor_dept_time (actor_dept_id, created_at);
