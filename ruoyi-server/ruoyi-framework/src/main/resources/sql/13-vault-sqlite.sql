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
