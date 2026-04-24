-- ============================================================
-- 灵档菜单路由统一更新脚本
-- 说明：
--   1. 本脚本用于修正和统一灵档（LingDoc）模块的所有菜单路由配置
--   2. 支持幂等执行（可重复执行，结果一致）
--   3. 后续新增灵档功能菜单时，在此脚本中追加对应的 UPDATE 语句
-- 执行时机：在已有数据库上执行（ry_20260321.sql 初始化之后）
-- 更新时间：2026-04-21
-- ============================================================

-- ----------------------------
-- 0. 强制当前会话使用 UTF-8（防止 source 时中文乱码/报错）
-- ----------------------------
SET NAMES utf8mb4;
SET character_set_client = utf8mb4;
SET character_set_connection = utf8mb4;
SET character_set_results = utf8mb4;

-- ----------------------------
-- 1. 确保"灵档"目录菜单存在（menu_id=2000）
-- ----------------------------
INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
SELECT '2000', '灵档', '0', '3', 'lingdoc', null, '0', '0', 'M', '0', '0', '', 'lingdoc', 'admin', sysdate(), '', null, '灵档功能目录'
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_id = '2000');

-- ----------------------------
-- 2. 修正所有灵档功能菜单的父目录为"灵档"（2000）
-- ----------------------------
UPDATE sys_menu SET parent_id = '2000' WHERE menu_id IN ('2001', '2002', '2003', '2004', '2005');

-- ----------------------------
-- 3. 修正所有灵档功能菜单的路由、组件、权限等配置（幂等）
--    以下每条 UPDATE 确保对应 menu_id 的配置完全正确，可重复执行
-- ----------------------------

-- 2001: 文档上传（原"自动规整"）
UPDATE sys_menu SET
  menu_name = '文件上传',
  path = 'file-upload',
  component = 'lingdoc/file-upload/index',
  perms = 'lingdoc:upload:list',
  icon = 'upload',
  order_num = '1',
  remark = '文件上传菜单',
  update_time = sysdate()
WHERE menu_id = '2001';

-- 2002: 智能检索（灵犀问答）
UPDATE sys_menu SET
  menu_name = '智能检索',
  path = 'agent',
  component = 'lingdoc/search/index',
  perms = 'lingdoc:search:query',
  icon = 'search',
  order_num = '2',
  remark = '智能检索菜单',
  update_time = sysdate()
WHERE menu_id = '2002';

-- 2003: 版本溯源
UPDATE sys_menu SET
  menu_name = '版本溯源',
  path = 'version',
  component = 'lingdoc/version/index',
  perms = 'lingdoc:version:list',
  icon = 'time',
  order_num = '3',
  remark = '版本溯源菜单',
  update_time = sysdate()
WHERE menu_id = '2003';

-- 2004: 关系图谱（万象星云）
-- 注意：此前曾被错误地改为 path='form'，与 2005 冲突，现修正为 path='graph'
UPDATE sys_menu SET
  menu_name = '关系图谱',
  path = 'graph',
  component = 'lingdoc/graph/index',
  perms = 'lingdoc:graph:list',
  icon = 'tree',
  order_num = '4',
  remark = '关系图谱菜单',
  update_time = sysdate()
WHERE menu_id = '2004';

-- 2005: 表格填写助手
UPDATE sys_menu SET
  menu_name = '表格填写助手',
  path = 'form',
  component = 'lingdoc/form/index',
  perms = 'lingdoc:form:list',
  icon = 'form',
  order_num = '5',
  remark = '表格填写助手菜单',
  update_time = sysdate()
WHERE menu_id = '2005';

-- ----------------------------
-- 4. 验证语句（可选，执行后取消注释查看结果）
-- ----------------------------
-- SELECT menu_id, menu_name, path, component, perms, remark FROM sys_menu WHERE menu_id BETWEEN 2000 AND 2005 ORDER BY menu_id;
