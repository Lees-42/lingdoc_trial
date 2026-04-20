-- ============================================================
-- 表格填写助手菜单更新脚本
-- 说明：将"关系图谱"菜单改为"表格填写助手"
-- ============================================================

-- 更新菜单名称和权限
UPDATE sys_menu SET 
  menu_name = '表格填写助手',
  perms = 'lingdoc:form:list,lingdoc:form:upload,lingdoc:form:edit,lingdoc:form:generate,lingdoc:form:download,lingdoc:form:delete',
  remark = '表格填写助手菜单：上传空白表格，AI自动识别字段并从Vault中提取信息填写',
  update_time = sysdate()
WHERE menu_id = '2004';

-- 检查更新结果
-- SELECT menu_id, menu_name, path, component, perms, remark FROM sys_menu WHERE menu_id = '2004';
