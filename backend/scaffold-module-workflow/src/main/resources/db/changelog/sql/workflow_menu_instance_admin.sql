-- 工作流增强：流程实例管理菜单（与 3001 父菜单同 module）
-- changeset workflow-20260507-instance-admin-menu，由 module-workflow.yml 引用
-- 备注：admin (role_id=1) 走 *:*:* 通配，不需要在 sys_role_menu 显式授权；
--      其他角色 / 流程发起人本身已经有 workflow:process:list 权限，可直接看到入口；
--      非 admin 调用 /workflow/process/instances 端点会被强制按 startedBy=current 过滤。
INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
VALUES (3027, '实例管理', 3001, 6, 'instance-admin', 'workflow/ProcessAdmin', '', 1, 0, 'C', '0', '0', 'workflow:process:list', 'monitor', 'admin', NOW(), '', NULL, '流程实例总列表（admin 全量 / 其他角色仅本人发起）');
