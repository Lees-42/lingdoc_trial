package com.ruoyi.system.mapper.lingdoc;

import java.util.List;
import com.ruoyi.system.domain.lingdoc.LingdocFileVersion;

/**
 * 文件版本记录Mapper接口
 * 
 * @author lingdoc
 */
public interface LingdocFileVersionMapper
{
    /**
     * 查询文件版本记录
     * 
     * @param versionId 版本ID
     * @return 文件版本记录
     */
    public LingdocFileVersion selectLingdocFileVersionById(String versionId);

    /**
     * 查询文件版本记录列表
     * 
     * @param lingdocFileVersion 文件版本记录
     * @return 文件版本记录集合
     */
    public List<LingdocFileVersion> selectLingdocFileVersionList(LingdocFileVersion lingdocFileVersion);

    /**
     * 查询指定文件的最大版本号
     * 
     * @param fileId 文件ID
     * @return 最大版本号
     */
    public Integer selectMaxVersionNoByFileId(String fileId);

    /**
     * 新增文件版本记录
     * 
     * @param lingdocFileVersion 文件版本记录
     * @return 结果
     */
    public int insertLingdocFileVersion(LingdocFileVersion lingdocFileVersion);

    /**
     * 修改文件版本记录
     * 
     * @param lingdocFileVersion 文件版本记录
     * @return 结果
     */
    public int updateLingdocFileVersion(LingdocFileVersion lingdocFileVersion);

    /**
     * 删除文件版本记录
     * 
     * @param versionId 版本ID
     * @return 结果
     */
    public int deleteLingdocFileVersionById(String versionId);

    /**
     * 根据文件ID删除所有版本记录
     * 
     * @param fileId 文件ID
     * @return 结果
     */
    public int deleteLingdocFileVersionByFileId(String fileId);

    /**
     * 批量删除文件版本记录
     * 
     * @param versionIds 需要删除的数据ID集合
     * @return 结果
     */
    public int deleteLingdocFileVersionByIds(String[] versionIds);
}
