package com.ruoyi.system.service.lingdoc.ai.impl;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;
import com.ruoyi.system.service.lingdoc.ai.IAiOrganizeService;
import com.ruoyi.system.service.lingdoc.ai.result.AiCategorySuggestion;
import com.ruoyi.system.service.lingdoc.ai.result.AiOrganizeResult;
import com.ruoyi.system.service.lingdoc.ai.result.AiRenameSuggestion;
import com.ruoyi.system.service.lingdoc.ai.result.AiTagSuggestion;

/**
 * 本地 AI 自动规整服务实现（替代 Dify）
 * 
 * @author lingdoc
 */
@Service
@Primary
public class LocalAiOrganizeServiceImpl implements IAiOrganizeService
{
    @Override
    public AiOrganizeResult organize(String fileId, String filePath, String fileName,
                                     String fileContent, Long userId)
    {
        AiOrganizeResult result = new AiOrganizeResult();
        result.setSummary("");
        result.setKeywords(new ArrayList<>());
        result.setTags(new ArrayList<>());
        result.setConfidence(new BigDecimal("0.5"));
        result.setTokenCost(0L);

        AiRenameSuggestion rename = new AiRenameSuggestion();
        rename.setSuggestedName(fileName);
        rename.setReason("保持原文件名");
        rename.setConfidence(new BigDecimal("0.5"));
        result.setRename(rename);

        return result;
    }
}
