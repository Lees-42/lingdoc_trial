package com.ruoyi.system.mapper.lingdoc;

import java.util.List;
import com.ruoyi.system.domain.lingdoc.LingdocTag;

/**
 * 标签定义Mapper接口
 * 
 * @author lingdoc
 */
public interface LingdocTagMapper
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
     * @param lingdocTag 标签定义
     * @return 标签定义集合
     */
    public List<LingdocTag> selectLingdocTagList(LingdocTag lingdocTag);

    /**
     * 根据标签名查询标签
     * 
     * @param tagName 标签名
     * @return 标签定义
     */
    public LingdocTag selectLingdocTagByName(String tagName);

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
     * @param tagIds 需要删除的数据ID集合
     * @return 结果
     */
    public int deleteLingdocTagByIds(String[] tagIds);
}
