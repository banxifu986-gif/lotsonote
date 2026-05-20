CREATE TABLE IF NOT EXISTS `question_solution` (
  `solution_id` INT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '题目参考解析 ID',
  `question_id` INT UNSIGNED NOT NULL COMMENT '题目 ID',
  `content` MEDIUMTEXT NOT NULL COMMENT 'Markdown 参考解析内容',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`solution_id`),
  UNIQUE KEY `uk_question_solution_question_id` (`question_id`),
  CONSTRAINT `fk_question_solution_question_id` FOREIGN KEY (`question_id`) REFERENCES `question` (`question_id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='题目参考解析表';
