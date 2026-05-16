-- 工作流菜单与按钮权限初始化（与模块绑定，模块下线后这些行可由运维手动清理）
-- 父菜单：工作流管理（顶层）
INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
VALUES (3001, '工作流', 0, 5, 'workflow', NULL, '', 1, 0, 'M', '0', '0', '', 'tree', 'admin', NOW(), '', NULL, '工作流管理（可插拔模块）');

-- 子菜单 1：流程定义
INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
VALUES (3002, '流程定义', 3001, 1, 'process', 'workflow/ProcessList', '', 1, 0, 'C', '0', '0', 'workflow:process:list', 'tree-table', 'admin', NOW(), '', NULL, '流程定义列表');

-- 子菜单 2：待办任务
INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
VALUES (3003, '待办任务', 3001, 2, 'todo', 'workflow/TodoList', '', 1, 0, 'C', '0', '0', 'workflow:task:list', 'edit', 'admin', NOW(), '', NULL, '我的待办');

-- 子菜单 3：已办任务
INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
VALUES (3004, '已办任务', 3001, 3, 'done', 'workflow/DoneList', '', 1, 0, 'C', '0', '0', 'workflow:task:list', 'log', 'admin', NOW(), '', NULL, '已完成任务');

-- 子菜单 4：流程设计器
INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
VALUES (3005, '流程设计器', 3001, 4, 'designer', 'workflow/Designer', '', 1, 0, 'C', '0', '0', 'workflow:process:deploy', 'code', 'admin', NOW(), '', NULL, 'BPMN 设计器');

-- 按钮权限：流程定义
INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark) VALUES (3010, '部署', 3002, 1, '#', '', '', 1, 0, 'F', '0', '0', 'workflow:process:deploy', '#', 'admin', NOW(), '', NULL, '');
INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark) VALUES (3011, '启动', 3002, 2, '#', '', '', 1, 0, 'F', '0', '0', 'workflow:process:start', '#', 'admin', NOW(), '', NULL, '');
INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark) VALUES (3012, '取消', 3002, 3, '#', '', '', 1, 0, 'F', '0', '0', 'workflow:process:cancel', '#', 'admin', NOW(), '', NULL, '');
INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark) VALUES (3013, '删除', 3002, 4, '#', '', '', 1, 0, 'F', '0', '0', 'workflow:process:remove', '#', 'admin', NOW(), '', NULL, '');

-- 按钮权限：任务
INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark) VALUES (3020, '完成', 3003, 1, '#', '', '', 1, 0, 'F', '0', '0', 'workflow:task:complete', '#', 'admin', NOW(), '', NULL, '');
INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark) VALUES (3021, '认领', 3003, 2, '#', '', '', 1, 0, 'F', '0', '0', 'workflow:task:claim', '#', 'admin', NOW(), '', NULL, '');
INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark) VALUES (3022, '转办', 3003, 3, '#', '', '', 1, 0, 'F', '0', '0', 'workflow:task:delegate', '#', 'admin', NOW(), '', NULL, '');
