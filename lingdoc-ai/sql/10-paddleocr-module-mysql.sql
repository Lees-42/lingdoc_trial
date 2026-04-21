-- PaddleOCR 模块数据库表结构
-- 创建时间：2026-04-21
-- 说明：PaddleOCR 识别任务表，存储文件上传和 OCR 识别结果

-- 创建 OCR 任务表
CREATE TABLE IF NOT EXISTS lingdoc_ocr_task (
    task_id VARCHAR(64) NOT NULL COMMENT '任务ID，主键',
    user_id BIGINT COMMENT '用户ID',
    task_name VARCHAR(128) COMMENT '任务名称',
    file_url VARCHAR(512) COMMENT '文件存储路径',
    file_name VARCHAR(256) COMMENT '原始文件名',
    file_type VARCHAR(16) COMMENT '文件类型：pdf/docx/doc/jpg/png/bmp',
    status CHAR(1) DEFAULT '0' COMMENT '状态：0待处理 1处理中 2成功 3失败',
    result_json LONGTEXT COMMENT 'OCR识别结果JSON',
    page_count INT DEFAULT 0 COMMENT '总页数',
    process_time BIGINT COMMENT '处理耗时（毫秒）',
    avg_page_time BIGINT COMMENT '平均单页耗时（毫秒）',
    char_count INT DEFAULT 0 COMMENT '识别字符总数',
    error_msg VARCHAR(512) COMMENT '错误信息',
    create_by VARCHAR(64) COMMENT '创建者',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_by VARCHAR(64) COMMENT '更新者',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    remark VARCHAR(512) COMMENT '备注',
    PRIMARY KEY (task_id),
    INDEX idx_user_id (user_id),
    INDEX idx_status (status),
    INDEX idx_create_time (create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='PaddleOCR识别任务表';

-- 插入菜单权限（如果需要前端菜单）
-- 注意：menu_id 请根据实际系统现有 ID 调整，避免冲突

-- 父菜单：AI文档处理（如果不存在，请先在系统管理-菜单管理中创建）
-- INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
-- VALUES ('OCR识别', (SELECT menu_id FROM sys_menu WHERE menu_name = '表格填写助手' LIMIT 1), 2, 'ocr', 'lingdoc/ocr/index', 1, 0, 'C', '0', '0', 'lingdoc:ocr:list', 'icon-ocr', 'admin', NOW(), 'admin', NOW(), 'PaddleOCR文档识别');

-- 按钮权限
-- INSERT INTO sys_menu (menu_name, parent_id, order_num, menu_type, visible, status, perms, create_by, create_time, update_by, update_time)
-- SELECT 'OCR识别查询', menu_id, 1, 'F', '0', '0', 'lingdoc:ocr:list', 'admin', NOW(), 'admin', NOW() FROM sys_menu WHERE menu_name = 'OCR识别' LIMIT 1;

-- INSERT INTO sys_menu (menu_name, parent_id, order_num, menu_type, visible, status, perms, create_by, create_time, update_by, update_time)
-- SELECT 'OCR识别新增', menu_id, 2, 'F', '0', '0', 'lingdoc:ocr:add', 'admin', NOW(), 'admin', NOW() FROM sys_menu WHERE menu_name = 'OCR识别' LIMIT 1;

-- INSERT INTO sys_menu (menu_name, parent_id, order_num, menu_type, visible, status, perms, create_by, create_time, update_by, update_time)
-- SELECT 'OCR识别编辑', menu_id, 3, 'F', '0', '0', 'lingdoc:ocr:edit', 'admin', NOW(), 'admin', NOW() FROM sys_menu WHERE menu_name = 'OCR识别' LIMIT 1;

-- INSERT INTO sys_menu (menu_name, parent_id, order_num, menu_type, visible, status, perms, create_by, create_time, update_by, update_time)
-- SELECT 'OCR识别删除', menu_id, 4, 'F', '0', '0', 'lingdoc:ocr:delete', 'admin', NOW(), 'admin', NOW() FROM sys_menu WHERE menu_name = 'OCR识别' LIMIT 1;
