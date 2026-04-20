package com.ruoyi.lingdoc.ai.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ruoyi.lingdoc.ai.domain.entity.KbKnowledgeBase;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 知识库Mapper
 */
public interface KbKnowledgeBaseMapper extends BaseMapper<KbKnowledgeBase> {

    /**
     * 根据kbId查询知识库
     */
    @Select("SELECT * FROM kb_knowledge_base WHERE kb_id = #{kbId} AND status != 0")
    KbKnowledgeBase selectByKbId(@Param("kbId") String kbId);

    /**
     * 根据用户UUID查询知识库列表
     */
    @Select("SELECT * FROM kb_knowledge_base WHERE user_uuid = #{userUuid} AND status != 0 ORDER BY created_at DESC")
    List<KbKnowledgeBase> selectByUserUuid(@Param("userUuid") String userUuid);

    /**
     * 更新知识库文档计数
     */
    @Select("UPDATE kb_knowledge_base SET doc_count = #{docCount}, chunk_count = #{chunkCount}, updated_at = NOW() WHERE kb_id = #{kbId}")
    void updateDocCount(@Param("kbId") String kbId, @Param("docCount") int docCount, @Param("chunkCount") int chunkCount);
}
