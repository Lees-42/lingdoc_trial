package com.ruoyi.system.service.lingdoc;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import com.ruoyi.system.domain.lingdoc.LingdocTag;
import com.ruoyi.system.domain.lingdoc.LingdocTagBinding;
import com.ruoyi.system.mapper.lingdoc.LingdocTagBindingMapper;
import com.ruoyi.system.mapper.lingdoc.LingdocTagMapper;

/**
 * 标签管理服务单元测试
 */
class LingdocTagServiceTest
{
    @Mock
    private LingdocTagMapper tagMapper;

    @Mock
    private LingdocTagBindingMapper tagBindingMapper;

    @InjectMocks
    private LingdocTagServiceImpl tagService;

    @BeforeEach
    void setUp()
    {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void testSelectLingdocTagById()
    {
        LingdocTag expected = new LingdocTag();
        expected.setTagId("t_001");
        expected.setTagName("奖学金");
        when(tagMapper.selectLingdocTagById("t_001")).thenReturn(expected);

        LingdocTag result = tagService.selectLingdocTagById("t_001");

        assertNotNull(result);
        assertEquals("奖学金", result.getTagName());
    }

    @Test
    void testInsertLingdocTag()
    {
        LingdocTag tag = new LingdocTag();
        tag.setTagName("操作系统");
        when(tagMapper.insertLingdocTag(any(LingdocTag.class))).thenReturn(1);

        int result = tagService.insertLingdocTag(tag);

        assertEquals(1, result);
        assertNotNull(tag.getTagId()); // 应自动生成UUID
    }

    @Test
    void testDeleteLingdocTagById()
    {
        LingdocTagBinding binding = new LingdocTagBinding();
        binding.setBindingId("b_001");
        binding.setTagId("t_001");
        when(tagBindingMapper.selectLingdocTagBindingList(any(LingdocTagBinding.class)))
            .thenReturn(Arrays.asList(binding));
        when(tagBindingMapper.deleteLingdocTagBindingById("b_001")).thenReturn(1);
        when(tagMapper.deleteLingdocTagById("t_001")).thenReturn(1);

        int result = tagService.deleteLingdocTagById("t_001");

        assertEquals(1, result);
        verify(tagBindingMapper).deleteLingdocTagBindingById("b_001");
        verify(tagMapper).deleteLingdocTagById("t_001");
    }

    @Test
    void testSelectTagsByTarget()
    {
        LingdocTagBinding binding = new LingdocTagBinding();
        binding.setBindingId("b_001");
        binding.setTagId("t_001");
        binding.setBindType("0");
        
        LingdocTag tag = new LingdocTag();
        tag.setTagId("t_001");
        tag.setTagName("奖学金");
        tag.setTagColor("#409EFF");
        
        when(tagBindingMapper.selectLingdocTagBindingByTarget("F", "f_001"))
            .thenReturn(Arrays.asList(binding));
        when(tagMapper.selectLingdocTagById("t_001")).thenReturn(tag);

        List<Map<String, Object>> result = tagService.selectTagsByTarget("F", "f_001");

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("奖学金", result.get(0).get("tagName"));
        assertEquals("0", result.get(0).get("bindType"));
    }

    @Test
    void testInsertLingdocTagBinding()
    {
        LingdocTagBinding binding = new LingdocTagBinding();
        binding.setTargetType("F");
        binding.setTargetId("f_001");
        binding.setTagId("t_001");
        when(tagBindingMapper.insertLingdocTagBinding(any(LingdocTagBinding.class))).thenReturn(1);

        int result = tagService.insertLingdocTagBinding(binding);

        assertEquals(1, result);
        assertNotNull(binding.getBindingId());
    }

    @Test
    void testDeleteLingdocTagBindingByTarget()
    {
        when(tagBindingMapper.deleteLingdocTagBindingByTarget("F", "f_001")).thenReturn(2);

        int result = tagService.deleteLingdocTagBindingByTarget("F", "f_001");

        assertEquals(2, result);
        verify(tagBindingMapper).deleteLingdocTagBindingByTarget("F", "f_001");
    }
}
