-- 报表模块（M-8）下线脚本
-- 该模块对其它模块零依赖，删除以下数据/表后从 admin pom 与 master changelog 移除即可彻底下线

DELETE FROM sys_menu WHERE menu_id BETWEEN 7001 AND 7030;
DELETE FROM sys_job  WHERE job_id IN (7025);

DROP TABLE IF EXISTS sys_report_dashboard_card;
DROP TABLE IF EXISTS sys_report_dashboard;
DROP TABLE IF EXISTS sys_report_run_log;
DROP TABLE IF EXISTS sys_report_template;
DROP TABLE IF EXISTS sys_report_datasource;
