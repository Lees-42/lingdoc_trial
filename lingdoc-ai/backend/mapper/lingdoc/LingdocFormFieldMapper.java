package com.ruoyi.system.mapper.lingdoc;

import java.util.List;
import com.ruoyi.system.domain.lingdoc.LingdocFormField;

/**
 * 表格字段Mapper接口
 * 
 * @author lingdoc
 */
public interface LingdocFormFieldMapper
{
    /**
     * 查询表格字段
     * 
     * @param fieldId 字段ID
     * @return 表格字段
     */
    public LingdocFormField selectLingdocFormFieldById(String fieldId);

    /**
     * 查询表格字段列表
     * 
     * @param lingdocFormField 表格字段
     * @return 表格字段集合
     */
    public List<LingdocFormField> selectLingdocFormFieldList(LingdocFormField lingdocFormField);

    /**
     * 根据任务ID查询字段列表
     * 
     * @param taskId 任务ID
     * @return 表格字段集合
     */
    public List<LingdocFormField> selectLingdocFormFieldByTaskId(String taskId);

    /**
     * 新增表格字段
     * 
     * @param lingdocFormField 表格字段
     * @return 结果
     */
    public int insertLingdocFormField(LingdocFormField lingdocFormField);

    /**
     * 批量新增表格字段
     * 
     * @param lingdocFormFieldList 表格字段列表
     * @return 结果
     */
    public int batchInsertLingdocFormField(List<LingdocFormField> lingdocFormFieldList);

    /**
     * 修改表格字段
     * 
     * @param lingdocFormField 表格字段
     * @return 结果
     */
    public int updateLingdocFormField(LingdocFormField lingdocFormField);

    /**
     * 批量修改表格字段
     * 
     * @param lingdocFormFieldList 表格字段列表
     * @return 结果
     */
    public int batchUpdateLingdocFormField(List<LingdocFormField> lingdocFormFieldList);

    /**
     * 删除表格字段
     * 
     * @param fieldId 字段ID
     * @return 结果
     */
    public int deleteLingdocFormFieldById(String fieldId);

    /**
     * 根据任务ID删除字段
     * 
     * @param taskId 任务ID
     * @return 结果
     */
    public int deleteLingdocFormFieldByTaskId(String taskId);

    /**
     * 批量删除表格字段
     * 
     * @param fieldIds 需要删除的数据ID
     * @return 结果
     */
    public int deleteLingdocFormFieldByIds(String[] fieldIds);
}
