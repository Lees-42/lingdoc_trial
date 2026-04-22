package com.ruoyi.system.service.lingdoc;

import java.util.List;
import java.util.Map;
import com.ruoyi.system.domain.lingdoc.LingdocTag;
import com.ruoyi.system.domain.lingdoc.LingdocTagBinding;

/**
 * 标签管理服务层接口
 * 
 * @author lingdoc
 */
public interface ILingdocTagService
{
    /**
     * 查询标签定义
     * 
     * @param tagId 标签唯一ID
     * @return 标签定义
     */
    public LingdocTag selectLingdocTagById(String tagId);

    /**
     * 查询标签定义列表
     * 
     * @param lingdocTag 查询条件
     * @return 标签定义集合
     */
    public List<LingdocTag> selectLingdocTagList(LingdocTag lingdocTag);

    /**
     * 新增标签定义
     * 
     * @param lingdocTag 标签定义
     * @return 结果
     */
    public int insertLingdocTag(LingdocTag lingdocTag);

    /**
     * 修改标签定义
     * 
     * @param lingdocTag 标签定义
     * @return 结果
     */
    public int updateLingdocTag(LingdocTag lingdocTag);

    /**
     * 删除标签定义
     * 
     * @param tagId 标签唯一ID
     * @return 结果
     */
    public int deleteLingdocTagById(String tagId);

    /**
     * 批量删除标签定义
     * 
     * @param tagIds 需要删除的标签ID数组
     * @return 结果
     */
    public int deleteLingdocTagByIds(String[] tagIds);

    /**
     * 查询标签绑定列表
     * 
     * @param lingdocTagBinding 查询条件
     * @return 标签绑定集合
     */
    public List<LingdocTagBinding> selectLingdocTagBindingList(LingdocTagBinding lingdocTagBinding);

    /**
     * 获取目标的所有标签（含继承）
     * 
     * @param targetType 目标类型
     * @param targetId 目标标识
     * @return 标签列表
     */
    public List<Map<String, Object>> selectTagsByTarget(String targetType, String targetId);

    /**
     * 绑定标签
     * 
     * @param lingdocTagBinding 标签绑定
     * @return 结果
     */
    public int insertLingdocTagBinding(LingdocTagBinding lingdocTagBinding);

    /**
     * 解绑标签
     * 
     * @param bindingId 绑定记录ID
     * @return 结果
     */
    public int deleteLingdocTagBindingById(String bindingId);

    /**
     * 根据目标解绑所有标签
     * 
     * @param targetType 目标类型
     * @param targetId 目标标识
     * @return 结果
     */
    public int deleteLingdocTagBindingByTarget(String targetType, String targetId);

    /**
     * 根据目标和标签ID解绑特定标签
     * 
     * @param targetType 目标类型
     * @param targetId 目标标识
     * @param tagId 标签ID
     * @return 结果
     */
    public int deleteLingdocTagBindingByTargetAndTagId(String targetType, String targetId, String tagId);
}
