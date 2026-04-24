package com.ruoyi.system.domain.lingdoc.ai;

import java.util.List;

/**
 * 表格填写助手 - 文档查询响应
 * <p>
 * 对应 /lingdoc/ai/form/query-docs 接口的 data 层级。
 *
 * @author lingdoc
 */
public class FormDocQueryResponse
{
    /** 匹配到的参考文档列表 */
    private List<DocSnippet> docs;

    /** 匹配到的文档总数 */
    private Integer totalMatched;

    /** 查询耗时（毫秒） */
    private Long queryTimeMs;

    public List<DocSnippet> getDocs()
    {
        return docs;
    }

    public void setDocs(List<DocSnippet> docs)
    {
        this.docs = docs;
    }

    public Integer getTotalMatched()
    {
        return totalMatched;
    }

    public void setTotalMatched(Integer totalMatched)
    {
        this.totalMatched = totalMatched;
    }

    public Long getQueryTimeMs()
    {
        return queryTimeMs;
    }

    public void setQueryTimeMs(Long queryTimeMs)
    {
        this.queryTimeMs = queryTimeMs;
    }

    /**
     * 文档片段
     */
    public static class DocSnippet
    {
        /** 文档ID */
        private String docId;

        /** 文档名称 */
        private String docName;

        /** 文档在 Vault 中的路径 */
        private String docPath;

        /** 文档文本内容（已截断） */
        private String content;

        /** 匹配原因说明 */
        private String matchReason;

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

        public String getContent()
        {
            return content;
        }

        public void setContent(String content)
        {
            this.content = content;
        }

        public String getMatchReason()
        {
            return matchReason;
        }

        public void setMatchReason(String matchReason)
        {
            this.matchReason = matchReason;
        }
    }
}
