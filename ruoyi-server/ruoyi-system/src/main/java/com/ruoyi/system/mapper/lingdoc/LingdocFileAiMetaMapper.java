package com.ruoyi.system.mapper.lingdoc;

import java.util.List;
import com.ruoyi.system.domain.lingdoc.LingdocFileAiMeta;

/**
 * 文件AI元数据Mapper接口（脚手架）
 * 
 * @author lingdoc
 */
public interface LingdocFileAiMetaMapper
{
    /**
     * 查询文件AI元数据
     * 
     * @param fileId 文件ID
     * @return 文件AI元数据
     */
    public LingdocFileAiMeta selectLingdocFileAiMetaById(String fileId);

    /**
     * 查询文件AI元数据列表
     * 
     * @param lingdocFileAiMeta 文件AI元数据
     * @return 文件AI元数据集合
     */
    public List<LingdocFileAiMeta> selectLingdocFileAiMetaList(LingdocFileAiMeta lingdocFileAiMeta);

    /**
     * 新增文件AI元数据
     * 
     * @param lingdocFileAiMeta 文件AI元数据
     * @return 结果
     */
    public int insertLingdocFileAiMeta(LingdocFileAiMeta lingdocFileAiMeta);

    /**
     * 修改文件AI元数据
     * 
     * @param lingdocFileAiMeta 文件AI元数据
     * @return 结果
     */
    public int updateLingdocFileAiMeta(LingdocFileAiMeta lingdocFileAiMeta);

    /**
     * 删除文件AI元数据
     * 
     * @param fileId 文件ID
     * @return 结果
     */
    public int deleteLingdocFileAiMetaById(String fileId);

    /**
     * 批量删除文件AI元数据
     * 
     * @param fileIds 需要删除的数据ID集合
     * @return 结果
     */
    public int deleteLingdocFileAiMetaByIds(String[] fileIds);
}
