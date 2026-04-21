package com.ruoyi.system.service.lingdoc;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import org.springframework.web.multipart.MultipartFile;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.system.domain.lingdoc.LingdocFileIndex;
import com.ruoyi.system.domain.lingdoc.LingdocFileVersion;

/**
 * Vault文件管理服务层接口
 * 
 * @author lingdoc
 */
public interface ILingdocVaultService
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
     * @param lingdocFileIndex 查询条件
     * @return Vault文件索引集合
     */
    public List<LingdocFileIndex> selectLingdocFileIndexList(LingdocFileIndex lingdocFileIndex);

    /**
     * 获取目录树
     * 
     * @param userId 用户ID
     * @return 目录树结构
     */
    public List<Map<String, Object>> getVaultTree(Long userId);

    /**
     * 获取文件文本内容
     * 
     * @param fileId 文件ID
     * @param userId 用户ID
     * @return 文件内容信息
     */
    public AjaxResult getFileContent(String fileId, Long userId) throws IOException;

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
     * 重命名文件
     * 
     * @param fileId 文件ID
     * @param newName 新文件名
     * @param userId 用户ID
     * @return 结果
     */
    public int renameFile(String fileId, String newName, Long userId) throws IOException;

    /**
     * 移动文件
     * 
     * @param fileId 文件ID
     * @param targetSubPath 目标子路径
     * @param userId 用户ID
     * @return 结果
     */
    public int moveFile(String fileId, String targetSubPath, Long userId) throws IOException;

    /**
     * 删除Vault文件索引
     * 
     * @param fileId 文件唯一ID
     * @param userId 用户ID
     * @return 结果
     */
    public int deleteLingdocFileIndexById(String fileId, Long userId) throws IOException;

    /**
     * 批量删除Vault文件索引
     * 
     * @param fileIds 需要删除的文件ID数组
     * @param userId 用户ID
     * @return 结果
     */
    public int deleteLingdocFileIndexByIds(String[] fileIds, Long userId) throws IOException;

    /**
     * 新建文件夹
     * 
     * @param subPath 子路径
     * @param userId 用户ID
     * @return 结果
     */
    public int createFolder(String subPath, Long userId);

    /**
     * 手动触发Vault扫描同步
     * 
     * @param userId 用户ID
     * @return 同步统计结果
     */
    public Map<String, Integer> syncVault(Long userId);

    /**
     * 查询重复文件列表
     * 
     * @param userId 用户ID
     * @return 按checksum分组的重复文件列表
     */
    public List<Map<String, Object>> getDuplicateFiles(Long userId);

    /**
     * 查询文件版本列表
     * 
     * @param fileId 文件ID
     * @return 版本列表
     */
    public List<LingdocFileVersion> selectFileVersionList(String fileId);

    /**
     * 记录文件版本
     * 
     * @param fileId 文件ID
     * @param snapshotPath 快照路径
     * @param operationType 操作类型
     * @param operatorId 操作人
     * @return 结果
     */
    public int recordFileVersion(String fileId, String snapshotPath, String operationType, Long operatorId);

    /**
     * 上传文件到Vault
     * 
     * @param file 上传的文件
     * @param subPath 目标子路径（相对于documents）
     * @param userId 用户ID
     * @return 上传后的文件索引信息
     */
    public LingdocFileIndex uploadFile(org.springframework.web.multipart.MultipartFile file, String subPath, Long userId) throws IOException;
}
