package com.ruoyi.lingdoc.ai.service;

import com.ruoyi.lingdoc.ai.domain.dto.KbCreateDTO;
import com.ruoyi.lingdoc.ai.domain.dto.KbUpdateDTO;
import com.ruoyi.lingdoc.ai.domain.vo.KbKnowledgeBaseVO;

import java.util.List;

/**
 * 知识库Service接口
 */
public interface IKnowledgeBaseService {
    
    /**
     * 创建知识库
     */
    KbKnowledgeBaseVO createKnowledgeBase(KbCreateDTO dto);
    
    /**
     * 获取当前用户的知识库列表
     */
    List<KbKnowledgeBaseVO> listByCurrentUser();
    
    /**
     * 根据kbId查询知识库
     */
    KbKnowledgeBaseVO getByKbId(String kbId);
    
    /**
     * 更新知识库
     */
    boolean updateKnowledgeBase(String kbId, KbUpdateDTO dto);
    
    /**
     * 删除知识库（含级联删除）
     */
    boolean deleteKnowledgeBase(String kbId);
    
    /**
     * 更新文档计数
     */
    void updateDocCount(String kbId, int docCount, int chunkCount);
}
