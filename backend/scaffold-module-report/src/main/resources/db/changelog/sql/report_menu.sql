-- 报表模块菜单 + 按钮权限初始化（M-8）
-- 菜单 ID 段：7001-7030（7001 父菜单 / 7002-7005 页面 / 7010-7022 按钮权限 / 7025 quartz 任务）

-- 父菜单：报表中心
INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
VALUES (7001, '报表中心', 0, 9, 'report', NULL, '', 1, 0, 'M', '0', '0', '', 'chart', 'admin', NOW(), '', NULL, '报表中心（M-8 可插拔模块）');

-- 报表模板列表
INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
VALUES (7002, '报表模板', 7001, 1, 'template', 'report/ReportList', '', 1, 0, 'C', '0', '0', 'report:template:list', 'list', 'admin', NOW(), '', NULL, 'SQL 模板维护');

-- 报表运行（在模板详情入口跳转）
INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
VALUES (7003, '看板列表', 7001, 2, 'dashboard', 'report/DashboardList', '', 1, 0, 'C', '0', '0', 'report:dashboard:list', 'monitor', 'admin', NOW(), '', NULL, '组合卡片看板');

-- 数据源管理（管理员）
INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
VALUES (7004, '数据源', 7001, 3, 'datasource', 'report/DataSourceList', '', 1, 0, 'C', '0', '0', 'report:datasource:list', 'database', 'admin', NOW(), '', NULL, '外部 JDBC 数据源（密码 AES 加密）');

-- 运行日志
INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
VALUES (7005, '运行日志', 7001, 4, 'log', 'report/RunLogList', '', 1, 0, 'C', '0', '0', 'report:log:list', 'log', 'admin', NOW(), '', NULL, '运行历史 + 慢查询查询');

-- 模板按钮权限（7010-7014）
INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark) VALUES (7010, '新增模板',  7002, 1, '#', '', '', 1, 0, 'F', '0', '0', 'report:template:add',    '#', 'admin', NOW(), '', NULL, '');
INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark) VALUES (7011, '编辑模板',  7002, 2, '#', '', '', 1, 0, 'F', '0', '0', 'report:template:edit',   '#', 'admin', NOW(), '', NULL, '');
INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark) VALUES (7012, '删除模板',  7002, 3, '#', '', '', 1, 0, 'F', '0', '0', 'report:template:remove', '#', 'admin', NOW(), '', NULL, '');
INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark) VALUES (7013, '运行模板',  7002, 4, '#', '', '', 1, 0, 'F', '0', '0', 'report:template:run',    '#', 'admin', NOW(), '', NULL, '走 ReportRunner，受 perm_key 二次校验');
INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark) VALUES (7014, '导出报表',  7002, 5, '#', '', '', 1, 0, 'F', '0', '0', 'report:template:export', '#', 'admin', NOW(), '', NULL, 'CSV / xlsx');

-- 看板按钮权限（7015-7018）
INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark) VALUES (7015, '新建看板',  7003, 1, '#', '', '', 1, 0, 'F', '0', '0', 'report:dashboard:add',    '#', 'admin', NOW(), '', NULL, '');
INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark) VALUES (7016, '编辑看板',  7003, 2, '#', '', '', 1, 0, 'F', '0', '0', 'report:dashboard:edit',   '#', 'admin', NOW(), '', NULL, '');
INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark) VALUES (7017, '删除看板',  7003, 3, '#', '', '', 1, 0, 'F', '0', '0', 'report:dashboard:remove', '#', 'admin', NOW(), '', NULL, '');
INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark) VALUES (7018, '查看看板',  7003, 4, '#', '', '', 1, 0, 'F', '0', '0', 'report:dashboard:view',   '#', 'admin', NOW(), '', NULL, '查看 = perm_key + report:dashboard:view 同时具备');

-- 数据源按钮权限（7019-7022）
INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark) VALUES (7019, '新增数据源', 7004, 1, '#', '', '', 1, 0, 'F', '0', '0', 'report:datasource:add',    '#', 'admin', NOW(), '', NULL, '');
INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark) VALUES (7020, '编辑数据源', 7004, 2, '#', '', '', 1, 0, 'F', '0', '0', 'report:datasource:edit',   '#', 'admin', NOW(), '', NULL, '');
INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark) VALUES (7021, '删除数据源', 7004, 3, '#', '', '', 1, 0, 'F', '0', '0', 'report:datasource:remove', '#', 'admin', NOW(), '', NULL, '');
INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark) VALUES (7022, '测试数据源', 7004, 4, '#', '', '', 1, 0, 'F', '0', '0', 'report:datasource:test',   '#', 'admin', NOW(), '', NULL, '');

-- quartz 定时任务条目：清理 90 天以上 run_log（每天凌晨 4 点跑）
INSERT INTO sys_job (job_id, job_name, job_group, invoke_target, cron_expression, misfire_policy, concurrent, status, create_by, create_time, remark)
VALUES (7025, '清理报表运行日志', 'DEFAULT', 'reportRunLogCleanupJob.purge()', '0 0 4 * * ?', '3', '1', '0', 'admin', NOW(), '报表中心：清 sys_report_run_log 超 90 天记录');
