package com.ruoyi.web.controller.lingdoc;

import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import com.ruoyi.common.annotation.Log;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.enums.BusinessType;
import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.system.domain.lingdoc.LingdocTag;
import com.ruoyi.system.domain.lingdoc.LingdocTagBinding;
import com.ruoyi.system.service.lingdoc.ILingdocTagService;

/**
 * 标签管理 Controller
 * 
 * @author lingdoc
 */
@RestController
@RequestMapping("/lingdoc/tag")
public class LingdocTagController extends BaseController
{
    private static final Logger log = LoggerFactory.getLogger(LingdocTagController.class);

    @Autowired
    private ILingdocTagService tagService;

    /**
     * 查询标签列表
     */
    @PreAuthorize("@ss.hasPermi('lingdoc:tag:list')")
    @GetMapping("/list")
    public AjaxResult list(LingdocTag tag)
    {
        List<LingdocTag> list = tagService.selectLingdocTagList(tag);
        return AjaxResult.success(list);
    }

    /**
     * 获取标签详情
     */
    @PreAuthorize("@ss.hasPermi('lingdoc:tag:list')")
    @GetMapping("/{tagId}")
    public AjaxResult getInfo(@PathVariable("tagId") String tagId)
    {
        return AjaxResult.success(tagService.selectLingdocTagById(tagId));
    }

    /**
     * 新增标签
     */
    @PreAuthorize("@ss.hasPermi('lingdoc:tag:edit')")
    @Log(title = "标签管理", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@RequestBody LingdocTag tag)
    {
        if (StringUtils.isEmpty(tag.getTagName()))
        {
            return AjaxResult.error("标签名不能为空");
        }
        // 检查同名标签是否已存在
        LingdocTag query = new LingdocTag();
        query.setTagName(tag.getTagName());
        List<LingdocTag> existing = tagService.selectLingdocTagList(query);
        if (existing != null && !existing.isEmpty())
        {
            return AjaxResult.success(existing.get(0));
        }
        return toAjax(tagService.insertLingdocTag(tag));
    }

    /**
     * 修改标签
     */
    @PreAuthorize("@ss.hasPermi('lingdoc:tag:edit')")
    @Log(title = "标签管理", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@RequestBody LingdocTag tag)
    {
        return toAjax(tagService.updateLingdocTag(tag));
    }

    /**
     * 删除标签
     */
    @PreAuthorize("@ss.hasPermi('lingdoc:tag:edit')")
    @Log(title = "标签管理", businessType = BusinessType.DELETE)
    @DeleteMapping("/{tagIds}")
    public AjaxResult remove(@PathVariable String[] tagIds)
    {
        return toAjax(tagService.deleteLingdocTagByIds(tagIds));
    }

    /**
     * 获取文件的标签（含继承）
     */
    @PreAuthorize("@ss.hasPermi('lingdoc:tag:list')")
    @GetMapping("/file/{fileId}")
    public AjaxResult fileTags(@PathVariable("fileId") String fileId)
    {
        List<Map<String, Object>> tags = tagService.selectTagsByTarget("F", fileId);
        return AjaxResult.success(tags);
    }

    /**
     * 绑定标签
     */
    @PreAuthorize("@ss.hasPermi('lingdoc:tag:edit')")
    @Log(title = "标签管理", businessType = BusinessType.INSERT)
    @PostMapping("/bind")
    public AjaxResult bind(@RequestBody LingdocTagBinding binding)
    {
        if (StringUtils.isEmpty(binding.getTargetId()) || StringUtils.isEmpty(binding.getTagId()))
        {
            return AjaxResult.error("目标ID和标签ID不能为空");
        }
        // 幂等检查：若已存在相同绑定，直接返回成功
        LingdocTagBinding query = new LingdocTagBinding();
        query.setTargetType(binding.getTargetType());
        query.setTargetId(binding.getTargetId());
        query.setTagId(binding.getTagId());
        query.setBindType(binding.getBindType());
        List<LingdocTagBinding> existing = tagService.selectLingdocTagBindingList(query);
        if (existing != null && !existing.isEmpty())
        {
            return AjaxResult.success("标签已绑定");
        }
        return toAjax(tagService.insertLingdocTagBinding(binding));
    }

    /**
     * 获取目录的标签（含继承）
     */
    @PreAuthorize("@ss.hasPermi('lingdoc:tag:list')")
    @GetMapping("/folder")
    public AjaxResult folderTags(@RequestParam(value = "path", required = false, defaultValue = "/") String path)
    {
        try
        {
            List<Map<String, Object>> tags = tagService.selectTagsByTarget("D", path);
            return AjaxResult.success(tags);
        }
        catch (NumberFormatException e)
        {
            log.error("目录标签查询参数类型错误: path={}, 错误信息: {}", path, e.getMessage(), e);
            return AjaxResult.error("目录路径参数无效: " + path);
        }
        catch (Exception e)
        {
            log.error("目录标签查询失败: path={}, 错误信息: {}", path, e.getMessage(), e);
            return AjaxResult.error("查询目录标签失败: " + e.getMessage());
        }
    }

    /**
     * 解绑标签
     */
    @PreAuthorize("@ss.hasPermi('lingdoc:tag:edit')")
    @Log(title = "标签管理", businessType = BusinessType.DELETE)
    @DeleteMapping("/bind/{bindingId}")
    public AjaxResult unbind(@PathVariable("bindingId") String bindingId)
    {
        return toAjax(tagService.deleteLingdocTagBindingById(bindingId));
    }

    /**
     * 按目标批量解绑标签
     */
    @PreAuthorize("@ss.hasPermi('lingdoc:tag:edit')")
    @Log(title = "标签管理", businessType = BusinessType.DELETE)
    @DeleteMapping("/bind/target")
    public AjaxResult unbindByTarget(@RequestParam("targetType") String targetType,
                                     @RequestParam("targetId") String targetId)
    {
        int result = tagService.deleteLingdocTagBindingByTarget(targetType, targetId);
        return AjaxResult.success(result);
    }

    /**
     * 按目标和标签ID解绑特定标签
     */
    @PreAuthorize("@ss.hasPermi('lingdoc:tag:edit')")
    @Log(title = "标签管理", businessType = BusinessType.DELETE)
    @DeleteMapping("/bind/target-tag")
    public AjaxResult unbindByTargetAndTagId(@RequestParam("targetType") String targetType,
                                               @RequestParam("targetId") String targetId,
                                               @RequestParam("tagId") String tagId)
    {
        return toAjax(tagService.deleteLingdocTagBindingByTargetAndTagId(targetType, targetId, tagId));
    }
}
