-- ============================================================
-- 表格填写助手菜单配置更新脚本
-- 执行时机：在已有数据库上执行（非首次初始化）
-- 作用：
--   1. 修正 menu_id=2004 为真正的"关系图谱"菜单
--   2. 新增 menu_id=2005 为"表格填写助手"菜单
-- ============================================================

-- 1. 修正关系图谱菜单（将之前错误指向 form 的组件改回 graph）
UPDATE sys_menu SET
  menu_name = '关系图谱',
  path = 'graph',
  component = 'lingdoc/graph/index',
  perms = 'lingdoc:graph:list',
  icon = 'tree',
  remark = '关系图谱菜单'
WHERE menu_id = '2004';

-- 2. 新增表格填写助手菜单
INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
VALUES ('2005', '表格填写助手', '2000', '5', 'form', 'lingdoc/form/index', '0', '0', 'C', '0', '0', 'lingdoc:form:list', 'form', 'admin', sysdate(), '', null, '表格填写助手菜单');

-- 3. 为表格填写助手添加按钮级权限（可选，如不需要可注释掉）
-- INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
-- VALUES ('2005001', '表格填写查询', '2005', '1', '#', '', '0', '0', 'F', '0', '0', 'lingdoc:form:list', '#', 'admin', sysdate(), '', null, '');
-- INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
-- VALUES ('2005002', '表格填写新增', '2005', '2', '#', '', '0', '0', 'F', '0', '0', 'lingdoc:form:upload', '#', 'admin', sysdate(), '', null, '');
-- INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
-- VALUES ('2005003', '表格填写修改', '2005', '3', '#', '', '0', '0', 'F', '0', '0', 'lingdoc:form:edit', '#', 'admin', sysdate(), '', null, '');
-- INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
-- VALUES ('2005004', '表格填写删除', '2005', '4', '#', '', '0', '0', 'F', '0', '0', 'lingdoc:form:delete', '#', 'admin', sysdate(), '', null, '');
-- INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
-- VALUES ('2005005', '表格填写导出', '2005', '5', '#', '', '0', '0', 'F', '0', '0', 'lingdoc:form:download', '#', 'admin', sysdate(), '', null, '');
