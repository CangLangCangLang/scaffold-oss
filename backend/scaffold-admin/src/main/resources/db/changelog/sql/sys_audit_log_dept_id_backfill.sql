-- 一次性回填历史 sys_audit_log.actor_dept_id（按 actor 关联到 sys_user.dept_id）
-- 第二次执行不会再做修改，因为回填后 actor_dept_id IS NULL 的行变少；preCondition 用 sqlCheck 保证幂等。
UPDATE sys_audit_log a
LEFT JOIN sys_user u ON u.user_name = a.actor
SET a.actor_dept_id = u.dept_id
WHERE a.actor_dept_id IS NULL
  AND a.actor IS NOT NULL
  AND u.dept_id IS NOT NULL;
