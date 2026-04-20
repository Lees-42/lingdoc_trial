package com.ruoyi.lingdoc.ai.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ruoyi.lingdoc.ai.domain.entity.KbDocument;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 文档Mapper
 */
public interface KbDocumentMapper extends BaseMapper<KbDocument> {

    /**
     * 根据知识库ID查询文档列表
     */
    @Select("SELECT * FROM kb_document WHERE kb_id = #{kbId} AND process_status != 3 ORDER BY created_at DESC")
    List<KbDocument> selectByKbId(@Param("kbId") String kbId);

    /**
     * 根据docId查询文档
     */
    @Select("SELECT * FROM kb_document WHERE doc_id = #{docId}")
    KbDocument selectByDocId(@Param("docId") String docId);

    /**
     * 更新处理状态
     */
    @Select("UPDATE kb_document SET process_status = #{status}, updated_at = NOW() WHERE doc_id = #{docId}")
    void updateStatus(@Param("docId") String docId, @Param("status") Integer status);

    /**
     * 更新处理结果
     */
    @Select("UPDATE kb_document SET process_status = #{status}, char_count = #{charCount}, chunk_count = #{chunkCount}, updated_at = NOW() WHERE doc_id = #{docId}")
    void updateProcessResult(@Param("docId") String docId, @Param("status") Integer status, 
                            @Param("charCount") Integer charCount, @Param("chunkCount") Integer chunkCount);

    /**
     * 删除知识库的所有文档
     */
    @Select("DELETE FROM kb_document WHERE kb_id = #{kbId}")
    void deleteByKbId(@Param("kbId") String kbId);
}
