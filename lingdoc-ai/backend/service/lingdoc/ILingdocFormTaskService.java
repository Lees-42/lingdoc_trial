package com.ruoyi.system.service.lingdoc;

import java.util.List;
import com.ruoyi.system.domain.lingdoc.LingdocFormField;
import com.ruoyi.system.domain.lingdoc.LingdocFormReference;
import com.ruoyi.system.domain.lingdoc.LingdocFormTask;

/**
 * 表格填写任务 服务层
 * 
 * @author lingdoc
 */
public interface ILingdocFormTaskService
{
    /**
     * 查询表格填写任务
     * 
     * @param taskId 任务ID
     * @return 表格填写任务
     */
    public LingdocFormTask selectLingdocFormTaskById(String taskId);

    /**
     * 查询表格填写任务列表
     * 
     * @param lingdocFormTask 表格填写任务
     * @return 表格填写任务集合
     */
    public List<LingdocFormTask> selectLingdocFormTaskList(LingdocFormTask lingdocFormTask);

    /**
     * 新增表格填写任务
     * 
     * @param lingdocFormTask 表格填写任务
     * @return 结果
     */
    public int insertLingdocFormTask(LingdocFormTask lingdocFormTask);

    /**
     * 修改表格填写任务
     * 
     * @param lingdocFormTask 表格填写任务
     * @return 结果
     */
    public int updateLingdocFormTask(LingdocFormTask lingdocFormTask);

    /**
     * 批量删除表格填写任务
     * 
     * @param taskIds 需要删除的任务ID
     * @return 结果
     */
    public int deleteLingdocFormTaskByIds(String[] taskIds);

    /**
     * 删除表格填写任务信息
     * 
     * @param taskId 任务ID
     * @return 结果
     */
    public int deleteLingdocFormTaskById(String taskId);

    /**
     * 查询任务的字段列表
     * 
     * @param taskId 任务ID
     * @return 字段列表
     */
    public List<LingdocFormField> selectFormFieldsByTaskId(String taskId);

    /**
     * 批量更新字段值
     * 
     * @param fields 字段列表
     * @return 结果
     */
    public int batchUpdateFormFields(List<LingdocFormField> fields);

    /**
     * 查询任务的参考文档列表
     * 
     * @param taskId 任务ID
     * @return 参考文档列表
     */
    public List<LingdocFormReference> selectFormReferencesByTaskId(String taskId);
}
