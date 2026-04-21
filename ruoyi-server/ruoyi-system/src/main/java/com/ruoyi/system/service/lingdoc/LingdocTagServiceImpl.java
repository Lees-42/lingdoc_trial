package com.ruoyi.system.service.lingdoc;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.common.utils.uuid.UUID;
import com.ruoyi.system.domain.lingdoc.LingdocTag;
import com.ruoyi.system.domain.lingdoc.LingdocTagBinding;
import com.ruoyi.system.mapper.lingdoc.LingdocTagBindingMapper;
import com.ruoyi.system.mapper.lingdoc.LingdocTagMapper;

/**
 * 标签管理服务层实现
 * 
 * @author lingdoc
 */
@Service
public class LingdocTagServiceImpl implements ILingdocTagService
{
    @Autowired
    private LingdocTagMapper tagMapper;

    @Autowired
    private LingdocTagBindingMapper tagBindingMapper;

    @Override
    public LingdocTag selectLingdocTagById(String tagId)
    {
        return tagMapper.selectLingdocTagById(tagId);
    }

    @Override
    public List<LingdocTag> selectLingdocTagList(LingdocTag lingdocTag)
    {
        return tagMapper.selectLingdocTagList(lingdocTag);
    }

    @Override
    public int insertLingdocTag(LingdocTag lingdocTag)
    {
        if (lingdocTag.getTagId() == null)
        {
            lingdocTag.setTagId(UUID.fastUUID().toString());
        }
        return tagMapper.insertLingdocTag(lingdocTag);
    }

    @Override
    public int updateLingdocTag(LingdocTag lingdocTag)
    {
        return tagMapper.updateLingdocTag(lingdocTag);
    }

    @Override
    @Transactional
    public int deleteLingdocTagById(String tagId)
    {
        // 先删除所有绑定关系
        LingdocTagBinding query = new LingdocTagBinding();
        query.setTagId(tagId);
        List<LingdocTagBinding> bindings = tagBindingMapper.selectLingdocTagBindingList(query);
        for (LingdocTagBinding binding : bindings)
        {
            tagBindingMapper.deleteLingdocTagBindingById(binding.getBindingId());
        }
        return tagMapper.deleteLingdocTagById(tagId);
    }

    @Override
    @Transactional
    public int deleteLingdocTagByIds(String[] tagIds)
    {
        int count = 0;
        for (String tagId : tagIds)
        {
            count += deleteLingdocTagById(tagId);
        }
        return count;
    }

    @Override
    public List<LingdocTagBinding> selectLingdocTagBindingList(LingdocTagBinding lingdocTagBinding)
    {
        return tagBindingMapper.selectLingdocTagBindingList(lingdocTagBinding);
    }

    @Override
    public List<Map<String, Object>> selectTagsByTarget(String targetType, String targetId)
    {
        List<Map<String, Object>> result = new ArrayList<>();
        
        // 直接绑定的标签
        List<LingdocTagBinding> directBindings = tagBindingMapper.selectLingdocTagBindingByTarget(targetType, targetId);
        for (LingdocTagBinding binding : directBindings)
        {
            LingdocTag tag = tagMapper.selectLingdocTagById(binding.getTagId());
            if (tag != null)
            {
                Map<String, Object> item = new HashMap<>();
                item.put("tagId", tag.getTagId());
                item.put("tagName", tag.getTagName());
                item.put("tagColor", tag.getTagColor());
                item.put("bindType", binding.getBindType());
                item.put("bindingId", binding.getBindingId());
                result.add(item);
            }
        }
        
        // 如果是文件，继承父目录的标签
        if ("F".equals(targetType))
        {
            // 从 file_index 中获取 sub_path
            // 这里简化处理：由调用方传入目录路径进行继承查询
            // 完整实现需要在 Controller 层获取文件的 sub_path，然后查询父目录标签
        }
        
        return result;
    }

    @Override
    public int insertLingdocTagBinding(LingdocTagBinding lingdocTagBinding)
    {
        if (lingdocTagBinding.getBindingId() == null)
        {
            lingdocTagBinding.setBindingId(UUID.fastUUID().toString());
        }
        return tagBindingMapper.insertLingdocTagBinding(lingdocTagBinding);
    }

    @Override
    @Transactional
    public int deleteLingdocTagBindingById(String bindingId)
    {
        LingdocTagBinding binding = tagBindingMapper.selectLingdocTagBindingById(bindingId);
        int result = tagBindingMapper.deleteLingdocTagBindingById(bindingId);
        if (binding != null)
        {
            cleanupOrphanTag(binding.getTagId());
        }
        return result;
    }

    @Override
    @Transactional
    public int deleteLingdocTagBindingByTarget(String targetType, String targetId)
    {
        List<LingdocTagBinding> bindings = tagBindingMapper.selectLingdocTagBindingByTarget(targetType, targetId);
        int result = tagBindingMapper.deleteLingdocTagBindingByTarget(targetType, targetId);
        for (LingdocTagBinding binding : bindings)
        {
            cleanupOrphanTag(binding.getTagId());
        }
        return result;
    }

    /**
     * 清理孤儿标签：如果标签不再关联任何目标，则删除标签定义
     */
    private void cleanupOrphanTag(String tagId)
    {
        if (StringUtils.isEmpty(tagId))
        {
            return;
        }
        LingdocTagBinding query = new LingdocTagBinding();
        query.setTagId(tagId);
        List<LingdocTagBinding> remaining = tagBindingMapper.selectLingdocTagBindingList(query);
        if (remaining == null || remaining.isEmpty())
        {
            tagMapper.deleteLingdocTagById(tagId);
        }
    }
}
