package com.ruoyi.system.mapper.lingdoc;

import java.util.List;
import com.ruoyi.system.domain.lingdoc.LingdocFileIndex;

/**
 * Vault文件索引Mapper接口
 * 
 * @author lingdoc
 */
public interface LingdocFileIndexMapper
{
    /**
     * 查询Vault文件索引
     * 
     * @param fileId 文件唯一ID
     * @return Vault文件索引
     */
    public LingdocFileIndex selectLingdocFileIndexById(String fileId);

    /**
     * 查询Vault文件索引列表
     * 
     * @param lingdocFileIndex Vault文件索引
     * @return Vault文件索引集合
     */
    public List<LingdocFileIndex> selectLingdocFileIndexList(LingdocFileIndex lingdocFileIndex);

    /**
     * 查询指定用户的所有子路径（用于构建目录树）
     * 
     * @param userId 用户ID
     * @return 子路径集合
     */
    public List<String> selectDistinctSubPathByUserId(Long userId);

    /**
     * 根据checksum查询文件
     * 
     * @param userId 用户ID
     * @param checksum 文件校验值
     * @return Vault文件索引
     */
    public List<LingdocFileIndex> selectLingdocFileIndexByChecksum(Long userId, String checksum);

    /**
     * 新增Vault文件索引
     * 
     * @param lingdocFileIndex Vault文件索引
     * @return 结果
     */
    public int insertLingdocFileIndex(LingdocFileIndex lingdocFileIndex);

    /**
     * 修改Vault文件索引
     * 
     * @param lingdocFileIndex Vault文件索引
     * @return 结果
     */
    public int updateLingdocFileIndex(LingdocFileIndex lingdocFileIndex);

    /**
     * 删除Vault文件索引
     * 
     * @param fileId 文件唯一ID
     * @return 结果
     */
    public int deleteLingdocFileIndexById(String fileId);

    /**
     * 批量删除Vault文件索引
     * 
     * @param fileIds 需要删除的数据ID集合
     * @return 结果
     */
    public int deleteLingdocFileIndexByIds(String[] fileIds);

    /**
     * 根据用户ID删除所有文件索引
     * 
     * @param userId 用户ID
     * @return 结果
     */
    public int deleteLingdocFileIndexByUserId(Long userId);

    /**
     * 查询指定用户下有重复内容的checksum列表
     * 
     * @param userId 用户ID
     * @return checksum列表
     */
    public List<String> selectDuplicateChecksums(Long userId);

    /**
     * 按子路径前缀查询文件（用于删除文件夹时级联查询）
     * 
     * @param userId 用户ID
     * @param subPath 子路径前缀
     * @return 文件索引集合
     */
    public List<LingdocFileIndex> selectBySubPathPrefix(Long userId, String subPath);
}
