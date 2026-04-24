package com.ruoyi.system.service.lingdoc.ai.dify;

import java.util.Map;

/**
 * Dify Workflow "form-generate" 业务输出
 * <p>
 * 对应文档生成 Workflow 的 data.outputs 层级结构。
 *
 * @author lingdoc
 */
public class DifyFormGenerateOutput
{
    /** 方式 A：AI 直接生成的填写后文件绝对路径 */
    private String filledFilePath;

    /** 方式 B：字段值映射表（key=字段名, value=填写值） */
    private Map<String, String> filledValues;

    /** Token 消耗 */
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
