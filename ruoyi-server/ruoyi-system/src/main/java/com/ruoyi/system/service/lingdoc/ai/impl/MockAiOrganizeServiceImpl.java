package com.ruoyi.system.service.lingdoc.ai.impl;

import java.math.BigDecimal;
import java.util.ArrayList;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;
import com.ruoyi.system.service.lingdoc.ai.IAiOrganizeService;
import com.ruoyi.system.service.lingdoc.ai.result.AiOrganizeResult;

/**
 * AI 自动规整服务 Mock 实现
 * <p>
 * 用于前后端联调阶段，真实 AI 逻辑由后续开发者替换。
 * 
 * @author lingdoc
 */
@Service
@Primary
public class MockAiOrganizeServiceImpl implements IAiOrganizeService
{
    @Override
    public AiOrganizeResult organize(String fileId, String filePath, String fileName,
                                     String fileContent, Long userId)
    {
        AiOrganizeResult r = new AiOrganizeResult();
        r.setCategory(null);
        r.setTags(new ArrayList<>());
        r.setRename(null);
        r.setSummary(null);
        r.setKeywords(new ArrayList<>());
        r.setConfidence(BigDecimal.ZERO);
        r.setTokenCost(0);
        return r;
    }
}
