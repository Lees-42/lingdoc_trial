-- ----------------------------
-- 灵档（LingDoc）用户仓库配置表
-- 支持默认仓库自动初始化、创建新仓库、迁移仓库
-- ----------------------------

-- ----------------------------
-- Table structure for lingdoc_user_repo
-- ----------------------------
DROP TABLE IF EXISTS `lingdoc_user_repo`;
CREATE TABLE `lingdoc_user_repo` (
    `repo_id`     VARCHAR(64)  NOT NULL COMMENT '仓库ID',
    `user_id`     BIGINT       NOT NULL COMMENT '用户ID',
    `repo_path`   VARCHAR(512) NOT NULL COMMENT '仓库绝对路径',
    `repo_name`   VARCHAR(100) DEFAULT '默认仓库' COMMENT '仓库名称',
    `create_time` DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `remark`      VARCHAR(500) DEFAULT NULL COMMENT '备注',
    PRIMARY KEY (`repo_id`),
    UNIQUE KEY `uk_user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户仓库配置表';
