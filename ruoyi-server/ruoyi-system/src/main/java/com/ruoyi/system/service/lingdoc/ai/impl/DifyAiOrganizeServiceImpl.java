package com.ruoyi.system.service.lingdoc.ai.impl;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;
import com.ruoyi.system.service.lingdoc.ai.dify.DifyWorkflowClient.DifyEnabledCondition;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.system.domain.lingdoc.LingdocTag;
import com.ruoyi.system.service.lingdoc.ILingdocTagService;
import com.ruoyi.system.service.lingdoc.ILingdocUserRepoService;
import com.ruoyi.system.service.lingdoc.ai.IAiOrganizeService;
import com.ruoyi.system.service.lingdoc.ai.dify.DifyOrganizeOutput;
import com.ruoyi.system.service.lingdoc.ai.dify.DifyWorkflowClient;
import com.ruoyi.system.service.lingdoc.ai.result.AiCategorySuggestion;
import com.ruoyi.system.service.lingdoc.ai.result.AiOrganizeResult;
import com.ruoyi.system.service.lingdoc.ai.result.AiRenameSuggestion;
import com.ruoyi.system.service.lingdoc.ai.result.AiTagSuggestion;

@Service
@Primary
@Conditional(DifyEnabledCondition.class)
public class DifyAiOrganizeServiceImpl implements IAiOrganizeService
{
    private static final Logger log = LoggerFactory.getLogger(DifyAiOrganizeServiceImpl.class);

    @Autowired
    private DifyWorkflowClient difyClient;

    @Autowired
    private ILingdocUserRepoService userRepoService;

    @Autowired
    private ILingdocTagService tagService;

    @Autowired
    ObjectMapper objectMapper;

    @Override
    public AiOrganizeResult organize(String fileId, String filePath, String fileName,
                                     String fileContent, Long userId)
    {
        Map<String, Object> inputs = new HashMap<>();
        inputs.put("fileName", fileName);
        inputs.put("fileContent", fileContent != null ? fileContent : "");
        inputs.put("existingDirs", getExistingDirs(userId));
        inputs.put("existingTags", getExistingTags(userId));

        log.info("调用 Dify 自动规整, fileId={}, fileName={}", fileId, fileName);

        var resp = difyClient.runWorkflow(inputs, String.valueOf(userId));

        if (resp == null || resp.getData() == null)
        {
            throw new RuntimeException("Dify 返回空响应");
        }
        if (!"succeeded".equals(resp.getData().getStatus()))
        {
            throw new RuntimeException("Dify 工作流执行失败, status=" + resp.getData().getStatus());
        }

        DifyOrganizeOutput output = objectMapper.convertValue(resp.getData().getOutputs(), DifyOrganizeOutput.class);
        AiOrganizeResult result = convertToAiOrganizeResult(output, fileName);

        log.info("Dify 自动规整完成, fileId={}, suggestedPath={}, suggestedName={}",
                fileId,
                result.getCategory() != null ? result.getCategory().getSuggestedSubPath() : null,
                result.getRename() != null ? result.getRename().getSuggestedName() : null);

        return result;
    }

    private AiOrganizeResult convertToAiOrganizeResult(DifyOrganizeOutput output, String originalFileName)
    {
        AiOrganizeResult result = new AiOrganizeResult();

        if (output == null)
        {
            applyFallbacks(result, originalFileName);
            return result;
        }

        if (StringUtils.isNotEmpty(output.getSuggestedSubPath()))
        {
            AiCategorySuggestion cat = new AiCategorySuggestion();
            cat.setSuggestedSubPath(output.getSuggestedSubPath());
            cat.setReason(output.getReason());
            cat.setConfidence(output.getConfidence());
            result.setCategory(cat);
        }

        if (output.getTags() != null && !output.getTags().isEmpty())
        {
            List<AiTagSuggestion> tagList = output.getTags().stream().map(t -> {
                AiTagSuggestion tag = new AiTagSuggestion();
                tag.setTagName(t.getTagName());
                tag.setTagColor(t.getTagColor());
                tag.setReason(t.getReason());
                tag.setConfidence(t.getConfidence());
                return tag;
            }).collect(Collectors.toList());
            result.setTags(tagList);
        }

        if (StringUtils.isNotEmpty(output.getSuggestedName()))
        {
            AiRenameSuggestion rename = new AiRenameSuggestion();
            rename.setSuggestedName(output.getSuggestedName());
            rename.setReason(output.getRenameReason());
            rename.setConfidence(output.getRenameConfidence());
            result.setRename(rename);
        }

        result.setSummary(output.getSummary());
        result.setKeywords(output.getKeywords());
        result.setConfidence(output.getConfidence());
        result.setTokenCost(output.getTokenCost());

        applyFallbacks(result, originalFileName);
        return result;
    }

    private void applyFallbacks(AiOrganizeResult result, String originalFileName)
    {
        if (result.getCategory() == null || StringUtils.isEmpty(result.getCategory().getSuggestedSubPath()))
        {
            AiCategorySuggestion cat = new AiCategorySuggestion();
            cat.setSuggestedSubPath("/");
            cat.setReason("默认根目录");
            cat.setConfidence(BigDecimal.ZERO);
            result.setCategory(cat);
        }

        if (result.getRename() == null || StringUtils.isEmpty(result.getRename().getSuggestedName()))
        {
            AiRenameSuggestion rename = new AiRenameSuggestion();
            rename.setSuggestedName(originalFileName);
            rename.setReason("保持原名");
            rename.setConfidence(BigDecimal.ZERO);
            result.setRename(rename);
        }

        if (result.getTags() == null)
        {
            result.setTags(new ArrayList<>());
        }

        if (result.getKeywords() == null)
        {
            result.setKeywords(new ArrayList<>());
        }

        if (result.getConfidence() == null)
        {
            result.setConfidence(BigDecimal.ZERO);
        }

        if (result.getTokenCost() == null)
        {
            result.setTokenCost(0L);
        }
    }

    private String getExistingDirs(Long userId)
    {
        try
        {
            String vaultRoot = userRepoService.getUserRepoPath(userId);
            java.nio.file.Path docsPath = java.nio.file.Paths.get(vaultRoot, "documents");
            if (!java.nio.file.Files.exists(docsPath))
            {
                return "";
            }

            List<String> dirs = new ArrayList<>();
            java.nio.file.Files.walk(docsPath)
                .filter(java.nio.file.Files::isDirectory)
                .filter(p -> !p.equals(docsPath))
                .forEach(p -> {
                    String rel = docsPath.relativize(p).toString().replace("\\", "/");
                    dirs.add(rel);
                });
            return String.join(",", dirs);
        }
        catch (Exception e)
        {
            log.warn("获取已有目录结构失败, userId={}", userId, e);
            return "";
        }
    }

    private String getExistingTags(Long userId)
    {
        try
        {
            LingdocTag query = new LingdocTag();
            List<LingdocTag> tags = tagService.selectLingdocTagList(query);
            if (tags == null || tags.isEmpty())
            {
                return "";
            }
            return tags.stream()
                .filter(t -> StringUtils.isNotEmpty(t.getTagName()))
                .map(t -> t.getTagName() + "#" + (StringUtils.isEmpty(t.getTagColor()) ? "#409EFF" : t.getTagColor()))
                .collect(Collectors.joining(","));
        }
        catch (Exception e)
        {
            log.warn("获取已有标签列表失败, userId={}", userId, e);
            return "";
        }
    }
}
