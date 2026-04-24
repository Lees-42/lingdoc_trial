package com.ruoyi.system.service.lingdoc;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import com.ruoyi.system.domain.lingdoc.LingdocFileIndex;
import com.ruoyi.system.domain.lingdoc.ai.FormDocQueryRequest;
import com.ruoyi.system.domain.lingdoc.ai.FormDocQueryResponse;
import com.ruoyi.system.mapper.lingdoc.LingdocFileIndexMapper;

/**
 * 文档查询服务单元测试
 */
class FormDocQueryServiceImplTest
{
    @Mock
    private LingdocFileIndexMapper fileIndexMapper;

    @InjectMocks
    private FormDocQueryServiceImpl queryService;

    @BeforeEach
    void setUp()
    {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void testQueryDocsSuccess()
    {
        // Mock 文件名匹配返回成绩单
        when(fileIndexMapper.selectLingdocFileIndexList(argThat(q ->
                q != null && "成绩单.pdf".equals(q.getFileName()))))
            .thenReturn(List.of(buildFile("doc_001", "成绩单.pdf", "学习/成绩单.pdf", "姓名：张三，学号：2023001，GPA：3.8")));

        // Mock 路径/内容匹配也返回同一文件（模拟多策略命中）
        when(fileIndexMapper.selectLingdocFileIndexList(argThat(q ->
                q != null && q.getParams().get("keyword") != null)))
            .thenReturn(List.of(
                buildFile("doc_001", "成绩单.pdf", "学习/成绩单.pdf", "姓名：张三，学号：2023001，GPA：3.8"),
                buildFile("doc_002", "简历.docx", "个人/简历.docx", "张三，计算机专业，电话：138****5678")
            ));

        FormDocQueryRequest request = new FormDocQueryRequest();
        request.setFieldNames(List.of("姓名", "学号", "GPA"));
        request.setTableType("奖学金申请表");
        request.setMaxDocs(3);
        request.setMaxCharsPerDoc(2000);

        FormDocQueryResponse response = queryService.queryDocs(request);

        assertNotNull(response);
        assertTrue(response.getTotalMatched() >= 1);
        assertNotNull(response.getDocs());
        assertNotNull(response.getQueryTimeMs());

        // 成绩单应该排在最前面（文件名匹配权重最高）
        if (!response.getDocs().isEmpty())
        {
            assertEquals("成绩单.pdf", response.getDocs().get(0).getDocName());
            assertNotNull(response.getDocs().get(0).getContent());
            assertNotNull(response.getDocs().get(0).getMatchReason());
        }
    }

    @Test
    void testQueryDocsEmptyKeywords()
    {
        FormDocQueryRequest request = new FormDocQueryRequest();
        request.setFieldNames(new ArrayList<>());
        request.setMaxDocs(3);

        FormDocQueryResponse response = queryService.queryDocs(request);

        assertNotNull(response);
        assertTrue(response.getDocs().isEmpty());
        assertEquals(0, response.getTotalMatched());
    }

    @Test
    void testQueryDocsNoMatch()
    {
        when(fileIndexMapper.selectLingdocFileIndexList(any())).thenReturn(new ArrayList<>());

        FormDocQueryRequest request = new FormDocQueryRequest();
        request.setFieldNames(List.of("不存在字段"));
        request.setMaxDocs(3);

        FormDocQueryResponse response = queryService.queryDocs(request);

        assertNotNull(response);
        assertTrue(response.getDocs().isEmpty());
        assertEquals(0, response.getTotalMatched());
    }

    @Test
    void testContentTruncation()
    {
        String longContent = "A".repeat(5000);
        when(fileIndexMapper.selectLingdocFileIndexList(any()))
            .thenReturn(List.of(buildFile("doc_001", "长文档.txt", "", longContent)));

        FormDocQueryRequest request = new FormDocQueryRequest();
        request.setFieldNames(List.of("测试"));
        request.setMaxDocs(1);
        request.setMaxCharsPerDoc(100);

        FormDocQueryResponse response = queryService.queryDocs(request);

        assertNotNull(response);
        assertEquals(1, response.getDocs().size());
        assertEquals(103, response.getDocs().get(0).getContent().length()); // 100 + "..."
        assertTrue(response.getDocs().get(0).getContent().endsWith("..."));
    }

    // ---------- 辅助方法 ----------

    private LingdocFileIndex buildFile(String fileId, String fileName, String subPath, String content)
    {
        LingdocFileIndex file = new LingdocFileIndex();
        file.setFileId(fileId);
        file.setFileName(fileName);
        file.setSubPath(subPath);
        file.setFileContent(content);
        return file;
    }
}
