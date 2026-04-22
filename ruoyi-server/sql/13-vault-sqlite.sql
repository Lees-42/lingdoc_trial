-- ============================================================
-- LingDoc Vault SQLite 数据库初始化脚本
-- 文档编号: LingDoc-FAST-013
-- 版本: v1.0
-- 数据库: SQLite 3.45+
-- 执行时机: 创建新 Vault 时自动执行
-- 文件位置: {vault_root}/.lingdoc/vault.db
-- ============================================================

-- 启用 WAL 模式提升并发性能
PRAGMA journal_mode = WAL;
PRAGMA foreign_keys = ON;
PRAGMA encoding = 'UTF-8';

-- ----------------------------
-- 1、Vault 文件主索引表
-- ----------------------------
CREATE TABLE IF NOT EXISTS `lingdoc_file_index` (
  `file_id`         TEXT    NOT NULL PRIMARY KEY,
  `user_id`         INTEGER NOT NULL,
  `file_name`       TEXT    NOT NULL,
  `vault_path`      TEXT    NOT NULL,
  `abs_path`        TEXT    NOT NULL,
  `file_type`       TEXT    NOT NULL,
  `file_size`       INTEGER NOT NULL,
  `checksum`        TEXT    NOT NULL,
  `sub_path`        TEXT    DEFAULT NULL,
  `source_type`     TEXT    DEFAULT '0',
  `file_content`    TEXT    DEFAULT NULL,
  `content_path`    TEXT    DEFAULT NULL,
  `content_size`    INTEGER DEFAULT 0,
  `is_desensitized` TEXT    DEFAULT '0',
  `create_time`     TEXT    DEFAULT (datetime('now','localtime')),
  `update_time`     TEXT    DEFAULT (datetime('now','localtime'))
);

CREATE INDEX IF NOT EXISTS `idx_file_index_user_id` ON `lingdoc_file_index` (`user_id`);
CREATE INDEX IF NOT EXISTS `idx_file_index_file_type` ON `lingdoc_file_index` (`file_type`);
CREATE INDEX IF NOT EXISTS `idx_file_index_source_type` ON `lingdoc_file_index` (`source_type`);
CREATE INDEX IF NOT EXISTS `idx_file_index_checksum` ON `lingdoc_file_index` (`checksum`);
CREATE INDEX IF NOT EXISTS `idx_file_index_user_checksum` ON `lingdoc_file_index` (`user_id`, `checksum`);
CREATE INDEX IF NOT EXISTS `idx_file_index_user_type_time` ON `lingdoc_file_index` (`user_id`, `file_type`, `create_time`);
CREATE INDEX IF NOT EXISTS `idx_file_index_name` ON `lingdoc_file_index` (`file_name`);

-- 触发器：自动更新 update_time
CREATE TRIGGER IF NOT EXISTS `trg_file_index_update_time`
AFTER UPDATE ON `lingdoc_file_index`
FOR EACH ROW
BEGIN
  UPDATE `lingdoc_file_index` SET `update_time` = datetime('now','localtime') WHERE `file_id` = NEW.file_id;
END;

-- ----------------------------
-- 2、文件版本记录表
-- ----------------------------
CREATE TABLE IF NOT EXISTS `lingdoc_file_version` (
  `version_id`     TEXT    NOT NULL PRIMARY KEY,
  `file_id`        TEXT    NOT NULL,
  `version_no`     INTEGER NOT NULL,
  `snapshot_path`  TEXT    NOT NULL,
  `snapshot_size`  INTEGER DEFAULT NULL,
  `operation_type` TEXT    DEFAULT '0',
  `checksum`       TEXT    DEFAULT NULL,
  `operator_id`    INTEGER DEFAULT NULL,
  `create_time`    TEXT    DEFAULT (datetime('now','localtime'))
);

CREATE INDEX IF NOT EXISTS `idx_file_version_file_id` ON `lingdoc_file_version` (`file_id`);
CREATE INDEX IF NOT EXISTS `idx_file_version_version_no` ON `lingdoc_file_version` (`version_no`);
CREATE INDEX IF NOT EXISTS `idx_file_version_create_time` ON `lingdoc_file_version` (`create_time`);

-- ----------------------------
-- 3、文件 AI 元数据表
-- ----------------------------
CREATE TABLE IF NOT EXISTS `lingdoc_file_ai_meta` (
  `file_id`          TEXT    NOT NULL PRIMARY KEY,
  `kb_id`            TEXT    DEFAULT NULL,
  `parse_status`     TEXT    DEFAULT '0',
  `parse_error`      TEXT    DEFAULT NULL,
  `chunk_count`      INTEGER DEFAULT 0,
  `embedding_status` TEXT    DEFAULT '0',
  `summary`          TEXT    DEFAULT NULL,
  `keywords`         TEXT    DEFAULT NULL,
  `create_time`      TEXT    DEFAULT (datetime('now','localtime')),
  `update_time`      TEXT    DEFAULT (datetime('now','localtime'))
);

CREATE INDEX IF NOT EXISTS `idx_file_ai_meta_kb_id` ON `lingdoc_file_ai_meta` (`kb_id`);
CREATE INDEX IF NOT EXISTS `idx_file_ai_meta_parse_status` ON `lingdoc_file_ai_meta` (`parse_status`);
CREATE INDEX IF NOT EXISTS `idx_file_ai_meta_embedding_status` ON `lingdoc_file_ai_meta` (`embedding_status`);

-- 触发器：自动更新 update_time
CREATE TRIGGER IF NOT EXISTS `trg_file_ai_meta_update_time`
AFTER UPDATE ON `lingdoc_file_ai_meta`
FOR EACH ROW
BEGIN
  UPDATE `lingdoc_file_ai_meta` SET `update_time` = datetime('now','localtime') WHERE `file_id` = NEW.file_id;
END;

-- ----------------------------
-- 4、标签定义表
-- ----------------------------
CREATE TABLE IF NOT EXISTS `lingdoc_tag` (
  `tag_id`      TEXT    NOT NULL PRIMARY KEY,
  `tag_name`    TEXT    NOT NULL UNIQUE,
  `tag_color`   TEXT    DEFAULT '#409EFF',
  `tag_scope`   TEXT    DEFAULT 'A',
  `sort_order`  INTEGER DEFAULT 0,
  `create_time` TEXT    DEFAULT (datetime('now','localtime'))
);

-- ----------------------------
-- 5、标签绑定表
-- ----------------------------
CREATE TABLE IF NOT EXISTS `lingdoc_tag_binding` (
  `binding_id`  TEXT    NOT NULL PRIMARY KEY,
  `target_type` TEXT    NOT NULL,
  `target_id`   TEXT    NOT NULL,
  `tag_id`      TEXT    NOT NULL,
  `bind_type`   TEXT    DEFAULT '0',
  `create_time` TEXT    DEFAULT (datetime('now','localtime')),
  UNIQUE (`target_type`, `target_id`, `tag_id`, `bind_type`)
);

CREATE INDEX IF NOT EXISTS `idx_tag_binding_target` ON `lingdoc_tag_binding` (`target_type`, `target_id`);
CREATE INDEX IF NOT EXISTS `idx_tag_binding_tag_id` ON `lingdoc_tag_binding` (`tag_id`);

-- ----------------------------
-- 6、脱敏文件表
-- ----------------------------
CREATE TABLE IF NOT EXISTS `lingdoc_desensitized_file` (
  `des_id`        TEXT    NOT NULL PRIMARY KEY,
  `file_id`       TEXT    NOT NULL UNIQUE,
  `des_path`      TEXT    NOT NULL,
  `des_content`   TEXT    NOT NULL,
  `des_checksum`  TEXT    NOT NULL,
  `rule_id`       TEXT    DEFAULT NULL,
  `create_time`   TEXT    DEFAULT (datetime('now','localtime'))
);

CREATE INDEX IF NOT EXISTS `idx_desensitized_checksum` ON `lingdoc_desensitized_file` (`des_checksum`);

-- ----------------------------
-- 7、表格填写任务表
-- ----------------------------
CREATE TABLE IF NOT EXISTS `lingdoc_form_task` (
  `task_id`           TEXT    NOT NULL PRIMARY KEY,
  `user_id`           INTEGER NOT NULL,
  `task_name`         TEXT    NOT NULL,
  `original_file_id`  TEXT    NOT NULL,
  `original_file_url` TEXT    NOT NULL,
  `original_file_name`TEXT    NOT NULL,
  `filled_file_id`    TEXT    DEFAULT NULL,
  `filled_file_url`   TEXT    DEFAULT NULL,
  `filled_file_name`  TEXT    DEFAULT NULL,
  `status`            TEXT    DEFAULT '0',
  `ai_result`         TEXT    DEFAULT NULL,
  `field_count`       INTEGER DEFAULT 0,
  `confirmed_count`   INTEGER DEFAULT 0,
  `token_cost`        INTEGER DEFAULT 0,
  `error_msg`         TEXT    DEFAULT NULL,
  `create_by`         TEXT    DEFAULT '',
  `create_time`       TEXT    DEFAULT (datetime('now','localtime')),
  `update_by`         TEXT    DEFAULT '',
  `update_time`       TEXT    DEFAULT (datetime('now','localtime')),
  `remark`            TEXT    DEFAULT NULL
);

CREATE INDEX IF NOT EXISTS `idx_form_task_user_id` ON `lingdoc_form_task` (`user_id`);
CREATE INDEX IF NOT EXISTS `idx_form_task_status` ON `lingdoc_form_task` (`status`);
CREATE INDEX IF NOT EXISTS `idx_form_task_create_time` ON `lingdoc_form_task` (`create_time`);
CREATE INDEX IF NOT EXISTS `idx_form_task_user_status` ON `lingdoc_form_task` (`user_id`, `status`);
CREATE INDEX IF NOT EXISTS `idx_form_task_name` ON `lingdoc_form_task` (`task_name`);

-- 触发器：自动更新 update_time
CREATE TRIGGER IF NOT EXISTS `trg_form_task_update_time`
AFTER UPDATE ON `lingdoc_form_task`
FOR EACH ROW
BEGIN
  UPDATE `lingdoc_form_task` SET `update_time` = datetime('now','localtime') WHERE `task_id` = NEW.task_id;
END;

-- ----------------------------
-- 8、表格字段表
-- ----------------------------
CREATE TABLE IF NOT EXISTS `lingdoc_form_field` (
  `field_id`        TEXT    NOT NULL PRIMARY KEY,
  `task_id`         TEXT    NOT NULL,
  `field_name`      TEXT    NOT NULL,
  `field_type`      TEXT    DEFAULT 'text',
  `field_label`     TEXT    DEFAULT NULL,
  `ai_value`        TEXT    DEFAULT NULL,
  `user_value`      TEXT    DEFAULT NULL,
  `is_confirmed`    TEXT    DEFAULT '0',
  `confidence`      REAL    DEFAULT 0.00,
  `source_doc_id`   TEXT    DEFAULT NULL,
  `source_doc_name` TEXT    DEFAULT NULL,
  `sort_order`      INTEGER DEFAULT 0,
  `create_time`     TEXT    DEFAULT (datetime('now','localtime')),
  `update_time`     TEXT    DEFAULT (datetime('now','localtime'))
);

CREATE INDEX IF NOT EXISTS `idx_form_field_task_id` ON `lingdoc_form_field` (`task_id`);
CREATE INDEX IF NOT EXISTS `idx_form_field_name` ON `lingdoc_form_field` (`field_name`);
CREATE INDEX IF NOT EXISTS `idx_form_field_confirmed` ON `lingdoc_form_field` (`task_id`, `is_confirmed`);

-- 触发器：自动更新 update_time
CREATE TRIGGER IF NOT EXISTS `trg_form_field_update_time`
AFTER UPDATE ON `lingdoc_form_field`
FOR EACH ROW
BEGIN
  UPDATE `lingdoc_form_field` SET `update_time` = datetime('now','localtime') WHERE `field_id` = NEW.field_id;
END;

-- ----------------------------
-- 9、任务参考文档表
-- ----------------------------
CREATE TABLE IF NOT EXISTS `lingdoc_form_reference` (
  `ref_id`        TEXT    NOT NULL PRIMARY KEY,
  `task_id`       TEXT    NOT NULL,
  `doc_id`        TEXT    NOT NULL,
  `doc_name`      TEXT    NOT NULL,
  `doc_path`      TEXT    NOT NULL,
  `doc_type`      TEXT    DEFAULT NULL,
  `relevance`     REAL    DEFAULT 0.00,
  `is_selected`   TEXT    DEFAULT '1',
  `create_time`   TEXT    DEFAULT (datetime('now','localtime'))
);

CREATE INDEX IF NOT EXISTS `idx_form_reference_task_id` ON `lingdoc_form_reference` (`task_id`);
CREATE INDEX IF NOT EXISTS `idx_form_reference_doc_id` ON `lingdoc_form_reference` (`doc_id`);
