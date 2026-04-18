-- AI模块数据库表结构（符合主仓库规范）
-- 字符集: utf8mb4
-- 排序规则: utf8mb4_unicode_ci

-- 知识库表
CREATE TABLE IF NOT EXISTS `kb_knowledge_base` (
    `kb_id` varchar(64) NOT NULL COMMENT '知识库ID',
    `user_uuid` varchar(64) NOT NULL COMMENT '用户UUID',
    `kb_name` varchar(128) NOT NULL COMMENT '知识库名称',
    `kb_description` varchar(512) DEFAULT NULL COMMENT '知识库描述',
    `chunk_size` int DEFAULT 512 COMMENT '分块大小',
    `chunk_overlap` int DEFAULT 50 COMMENT '分块重叠长度',
    `embedding_model` varchar(64) DEFAULT 'text-embedding-3-small' COMMENT '嵌入模型',
    `total_files` int DEFAULT 0 COMMENT '文件总数',
    `total_chunks` int DEFAULT 0 COMMENT '分块总数',
    `status` char(1) DEFAULT '0' COMMENT '状态（0正常 1停用）',
    `create_by` varchar(64) DEFAULT '' COMMENT '创建者',
    `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_by` varchar(64) DEFAULT '' COMMENT '更新者',
    `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `remark` varchar(500) DEFAULT NULL COMMENT '备注',
    PRIMARY KEY (`kb_id`),
    KEY `idx_user_uuid` (`user_uuid`),
    KEY `idx_kb_name` (`kb_name`),
    KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='知识库表';

-- 文档表
CREATE TABLE IF NOT EXISTS `kb_document` (
    `doc_id` varchar(64) NOT NULL COMMENT '文档ID',
    `kb_id` varchar(64) NOT NULL COMMENT '知识库ID',
    `file_name` varchar(256) NOT NULL COMMENT '文件名',
    `file_path` varchar(512) DEFAULT NULL COMMENT '文件存储路径',
    `file_size` bigint DEFAULT 0 COMMENT '文件大小（字节）',
    `file_type` varchar(32) DEFAULT NULL COMMENT '文件类型（pdf/doc/txt等）',
    `content_text` longtext COMMENT '提取的文本内容',
    `chunk_count` int DEFAULT 0 COMMENT '分块数量',
    `status` char(1) DEFAULT '0' COMMENT '状态（0正常 1停用 2处理中 3失败）',
    `error_msg` varchar(512) DEFAULT NULL COMMENT '处理错误信息',
    `create_by` varchar(64) DEFAULT '' COMMENT '创建者',
    `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_by` varchar(64) DEFAULT '' COMMENT '更新者',
    `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`doc_id`),
    KEY `idx_kb_id` (`kb_id`),
    KEY `idx_file_name` (`file_name`),
    KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='知识库文档表';

-- 分块表
CREATE TABLE IF NOT EXISTS `kb_chunk` (
    `chunk_id` varchar(64) NOT NULL COMMENT '分块ID',
    `doc_id` varchar(64) NOT NULL COMMENT '文档ID',
    `kb_id` varchar(64) NOT NULL COMMENT '知识库ID',
    `chunk_index` int DEFAULT 0 COMMENT '分块序号',
    `chunk_text` text NOT NULL COMMENT '分块文本内容',
    `token_count` int DEFAULT 0 COMMENT 'Token数量',
    `char_count` int DEFAULT 0 COMMENT '字符数量',
    `status` char(1) DEFAULT '0' COMMENT '状态（0正常 1停用）',
    `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`chunk_id`),
    KEY `idx_doc_id` (`doc_id`),
    KEY `idx_kb_id` (`kb_id`),
    FULLTEXT KEY `idx_chunk_text` (`chunk_text`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='知识库分块表';

-- 向量表
CREATE TABLE IF NOT EXISTS `kb_embedding` (
    `embedding_id` varchar(64) NOT NULL COMMENT '向量ID',
    `chunk_id` varchar(64) NOT NULL COMMENT '分块ID',
    `kb_id` varchar(64) NOT NULL COMMENT '知识库ID',
    `model_name` varchar(64) DEFAULT NULL COMMENT '模型名称',
    `vector_dimension` int DEFAULT 1536 COMMENT '向量维度',
    `vector_data` blob COMMENT '向量数据（二进制存储）',
    `normalized` tinyint DEFAULT 1 COMMENT '是否归一化（0否 1是）',
    `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`embedding_id`),
    KEY `idx_chunk_id` (`chunk_id`),
    KEY `idx_kb_id` (`kb_id`),
    KEY `idx_model` (`model_name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='知识库向量表';

-- AI会话表
CREATE TABLE IF NOT EXISTS `ai_chat_session` (
    `session_id` varchar(64) NOT NULL COMMENT '会话ID',
    `user_uuid` varchar(64) NOT NULL COMMENT '用户UUID',
    `kb_id` varchar(64) DEFAULT NULL COMMENT '关联知识库ID',
    `session_title` varchar(256) DEFAULT '新对话' COMMENT '会话标题',
    `session_type` char(1) DEFAULT '1' COMMENT '会话类型（1知识库问答 2自由对话）',
    `model_name` varchar(64) DEFAULT 'gpt-4o-mini' COMMENT '模型名称',
    `message_count` int DEFAULT 0 COMMENT '消息数量',
    `total_tokens_in` int DEFAULT 0 COMMENT '输入Token总数',
    `total_tokens_out` int DEFAULT 0 COMMENT '输出Token总数',
    `is_pinned` char(1) DEFAULT '0' COMMENT '是否置顶（0否 1是）',
    `status` char(1) DEFAULT '0' COMMENT '状态（0正常 1停用）',
    `last_message_at` datetime DEFAULT NULL COMMENT '最后消息时间',
    `create_by` varchar(64) DEFAULT '' COMMENT '创建者',
    `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_by` varchar(64) DEFAULT '' COMMENT '更新者',
    `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`session_id`),
    KEY `idx_user_uuid` (`user_uuid`),
    KEY `idx_kb_id` (`kb_id`),
    KEY `idx_status` (`status`),
    KEY `idx_last_message` (`last_message_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='AI会话表';

-- AI消息表
CREATE TABLE IF NOT EXISTS `ai_chat_message` (
    `message_id` varchar(64) NOT NULL COMMENT '消息ID',
    `session_id` varchar(64) NOT NULL COMMENT '会话ID',
    `parent_message_id` varchar(64) DEFAULT NULL COMMENT '父消息ID（支持分支对话）',
    `role` char(1) DEFAULT '1' COMMENT '角色（1用户 2AI助手 3系统）',
    `content` longtext COMMENT '消息内容',
    `content_type` varchar(32) DEFAULT 'text' COMMENT '内容类型（text/markdown/json）',
    `tokens_in` int DEFAULT 0 COMMENT '输入Token数',
    `tokens_out` int DEFAULT 0 COMMENT '输出Token数',
    `latency_ms` int DEFAULT 0 COMMENT '响应延迟（毫秒）',
    `model_name` varchar(64) DEFAULT NULL COMMENT '使用的模型',
    `retrieval_chunks` json DEFAULT NULL COMMENT '检索到的分块ID列表',
    `status` char(1) DEFAULT '0' COMMENT '状态（0正常 1停用 2生成中）',
    `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`message_id`),
    KEY `idx_session_id` (`session_id`),
    KEY `idx_parent_id` (`parent_message_id`),
    KEY `idx_role` (`role`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='AI消息表';
