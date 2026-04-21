-- ============================================================
-- LingDoc Vault 文件管理模块数据库脚本
-- 文档编号: LingDoc-FAST-009
-- 版本: v1.0
-- 数据库: MySQL 8.0+
-- 字符集: utf8mb4_unicode_ci
-- 执行顺序: 在 ry_20260321.sql、07-ai-module-mysql.sql、08-form-module-mysql.sql 之后执行
-- ============================================================

SET NAMES utf8mb4;
SET character_set_client = utf8mb4;
SET character_set_connection = utf8mb4;
SET character_set_results = utf8mb4;

-- ----------------------------
-- 1、Vault 文件主索引表
-- ----------------------------
DROP TABLE IF EXISTS `lingdoc_file_index`;
CREATE TABLE `lingdoc_file_index` (
  `file_id`         varchar(64)   NOT NULL COMMENT '文件唯一ID（UUID）',
  `user_id`         bigint(20)    NOT NULL COMMENT '所属用户ID（sys_user.user_id）',
  `file_name`       varchar(256)  NOT NULL COMMENT '文件名（含扩展名）',
  `vault_path`      varchar(512)  NOT NULL COMMENT 'Vault内相对路径（如 documents/学习资料/大三上/）',
  `abs_path`        varchar(512)  NOT NULL COMMENT '绝对路径',
  `file_type`       varchar(32)   NOT NULL COMMENT '文件类型（pdf/docx/xlsx/doc/xls/txt/md/csv/json/xml/yaml/png/jpg/jpeg等）',
  `file_size`       bigint(20)    NOT NULL COMMENT '文件大小（字节）',
  `checksum`        varchar(64)   NOT NULL COMMENT 'SHA256（去重用）',
  `sub_path`        varchar(512)  DEFAULT NULL COMMENT '子分类路径（相对于vault_root/documents/，如 学习资料/大三上/操作系统/）',
  `source_type`     char(1)       DEFAULT '0' COMMENT '来源：0手动上传 1自动规整 2表格助手生成',
  `file_content`    longtext      DEFAULT NULL COMMENT '文件文本内容（全文检索用）',
  `content_path`    varchar(512)  DEFAULT NULL COMMENT '大文件文本内容的外部存储路径（分层存储预留）',
  `content_size`    bigint        DEFAULT 0 COMMENT '文本内容字节数',
  `is_desensitized` char(1)       DEFAULT '0' COMMENT '是否有脱敏副本：0否 1是',
  `create_time`     datetime      DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time`     datetime      DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`file_id`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_file_type` (`file_type`),
  KEY `idx_source_type` (`source_type`),
  KEY `idx_checksum` (`checksum`),
  KEY `idx_user_checksum` (`user_id`, `checksum`),
  KEY `idx_user_type_time` (`user_id`, `file_type`, `create_time`),
  FULLTEXT KEY `ft_name_content` (`file_name`, `file_content`) WITH PARSER ngram
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Vault文件索引表';

-- ----------------------------
-- 2、文件版本记录表
-- ----------------------------
DROP TABLE IF EXISTS `lingdoc_file_version`;
CREATE TABLE `lingdoc_file_version` (
  `version_id`    varchar(64)   NOT NULL COMMENT '版本ID',
  `file_id`       varchar(64)   NOT NULL COMMENT '原文件ID（lingdoc_file_index.file_id）',
  `version_no`    int           NOT NULL COMMENT '版本号',
  `snapshot_path` varchar(512)  NOT NULL COMMENT '版本快照存储路径',
  `snapshot_size` bigint        DEFAULT NULL COMMENT '快照文件大小',
  `operation_type` char(1)      DEFAULT '0' COMMENT '操作类型：0编辑 1重命名 2移动 3回滚',
  `checksum`      varchar(64)   DEFAULT NULL COMMENT '版本文件checksum',
  `operator_id`   bigint        DEFAULT NULL COMMENT '操作人',
  `create_time`   datetime      DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`version_id`),
  KEY `idx_file_id` (`file_id`),
  KEY `idx_version_no` (`version_no`),
  KEY `idx_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='文件版本记录表';

-- ----------------------------
-- 3、文件 AI 元数据表（脚手架）
-- ----------------------------
DROP TABLE IF EXISTS `lingdoc_file_ai_meta`;
CREATE TABLE `lingdoc_file_ai_meta` (
  `file_id`          varchar(64)   NOT NULL COMMENT '文件ID（= lingdoc_file_index.file_id），1:1关联',
  `kb_id`            varchar(64)   DEFAULT NULL COMMENT '所属知识库ID（NULL表示未入知识库）',
  `parse_status`     char(1)       DEFAULT '0' COMMENT '文本解析状态：0待处理 1处理中 2已完成 3失败',
  `parse_error`      varchar(512)  DEFAULT NULL COMMENT '解析错误信息',
  `chunk_count`      int           DEFAULT 0 COMMENT '文本分块数量',
  `embedding_status` char(1)       DEFAULT '0' COMMENT '向量化状态：0未处理 1处理中 2已完成 3失败',
  `summary`          varchar(512)  DEFAULT NULL COMMENT 'AI一句话概括',
  `keywords`         varchar(1024) DEFAULT NULL COMMENT 'AI提取关键词（JSON数组）',
  `create_time`      datetime      DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time`      datetime      DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`file_id`),
  KEY `idx_kb_id` (`kb_id`),
  KEY `idx_parse_status` (`parse_status`),
  KEY `idx_embedding_status` (`embedding_status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='文件AI元数据表（脚手架）';

-- ----------------------------
-- 4、标签定义表
-- ----------------------------
DROP TABLE IF EXISTS `lingdoc_tag`;
CREATE TABLE `lingdoc_tag` (
  `tag_id`      varchar(64)   NOT NULL COMMENT '标签唯一ID（UUID）',
  `tag_name`    varchar(128)  NOT NULL COMMENT '标签名（如"奖学金"、"操作系统"）',
  `tag_color`   varchar(32)   DEFAULT '#409EFF' COMMENT '标签颜色（前端展示用）',
  `tag_scope`   char(1)       DEFAULT 'A' COMMENT '适用范围：A所有 F仅文件 D仅目录',
  `sort_order`  int           DEFAULT 0 COMMENT '排序号',
  `create_time` datetime      DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`tag_id`),
  UNIQUE KEY `uk_tag_name` (`tag_name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='标签定义表';

-- ----------------------------
-- 5、标签绑定表
-- ----------------------------
DROP TABLE IF EXISTS `lingdoc_tag_binding`;
CREATE TABLE `lingdoc_tag_binding` (
  `binding_id`  varchar(64)   NOT NULL COMMENT '绑定记录ID',
  `target_type` char(1)       NOT NULL COMMENT '目标类型：F文件 D目录',
  `target_id`   varchar(512)  NOT NULL COMMENT '目标标识：文件时为file_id，目录时为目录路径',
  `tag_id`      varchar(64)   NOT NULL COMMENT '标签ID',
  `bind_type`   char(1)       DEFAULT '0' COMMENT '绑定类型：0直接绑定 1继承自父目录',
  `create_time` datetime      DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`binding_id`),
  KEY `idx_target` (`target_type`, `target_id`),
  KEY `idx_tag_id` (`tag_id`),
  UNIQUE KEY `uk_target_tag` (`target_type`, `target_id`, `tag_id`, `bind_type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='标签绑定表';

-- ----------------------------
-- 6、脱敏文件表
-- ----------------------------
DROP TABLE IF EXISTS `lingdoc_desensitized_file`;
CREATE TABLE `lingdoc_desensitized_file` (
  `des_id`        varchar(64)   NOT NULL COMMENT '脱敏记录ID',
  `file_id`       varchar(64)   NOT NULL COMMENT '原文件ID（lingdoc_file_index.file_id）',
  `des_path`      varchar(512)  NOT NULL COMMENT '脱敏文件物理路径',
  `des_content`   longtext      NOT NULL COMMENT '脱敏后的纯文本内容（供AI直接读取上传）',
  `des_checksum`  varchar(64)   NOT NULL COMMENT '脱敏内容校验（SHA-256）',
  `rule_id`       varchar(64)   DEFAULT NULL COMMENT '使用的脱敏规则ID（预留）',
  `create_time`   datetime      DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`des_id`),
  UNIQUE KEY `uk_file_id` (`file_id`),
  KEY `idx_checksum` (`des_checksum`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='脱敏文件表';

-- ----------------------------
-- 7、Vault 文件浏览器菜单初始化
-- ----------------------------
INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
SELECT '2006', '文件浏览器', '2000', '6', 'vault', 'lingdoc/vault/index', '0', '0', 'C', '0', '0', 'lingdoc:vault:list,lingdoc:vault:download,lingdoc:vault:edit,lingdoc:vault:delete', 'folder', 'admin', sysdate(), '', null, 'Vault文件浏览器菜单：浏览本地Vault中的所有文件，支持预览、搜索、管理'
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_id = '2006');

-- ----------------------------
-- 8、标签管理菜单初始化
-- ----------------------------
INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
SELECT '2007', '标签管理', '2000', '7', 'tag', 'lingdoc/tag/index', '0', '0', 'C', '0', '0', 'lingdoc:tag:list,lingdoc:tag:edit', 'price-tag', 'admin', sysdate(), '', null, '标签管理菜单：管理Vault文件标签体系'
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_id = '2007');
