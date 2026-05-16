-- 表单引擎模块卸载 SQL（M-10）
-- 已应用的 Liquibase changeset 不会自动回滚；运维需手动执行本脚本清表 + 清菜单。

DELETE FROM sys_role_menu WHERE menu_id IN (SELECT menu_id FROM sys_menu WHERE menu_id BETWEEN 5001 AND 5030);
DELETE FROM sys_menu WHERE menu_id BETWEEN 5001 AND 5030;

DROP TABLE IF EXISTS form_submission;
DROP TABLE IF EXISTS form_template;
