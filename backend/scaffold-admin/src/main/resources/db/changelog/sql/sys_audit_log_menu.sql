-- 为 sys_audit_log 列表 / 清理 / 详情 注册菜单与权限串
-- changeset 20260506-sys-audit-log-menu

-- 主菜单：操作审计（挂在 108 日志管理 下，order=3，跟在登录日志后面）
INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component,
                      query, route_name, is_frame, is_cache, menu_type, visible, status,
                      perms, icon, create_by, create_time, update_by, update_time, remark)
VALUES (502, '操作审计', 108, 3, 'auditlog', 'system/auditlog/index',
        '', '', 1, 0, 'C', '0', '0',
        'system:audit:list', 'log', 'admin', NOW(), '', NULL, '操作审计列表');

-- 按钮权限：清理（保留 N 天前数据）
-- 注：menu_id 5023（502 父菜单下的子按钮）。原 5021 与 form 模块的 form_menu.sql 5021
-- ('导出记录') 重号，fresh db 上跑会撞 sys_menu.PRIMARY，已改用 5023（502 段未占用号）。
INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component,
                      query, route_name, is_frame, is_cache, menu_type, visible, status,
                      perms, icon, create_by, create_time, update_by, update_time, remark)
VALUES (5023, '审计清理', 502, 1, '#', '', '', '', 1, 0, 'F', '0', '0',
        'system:audit:clean', '#', 'admin', NOW(), '', NULL, '清理 N 天前的审计日志');

-- 把这些菜单分配给超级管理员（role_id=1），方便回归
INSERT IGNORE INTO sys_role_menu (role_id, menu_id) VALUES (1, 502);
INSERT IGNORE INTO sys_role_menu (role_id, menu_id) VALUES (1, 5023);
