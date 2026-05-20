CREATE TABLE IF NOT EXISTS `ai_chat_session` (
  `session_id` BIGINT NOT NULL AUTO_INCREMENT COMMENT 'AI 会话 ID',
  `user_id` BIGINT NOT NULL COMMENT '所属用户 ID',
  `scene_type` VARCHAR(64) NOT NULL COMMENT '场景类型',
  `biz_type` VARCHAR(64) NOT NULL COMMENT '业务类型',
  `biz_id` VARCHAR(64) DEFAULT NULL COMMENT '业务 ID',
  `title` VARCHAR(255) NOT NULL COMMENT '会话标题',
  `context_snapshot` JSON DEFAULT NULL COMMENT '上下文快照',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`session_id`),
  KEY `idx_ai_chat_session_user_id_updated_at` (`user_id`, `updated_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='AI 会话表';

CREATE TABLE IF NOT EXISTS `ai_chat_message` (
  `message_id` BIGINT NOT NULL AUTO_INCREMENT COMMENT 'AI 消息 ID',
  `session_id` BIGINT NOT NULL COMMENT '会话 ID',
  `role` VARCHAR(32) NOT NULL COMMENT '消息角色',
  `content` TEXT NOT NULL COMMENT '消息内容',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`message_id`),
  KEY `idx_ai_chat_message_session_id_message_id` (`session_id`, `message_id`),
  CONSTRAINT `fk_ai_chat_message_session_id` FOREIGN KEY (`session_id`) REFERENCES `ai_chat_session` (`session_id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='AI 会话消息表';
