-- 外部身份与本地用户的绑定
CREATE TABLE IF NOT EXISTS sys_user_external_identity
(
    id            BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    user_id       BIGINT       NOT NULL COMMENT '本地 sys_user.user_id',
    provider      VARCHAR(64)  NOT NULL COMMENT 'IDP 注册名（与 spring.security.oauth2.client.registration.<id> 一致）',
    subject       VARCHAR(255) NOT NULL COMMENT 'IDP 返回的 sub / unionId 等稳定 ID',
    email         VARCHAR(255) DEFAULT NULL COMMENT '首次绑定时的邮箱，仅作信息展示',
    raw_profile   TEXT         DEFAULT NULL COMMENT '首次绑定时的 IDP 原始 claims（JSON）',
    bound_at      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    last_login_at DATETIME     DEFAULT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_provider_subject (provider, subject),
    KEY idx_user_id (user_id)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT '外部身份绑定表（OAuth2 / OIDC SSO）';
