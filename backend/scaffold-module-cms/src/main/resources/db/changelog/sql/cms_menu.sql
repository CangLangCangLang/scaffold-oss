-- CMS 菜单 + 按钮权限初始化（与模块绑定，模块下线后这些行可由运维手动清理或执行 cms_uninstall.sql）
-- 父菜单：CMS 内容管理（顶层）
INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
VALUES (4001, 'CMS', 0, 6, 'cms', NULL, '', 1, 0, 'M', '0', '0', '', 'documentation', 'admin', NOW(), '', NULL, '内容管理（可插拔模块）');

-- 子菜单 1：栏目管理
INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
VALUES (4002, '栏目管理', 4001, 1, 'channel', 'cms/ChannelList', '', 1, 0, 'C', '0', '0', 'cms:channel:list', 'tree', 'admin', NOW(), '', NULL, 'CMS 栏目树');

-- 子菜单 2：文章管理
INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
VALUES (4003, '文章管理', 4001, 2, 'article', 'cms/ArticleList', '', 1, 0, 'C', '0', '0', 'cms:article:list', 'edit', 'admin', NOW(), '', NULL, 'CMS 文章列表 + 状态机');

-- 子菜单 3：标签管理
INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
VALUES (4004, '标签管理', 4001, 3, 'tag', 'cms/TagList', '', 1, 0, 'C', '0', '0', 'cms:tag:list', 'tag', 'admin', NOW(), '', NULL, 'CMS 标签字典');

-- 子菜单 4：文章编辑（隐藏路由，从文章列表跳过来）
INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
VALUES (4005, '文章编辑', 4001, 4, 'article-edit/:id?', 'cms/ArticleEdit', '', 1, 0, 'C', '1', '0', 'cms:article:edit', 'edit', 'admin', NOW(), '', NULL, 'CMS 文章编辑（隐藏路由）');

-- 按钮权限：栏目（4010-4013）
INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark) VALUES (4010, '新增', 4002, 1, '#', '', '', 1, 0, 'F', '0', '0', 'cms:channel:add', '#', 'admin', NOW(), '', NULL, '');
INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark) VALUES (4011, '编辑', 4002, 2, '#', '', '', 1, 0, 'F', '0', '0', 'cms:channel:edit', '#', 'admin', NOW(), '', NULL, '');
INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark) VALUES (4012, '删除', 4002, 3, '#', '', '', 1, 0, 'F', '0', '0', 'cms:channel:remove', '#', 'admin', NOW(), '', NULL, '');

-- 按钮权限：文章（4015-4023）
INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark) VALUES (4015, '新增', 4003, 1, '#', '', '', 1, 0, 'F', '0', '0', 'cms:article:add', '#', 'admin', NOW(), '', NULL, '');
INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark) VALUES (4016, '编辑', 4003, 2, '#', '', '', 1, 0, 'F', '0', '0', 'cms:article:edit', '#', 'admin', NOW(), '', NULL, '');
INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark) VALUES (4017, '删除', 4003, 3, '#', '', '', 1, 0, 'F', '0', '0', 'cms:article:remove', '#', 'admin', NOW(), '', NULL, '');
INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark) VALUES (4018, '提交审核', 4003, 4, '#', '', '', 1, 0, 'F', '0', '0', 'cms:article:submit', '#', 'admin', NOW(), '', NULL, '');
INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark) VALUES (4019, '审核通过', 4003, 5, '#', '', '', 1, 0, 'F', '0', '0', 'cms:article:approve', '#', 'admin', NOW(), '', NULL, '');
INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark) VALUES (4020, '上线', 4003, 6, '#', '', '', 1, 0, 'F', '0', '0', 'cms:article:publish', '#', 'admin', NOW(), '', NULL, '');
INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark) VALUES (4021, '下线', 4003, 7, '#', '', '', 1, 0, 'F', '0', '0', 'cms:article:unpublish', '#', 'admin', NOW(), '', NULL, '');

-- 按钮权限：标签（4025-4027）
INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark) VALUES (4025, '新增', 4004, 1, '#', '', '', 1, 0, 'F', '0', '0', 'cms:tag:add', '#', 'admin', NOW(), '', NULL, '');
INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark) VALUES (4026, '编辑', 4004, 2, '#', '', '', 1, 0, 'F', '0', '0', 'cms:tag:edit', '#', 'admin', NOW(), '', NULL, '');
INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark) VALUES (4027, '删除', 4004, 3, '#', '', '', 1, 0, 'F', '0', '0', 'cms:tag:remove', '#', 'admin', NOW(), '', NULL, '');

-- 上传图片权限（不挂菜单按钮，但要在 sys_menu 占位以便授权）
INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark) VALUES (4030, '上传图片', 4003, 99, '#', '', '', 1, 0, 'F', '1', '0', 'cms:upload:image', '#', 'admin', NOW(), '', NULL, '富文本编辑器内嵌图片上传');

-- 把 CMS 全部菜单/按钮挂到 admin 角色（role_id=1）；超管 user_id=1 走 SUPER_ADMIN 分支不需要这些行，
-- 但任何被赋予 admin 角色的普通用户（如 verify-cms-workflow.ps1 创建的 cms_author）需要靠 sys_role_menu 才能拿到 cms 权限。
INSERT IGNORE INTO sys_role_menu (role_id, menu_id) VALUES
  (1, 4001), (1, 4002), (1, 4003), (1, 4004), (1, 4005),
  (1, 4010), (1, 4011), (1, 4012),
  (1, 4015), (1, 4016), (1, 4017), (1, 4018), (1, 4019), (1, 4020), (1, 4021),
  (1, 4025), (1, 4026), (1, 4027),
  (1, 4030);
