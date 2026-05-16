-- 工作流表单引擎：schema 模板存档表
-- changeset workflow-20260506-form-schema
CREATE TABLE wf_form_schema (
    id              BIGINT          NOT NULL AUTO_INCREMENT          COMMENT '主键',
    process_definition_key VARCHAR(64) NOT NULL                       COMMENT '流程定义 key（按 key 关联，跨版本生效）',
    activity_id     VARCHAR(64)     NOT NULL DEFAULT '__START__'      COMMENT 'BPMN activity id；__START__ 表示启动表单',
    name            VARCHAR(120)    NOT NULL DEFAULT ''               COMMENT '表单名称',
    version         INT             NOT NULL DEFAULT 1                COMMENT '版本号，按 key + activity_id 累加',
    schema_json     LONGTEXT        NOT NULL                          COMMENT 'form-create schema JSON（顶层 array 即组件树）',
    enabled         TINYINT(1)      NOT NULL DEFAULT 1                COMMENT '是否启用：1=启用，0=作废',
    create_by       VARCHAR(64)     DEFAULT ''                        COMMENT '创建人',
    create_time     DATETIME        DEFAULT CURRENT_TIMESTAMP         COMMENT '创建时间',
    update_by       VARCHAR(64)     DEFAULT ''                        COMMENT '更新人',
    update_time     DATETIME        DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    KEY idx_wf_form_schema_def (process_definition_key, activity_id, enabled, version)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '工作流动态表单 schema 存档';
