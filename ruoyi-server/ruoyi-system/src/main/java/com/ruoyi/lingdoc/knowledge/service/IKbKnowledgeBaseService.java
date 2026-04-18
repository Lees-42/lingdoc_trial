package com.ruoyi.lingdoc.knowledge.service;

import com.ruoyi.lingdoc.knowledge.domain.KbKnowledgeBase;
import com.ruoyi.lingdoc.knowledge.domain.dto.KbKnowledgeBaseDTO;
import com.ruoyi.lingdoc.knowledge.domain.vo.KbKnowledgeBaseVO;
import com.ruoyi.common.core.page.TableDataInfo;

import java.util.List;

/**
 * 知识库Service接口
 */
public interface IKbKnowledgeBaseService {

    /**
     * 查询知识库列表
     */
    TableDataInfo listKbKnowledgeBase(KbKnowledgeBaseDTO dto);

    /**
     * 查询知识库详情
     */
    KbKnowledgeBaseVO getKbKnowledgeBase(String kbId);

    /**
     * 新增知识库
     */
    int addKbKnowledgeBase(KbKnowledgeBaseDTO dto);

    /**
     * 修改知识库
     */
    int updateKbKnowledgeBase(KbKnowledgeBaseDTO dto);

    /**
     * 删除知识库
     */
    int delKbKnowledgeBase(String kbId);

    /**
     * 获取用户可访问的知识库列表
     */
    List<KbKnowledgeBaseVO> listAccessibleKb(String userId);
}
