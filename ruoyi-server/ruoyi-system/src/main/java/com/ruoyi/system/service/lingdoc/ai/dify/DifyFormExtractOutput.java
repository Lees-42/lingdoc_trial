package com.ruoyi.system.service.lingdoc.ai.dify;

import java.math.BigDecimal;
import java.util.List;

/**
 * Dify Workflow "form-extract" 业务输出
 * <p>
 * 对应表格字段识别 Workflow 的 data.outputs 层级结构。
 *
 * @author lingdoc
 */
public class DifyFormExtractOutput
{
    /** 识别出的字段列表 */
    private List<DifyFieldOutput> fields;

    /** 参考文档列表 */
    private List<DifyReferenceOutput> references;

    /** Token 消耗 */
    private Integer tokenCost;

    public List<DifyFieldOutput> getFields()
    {
        return fields;
    }

    public void setFields(List<DifyFieldOutput> fields)
    {
        this.fields = fields;
    }

    public List<DifyReferenceOutput> getReferences()
    {
        return references;
    }

    public void setReferences(List<DifyReferenceOutput> references)
    {
        this.references = references;
    }

    public Integer getTokenCost()
    {
        return tokenCost;
    }

    public void setTokenCost(Integer tokenCost)
    {
        this.tokenCost = tokenCost;
    }

    /**
     * 字段输出嵌套类
     */
    public static class DifyFieldOutput
    {
        private String fieldName;
        private String fieldType;
        private String fieldLabel;
        private String suggestedValue;
        private BigDecimal confidence;
        private String sourceDocId;
        private String sourceDocName;
        private Integer sortOrder;
        private List<String> options;

        public String getFieldName()
        {
            return fieldName;
        }

        public void setFieldName(String fieldName)
        {
            this.fieldName = fieldName;
        }

        public String getFieldType()
        {
            return fieldType;
        }

        public void setFieldType(String fieldType)
        {
            this.fieldType = fieldType;
        }

        public String getFieldLabel()
        {
            return fieldLabel;
        }

        public void setFieldLabel(String fieldLabel)
        {
            this.fieldLabel = fieldLabel;
        }

        public String getSuggestedValue()
        {
            return suggestedValue;
        }

        public void setSuggestedValue(String suggestedValue)
        {
            this.suggestedValue = suggestedValue;
        }

        public BigDecimal getConfidence()
        {
            return confidence;
        }

        public void setConfidence(BigDecimal confidence)
        {
            this.confidence = confidence;
        }

        public String getSourceDocId()
        {
            return sourceDocId;
        }

        public void setSourceDocId(String sourceDocId)
        {
            this.sourceDocId = sourceDocId;
        }

        public String getSourceDocName()
        {
            return sourceDocName;
        }

        public void setSourceDocName(String sourceDocName)
        {
            this.sourceDocName = sourceDocName;
        }

        public Integer getSortOrder()
        {
            return sortOrder;
        }

        public void setSortOrder(Integer sortOrder)
        {
            this.sortOrder = sortOrder;
        }

        public List<String> getOptions()
        {
            return options;
        }

        public void setOptions(List<String> options)
        {
            this.options = options;
        }
    }

    /**
     * 参考文档输出嵌套类
     */
    public static class DifyReferenceOutput
    {
        private String docId;
        private String docName;
        private String docPath;
        private String docType;
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
}
