-- 表单引擎模块菜单 + 按钮权限初始化（M-10）
-- 菜单 ID 段：5001-5030（5001 父 / 5002-5005 子页面 / 5010-5025 按钮权限）

-- 父菜单：表单引擎
INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
VALUES (5001, '表单引擎', 0, 7, 'form', NULL, '', 1, 0, 'M', '0', '0', '', 'form', 'admin', NOW(), '', NULL, '通用表单（M-10 可插拔模块）');

-- 子菜单 1：模板列表
INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
VALUES (5002, '表单模板', 5001, 1, 'template', 'form/TemplateList', '', 1, 0, 'C', '0', '0', 'form:template:list', 'list', 'admin', NOW(), '', NULL, '表单模板列表');

-- 子菜单 2：模板设计器（隐藏路由）
INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
VALUES (5003, '模板设计', 5001, 2, 'template-design/:id?', 'form/TemplateDesign', '', 1, 0, 'C', '1', '0', 'form:template:edit', 'build', 'admin', NOW(), '', NULL, '隐藏路由 / 从模板列表跳');

-- 子菜单 3：表单填报（隐藏路由）
INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
VALUES (5004, '填报', 5001, 3, 'fill/:id', 'form/FormFill', '', 1, 0, 'C', '1', '0', 'form:submission:add', 'edit', 'admin', NOW(), '', NULL, '隐藏路由 / 从模板列表跳');

-- 子菜单 4：提交记录列表
INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
VALUES (5005, '提交记录', 5001, 4, 'submission', 'form/SubmissionList', '', 1, 0, 'C', '0', '0', 'form:submission:list', 'documentation', 'admin', NOW(), '', NULL, '表单提交记录列表');

-- 按钮权限：模板（5010-5014）
INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark) VALUES (5010, '新增模板', 5002, 1, '#', '', '', 1, 0, 'F', '0', '0', 'form:template:add', '#', 'admin', NOW(), '', NULL, '');
INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark) VALUES (5011, '编辑模板', 5002, 2, '#', '', '', 1, 0, 'F', '0', '0', 'form:template:edit', '#', 'admin', NOW(), '', NULL, '');
INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark) VALUES (5012, '删除模板', 5002, 3, '#', '', '', 1, 0, 'F', '0', '0', 'form:template:remove', '#', 'admin', NOW(), '', NULL, '');
INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark) VALUES (5013, '发布模板', 5002, 4, '#', '', '', 1, 0, 'F', '0', '0', 'form:template:publish', '#', 'admin', NOW(), '', NULL, '');

-- 按钮权限：提交（5020-5022）
INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark) VALUES (5020, '提交表单', 5005, 1, '#', '', '', 1, 0, 'F', '0', '0', 'form:submission:add', '#', 'admin', NOW(), '', NULL, '');
INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark) VALUES (5021, '导出记录', 5005, 2, '#', '', '', 1, 0, 'F', '0', '0', 'form:submission:export', '#', 'admin', NOW(), '', NULL, '');

-- 通用文件上传（与 framework SystemUploadController /system/upload/file 协同；放本模块菜单方便给 cms_author / form_user 等业务角色单挂）
INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
VALUES (5025, '上传文件', 5001, 5, '#', '', '', 1, 0, 'F', '0', '0', 'system:upload:file', '#', 'admin', NOW(), '', NULL, '通用文件上传（/system/upload/file）');
