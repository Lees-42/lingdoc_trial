package com.ruoyi.system.service.lingdoc;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.system.domain.lingdoc.LingdocFileIndex;
import com.ruoyi.system.domain.lingdoc.ai.FormDocQueryRequest;
import com.ruoyi.system.domain.lingdoc.ai.FormDocQueryResponse;
import com.ruoyi.system.mapper.lingdoc.LingdocFileIndexMapper;

/**
 * 表格填写助手 - 文档查询服务实现
 * <p>
 * 多策略检索 Vault 中的参考文档：
 * 1. 字段名 → 文档类型关键词映射
 * 2. 文件名匹配、路径匹配、内容搜索
 * 3. 加权去重排序
 *
 * @author lingdoc
 */
@Service
public class FormDocQueryServiceImpl implements IFormDocQueryService
{
    private static final Logger log = LoggerFactory.getLogger(FormDocQueryServiceImpl.class);

    @Autowired
    private LingdocFileIndexMapper fileIndexMapper;

    /** 字段名 → 文档类型关键词映射表 */
    private static final Map<String, List<String>> FIELD_KEYWORD_MAP = new HashMap<>();

    static
    {
        // 学业相关
        FIELD_KEYWORD_MAP.put("姓名", List.of("简历", "证件", "身份证", "成绩单", "学籍"));
        FIELD_KEYWORD_MAP.put("学号", List.of("成绩单", "学籍", "学生证", "在读证明"));
        FIELD_KEYWORD_MAP.put("gpa", List.of("成绩单", "成绩", "绩点"));
        FIELD_KEYWORD_MAP.put("绩点", List.of("成绩单", "成绩", "绩点"));
        FIELD_KEYWORD_MAP.put("成绩", List.of("成绩单", "成绩"));
        FIELD_KEYWORD_MAP.put("排名", List.of("成绩单", "排名", "证明"));
        FIELD_KEYWORD_MAP.put("获奖", List.of("获奖", "证书", "荣誉", "奖状"));
        FIELD_KEYWORD_MAP.put("奖项", List.of("获奖", "证书", "荣誉", "奖状"));
        FIELD_KEYWORD_MAP.put("证书", List.of("证书", "证明", "资格"));
        FIELD_KEYWORD_MAP.put("论文", List.of("论文", "发表", "期刊"));
        FIELD_KEYWORD_MAP.put("专利", List.of("专利", "发明", "知识产权"));
        FIELD_KEYWORD_MAP.put("项目", List.of("项目", "科研", "课题"));
        FIELD_KEYWORD_MAP.put("实习", List.of("实习", "工作证明", "证明"));
        FIELD_KEYWORD_MAP.put("志愿", List.of("志愿", "服务", "活动"));
        FIELD_KEYWORD_MAP.put("活动", List.of("活动", "社团", "经历"));

        // 求职相关
        FIELD_KEYWORD_MAP.put("公司", List.of("合同", " offer", "证明", "离职"));
        FIELD_KEYWORD_MAP.put("职位", List.of("简历", "合同", "证明"));
        FIELD_KEYWORD_MAP.put("薪资", List.of("合同", "薪资", "证明", "银行"));
        FIELD_KEYWORD_MAP.put("入职", List.of("合同", " offer", "入职"));
        FIELD_KEYWORD_MAP.put("离职", List.of("离职", "证明", "合同"));

        // 财务相关
        FIELD_KEYWORD_MAP.put("金额", List.of("发票", "报销", "合同", "银行", "流水"));
        FIELD_KEYWORD_MAP.put("发票", List.of("发票", "报销", "税务"));
        FIELD_KEYWORD_MAP.put("报销", List.of("报销", "发票", "单据"));
        FIELD_KEYWORD_MAP.put("银行", List.of("银行", "流水", "账户", "卡"));
        FIELD_KEYWORD_MAP.put("账号", List.of("银行", "账户", "卡号"));

        // 通用
        FIELD_KEYWORD_MAP.put("日期", List.of("证明", "合同", "申请"));
        FIELD_KEYWORD_MAP.put("地址", List.of("证明", "证件", "合同"));
        FIELD_KEYWORD_MAP.put("电话", List.of("简历", "联系方式", "合同"));
        FIELD_KEYWORD_MAP.put("邮箱", List.of("简历", "联系方式"));
    }

    @Override
    public FormDocQueryResponse queryDocs(FormDocQueryRequest request)
    {
        long startTime = System.currentTimeMillis();

        // 1. 构建关键词列表
        Set<String> keywords = buildKeywords(request.getFieldNames(), request.getTableType());
        if (keywords.isEmpty())
        {
            return emptyResponse(startTime);
        }

        log.debug("文档查询关键词: {}", keywords);

        // 2. 多策略检索候选文档
        Map<String, CandidateDoc> candidates = new HashMap<>();

        // 策略 A：文件名匹配
        searchByFileName(keywords, candidates);

        // 策略 B：路径匹配
        searchByPath(keywords, candidates);

        // 策略 C：内容搜索
        searchByContent(keywords, candidates);

        if (candidates.isEmpty())
        {
            return emptyResponse(startTime);
        }

        // 3. 按相关性分数排序，取 Top-N
        int maxDocs = request.getMaxDocs() != null ? request.getMaxDocs() : 3;
        int maxChars = request.getMaxCharsPerDoc() != null ? request.getMaxCharsPerDoc() : 2000;

        List<CandidateDoc> sorted = candidates.values().stream()
                .sorted(Comparator.comparingDouble(CandidateDoc::getScore).reversed())
                .limit(maxDocs)
                .collect(Collectors.toList());

        // 4. 组装响应
        FormDocQueryResponse response = new FormDocQueryResponse();
        List<FormDocQueryResponse.DocSnippet> docs = new ArrayList<>();

        for (CandidateDoc cand : sorted)
        {
            FormDocQueryResponse.DocSnippet snippet = new FormDocQueryResponse.DocSnippet();
            snippet.setDocId(cand.getFileId());
            snippet.setDocName(cand.getFileName());
            snippet.setDocPath(cand.getSubPath());
            snippet.setContent(truncateContent(cand.getContent(), maxChars));
            snippet.setMatchReason(buildMatchReason(cand, keywords));
            docs.add(snippet);
        }

        response.setDocs(docs);
        response.setTotalMatched(candidates.size());
        response.setQueryTimeMs(System.currentTimeMillis() - startTime);

        log.info("文档查询完成, keywords={}, candidates={}, returned={}, timeMs={}",
                keywords, candidates.size(), docs.size(), response.getQueryTimeMs());

        return response;
    }

    // ---------- 检索策略 ----------

    private void searchByFileName(Set<String> keywords, Map<String, CandidateDoc> candidates)
    {
        for (String keyword : keywords)
        {
            LingdocFileIndex query = new LingdocFileIndex();
            query.setFileName(keyword);
            List<LingdocFileIndex> list = fileIndexMapper.selectLingdocFileIndexList(query);
            for (LingdocFileIndex file : list)
            {
                CandidateDoc cand = candidates.computeIfAbsent(file.getFileId(), k -> new CandidateDoc(file));
                cand.addScore(3.0); // 文件名匹配权重最高
                cand.addMatch("文件名");
            }
        }
    }

    private void searchByPath(Set<String> keywords, Map<String, CandidateDoc> candidates)
    {
        for (String keyword : keywords)
        {
            // 通过 sub_path 模糊查询：使用 params.keyword 参数（Mapper 中已实现）
            LingdocFileIndex query = new LingdocFileIndex();
            query.getParams().put("keyword", keyword);
            List<LingdocFileIndex> list = fileIndexMapper.selectLingdocFileIndexList(query);
            for (LingdocFileIndex file : list)
            {
                // 排除已按文件名命中的（避免重复加分过多）
                CandidateDoc cand = candidates.computeIfAbsent(file.getFileId(), k -> new CandidateDoc(file));
                if (!cand.hasMatch("文件名"))
                {
                    cand.addScore(1.5); // 路径匹配权重中等
                    cand.addMatch("路径");
                }
            }
        }
    }

    private void searchByContent(Set<String> keywords, Map<String, CandidateDoc> candidates)
    {
        for (String keyword : keywords)
        {
            LingdocFileIndex query = new LingdocFileIndex();
            query.getParams().put("keyword", keyword);
            List<LingdocFileIndex> list = fileIndexMapper.selectLingdocFileIndexList(query);
            for (LingdocFileIndex file : list)
            {
                CandidateDoc cand = candidates.computeIfAbsent(file.getFileId(), k -> new CandidateDoc(file));
                if (!cand.hasMatch("文件名") && !cand.hasMatch("路径"))
                {
                    cand.addScore(1.0); // 内容匹配权重最低
                    cand.addMatch("内容");
                }
            }
        }
    }

    // ---------- 工具方法 ----------

    private Set<String> buildKeywords(List<String> fieldNames, String tableType)
    {
        Set<String> keywords = new HashSet<>();

        if (fieldNames != null)
        {
            for (String field : fieldNames)
            {
                if (StringUtils.isEmpty(field))
                {
                    continue;
                }
                String lowerField = field.toLowerCase().trim();

                // 直接加入字段名本身（用于内容搜索）
                keywords.add(field.trim());

                // 加入映射的关键词
                List<String> mapped = FIELD_KEYWORD_MAP.get(lowerField);
                if (mapped != null)
                {
                    keywords.addAll(mapped);
                }

                // 模糊匹配：检查 field 是否包含映射表中的键
                for (Map.Entry<String, List<String>> entry : FIELD_KEYWORD_MAP.entrySet())
                {
                    if (lowerField.contains(entry.getKey()))
                    {
                        keywords.addAll(entry.getValue());
                    }
                }
            }
        }

        // tableType 也作为关键词
        if (StringUtils.isNotEmpty(tableType))
        {
            keywords.add(tableType.trim());
        }

        // 过滤空字符串和过短词
        return keywords.stream()
                .filter(k -> StringUtils.isNotEmpty(k) && k.length() >= 2)
                .collect(Collectors.toSet());
    }

    private String truncateContent(String content, int maxChars)
    {
        if (content == null)
        {
            return "";
        }
        if (content.length() <= maxChars)
        {
            return content;
        }
        return content.substring(0, maxChars) + "...";
    }

    private String buildMatchReason(CandidateDoc cand, Set<String> keywords)
    {
        StringBuilder sb = new StringBuilder();
        sb.append("通过").append(String.join("、", cand.getMatchTypes()));
        sb.append("匹配到文档「").append(cand.getFileName()).append("」");
        return sb.toString();
    }

    private FormDocQueryResponse emptyResponse(long startTime)
    {
        FormDocQueryResponse response = new FormDocQueryResponse();
        response.setDocs(new ArrayList<>());
        response.setTotalMatched(0);
        response.setQueryTimeMs(System.currentTimeMillis() - startTime);
        return response;
    }

    // ---------- 内部类 ----------

    /**
     * 候选文档（带分数）
     */
    private static class CandidateDoc
    {
        private final LingdocFileIndex file;
        private double score;
        private final Set<String> matchTypes = new HashSet<>();

        CandidateDoc(LingdocFileIndex file)
        {
            this.file = file;
            this.score = 0;
        }

        void addScore(double points)
        {
            this.score += points;
        }

        void addMatch(String type)
        {
            this.matchTypes.add(type);
        }

        boolean hasMatch(String type)
        {
            return this.matchTypes.contains(type);
        }

        String getFileId()
        {
            return file.getFileId();
        }

        String getFileName()
        {
            return file.getFileName();
        }

        String getSubPath()
        {
            return file.getSubPath();
        }

        String getContent()
        {
            return file.getFileContent();
        }

        double getScore()
        {
            return score;
        }

        Set<String> getMatchTypes()
        {
            return matchTypes;
        }
    }
}
