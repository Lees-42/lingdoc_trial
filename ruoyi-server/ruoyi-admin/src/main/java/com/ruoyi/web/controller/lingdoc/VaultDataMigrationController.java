package com.ruoyi.web.controller.lingdoc;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import javax.sql.DataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.framework.datasource.VaultDataSourceManager;
import com.ruoyi.system.domain.lingdoc.LingdocUserRepo;
import com.ruoyi.system.mapper.lingdoc.LingdocUserRepoMapper;
import com.ruoyi.system.service.lingdoc.impl.VaultDbInitializer;

/**
 * Vault 数据迁移 Controller
 * 
 * 将中心 MySQL 中的 vault 业务数据迁移到各 vault 的 SQLite 数据库中。
 * 一次性执行，执行前请备份数据。
 * 
 * @author lingdoc
 */
@RestController
@RequestMapping("/lingdoc/migrate")
public class VaultDataMigrationController
{
    private static final Logger log = LoggerFactory.getLogger(VaultDataMigrationController.class);

    @Autowired
    @Qualifier("dynamicDataSource")
    private DataSource masterDataSource;

    @Autowired
    private LingdocUserRepoMapper userRepoMapper;

    @Autowired
    private VaultDbInitializer vaultDbInitializer;

    /**
     * 迁移指定仓库的数据（从 MySQL 到 SQLite）
     * 
     * @param repoId 仓库ID
     */
    @PreAuthorize("@ss.hasPermi('lingdoc:vault:edit')")
    @PostMapping("/vault")
    public AjaxResult migrateVault(@RequestParam("repoId") String repoId)
    {
        try
        {
            LingdocUserRepo repo = userRepoMapper.selectByRepoId(repoId);
            if (repo == null)
            {
                return AjaxResult.error("仓库不存在");
            }

            // 确保 SQLite 数据库已初始化
            vaultDbInitializer.initializeVaultDatabase(repo.getRepoPath());

            // 获取 SQLite DataSource
            DataSource sqliteDs = VaultDataSourceManager.getOrCreateDataSource(repo.getRepoPath());

            int totalMigrated = 0;
            Long userId = repo.getUserId();

            // 迁移 lingdoc_file_index
            totalMigrated += migrateTable(masterDataSource, sqliteDs,
                    "lingdoc_file_index", userId,
                    "file_id, user_id, file_name, vault_path, abs_path, file_type, file_size, checksum, sub_path, source_type, file_content, content_path, content_size, is_desensitized, create_time, update_time");

            // 迁移 lingdoc_file_version
            totalMigrated += migrateTable(masterDataSource, sqliteDs,
                    "lingdoc_file_version", null,
                    "version_id, file_id, version_no, snapshot_path, snapshot_size, operation_type, checksum, operator_id, create_time");

            // 迁移 lingdoc_file_ai_meta
            totalMigrated += migrateTable(masterDataSource, sqliteDs,
                    "lingdoc_file_ai_meta", null,
                    "file_id, kb_id, parse_status, parse_error, chunk_count, embedding_status, summary, keywords, create_time, update_time");

            // 迁移 lingdoc_tag
            totalMigrated += migrateTable(masterDataSource, sqliteDs,
                    "lingdoc_tag", null,
                    "tag_id, tag_name, tag_color, tag_scope, sort_order, create_time");

            // 迁移 lingdoc_tag_binding
            totalMigrated += migrateTable(masterDataSource, sqliteDs,
                    "lingdoc_tag_binding", null,
                    "binding_id, target_type, target_id, tag_id, bind_type, create_time");

            // 迁移 lingdoc_desensitized_file
            totalMigrated += migrateTable(masterDataSource, sqliteDs,
                    "lingdoc_desensitized_file", null,
                    "des_id, file_id, des_path, des_content, des_checksum, rule_id, create_time");

            // 迁移表单任务（按 user_id 过滤）
            totalMigrated += migrateTable(masterDataSource, sqliteDs,
                    "lingdoc_form_task", userId,
                    "task_id, user_id, task_name, original_file_id, original_file_url, original_file_name, filled_file_id, filled_file_url, filled_file_name, status, ai_result, field_count, confirmed_count, token_cost, error_msg, create_by, create_time, update_by, update_time, remark");

            // 迁移表单字段（通过 task_id 关联该用户的任务）
            totalMigrated += migrateFormField(masterDataSource, sqliteDs, userId);

            // 迁移参考文档（通过 task_id 关联该用户的任务）
            totalMigrated += migrateFormReference(masterDataSource, sqliteDs, userId);

            log.info("Vault 数据迁移完成: {}, 迁移记录数: {}", repo.getRepoPath(), totalMigrated);
            return AjaxResult.success("迁移完成，共迁移 " + totalMigrated + " 条记录");
        }
        catch (Exception e)
        {
            log.error("Vault 数据迁移失败", e);
            return AjaxResult.error("迁移失败: " + e.getMessage());
        }
    }

    private int migrateTable(DataSource sourceDs, DataSource targetDs,
                             String tableName, Long userId, String columns) throws SQLException
    {
        int count = 0;
        String selectSql = userId != null
                ? "SELECT " + columns + " FROM " + tableName + " WHERE user_id = ?"
                : "SELECT " + columns + " FROM " + tableName;

        String[] colArray = columns.split(",");
        String placeholders = String.join(",", java.util.Collections.nCopies(colArray.length, "?"));
        String insertSql = "INSERT OR IGNORE INTO " + tableName + " (" + columns + ") VALUES (" + placeholders + ")";

        try (Connection sourceConn = sourceDs.getConnection();
             PreparedStatement selectStmt = sourceConn.prepareStatement(selectSql);
             Connection targetConn = targetDs.getConnection();
             PreparedStatement insertStmt = targetConn.prepareStatement(insertSql))
        {
            if (userId != null)
            {
                selectStmt.setLong(1, userId);
            }

            ResultSet rs = selectStmt.executeQuery();
            int columnCount = rs.getMetaData().getColumnCount();

            while (rs.next())
            {
                for (int i = 1; i <= columnCount; i++)
                {
                    insertStmt.setObject(i, rs.getObject(i));
                }
                insertStmt.addBatch();
                count++;

                // 每 500 条提交一次
                if (count % 500 == 0)
                {
                    insertStmt.executeBatch();
                }
            }
            insertStmt.executeBatch();
        }

        log.info("表 {} 迁移完成: {} 条记录", tableName, count);
        return count;
    }

    /**
     * 迁移表单字段（通过 task_id 关联过滤该用户的任务）
     */
    private int migrateFormField(DataSource sourceDs, DataSource targetDs, Long userId) throws SQLException
    {
        String selectSql = "SELECT field_id, task_id, field_name, field_type, field_label, ai_value, user_value, is_confirmed, confidence, source_doc_id, source_doc_name, sort_order, create_time, update_time FROM lingdoc_form_field WHERE task_id IN (SELECT task_id FROM lingdoc_form_task WHERE user_id = ?)";
        String insertSql = "INSERT OR IGNORE INTO lingdoc_form_field (field_id, task_id, field_name, field_type, field_label, ai_value, user_value, is_confirmed, confidence, source_doc_id, source_doc_name, sort_order, create_time, update_time) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

        int count = 0;
        try (Connection sourceConn = sourceDs.getConnection();
             PreparedStatement selectStmt = sourceConn.prepareStatement(selectSql);
             Connection targetConn = targetDs.getConnection();
             PreparedStatement insertStmt = targetConn.prepareStatement(insertSql))
        {
            selectStmt.setLong(1, userId);
            ResultSet rs = selectStmt.executeQuery();
            int columnCount = rs.getMetaData().getColumnCount();

            while (rs.next())
            {
                for (int i = 1; i <= columnCount; i++)
                {
                    insertStmt.setObject(i, rs.getObject(i));
                }
                insertStmt.addBatch();
                count++;
                if (count % 500 == 0)
                {
                    insertStmt.executeBatch();
                }
            }
            insertStmt.executeBatch();
        }

        log.info("表 lingdoc_form_field 迁移完成: {} 条记录", count);
        return count;
    }

    /**
     * 迁移参考文档（通过 task_id 关联过滤该用户的任务）
     */
    private int migrateFormReference(DataSource sourceDs, DataSource targetDs, Long userId) throws SQLException
    {
        String selectSql = "SELECT ref_id, task_id, doc_id, doc_name, doc_path, doc_type, relevance, is_selected, create_time FROM lingdoc_form_reference WHERE task_id IN (SELECT task_id FROM lingdoc_form_task WHERE user_id = ?)";
        String insertSql = "INSERT OR IGNORE INTO lingdoc_form_reference (ref_id, task_id, doc_id, doc_name, doc_path, doc_type, relevance, is_selected, create_time) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";

        int count = 0;
        try (Connection sourceConn = sourceDs.getConnection();
             PreparedStatement selectStmt = sourceConn.prepareStatement(selectSql);
             Connection targetConn = targetDs.getConnection();
             PreparedStatement insertStmt = targetConn.prepareStatement(insertSql))
        {
            selectStmt.setLong(1, userId);
            ResultSet rs = selectStmt.executeQuery();
            int columnCount = rs.getMetaData().getColumnCount();

            while (rs.next())
            {
                for (int i = 1; i <= columnCount; i++)
                {
                    insertStmt.setObject(i, rs.getObject(i));
                }
                insertStmt.addBatch();
                count++;
                if (count % 500 == 0)
                {
                    insertStmt.executeBatch();
                }
            }
            insertStmt.executeBatch();
        }

        log.info("表 lingdoc_form_reference 迁移完成: {} 条记录", count);
        return count;
    }
}
