-- form_template：表单模板（同 formKey 多版本共存）
CREATE TABLE IF NOT EXISTS form_template (
  id            BIGINT(20)   NOT NULL AUTO_INCREMENT COMMENT '主键 ID',
  form_key      VARCHAR(64)  NOT NULL COMMENT '模板 key（业务侧识别用）',
  name          VARCHAR(200) NOT NULL COMMENT '模板名称',
  category      VARCHAR(64)  DEFAULT NULL COMMENT '分类（HR / 财务 / IT 等）',
  schema_json   MEDIUMTEXT   NOT NULL COMMENT 'form-create rule[] JSON 字符串',
  version       INT(11)      NOT NULL DEFAULT 1 COMMENT '版本号',
  status        VARCHAR(16)  NOT NULL DEFAULT 'DRAFT' COMMENT 'DRAFT / PUBLISHED / ARCHIVED',
  description   VARCHAR(500) DEFAULT NULL COMMENT '描述 / 备注',
  published_at  DATETIME     DEFAULT NULL COMMENT '首次发布时间',
  del_flag      CHAR(1)      NOT NULL DEFAULT '0' COMMENT '0=正常 / 2=软删',
  create_by     VARCHAR(64)  DEFAULT '' COMMENT '创建者',
  create_time   DATETIME     DEFAULT NULL COMMENT '创建时间',
  update_by     VARCHAR(64)  DEFAULT '' COMMENT '更新者',
  update_time   DATETIME     DEFAULT NULL COMMENT '更新时间',
  remark        VARCHAR(500) DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (id),
  KEY idx_form_key_version (form_key, version),
  KEY idx_status (status),
  KEY idx_category (category)
) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8mb4 COMMENT='表单模板';

-- form_submission：表单提交记录（混合存储）
CREATE TABLE IF NOT EXISTS form_submission (
  id                BIGINT(20)   NOT NULL AUTO_INCREMENT COMMENT '主键 ID',
  template_id       BIGINT(20)   NOT NULL COMMENT '所属模板 ID',
  template_key      VARCHAR(64)  NOT NULL COMMENT '模板 key（冗余）',
  template_version  INT(11)      NOT NULL COMMENT '模板版本号（提交瞬间快照）',
  submitter         VARCHAR(64)  NOT NULL COMMENT '提交人 username',
  submitter_name    VARCHAR(64)  DEFAULT NULL COMMENT '提交人显示名（昵称）',
  status            VARCHAR(16)  NOT NULL DEFAULT 'SUBMITTED' COMMENT '状态',
  data              MEDIUMTEXT   NOT NULL COMMENT '业务字段 JSON',
  create_time       DATETIME     DEFAULT NULL COMMENT '提交时间',
  update_time       DATETIME     DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (id),
  KEY idx_template (template_id, create_time),
  KEY idx_submitter_time (submitter, create_time),
  KEY idx_template_key (template_key)
) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8mb4 COMMENT='表单提交记录';
