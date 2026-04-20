package com.ruoyi.lingdoc.ai.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ruoyi.lingdoc.ai.domain.entity.KbDocumentChunk;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 文档分块Mapper
 */
public interface KbDocumentChunkMapper extends BaseMapper<KbDocumentChunk> {

    /**
     * 根据知识库ID查询分块列表
     */
    @Select("SELECT * FROM kb_document_chunk WHERE kb_id = #{kbId} AND status = 1 ORDER BY file_id, chunk_index")
    List<KbDocumentChunk> selectByKbId(@Param("kbId") String kbId);

    /**
     * 根据文件ID查询分块列表
     */
    @Select("SELECT * FROM kb_document_chunk WHERE file_id = #{fileId} AND status = 1 ORDER BY chunk_index")
    List<KbDocumentChunk> selectByFileId(@Param("fileId") String fileId);

    /**
     * 全文检索
     */
    @Select("SELECT * FROM kb_document_chunk WHERE kb_id = #{kbId} AND status = 1 " +
            "AND MATCH(chunk_text) AGAINST(#{keywords} IN BOOLEAN MODE) " +
            "LIMIT #{limit}")
    List<KbDocumentChunk> fullTextSearch(@Param("kbId") String kbId, @Param("keywords") String keywords, @Param("limit") int limit);

    /**
     * 删除知识库的所有分块
     */
    @Select("DELETE FROM kb_document_chunk WHERE kb_id = #{kbId}")
    void deleteByKbId(@Param("kbId") String kbId);

    /**
     * 删除文件的所有分块
     */
    @Select("DELETE FROM kb_document_chunk WHERE file_id = #{fileId}")
    void deleteByFileId(@Param("fileId") String fileId);

    /**
     * 根据ID查询分块
     */
    @Select("SELECT * FROM kb_document_chunk WHERE chunk_id = #{chunkId}")
    KbDocumentChunk selectByChunkId(@Param("chunkId") String chunkId);
}
