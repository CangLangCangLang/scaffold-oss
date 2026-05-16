-- 工作流增强：前加签按钮权限（与 3001 父菜单同 module）
-- changeset workflow-20260507-presign-menu，由 module-workflow.yml 引用
-- 备注：admin (role_id=1) 走 *:*:* 通配，不需要在 sys_role_menu 显式授权；
--      其他角色按需在角色管理界面勾选"前加签"按钮即可。
INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark) VALUES (3026, '前加签', 3003, 7, '#', '', '', 1, 0, 'F', '0', '0', 'workflow:task:addsign-before', '#', 'admin', NOW(), '', NULL, '当前任务前并行插入审批人');
