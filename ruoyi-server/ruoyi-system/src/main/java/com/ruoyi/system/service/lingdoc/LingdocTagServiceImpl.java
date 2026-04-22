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
import com.ruoyi.system.domain.lingdoc.LingdocFileIndex;
import com.ruoyi.system.domain.lingdoc.LingdocTag;
import com.ruoyi.system.domain.lingdoc.LingdocTagBinding;
import com.ruoyi.system.mapper.lingdoc.LingdocFileIndexMapper;
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

    @Autowired
    private LingdocFileIndexMapper fileIndexMapper;

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
        // 先删除所有绑定关系（通过 Service 方法触发 cleanupOrphanTag）
        LingdocTagBinding query = new LingdocTagBinding();
        query.setTagId(tagId);
        List<LingdocTagBinding> bindings = tagBindingMapper.selectLingdocTagBindingList(query);
        for (LingdocTagBinding binding : bindings)
        {
            deleteLingdocTagBindingById(binding.getBindingId());
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
        
        // 如果是文件，查询父目录的继承标签（含根目录）
        if ("F".equals(targetType))
        {
            LingdocFileIndex file = fileIndexMapper.selectLingdocFileIndexById(targetId);
            if (file != null)
            {
                // 先查询根目录标签（target_id = '/'）
                List<LingdocTagBinding> rootBindings = tagBindingMapper.selectLingdocTagBindingByTarget("D", "/");
                for (LingdocTagBinding binding : rootBindings)
                {
                    LingdocTag tag = tagMapper.selectLingdocTagById(binding.getTagId());
                    if (tag != null && !containsTagId(result, tag.getTagId()))
                    {
                        Map<String, Object> item = new HashMap<>();
                        item.put("tagId", tag.getTagId());
                        item.put("tagName", tag.getTagName());
                        item.put("tagColor", tag.getTagColor());
                        item.put("bindType", "1"); // 1 = 继承
                        item.put("bindingId", binding.getBindingId());
                        result.add(item);
                    }
                }
                
                // 再逐级查询父目录标签链
                String subPath = file.getSubPath();
                if (StringUtils.isNotEmpty(subPath))
                {
                    String[] parts = subPath.split("/");
                    String currentPath = "";
                    for (int i = 0; i < parts.length; i++)
                    {
                        String part = parts[i].trim();
                        if (StringUtils.isEmpty(part))
                        {
                            continue;
                        }
                        currentPath = StringUtils.isEmpty(currentPath) ? part : currentPath + "/" + part;
                        
                        List<LingdocTagBinding> inheritedBindings = tagBindingMapper.selectLingdocTagBindingByTarget("D", currentPath);
                        for (LingdocTagBinding binding : inheritedBindings)
                        {
                            LingdocTag tag = tagMapper.selectLingdocTagById(binding.getTagId());
                            if (tag != null && !containsTagId(result, tag.getTagId()))
                            {
                                Map<String, Object> item = new HashMap<>();
                                item.put("tagId", tag.getTagId());
                                item.put("tagName", tag.getTagName());
                                item.put("tagColor", tag.getTagColor());
                                item.put("bindType", "1"); // 1 = 继承
                                item.put("bindingId", binding.getBindingId());
                                result.add(item);
                            }
                        }
                    }
                }
            }
        }
        
        return result;
    }

    /**
     * 检查结果列表中是否已包含指定 tagId
     */
    private boolean containsTagId(List<Map<String, Object>> result, String tagId)
    {
        for (Map<String, Object> item : result)
        {
            if (tagId.equals(item.get("tagId")))
            {
                return true;
            }
        }
        return false;
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

    @Override
    @Transactional
    public int deleteLingdocTagBindingByTargetAndTagId(String targetType, String targetId, String tagId)
    {
        LingdocTagBinding query = new LingdocTagBinding();
        query.setTargetType(targetType);
        query.setTargetId(targetId);
        query.setTagId(tagId);
        List<LingdocTagBinding> bindings = tagBindingMapper.selectLingdocTagBindingList(query);
        if (bindings == null || bindings.isEmpty())
        {
            return 0;
        }
        int result = 0;
        for (LingdocTagBinding binding : bindings)
        {
            result += tagBindingMapper.deleteLingdocTagBindingById(binding.getBindingId());
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
