package com.ruoyi.system.mapper.lingdoc;

import java.util.List;
import com.ruoyi.system.domain.lingdoc.LingdocFormTask;

/**
 * 表格填写任务Mapper接口
 * 
 * @author lingdoc
 */
public interface LingdocFormTaskMapper
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
     * 删除表格填写任务
     * 
     * @param taskId 任务ID
     * @return 结果
     */
    public int deleteLingdocFormTaskById(String taskId);

    /**
     * 批量删除表格填写任务
     * 
     * @param taskIds 需要删除的数据ID
     * @return 结果
     */
    public int deleteLingdocFormTaskByIds(String[] taskIds);
}
