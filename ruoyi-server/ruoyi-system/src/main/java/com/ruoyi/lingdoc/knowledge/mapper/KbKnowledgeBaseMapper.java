package com.ruoyi.lingdoc.knowledge.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ruoyi.lingdoc.knowledge.domain.KbKnowledgeBase;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 知识库Mapper
 */
public interface KbKnowledgeBaseMapper extends BaseMapper<KbKnowledgeBase> {

    /**
     * 根据ID查询知识库
     */
    @Select("SELECT * FROM kb_knowledge_base WHERE kb_id = #{kbId} AND status = '0'")
    KbKnowledgeBase selectByKbId(@Param("kbId") String kbId);

    /**
     * 查询用户的知识库列表
     */
    @Select("SELECT * FROM kb_knowledge_base WHERE user_uuid = #{userId} AND status = '0' ORDER BY create_time DESC")
    List<KbKnowledgeBase> selectByUserId(@Param("userId") String userId);

    /**
     * 检查知识库名称是否重复
     */
    @Select("SELECT COUNT(*) FROM kb_knowledge_base WHERE user_uuid = #{userId} AND kb_name = #{kbName} AND status = '0'")
    int countByName(@Param("userId") String userId, @Param("kbName") String kbName);
}
