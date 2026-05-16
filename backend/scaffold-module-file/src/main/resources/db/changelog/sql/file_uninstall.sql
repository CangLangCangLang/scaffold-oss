-- M-6 文件中心模块卸载脚本（手工执行，逆向）。
-- 注意：执行前先确保业务侧（CMS / Form 等）不再引用 sys_file 的记录，否则下游会出现悬空 URL。
-- 物理文件（profile/file/yyyyMM/uuid.ext）需另外手工清磁盘 — 本脚本不动 disk。

DELETE FROM sys_role_menu WHERE menu_id BETWEEN 6001 AND 6030;
DELETE FROM sys_menu WHERE menu_id BETWEEN 6001 AND 6030;

DROP TABLE IF EXISTS sys_file_ref;
DROP TABLE IF EXISTS sys_file_share;
DROP TABLE IF EXISTS sys_file_folder;
DROP TABLE IF EXISTS sys_file;
