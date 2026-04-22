-- ============================================================
-- LingDoc MySQL 一键初始化脚本
-- 文档编号: LingDoc-FAST-014
-- 版本: v1.0
-- 数据库: MySQL 8.0+
-- 字符集: utf8mb4_unicode_ci
-- 执行时机: 新环境初始化 或 已有环境升级
-- 说明:
--   1. 本脚本幂等执行（可重复执行，结果一致）
--   2. 新环境：创建 lingdoc_user_repo + 插入菜单
--   3. 已有环境：清理旧业务表 + 确保表结构最新 + 确保菜单完整
--   4. 所有业务数据表已迁移到各 Vault 的 SQLite，MySQL 不再保留
-- 执行顺序: 在 ry_20260321.sql 之后执行
-- ============================================================

SET NAMES utf8mb4;

-- ============================================================
-- 第一部分：清理旧业务表（兼容已有环境升级）
-- ============================================================

-- Vault 业务表（已迁移到 SQLite）
DROP TABLE IF EXISTS `lingdoc_file_index`;
DROP TABLE IF EXISTS `lingdoc_file_version`;
DROP TABLE IF EXISTS `lingdoc_file_ai_meta`;
DROP TABLE IF EXISTS `lingdoc_tag`;
DROP TABLE IF EXISTS `lingdoc_tag_binding`;
DROP TABLE IF EXISTS `lingdoc_desensitized_file`;

-- 表单业务表（已迁移到 SQLite）
DROP TABLE IF EXISTS `lingdoc_form_task`;
DROP TABLE IF EXISTS `lingdoc_form_field`;
DROP TABLE IF EXISTS `lingdoc_form_reference`;

-- ============================================================
-- 第二部分：创建/更新 lingdoc_user_repo（Vault 注册配置表）
-- ============================================================

-- 若旧表存在则删除重建（确保结构最新）
DROP TABLE IF EXISTS `lingdoc_user_repo`;

CREATE TABLE `lingdoc_user_repo` (
    `repo_id`     VARCHAR(64)  NOT NULL COMMENT '仓库ID',
    `user_id`     BIGINT       NOT NULL COMMENT '用户ID',
    `repo_path`   VARCHAR(512) NOT NULL COMMENT '仓库绝对路径',
    `repo_name`   VARCHAR(100) DEFAULT '默认仓库' COMMENT '仓库名称',
    `is_default`  CHAR(1)      DEFAULT '1' COMMENT '是否默认仓库：0否 1是',
    `is_active`   CHAR(1)      DEFAULT '1' COMMENT '是否激活：0禁用 1激活',
    `create_time` DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `remark`      VARCHAR(500) DEFAULT NULL COMMENT '备注',
    PRIMARY KEY (`repo_id`),
    INDEX `idx_user_id` (`user_id`),
    UNIQUE KEY `uk_user_default` (`user_id`, `is_default`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户仓库配置表（中心库唯一保留的 LingDoc 配置表）';

-- ============================================================
-- 第三部分：灵档菜单数据（幂等插入）
-- ============================================================

-- 目录菜单：灵档（2000）
INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
SELECT '2000', '灵档', '0', '3', 'lingdoc', null, '0', '0', 'M', '0', '0', '', 'lingdoc', 'admin', sysdate(), '', null, '灵档功能目录'
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_id = '2000');

-- 功能菜单：文档上传（2001）
INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
SELECT '2001', '文档上传', '2000', '1', 'upload', 'lingdoc/upload/index', '0', '0', 'C', '0', '0', 'lingdoc:upload:list', 'upload', 'admin', sysdate(), '', null, '文档上传菜单'
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_id = '2001');

-- 功能菜单：智能检索（2002）
INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
SELECT '2002', '智能检索', '2000', '2', 'agent', 'lingdoc/search/index', '0', '0', 'C', '0', '0', 'lingdoc:search:query', 'search', 'admin', sysdate(), '', null, '智能检索菜单'
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_id = '2002');

-- 功能菜单：版本溯源（2003）
INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
SELECT '2003', '版本溯源', '2000', '3', 'version', 'lingdoc/version/index', '0', '0', 'C', '0', '0', 'lingdoc:version:list', 'time', 'admin', sysdate(), '', null, '版本溯源菜单'
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_id = '2003');

-- 功能菜单：关系图谱（2004）
INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
SELECT '2004', '关系图谱', '2000', '4', 'graph', 'lingdoc/graph/index', '0', '0', 'C', '0', '0', 'lingdoc:graph:list', 'tree', 'admin', sysdate(), '', null, '关系图谱菜单'
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_id = '2004');

-- 功能菜单：表格填写助手（2005）
INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
SELECT '2005', '表格填写助手', '2000', '5', 'form', 'lingdoc/form/index', '0', '0', 'C', '0', '0', 'lingdoc:form:list', 'form', 'admin', sysdate(), '', null, '表格填写助手菜单'
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_id = '2005');

-- 功能菜单：仓库管理（2008）
INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
SELECT '2008', '仓库管理', '2000', '8', 'repo', 'lingdoc/repo/index', '0', '0', 'C', '0', '0', 'lingdoc:repo:list,lingdoc:repo:add,lingdoc:repo:edit,lingdoc:repo:delete', 'folder-opened', 'admin', sysdate(), '', null, '多仓库管理菜单：创建、切换、管理多个Vault仓库'
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_id = '2008');

-- ============================================================
-- 第四部分：修正菜单父目录和路由配置（幂等更新）
-- ============================================================

UPDATE sys_menu SET parent_id = '2000' WHERE menu_id IN ('2001', '2002', '2003', '2004', '2005', '2008');
