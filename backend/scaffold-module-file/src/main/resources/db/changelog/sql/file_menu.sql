-- 文件中心模块菜单 + 按钮权限初始化（M-6）
-- 菜单 ID 段：6001-6030（6001 父菜单 / 6002-6004 页面 / 6010-6021 按钮权限 / 6025 quartz 任务）

-- 父菜单：文件中心
INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
VALUES (6001, '文件中心', 0, 8, 'file', NULL, '', 1, 0, 'M', '0', '0', '', 'documentation', 'admin', NOW(), '', NULL, '文件中心（M-6 可插拔模块）');

-- 子页面：我的文件 / 全部文件（同一组件，按权限分支）
INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
VALUES (6002, '我的文件', 6001, 1, 'mine', 'file/FileList', '', 1, 0, 'C', '0', '0', 'file:list:mine', 'folder-opened', 'admin', NOW(), '', NULL, '当前用户视角');

INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
VALUES (6003, '全部文件', 6001, 2, 'all', 'file/FileList', '', 1, 0, 'C', '0', '0', 'file:list', 'folder-checked', 'admin', NOW(), '', NULL, '管理员视角');

-- 子页面：分享管理
INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
VALUES (6004, '分享管理', 6001, 3, 'share', 'file/ShareList', '', 1, 0, 'C', '0', '0', 'file:share:list', 'link', 'admin', NOW(), '', NULL, '分享链接列表');

-- 按钮权限：文件（6010-6015）
INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark) VALUES (6010, '上传文件', 6002, 1, '#', '', '', 1, 0, 'F', '0', '0', 'file:file:upload', '#', 'admin', NOW(), '', NULL, '');
INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark) VALUES (6011, '编辑文件', 6002, 2, '#', '', '', 1, 0, 'F', '0', '0', 'file:file:edit', '#', 'admin', NOW(), '', NULL, '改名 / 移动 / 改分类');
INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark) VALUES (6012, '删除文件', 6002, 3, '#', '', '', 1, 0, 'F', '0', '0', 'file:file:remove', '#', 'admin', NOW(), '', NULL, '软删 / 硬删');
INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark) VALUES (6013, '下载文件', 6002, 4, '#', '', '', 1, 0, 'F', '0', '0', 'file:file:download', '#', 'admin', NOW(), '', NULL, '走鉴权下载 /file/download/{id}');
INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark) VALUES (6014, '批量删', 6002, 5, '#', '', '', 1, 0, 'F', '0', '0', 'file:file:batch-remove', '#', 'admin', NOW(), '', NULL, '');
INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark) VALUES (6015, '彻底清回收站', 6003, 6, '#', '', '', 1, 0, 'F', '0', '0', 'file:file:purge', '#', 'admin', NOW(), '', NULL, '管理员立即清磁盘');

-- 按钮权限：文件夹（6016-6018）
INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark) VALUES (6016, '建文件夹', 6002, 6, '#', '', '', 1, 0, 'F', '0', '0', 'file:folder:add', '#', 'admin', NOW(), '', NULL, '');
INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark) VALUES (6017, '改文件夹名', 6002, 7, '#', '', '', 1, 0, 'F', '0', '0', 'file:folder:edit', '#', 'admin', NOW(), '', NULL, '');
INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark) VALUES (6018, '删文件夹', 6002, 8, '#', '', '', 1, 0, 'F', '0', '0', 'file:folder:remove', '#', 'admin', NOW(), '', NULL, '');

-- 按钮权限：分享（6019-6021）
INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark) VALUES (6019, '创分享', 6002, 9, '#', '', '', 1, 0, 'F', '0', '0', 'file:share:add', '#', 'admin', NOW(), '', NULL, '');
INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark) VALUES (6020, '停用分享', 6004, 1, '#', '', '', 1, 0, 'F', '0', '0', 'file:share:disable', '#', 'admin', NOW(), '', NULL, '');
INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark) VALUES (6021, '删分享', 6004, 2, '#', '', '', 1, 0, 'F', '0', '0', 'file:share:remove', '#', 'admin', NOW(), '', NULL, '');

-- quartz 定时任务条目：30 天后清磁盘（每天凌晨 3 点跑）
-- 通过反射调 service 的 purge 方法；如果脚手架未启用 quartz，则手动调 POST /file/file/purge-now 即可
INSERT INTO sys_job (job_id, job_name, job_group, invoke_target, cron_expression, misfire_policy, concurrent, status, create_by, create_time, remark)
VALUES (6025, '清理软删 30 天文件', 'DEFAULT', 'fileCleanupJob.purge()', '0 0 3 * * ?', '3', '1', '0', 'admin', NOW(), '文件中心：清磁盘软删超 30 天文件 + ref_count=0');
