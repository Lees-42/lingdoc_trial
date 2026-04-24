package com.ruoyi.system.mapper.lingdoc;

import java.util.List;
import com.ruoyi.system.domain.lingdoc.LingdocTagBinding;

/**
 * 标签绑定Mapper接口
 * 
 * @author lingdoc
 */
public interface LingdocTagBindingMapper
{
    /**
     * 查询标签绑定
     * 
     * @param bindingId 绑定记录ID
     * @return 标签绑定
     */
    public LingdocTagBinding selectLingdocTagBindingById(String bindingId);

    /**
     * 查询标签绑定列表
     * 
     * @param lingdocTagBinding 标签绑定
     * @return 标签绑定集合
     */
    public List<LingdocTagBinding> selectLingdocTagBindingList(LingdocTagBinding lingdocTagBinding);

    /**
     * 查询指定文件的所有标签绑定（含继承）
     * 
     * @param targetType 目标类型
     * @param targetId 目标标识
     * @return 标签绑定集合
     */
    public List<LingdocTagBinding> selectLingdocTagBindingByTarget(String targetType, String targetId);

    /**
     * 新增标签绑定
     * 
     * @param lingdocTagBinding 标签绑定
     * @return 结果
     */
    public int insertLingdocTagBinding(LingdocTagBinding lingdocTagBinding);

    /**
     * 修改标签绑定
     * 
     * @param lingdocTagBinding 标签绑定
     * @return 结果
     */
    public int updateLingdocTagBinding(LingdocTagBinding lingdocTagBinding);

    /**
     * 删除标签绑定
     * 
     * @param bindingId 绑定记录ID
     * @return 结果
     */
    public int deleteLingdocTagBindingById(String bindingId);

    /**
     * 根据目标删除标签绑定
     * 
     * @param targetType 目标类型
     * @param targetId 目标标识
     * @return 结果
     */
    public int deleteLingdocTagBindingByTarget(String targetType, String targetId);

    /**
     * 批量删除标签绑定
     * 
     * @param bindingIds 需要删除的数据ID集合
     * @return 结果
     */
    public int deleteLingdocTagBindingByIds(String[] bindingIds);

    /**
     * 按目标路径前缀删除标签绑定（用于删除文件夹时级联删除目录标签）
     * 
     * @param targetType 目标类型
     * @param targetIdPrefix 目标ID前缀
     * @return 结果
     */
    public int deleteByTargetPathPrefix(String targetType, String targetIdPrefix);
}
