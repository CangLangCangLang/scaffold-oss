-- Q-3 可观测性：慢请求 / 错误请求记录表（被 SlowApiAlertJob 消费）
CREATE TABLE IF NOT EXISTS sys_slow_request (
  id            BIGINT(20)   NOT NULL AUTO_INCREMENT COMMENT '主键',
  request_uri   VARCHAR(500) NOT NULL COMMENT '请求 URI（不含 query）',
  method        VARCHAR(16)  NOT NULL COMMENT 'HTTP 方法',
  status        INT(11)      NOT NULL COMMENT '响应状态码',
  cost_ms       BIGINT(20)   NOT NULL COMMENT '耗时（毫秒）',
  trace_id      VARCHAR(64)  DEFAULT NULL COMMENT 'TraceId（X-Trace-Id）',
  username      VARCHAR(64)  DEFAULT NULL COMMENT '触发用户（匿名为空）',
  client_ip     VARCHAR(64)  DEFAULT NULL COMMENT '客户端 IP',
  reason        VARCHAR(32)  NOT NULL COMMENT '原因：SLOW=慢 / SERVER_ERROR=5xx / CLIENT_ERROR=4xx',
  exception_msg VARCHAR(500) DEFAULT NULL COMMENT '异常摘要（来自 GlobalExceptionHandler）',
  alerted       CHAR(1)      NOT NULL DEFAULT '0' COMMENT '0=未告警 / 1=已告警（避免重发）',
  create_time   DATETIME     NOT NULL COMMENT '记录时间',
  PRIMARY KEY (id),
  KEY idx_create_time (create_time),
  KEY idx_alerted (alerted, create_time),
  KEY idx_uri_time (request_uri, create_time)
) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8mb4 COMMENT='慢请求 / 错误请求记录（Q-3 可观测性）';
