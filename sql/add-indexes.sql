-- SQL 性能优化：添加复合索引
-- 执行前请先备份数据库
-- 注意：idx_note_created_at / idx_user_created_at / idx_user_last_login_at 已存在，此处跳过

CREATE INDEX idx_note_author_created            ON note(author_id, created_at);
CREATE INDEX idx_comment_note_parent_created    ON comment(note_id, parent_id, created_at);
CREATE INDEX idx_message_receiver_read_created  ON message(receiver_id, is_read, created_at);
CREATE INDEX idx_message_receiver_type          ON message(receiver_id, type);

-- 验证索引
SHOW INDEX FROM note;
SHOW INDEX FROM comment;
SHOW INDEX FROM message;
