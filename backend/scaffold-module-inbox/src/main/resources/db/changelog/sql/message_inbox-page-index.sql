-- P2 全页面分页用：按 target + status + created_at 排序，命中索引即排序，避免 filesort。
-- 与原表 idx_target_status (scope, target, status) 错开顺序、错开范围列：
--   原索引服务 popover「未读 N 条 ORDER BY id」；
--   新索引服务全页面「按时间倒序分页」。
ALTER TABLE sys_message_inbox
    ADD INDEX idx_target_status_created (target, status, created_at);
