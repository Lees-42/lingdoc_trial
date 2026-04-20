-- ----------------------------
-- 灵档菜单路由修复迁移脚本
-- 执行日期: 2026-04-18
-- 问题: 四个灵档功能菜单缺少父目录，导致 Vue Router 生成多个冲突的 path:"/" 路由
-- ----------------------------

-- 1. 插入"灵档"目录菜单（如果已存在则跳过）
INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
SELECT '2000', '灵档', '0', '3', 'lingdoc', null, '0', '0', 'M', '0', '0', '', 'lingdoc', 'admin', sysdate(), '', null, '灵档功能目录'
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_id = '2000');

-- 2. 将四个功能菜单的 parent_id 改为"灵档"目录
UPDATE sys_menu SET parent_id = '2000' WHERE menu_id IN ('2001', '2002', '2003', '2004');

-- 3. 将四个功能菜单的 path 改为相对路径（去掉 lingdoc/ 前缀）
UPDATE sys_menu SET path = 'organize'  WHERE menu_id = '2001';
UPDATE sys_menu SET path = 'search'    WHERE menu_id = '2002';
UPDATE sys_menu SET path = 'version'   WHERE menu_id = '2003';
UPDATE sys_menu SET path = 'form'      WHERE menu_id = '2004';

-- 4. 调整四个功能菜单的排序号
UPDATE sys_menu SET order_num = '1' WHERE menu_id = '2001';
UPDATE sys_menu SET order_num = '2' WHERE menu_id = '2002';
UPDATE sys_menu SET order_num = '3' WHERE menu_id = '2003';
UPDATE sys_menu SET order_num = '4' WHERE menu_id = '2004';
