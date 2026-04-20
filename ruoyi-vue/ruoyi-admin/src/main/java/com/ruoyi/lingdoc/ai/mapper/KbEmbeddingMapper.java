package com.ruoyi.lingdoc.ai.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ruoyi.lingdoc.ai.domain.entity.KbEmbedding;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 向量嵌入Mapper
 */
public interface KbEmbeddingMapper extends BaseMapper<KbEmbedding> {

    /**
     * 根据知识库ID查询向量列表
     */
    @Select("SELECT * FROM kb_embedding WHERE kb_id = #{kbId}")
    List<KbEmbedding> selectByKbId(@Param("kbId") String kbId);

    /**
     * 根据分块ID查询向量
     */
    @Select("SELECT * FROM kb_embedding WHERE chunk_id = #{chunkId} LIMIT 1")
    KbEmbedding selectByChunkId(@Param("chunkId") String chunkId);

    /**
     * 删除知识库的所有向量
     */
    @Select("DELETE FROM kb_embedding WHERE kb_id = #{kbId}")
    void deleteByKbId(@Param("kbId") String kbId);
}
