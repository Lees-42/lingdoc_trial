-- ----------------------------
-- 灵档菜单路由修复迁移脚本
-- 执行日期: 2026-04-18
-- 问题: 五个灵档功能菜单缺少父目录，导致 Vue Router 生成多个冲突的 path:"/" 路由
-- ----------------------------

-- 1. 插入"灵档"目录菜单（如果已存在则跳过）
INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
SELECT '2000', '灵档', '0', '3', 'lingdoc', null, '0', '0', 'M', '0', '0', '', 'lingdoc', 'admin', sysdate(), '', null, '灵档功能目录'
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_id = '2000');

-- 2. 将五个功能菜单的 parent_id 改为"灵档"目录
UPDATE sys_menu SET parent_id = '2000' WHERE menu_id IN ('2001', '2002', '2003', '2004', '2005');

-- 3. 将五个功能菜单的 path 改为相对路径（去掉 lingdoc/ 前缀）
UPDATE sys_menu SET path = 'file-upload', component = 'lingdoc/file-upload/index', menu_name = '文件上传', perms = 'lingdoc:upload:list', remark = '文件上传菜单' WHERE menu_id = '2001';
UPDATE sys_menu SET path = 'agent', component = 'lingdoc/search/index', menu_name = '智能检索', perms = 'lingdoc:search:list', remark = '文档搜索菜单' WHERE menu_id = '2002';
UPDATE sys_menu SET path = 'version', component = 'lingdoc/version/index', menu_name = '版本管理', perms = 'lingdoc:version:list', remark = '版本管理菜单' WHERE menu_id = '2003';
UPDATE sys_menu SET path = 'form', component = 'lingdoc/form/index', menu_name = '表格填写', perms = 'lingdoc:form:list', remark = '表单管理菜单' WHERE menu_id = '2004';
UPDATE sys_menu SET path = 'graph', component = 'lingdoc/graph/index', menu_name = '关系图谱', perms = 'lingdoc:graph:list', remark = '图表展示菜单' WHERE menu_id = '2005';

-- 4. 调整五个功能菜单的排序号
UPDATE sys_menu SET order_num = '1' WHERE menu_id = '2001';
UPDATE sys_menu SET order_num = '2' WHERE menu_id = '2002';
UPDATE sys_menu SET order_num = '3' WHERE menu_id = '2003';
UPDATE sys_menu SET order_num = '4' WHERE menu_id = '2004';
UPDATE sys_menu SET order_num = '5' WHERE menu_id = '2005';
