package com.ruoyi.system.domain.lingdoc.ai;

import java.math.BigDecimal;

/**
 * AI 识别的参考文档定义
 * 
 * @author lingdoc
 */
public class AiReference
{
    /** Vault 文档ID */
    private String docId;

    /** 文档名称 */
    private String docName;

    /** 文档存储路径 */
    private String docPath;

    /** 文档类型 */
    private String docType;

    /** 相关性评分（0.00~1.00） */
    private BigDecimal relevance;

    public String getDocId()
    {
        return docId;
    }

    public void setDocId(String docId)
    {
        this.docId = docId;
    }

    public String getDocName()
    {
        return docName;
    }

    public void setDocName(String docName)
    {
        this.docName = docName;
    }

    public String getDocPath()
    {
        return docPath;
    }

    public void setDocPath(String docPath)
    {
        this.docPath = docPath;
    }

    public String getDocType()
    {
        return docType;
    }

    public void setDocType(String docType)
    {
        this.docType = docType;
    }

    public BigDecimal getRelevance()
    {
        return relevance;
    }

    public void setRelevance(BigDecimal relevance)
    {
        this.relevance = relevance;
    }
}
