-- ============================================================
-- LingDoc Vault SQLite 鏁版嵁搴撳垵濮嬪寲鑴氭湰
-- 鏂囨。缂栧彿: LingDoc-FAST-013
-- 鐗堟湰: v1.0
-- 鏁版嵁搴? SQLite 3.45+
-- 鎵ц鏃舵満: 鍒涘缓鏂?Vault 鏃惰嚜鍔ㄦ墽琛?-- 鏂囦欢浣嶇疆: {vault_root}/.lingdoc/vault.db
-- ============================================================

-- 鍚敤 WAL 妯″紡鎻愬崌骞跺彂鎬ц兘
PRAGMA journal_mode = WAL;
PRAGMA foreign_keys = ON;
PRAGMA encoding = 'UTF-8';

-- ----------------------------
-- 1銆乂ault 鏂囦欢涓荤储寮曡〃
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

-- 瑙﹀彂鍣細鑷姩鏇存柊 update_time
CREATE TRIGGER IF NOT EXISTS `trg_file_index_update_time`
AFTER UPDATE ON `lingdoc_file_index`
FOR EACH ROW
BEGIN
  UPDATE `lingdoc_file_index` SET `update_time` = datetime('now','localtime') WHERE `file_id` = NEW.file_id;
END;

-- ----------------------------
-- 2銆佹枃浠剁増鏈褰曡〃
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
-- 3銆佹枃浠?AI 鍏冩暟鎹〃
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

-- 瑙﹀彂鍣細鑷姩鏇存柊 update_time
CREATE TRIGGER IF NOT EXISTS `trg_file_ai_meta_update_time`
AFTER UPDATE ON `lingdoc_file_ai_meta`
FOR EACH ROW
BEGIN
  UPDATE `lingdoc_file_ai_meta` SET `update_time` = datetime('now','localtime') WHERE `file_id` = NEW.file_id;
END;

-- ----------------------------
-- 4銆佹爣绛惧畾涔夎〃
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
-- 5銆佹爣绛剧粦瀹氳〃
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
-- 6銆佽劚鏁忔枃浠惰〃
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
-- 7銆佽〃鏍煎～鍐欎换鍔¤〃
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

-- 瑙﹀彂鍣細鑷姩鏇存柊 update_time
CREATE TRIGGER IF NOT EXISTS `trg_form_task_update_time`
AFTER UPDATE ON `lingdoc_form_task`
FOR EACH ROW
BEGIN
  UPDATE `lingdoc_form_task` SET `update_time` = datetime('now','localtime') WHERE `task_id` = NEW.task_id;
END;

-- ----------------------------
-- 8銆佽〃鏍煎瓧娈佃〃
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

-- 瑙﹀彂鍣細鑷姩鏇存柊 update_time
CREATE TRIGGER IF NOT EXISTS `trg_form_field_update_time`
AFTER UPDATE ON `lingdoc_form_field`
FOR EACH ROW
BEGIN
  UPDATE `lingdoc_form_field` SET `update_time` = datetime('now','localtime') WHERE `field_id` = NEW.field_id;
END;

-- ----------------------------
-- 9銆佷换鍔″弬鑰冩枃妗ｈ〃
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

-- ----------------------------
-- 10銆佹敹浠剁锛圛nbox锛夋枃浠惰〃
-- 瀛樺偍宸蹭笂浼犱絾鏈‘璁ゅ綊妗ｇ殑鏂囦欢锛岀‘璁ゅ悗绉诲叆 lingdoc_file_index
-- ----------------------------
CREATE TABLE IF NOT EXISTS `lingdoc_inbox` (
  `inbox_id`        TEXT    NOT NULL PRIMARY KEY,
  `user_id`         INTEGER NOT NULL,
  `original_name`   TEXT    NOT NULL,
  `file_type`       TEXT    NOT NULL,
  `file_size`       INTEGER NOT NULL,
  `abs_path`        TEXT    NOT NULL,
  `status`          TEXT    DEFAULT 'uploaded',
  `suggested_name`  TEXT    DEFAULT NULL,
  `suggested_path`  TEXT    DEFAULT NULL,
  `tag_ids`         TEXT    DEFAULT NULL,
  `ai_summary`      TEXT    DEFAULT NULL,
  `ai_keywords`     TEXT    DEFAULT NULL,
  `confidence`      REAL    DEFAULT 0.00,
  `token_cost`      INTEGER DEFAULT 0,
  `error_msg`       TEXT    DEFAULT NULL,
  `remark`          TEXT    DEFAULT NULL,
  `create_time`     TEXT    DEFAULT (datetime('now','localtime')),
  `update_time`     TEXT    DEFAULT (datetime('now','localtime'))
);

CREATE INDEX IF NOT EXISTS `idx_inbox_user_id` ON `lingdoc_inbox` (`user_id`);
CREATE INDEX IF NOT EXISTS `idx_inbox_status` ON `lingdoc_inbox` (`status`);
CREATE INDEX IF NOT EXISTS `idx_inbox_user_status` ON `lingdoc_inbox` (`user_id`, `status`);

-- 瑙﹀彂鍣細鑷姩鏇存柊 update_time
CREATE TRIGGER IF NOT EXISTS `trg_inbox_update_time`
AFTER UPDATE ON `lingdoc_inbox`
FOR EACH ROW
BEGIN
  UPDATE `lingdoc_inbox` SET `update_time` = datetime('now','localtime') WHERE `inbox_id` = NEW.inbox_id;
END;

