-- Q-3 可观测性菜单（system 9001）+ Quartz 9001（5min 慢请求告警）

-- 父菜单：可观测性（系统监控 menu_id=2 下的子目录）
-- 注意：path 写成"observability-xxx"（与前端常量路由对齐，不带斜杠子路径）
INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark) VALUES
  (9001, '可观测性',     2,    20, 'observability',           NULL,                                    '', 1, 0, 'M', '0', '0', '',                       'eye-open', 'admin', NOW(), '', NULL, 'Q-3 可观测性父菜单'),
  (9002, '慢请求列表',   9001, 1, 'observability-slow',      'monitor/observability/SlowRequestList', '', 1, 0, 'C', '0', '0', 'monitor:slow:list',     'log',      'admin', NOW(), '', NULL, '慢请求 / 5xx 记录列表'),
  (9003, '业务指标',     9001, 2, 'observability-metrics',   'monitor/observability/BusinessMetrics', '', 1, 0, 'C', '0', '0', 'monitor:metrics:view',  'chart',    'admin', NOW(), '', NULL, '业务模块指标实时采样'),
  (9004, '健康检查',     9001, 3, 'observability-health',    'monitor/observability/HealthDashboard', '', 1, 0, 'C', '0', '0', 'monitor:health:view',   'monitor',  'admin', NOW(), '', NULL, '聚合 /actuator/health 与模块依赖');

INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark) VALUES
  (9010, '查询慢请求',   9002, 1, '#', '', '', 1, 0, 'F', '0', '0', 'monitor:slow:list',    '#', 'admin', NOW(), '', NULL, ''),
  (9011, '清慢请求',     9002, 2, '#', '', '', 1, 0, 'F', '0', '0', 'monitor:slow:purge',   '#', 'admin', NOW(), '', NULL, ''),
  (9012, '查询业务指标', 9003, 1, '#', '', '', 1, 0, 'F', '0', '0', 'monitor:metrics:view', '#', 'admin', NOW(), '', NULL, ''),
  (9013, '查询健康',     9004, 1, '#', '', '', 1, 0, 'F', '0', '0', 'monitor:health:view',  '#', 'admin', NOW(), '', NULL, '');

-- 默认把上述权限分给"超级管理员"（role_id=1）
INSERT IGNORE INTO sys_role_menu (role_id, menu_id) VALUES
  (1, 9001), (1, 9002), (1, 9003), (1, 9004),
  (1, 9010), (1, 9011), (1, 9012), (1, 9013);

-- Quartz 任务：慢请求告警（5 分钟一次）
INSERT INTO sys_job (job_id, job_name, job_group, invoke_target, cron_expression, misfire_policy, concurrent, status, create_by, create_time, remark)
VALUES (9001, '慢请求告警扫描', 'DEFAULT', 'slowApiAlertJob.scanAndAlert()', '0 */5 * * * ?', '3', '1', '0', 'admin', NOW(), 'Q-3：5min 扫上窗口慢请求 / 5xx，超阈发 inbox');
