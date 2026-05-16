-- 卸载脚本（可选）：从主项目移除 scaffold-module-inbox 后，
-- 如需清理数据，可在维护窗口手工执行此 SQL。
-- 注意：这是物理删除，先做好备份。
DROP TABLE IF EXISTS sys_message_inbox;
