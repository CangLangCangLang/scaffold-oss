-- 离线消息盒：业务调用 MessagePublisher.toUser/toTopic 时先写一条记录，
-- 客户端上线后拉取 status=0 的消息，前端 ack 后把状态置为 1。
CREATE TABLE IF NOT EXISTS sys_message_inbox
(
    id          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    message_id  VARCHAR(64)  NOT NULL COMMENT '业务消息 ID（来自 PushMessage.id），用于幂等去重',
    scope       VARCHAR(16)  NOT NULL COMMENT 'USER 或 TOPIC',
    target      VARCHAR(128) NOT NULL COMMENT 'scope=USER 时为用户名，scope=TOPIC 时为主题名',
    type        VARCHAR(64)  NOT NULL COMMENT '业务类型，例如 order.shipped',
    payload     LONGTEXT     DEFAULT NULL COMMENT 'JSON 序列化的 payload',
    status      TINYINT(1)   NOT NULL DEFAULT 0 COMMENT '0=未读 1=已读 2=已过期',
    created_at  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    read_at     DATETIME     DEFAULT NULL,
    expire_at   DATETIME     DEFAULT NULL COMMENT '过期时间，到期后由清理任务置为 status=2 或物理删除',
    PRIMARY KEY (id),
    UNIQUE KEY uk_scope_target_msg (scope, target, message_id),
    KEY idx_target_status (scope, target, status),
    KEY idx_expire_at (expire_at)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT '离线消息盒（与 WebSocket 推送总线协同）';
