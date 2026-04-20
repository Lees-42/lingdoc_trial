package com.ruoyi.system.service.lingdoc;

import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.ruoyi.system.domain.lingdoc.LingdocFormField;
import com.ruoyi.system.domain.lingdoc.LingdocFormReference;
import com.ruoyi.system.domain.lingdoc.LingdocFormTask;
import com.ruoyi.system.mapper.lingdoc.LingdocFormFieldMapper;
import com.ruoyi.system.mapper.lingdoc.LingdocFormReferenceMapper;
import com.ruoyi.system.mapper.lingdoc.LingdocFormTaskMapper;

/**
 * 表格填写任务 服务层实现
 * 
 * @author lingdoc
 */
@Service
public class LingdocFormTaskServiceImpl implements ILingdocFormTaskService
{
    @Autowired
    private LingdocFormTaskMapper formTaskMapper;

    @Autowired
    private LingdocFormFieldMapper formFieldMapper;

    @Autowired
    private LingdocFormReferenceMapper formReferenceMapper;

    /**
     * 查询表格填写任务
     * 
     * @param taskId 任务ID
     * @return 表格填写任务
     */
    @Override
    public LingdocFormTask selectLingdocFormTaskById(String taskId)
    {
        return formTaskMapper.selectLingdocFormTaskById(taskId);
    }

    /**
     * 查询表格填写任务列表
     * 
     * @param lingdocFormTask 表格填写任务
     * @return 表格填写任务集合
     */
    @Override
    public List<LingdocFormTask> selectLingdocFormTaskList(LingdocFormTask lingdocFormTask)
    {
        return formTaskMapper.selectLingdocFormTaskList(lingdocFormTask);
    }

    /**
     * 新增表格填写任务
     * 
     * @param lingdocFormTask 表格填写任务
     * @return 结果
     */
    @Override
    public int insertLingdocFormTask(LingdocFormTask lingdocFormTask)
    {
        return formTaskMapper.insertLingdocFormTask(lingdocFormTask);
    }

    /**
     * 修改表格填写任务
     * 
     * @param lingdocFormTask 表格填写任务
     * @return 结果
     */
    @Override
    public int updateLingdocFormTask(LingdocFormTask lingdocFormTask)
    {
        return formTaskMapper.updateLingdocFormTask(lingdocFormTask);
    }

    /**
     * 批量删除表格填写任务
     * 
     * @param taskIds 需要删除的任务ID
     * @return 结果
     */
    @Override
    @Transactional
    public int deleteLingdocFormTaskByIds(String[] taskIds)
    {
        for (String taskId : taskIds)
        {
            formFieldMapper.deleteLingdocFormFieldByTaskId(taskId);
            formReferenceMapper.deleteLingdocFormReferenceByTaskId(taskId);
        }
        return formTaskMapper.deleteLingdocFormTaskByIds(taskIds);
    }

    /**
     * 删除表格填写任务信息
     * 
     * @param taskId 任务ID
     * @return 结果
     */
    @Override
    @Transactional
    public int deleteLingdocFormTaskById(String taskId)
    {
        formFieldMapper.deleteLingdocFormFieldByTaskId(taskId);
        formReferenceMapper.deleteLingdocFormReferenceByTaskId(taskId);
        return formTaskMapper.deleteLingdocFormTaskById(taskId);
    }

    /**
     * 查询任务的字段列表
     * 
     * @param taskId 任务ID
     * @return 字段列表
     */
    @Override
    public List<LingdocFormField> selectFormFieldsByTaskId(String taskId)
    {
        return formFieldMapper.selectLingdocFormFieldByTaskId(taskId);
    }

    /**
     * 批量更新字段值
     * 
     * @param fields 字段列表
     * @return 结果
     */
    @Override
    public int batchUpdateFormFields(List<LingdocFormField> fields)
    {
        if (fields == null || fields.isEmpty())
        {
            return 0;
        }
        return formFieldMapper.batchUpdateLingdocFormField(fields);
    }

    /**
     * 查询任务的参考文档列表
     * 
     * @param taskId 任务ID
     * @return 参考文档列表
     */
    @Override
    public List<LingdocFormReference> selectFormReferencesByTaskId(String taskId)
    {
        return formReferenceMapper.selectLingdocFormReferenceByTaskId(taskId);
    }
}
