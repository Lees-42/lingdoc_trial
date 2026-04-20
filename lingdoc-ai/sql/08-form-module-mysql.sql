-- ============================================================
-- LingDoc 表格填写助手模块数据库脚本
-- 版本: v1.0
-- 数据库: MySQL 8.0+
-- 字符集: utf8mb4_unicode_ci
-- 执行顺序: 在 ry_20260321.sql 和 07-ai-module-mysql.sql 之后执行
-- ============================================================

-- ----------------------------
-- 1、表格填写任务表
-- ----------------------------
DROP TABLE IF EXISTS `lingdoc_form_task`;
CREATE TABLE `lingdoc_form_task` (
  `task_id`           varchar(64)   NOT NULL COMMENT '任务ID',
  `user_id`           bigint(20)    NOT NULL COMMENT '用户ID',
  `task_name`         varchar(256)  NOT NULL COMMENT '任务名称',
  `original_file_id`  varchar(64)   NOT NULL COMMENT '原始空白文档文件ID',
  `original_file_url` varchar(512)  NOT NULL COMMENT '原始文件存储路径',
  `original_file_name`varchar(256)  NOT NULL COMMENT '原始文件名',
  `filled_file_id`    varchar(64)   DEFAULT NULL COMMENT 'AI填写后文档文件ID',
  `filled_file_url`   varchar(512)  DEFAULT NULL COMMENT '填写后文件存储路径',
  `filled_file_name`  varchar(256)  DEFAULT NULL COMMENT '填写后文件名',
  `status`            char(1)       DEFAULT '0' COMMENT '状态：0待上传 1识别中 2待确认 3已生成 4失败',
  `ai_result`         longtext      DEFAULT NULL COMMENT 'AI原始识别结果JSON',
  `field_count`       int           DEFAULT 0 COMMENT '字段总数',
  `confirmed_count`   int           DEFAULT 0 COMMENT '用户已确认字段数',
  `token_cost`        int           DEFAULT 0 COMMENT 'Token消耗总量',
  `error_msg`         varchar(512)  DEFAULT NULL COMMENT '错误信息',
  `create_by`         varchar(64)   DEFAULT '' COMMENT '创建者',
  `create_time`       datetime      DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by`         varchar(64)   DEFAULT '' COMMENT '更新者',
  `update_time`       datetime      DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `remark`            varchar(500)  DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (`task_id`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_status` (`status`),
  KEY `idx_create_time` (`create_time`),
  KEY `idx_user_status` (`user_id`, `status`),
  FULLTEXT KEY `ft_task_name` (`task_name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='表格填写任务表';

-- ----------------------------
-- 2、表格字段表
-- ----------------------------
DROP TABLE IF EXISTS `lingdoc_form_field`;
CREATE TABLE `lingdoc_form_field` (
  `field_id`        varchar(64)   NOT NULL COMMENT '字段ID',
  `task_id`         varchar(64)   NOT NULL COMMENT '所属任务ID',
  `field_name`      varchar(128)  NOT NULL COMMENT '字段名称',
  `field_type`      varchar(32)   DEFAULT 'text' COMMENT '字段类型：text/date/number/select/checkbox',
  `field_label`     varchar(256)  DEFAULT NULL COMMENT '字段在文档中的原始标签文本',
  `ai_value`        varchar(512)  DEFAULT NULL COMMENT 'AI建议的填写值',
  `user_value`      varchar(512)  DEFAULT NULL COMMENT '用户最终确认的值',
  `is_confirmed`    char(1)       DEFAULT '0' COMMENT '是否已确认：0否 1是',
  `confidence`      decimal(3,2)  DEFAULT 0.00 COMMENT 'AI置信度（0.00~1.00）',
  `source_doc_id`   varchar(64)   DEFAULT NULL COMMENT '值来源的Vault文档ID',
  `source_doc_name` varchar(256)  DEFAULT NULL COMMENT '来源文档名称',
  `sort_order`      int           DEFAULT 0 COMMENT '字段排序号',
  `create_time`     datetime      DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time`     datetime      DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`field_id`),
  KEY `idx_task_id` (`task_id`),
  KEY `idx_field_name` (`field_name`),
  KEY `idx_confirmed` (`task_id`, `is_confirmed`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='表格字段表';

-- ----------------------------
-- 3、任务参考文档关联表
-- ----------------------------
DROP TABLE IF EXISTS `lingdoc_form_reference`;
CREATE TABLE `lingdoc_form_reference` (
  `ref_id`        varchar(64)   NOT NULL COMMENT '关联ID',
  `task_id`       varchar(64)   NOT NULL COMMENT '任务ID',
  `doc_id`        varchar(64)   NOT NULL COMMENT 'Vault文档ID',
  `doc_name`      varchar(256)  NOT NULL COMMENT '文档名称',
  `doc_path`      varchar(512)  NOT NULL COMMENT '文档存储路径',
  `doc_type`      varchar(32)   DEFAULT NULL COMMENT '文档类型',
  `relevance`     decimal(3,2)  DEFAULT 0.00 COMMENT '相关性评分',
  `is_selected`   char(1)       DEFAULT '1' COMMENT '是否被选中：0否 1是',
  `create_time`   datetime      DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`ref_id`),
  KEY `idx_task_id` (`task_id`),
  KEY `idx_doc_id` (`doc_id`),
  UNIQUE KEY `uk_task_doc` (`task_id`, `doc_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='任务参考文档关联表';
