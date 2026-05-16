-- sys_report_datasource：外部 JDBC 数据源（主库不入此表，主库逻辑在 service 中以保留 id=0 表示）
CREATE TABLE IF NOT EXISTS sys_report_datasource (
  id              BIGINT(20)    NOT NULL AUTO_INCREMENT COMMENT '主键 ID',
  code            VARCHAR(64)   NOT NULL COMMENT '数据源编码（report 模板引用用）',
  name            VARCHAR(128)  NOT NULL COMMENT '展示名',
  type            VARCHAR(32)   NOT NULL DEFAULT 'mysql' COMMENT 'mysql / postgres / sqlserver / oracle',
  jdbc_url        VARCHAR(512)  NOT NULL COMMENT 'JDBC URL',
  driver_class    VARCHAR(255)  DEFAULT NULL COMMENT '驱动全限定名（留空用默认）',
  username        VARCHAR(128)  DEFAULT NULL COMMENT '账户',
  password_enc    VARCHAR(512)  DEFAULT NULL COMMENT '密码（Aes256Util ENC(...) 密文）',
  status          CHAR(1)       NOT NULL DEFAULT '0' COMMENT '0=启用 / 1=停用',
  remark          VARCHAR(500)  DEFAULT NULL COMMENT '备注',
  create_by       VARCHAR(64)   DEFAULT '' COMMENT '创建者',
  create_time     DATETIME      DEFAULT NULL COMMENT '创建时间',
  update_by       VARCHAR(64)   DEFAULT '' COMMENT '更新者',
  update_time     DATETIME      DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (id),
  UNIQUE KEY uk_code (code),
  KEY idx_status (status)
) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8mb4 COMMENT='报表 - 外部 JDBC 数据源';

-- sys_report_template：SQL 模板（一行 = 一份用户保存的查询）
CREATE TABLE IF NOT EXISTS sys_report_template (
  id              BIGINT(20)    NOT NULL AUTO_INCREMENT COMMENT '主键 ID',
  code            VARCHAR(64)   NOT NULL COMMENT '业务编码（唯一）',
  name            VARCHAR(128)  NOT NULL COMMENT '模板名',
  category        VARCHAR(64)   DEFAULT NULL COMMENT '分类（用户自定义）',
  datasource_id   BIGINT(20)    NOT NULL DEFAULT 0 COMMENT '数据源 ID（0 = 主库）',
  sql_text        TEXT          NOT NULL COMMENT 'SELECT 模板（仅允许 SELECT；参数用 ${name} 占位）',
  param_schema    TEXT          DEFAULT NULL COMMENT '参数声明 JSON（数组：[{name,type,label,required,default}]）',
  row_limit       INT(11)       NOT NULL DEFAULT 10000 COMMENT '行数上限（不能超过全局上限 10000）',
  timeout_ms      INT(11)       NOT NULL DEFAULT 30000 COMMENT '查询超时（不能超过全局上限 30s）',
  perm_key        VARCHAR(64)   DEFAULT NULL COMMENT '运行此模板需要的权限 key（可空 = 仅登录）',
  status          CHAR(1)       NOT NULL DEFAULT '0' COMMENT '0=启用 / 1=停用',
  remark          VARCHAR(500)  DEFAULT NULL COMMENT '备注',
  create_by       VARCHAR(64)   DEFAULT '' COMMENT '创建者',
  create_time     DATETIME      DEFAULT NULL COMMENT '创建时间',
  update_by       VARCHAR(64)   DEFAULT '' COMMENT '更新者',
  update_time     DATETIME      DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (id),
  UNIQUE KEY uk_code (code),
  KEY idx_category (category),
  KEY idx_status (status)
) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8mb4 COMMENT='报表 - SQL 模板';

-- sys_report_run_log：执行历史（含慢查询；超过 timeout_ms 的也写一条 status=2）
CREATE TABLE IF NOT EXISTS sys_report_run_log (
  id              BIGINT(20)    NOT NULL AUTO_INCREMENT COMMENT '主键 ID',
  template_id     BIGINT(20)    DEFAULT NULL COMMENT '模板 ID（即席查询为 NULL）',
  template_code   VARCHAR(64)   DEFAULT NULL COMMENT '模板编码（冗余）',
  datasource_id   BIGINT(20)    NOT NULL DEFAULT 0 COMMENT '数据源 ID',
  sql_preview     VARCHAR(2000) DEFAULT NULL COMMENT 'SQL 预览（已替换参数；超长截断）',
  param_json      TEXT          DEFAULT NULL COMMENT '参数 JSON',
  row_count       INT(11)       DEFAULT NULL COMMENT '返回行数（截断后）',
  cost_ms         BIGINT(20)    DEFAULT NULL COMMENT '耗时 ms',
  status          CHAR(1)       NOT NULL DEFAULT '0' COMMENT '0=成功 / 1=失败 / 2=超时',
  error_msg       VARCHAR(2000) DEFAULT NULL COMMENT '失败错误（status<>0）',
  create_by       VARCHAR(64)   DEFAULT '' COMMENT '执行者 username',
  create_time     DATETIME      DEFAULT NULL COMMENT '执行时间',
  PRIMARY KEY (id),
  KEY idx_template_time (template_id, create_time),
  KEY idx_creator_time (create_by, create_time),
  KEY idx_status_cost (status, cost_ms)
) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8mb4 COMMENT='报表 - 运行日志';

-- sys_report_dashboard：看板（一行 = 一个用户保存的卡片组合页）
CREATE TABLE IF NOT EXISTS sys_report_dashboard (
  id              BIGINT(20)    NOT NULL AUTO_INCREMENT COMMENT '主键 ID',
  code            VARCHAR(64)   NOT NULL COMMENT '业务编码（唯一）',
  name            VARCHAR(128)  NOT NULL COMMENT '看板名',
  category        VARCHAR(64)   DEFAULT NULL COMMENT '分类',
  layout_json     TEXT          DEFAULT NULL COMMENT '布局 JSON（卡片网格 + 各卡片配置）',
  perm_key        VARCHAR(64)   DEFAULT NULL COMMENT '查看此看板需要的权限 key',
  status          CHAR(1)       NOT NULL DEFAULT '0' COMMENT '0=启用 / 1=停用',
  remark          VARCHAR(500)  DEFAULT NULL COMMENT '备注',
  create_by       VARCHAR(64)   DEFAULT '' COMMENT '创建者',
  create_time     DATETIME      DEFAULT NULL COMMENT '创建时间',
  update_by       VARCHAR(64)   DEFAULT '' COMMENT '更新者',
  update_time     DATETIME      DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (id),
  UNIQUE KEY uk_code (code),
  KEY idx_category (category),
  KEY idx_status (status)
) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8mb4 COMMENT='报表 - 看板';

-- sys_report_dashboard_card：看板卡片（也保留为表，便于运维直查与权限粒度未来下沉）
CREATE TABLE IF NOT EXISTS sys_report_dashboard_card (
  id              BIGINT(20)    NOT NULL AUTO_INCREMENT COMMENT '主键 ID',
  dashboard_id    BIGINT(20)    NOT NULL COMMENT '看板 ID',
  template_id     BIGINT(20)    NOT NULL COMMENT '关联模板 ID',
  title           VARCHAR(128)  NOT NULL COMMENT '卡片标题',
  chart_type      VARCHAR(32)   NOT NULL DEFAULT 'table' COMMENT 'table / line / bar / pie / number',
  config_json     TEXT          DEFAULT NULL COMMENT 'ECharts / 表格 列映射等配置',
  param_json      TEXT          DEFAULT NULL COMMENT '默认参数（看板进入时可传）',
  pos_x           INT(11)       NOT NULL DEFAULT 0 COMMENT '栅格 x',
  pos_y           INT(11)       NOT NULL DEFAULT 0 COMMENT '栅格 y',
  pos_w           INT(11)       NOT NULL DEFAULT 6 COMMENT '宽度（24 列栅格）',
  pos_h           INT(11)       NOT NULL DEFAULT 6 COMMENT '高度',
  order_num       INT(11)       NOT NULL DEFAULT 0 COMMENT '排序',
  create_time     DATETIME      DEFAULT NULL COMMENT '创建时间',
  update_time     DATETIME      DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (id),
  KEY idx_dashboard (dashboard_id, order_num),
  KEY idx_template (template_id)
) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8mb4 COMMENT='报表 - 看板卡片';
