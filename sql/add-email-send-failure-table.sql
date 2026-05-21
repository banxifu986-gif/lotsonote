CREATE TABLE IF NOT EXISTS `email_send_failure` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `task_id` varchar(64) NOT NULL COMMENT '邮件任务ID',
  `email` varchar(255) NOT NULL COMMENT '目标邮箱',
  `type` varchar(32) NOT NULL COMMENT '验证码类型',
  `retry_count` int NOT NULL DEFAULT '0' COMMENT '重试次数',
  `reason` varchar(512) DEFAULT NULL COMMENT '失败原因',
  `created_at` datetime NOT NULL COMMENT '任务创建时间',
  `failed_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '失败入库时间',
  `trace_id` varchar(64) DEFAULT NULL COMMENT '链路追踪ID',
  `expired_flag` tinyint(1) NOT NULL DEFAULT '0' COMMENT '是否过期',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_email_send_failure_task_id` (`task_id`),
  KEY `idx_email_send_failure_email` (`email`),
  KEY `idx_email_send_failure_failed_at` (`failed_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='邮箱发送失败审计表';
