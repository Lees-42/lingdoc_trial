package com.ruoyi.system.domain.lingdoc.ai;

import java.util.List;

/**
 * 表格填写助手 - 文档查询请求
 * <p>
 * Dify HTTP Request 节点调用 /lingdoc/ai/form/query-docs 时的请求体。
 *
 * @author lingdoc
 */
public class FormDocQueryRequest
{
    /** 表格字段名称列表 */
    private List<String> fieldNames;

    /** 表格类型推断 */
    private String tableType;

    /** 最多返回文档数，默认 3 */
    private Integer maxDocs = 3;

    /** 每个文档内容最大字符数，默认 2000 */
    private Integer maxCharsPerDoc = 2000;

    public List<String> getFieldNames()
    {
        return fieldNames;
    }

    public void setFieldNames(List<String> fieldNames)
    {
        this.fieldNames = fieldNames;
    }

    public String getTableType()
    {
        return tableType;
    }

    public void setTableType(String tableType)
    {
        this.tableType = tableType;
    }

    public Integer getMaxDocs()
    {
        return maxDocs;
    }

    public void setMaxDocs(Integer maxDocs)
    {
        this.maxDocs = maxDocs;
    }

    public Integer getMaxCharsPerDoc()
    {
        return maxCharsPerDoc;
    }

    public void setMaxCharsPerDoc(Integer maxCharsPerDoc)
    {
        this.maxCharsPerDoc = maxCharsPerDoc;
    }
}
