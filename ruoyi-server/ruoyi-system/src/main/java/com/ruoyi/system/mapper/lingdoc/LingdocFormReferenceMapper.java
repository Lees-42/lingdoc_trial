package com.ruoyi.system.mapper.lingdoc;

import java.util.List;
import com.ruoyi.system.domain.lingdoc.LingdocFormReference;

/**
 * 任务参考文档关联Mapper接口
 * 
 * @author lingdoc
 */
public interface LingdocFormReferenceMapper
{
    /**
     * 查询任务参考文档关联
     * 
     * @param refId 关联ID
     * @return 任务参考文档关联
     */
    public LingdocFormReference selectLingdocFormReferenceById(String refId);

    /**
     * 查询任务参考文档关联列表
     * 
     * @param lingdocFormReference 任务参考文档关联
     * @return 任务参考文档关联集合
     */
    public List<LingdocFormReference> selectLingdocFormReferenceList(LingdocFormReference lingdocFormReference);

    /**
     * 根据任务ID查询参考文档列表
     * 
     * @param taskId 任务ID
     * @return 任务参考文档关联集合
     */
    public List<LingdocFormReference> selectLingdocFormReferenceByTaskId(String taskId);

    /**
     * 新增任务参考文档关联
     * 
     * @param lingdocFormReference 任务参考文档关联
     * @return 结果
     */
    public int insertLingdocFormReference(LingdocFormReference lingdocFormReference);

    /**
     * 批量新增任务参考文档关联
     * 
     * @param lingdocFormReferenceList 任务参考文档关联列表
     * @return 结果
     */
    public int batchInsertLingdocFormReference(List<LingdocFormReference> lingdocFormReferenceList);

    /**
     * 修改任务参考文档关联
     * 
     * @param lingdocFormReference 任务参考文档关联
     * @return 结果
     */
    public int updateLingdocFormReference(LingdocFormReference lingdocFormReference);

    /**
     * 删除任务参考文档关联
     * 
     * @param refId 关联ID
     * @return 结果
     */
    public int deleteLingdocFormReferenceById(String refId);

    /**
     * 根据任务ID删除参考文档关联
     * 
     * @param taskId 任务ID
     * @return 结果
     */
    public int deleteLingdocFormReferenceByTaskId(String taskId);

    /**
     * 批量删除任务参考文档关联
     * 
     * @param refIds 需要删除的数据ID
     * @return 结果
     */
    public int deleteLingdocFormReferenceByIds(String[] refIds);
}
