package com.ruoyi.system.mapper.lingdoc;

import java.util.List;
import com.ruoyi.system.domain.lingdoc.LingdocDesensitizedFile;

/**
 * 脱敏文件Mapper接口
 * 
 * @author lingdoc
 */
public interface LingdocDesensitizedFileMapper
{
    /**
     * 查询脱敏文件
     * 
     * @param desId 脱敏记录ID
     * @return 脱敏文件
     */
    public LingdocDesensitizedFile selectLingdocDesensitizedFileById(String desId);

    /**
     * 根据原文件ID查询脱敏文件
     * 
     * @param fileId 原文件ID
     * @return 脱敏文件
     */
    public LingdocDesensitizedFile selectLingdocDesensitizedFileByFileId(String fileId);

    /**
     * 查询脱敏文件列表
     * 
     * @param lingdocDesensitizedFile 脱敏文件
     * @return 脱敏文件集合
     */
    public List<LingdocDesensitizedFile> selectLingdocDesensitizedFileList(LingdocDesensitizedFile lingdocDesensitizedFile);

    /**
     * 新增脱敏文件
     * 
     * @param lingdocDesensitizedFile 脱敏文件
     * @return 结果
     */
    public int insertLingdocDesensitizedFile(LingdocDesensitizedFile lingdocDesensitizedFile);

    /**
     * 修改脱敏文件
     * 
     * @param lingdocDesensitizedFile 脱敏文件
     * @return 结果
     */
    public int updateLingdocDesensitizedFile(LingdocDesensitizedFile lingdocDesensitizedFile);

    /**
     * 删除脱敏文件
     * 
     * @param desId 脱敏记录ID
     * @return 结果
     */
    public int deleteLingdocDesensitizedFileById(String desId);

    /**
     * 根据原文件ID删除脱敏文件
     * 
     * @param fileId 原文件ID
     * @return 结果
     */
    public int deleteLingdocDesensitizedFileByFileId(String fileId);

    /**
     * 批量删除脱敏文件
     * 
     * @param desIds 需要删除的数据ID集合
     * @return 结果
     */
    public int deleteLingdocDesensitizedFileByIds(String[] desIds);
}
