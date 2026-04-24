package com.ruoyi.system.domain.lingdoc.ai;

import java.util.Map;

/**
 * AI 文档生成返回结果
 * <p>
 * 支持两种模式：
 * 1. 方式 A（AI 直接生成）：filledFilePath 不为空，后端直接使用该文件
 * 2. 方式 B（后端渲染）：filledValues 不为空，后端根据模板 + JSON 渲染
 * 
 * @author lingdoc
 */
public class AiGenerateResult
{
    /** 方式 A：AI 直接生成的填写后文件绝对路径 */
    private String filledFilePath;

    /** 方式 B：字段值映射表：key=字段名, value=填写值 */
    private Map<String, String> filledValues;

    /** Token 消耗量 */
    private Integer tokenCost;

    public String getFilledFilePath()
    {
        return filledFilePath;
    }

    public void setFilledFilePath(String filledFilePath)
    {
        this.filledFilePath = filledFilePath;
    }

    public Map<String, String> getFilledValues()
    {
        return filledValues;
    }

    public void setFilledValues(Map<String, String> filledValues)
    {
        this.filledValues = filledValues;
    }

    public Integer getTokenCost()
    {
        return tokenCost;
    }

    public void setTokenCost(Integer tokenCost)
    {
        this.tokenCost = tokenCost;
    }
}
