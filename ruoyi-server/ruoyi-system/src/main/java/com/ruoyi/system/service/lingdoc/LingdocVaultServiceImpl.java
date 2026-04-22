package com.ruoyi.system.service.lingdoc;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.common.utils.file.FileUtils;
import com.ruoyi.common.utils.uuid.UUID;
import java.security.MessageDigest;
import java.io.FileInputStream;
import com.ruoyi.system.domain.lingdoc.LingdocFileIndex;
import com.ruoyi.system.domain.lingdoc.LingdocFileVersion;
import com.ruoyi.system.mapper.lingdoc.LingdocDesensitizedFileMapper;
import com.ruoyi.system.mapper.lingdoc.LingdocFileAiMetaMapper;
import com.ruoyi.system.mapper.lingdoc.LingdocFileIndexMapper;
import com.ruoyi.system.mapper.lingdoc.LingdocFileVersionMapper;
import com.ruoyi.system.mapper.lingdoc.LingdocTagBindingMapper;
import com.ruoyi.system.service.lingdoc.ILingdocUserRepoService;

/**
 * Vault文件管理服务层实现
 * 
 * @author lingdoc
 */
@Service
public class LingdocVaultServiceImpl implements ILingdocVaultService
{
    private static final Logger log = LoggerFactory.getLogger(LingdocVaultServiceImpl.class);

    @Autowired
    private ILingdocUserRepoService userRepoService;

    @Autowired
    private LingdocFileIndexMapper fileIndexMapper;

    @Autowired
    private LingdocFileVersionMapper fileVersionMapper;

    @Autowired
    private LingdocFileAiMetaMapper fileAiMetaMapper;

    @Autowired
    private LingdocDesensitizedFileMapper desensitizedFileMapper;

    @Autowired
    private LingdocTagBindingMapper tagBindingMapper;

    @Autowired
    private ILingdocTagService tagService;

    @Override
    public LingdocFileIndex selectLingdocFileIndexById(String fileId)
    {
        return fileIndexMapper.selectLingdocFileIndexById(fileId);
    }

    @Override
    public List<LingdocFileIndex> selectLingdocFileIndexList(LingdocFileIndex lingdocFileIndex)
    {
        return fileIndexMapper.selectLingdocFileIndexList(lingdocFileIndex);
    }

    @Override
    public List<Map<String, Object>> getVaultTree(Long userId)
    {
        String vaultRoot = userRepoService.getUserRepoPath(userId);
        Path documentsPath = Paths.get(vaultRoot, "documents");
        if (!Files.exists(documentsPath))
        {
            return new ArrayList<>();
        }
        
        List<String> subPaths = new ArrayList<>();
        try
        {
            Files.walk(documentsPath)
                .filter(Files::isDirectory)
                .map(p -> documentsPath.relativize(p).toString().replace("\\", "/"))
                .filter(s -> !s.isEmpty())
                .forEach(subPaths::add);
        }
        catch (IOException e)
        {
            log.error("扫描 Vault 目录结构失败", e);
        }
        
        return buildTreeFromSubPaths(subPaths);
    }

    /**
     * 从子路径列表构建目录树
     */
    private List<Map<String, Object>> buildTreeFromSubPaths(List<String> subPaths)
    {
        Map<String, Map<String, Object>> nodeMap = new LinkedHashMap<>();
        
        for (String subPath : subPaths)
        {
            if (StringUtils.isEmpty(subPath))
            {
                continue;
            }
            String[] parts = subPath.split("/");
            String currentPath = "";
            for (int i = 0; i < parts.length; i++)
            {
                String part = parts[i].trim();
                if (StringUtils.isEmpty(part))
                {
                    continue;
                }
                String parentPath = currentPath;
                currentPath = StringUtils.isEmpty(currentPath) ? part : currentPath + "/" + part;
                
                if (!nodeMap.containsKey(currentPath))
                {
                    Map<String, Object> node = new LinkedHashMap<>();
                    node.put("label", part);
                    node.put("value", currentPath);
                    node.put("children", new ArrayList<Map<String, Object>>());
                    nodeMap.put(currentPath, node);
                    
                    if (StringUtils.isNotEmpty(parentPath) && nodeMap.containsKey(parentPath))
                    {
                        @SuppressWarnings("unchecked")
                        List<Map<String, Object>> children = (List<Map<String, Object>>) nodeMap.get(parentPath).get("children");
                        children.add(node);
                    }
                }
            }
        }
        
        List<Map<String, Object>> result = new ArrayList<>();
        for (Map<String, Object> node : nodeMap.values())
        {
            String value = (String) node.get("value");
            boolean isRoot = !value.contains("/");
            if (isRoot)
            {
                result.add(node);
            }
        }
        return result;
    }

    @Override
    public AjaxResult getFileContent(String fileId, Long userId) throws IOException
    {
        LingdocFileIndex file = fileIndexMapper.selectLingdocFileIndexById(fileId);
        if (file == null || !userId.equals(file.getUserId()))
        {
            return AjaxResult.error("文件不存在或无权限");
        }
        
        String fileType = file.getFileType();
        List<String> textTypes = List.of("txt", "md", "csv", "json", "xml", "yaml", "yml", "html", "htm", "js", "css", "java", "py", "c", "cpp", "h", "sql");
        if (!textTypes.contains(fileType.toLowerCase()))
        {
            return AjaxResult.error(403, "该文件类型不支持在线预览");
        }
        
        String content;
        if (StringUtils.isNotEmpty(file.getContentPath()) && Files.exists(Paths.get(file.getContentPath())))
        {
            content = Files.readString(Paths.get(file.getContentPath()));
        }
        else if (StringUtils.isNotEmpty(file.getFileContent()))
        {
            content = file.getFileContent();
        }
        else if (Files.exists(Paths.get(file.getAbsPath())))
        {
            content = Files.readString(Paths.get(file.getAbsPath()));
        }
        else
        {
            return AjaxResult.error("文件内容不存在");
        }
        
        Map<String, Object> data = new HashMap<>();
        data.put("content", content);
        data.put("encoding", "UTF-8");
        data.put("lineCount", content.split("\r?\n").length);
        return AjaxResult.success(data);
    }

    @Override
    public int insertLingdocFileIndex(LingdocFileIndex lingdocFileIndex)
    {
        return fileIndexMapper.insertLingdocFileIndex(lingdocFileIndex);
    }

    @Override
    public int updateLingdocFileIndex(LingdocFileIndex lingdocFileIndex)
    {
        return fileIndexMapper.updateLingdocFileIndex(lingdocFileIndex);
    }

    @Override
    @Transactional
    public int renameFile(String fileId, String newName, Long userId) throws IOException
    {
        LingdocFileIndex file = fileIndexMapper.selectLingdocFileIndexById(fileId);
        if (file == null || !userId.equals(file.getUserId()))
        {
            throw new RuntimeException("文件不存在或无权限");
        }
        
        // 校验新文件名合法
        if (!newName.matches("[a-zA-Z0-9_\\-\\|\\.\\u4e00-\\u9fa5]+"))
        {
            throw new RuntimeException("文件名包含非法字符");
        }
        
        // 校验扩展名不变
        String oldExt = file.getFileName().substring(file.getFileName().lastIndexOf(".") + 1);
        String newExt = newName.substring(newName.lastIndexOf(".") + 1);
        if (!oldExt.equalsIgnoreCase(newExt))
        {
            throw new RuntimeException("不允许修改文件扩展名");
        }
        
        Path oldPath = Paths.get(file.getAbsPath());
        Path newPath = oldPath.resolveSibling(newName);
        
        // 物理文件重命名
        if (Files.exists(oldPath))
        {
            Files.move(oldPath, newPath, StandardCopyOption.REPLACE_EXISTING);
        }
        
        // 记录版本
        recordFileVersion(fileId, file.getAbsPath(), "1", userId);
        
        // 更新数据库
        file.setFileName(newName);
        file.setAbsPath(newPath.toString());
        String newVaultPath = file.getVaultPath().substring(0, file.getVaultPath().lastIndexOf("/") + 1) + newName;
        file.setVaultPath(newVaultPath);
        
        return fileIndexMapper.updateLingdocFileIndex(file);
    }

    @Override
    @Transactional
    public int moveFile(String fileId, String targetSubPath, Long userId) throws IOException
    {
        LingdocFileIndex file = fileIndexMapper.selectLingdocFileIndexById(fileId);
        if (file == null || !userId.equals(file.getUserId()))
        {
            throw new RuntimeException("文件不存在或无权限");
        }
        
        String vaultRoot = userRepoService.getUserRepoPath(userId);
        Path targetDir = Paths.get(vaultRoot, "documents", targetSubPath);
        if (!Files.exists(targetDir))
        {
            Files.createDirectories(targetDir);
        }

        Path oldPath = Paths.get(file.getAbsPath());
        Path newPath = targetDir.resolve(file.getFileName());

        // 物理文件移动
        if (Files.exists(oldPath))
        {
            Files.move(oldPath, newPath, StandardCopyOption.REPLACE_EXISTING);
        }
        
        // 记录版本
        recordFileVersion(fileId, file.getAbsPath(), "2", userId);
        
        // 更新数据库
        file.setSubPath(targetSubPath);
        file.setAbsPath(newPath.toString());
        String newVaultPath = StringUtils.isEmpty(targetSubPath) ? file.getFileName() : targetSubPath + "/" + file.getFileName();
        file.setVaultPath(newVaultPath);
        
        return fileIndexMapper.updateLingdocFileIndex(file);
    }

    @Override
    @Transactional
    public int deleteLingdocFileIndexById(String fileId, Long userId) throws IOException
    {
        LingdocFileIndex file = fileIndexMapper.selectLingdocFileIndexById(fileId);
        if (file == null || !userId.equals(file.getUserId()))
        {
            throw new RuntimeException("文件不存在或无权限");
        }
        
        // 删除物理文件
        if (StringUtils.isNotEmpty(file.getAbsPath()))
        {
            FileUtils.deleteFile(file.getAbsPath());
        }
        
        // 级联删除关联数据
        fileVersionMapper.deleteLingdocFileVersionByFileId(fileId);
        fileAiMetaMapper.deleteLingdocFileAiMetaById(fileId);
        desensitizedFileMapper.deleteLingdocDesensitizedFileByFileId(fileId);
        tagService.deleteLingdocTagBindingByTarget("F", fileId);
        
        return fileIndexMapper.deleteLingdocFileIndexById(fileId);
    }

    @Override
    @Transactional
    public int deleteLingdocFileIndexByIds(String[] fileIds, Long userId) throws IOException
    {
        int count = 0;
        for (String fileId : fileIds)
        {
            count += deleteLingdocFileIndexById(fileId, userId);
        }
        return count;
    }

    @Override
    public int createFolder(String subPath, Long userId)
    {
        String vaultRoot = userRepoService.getUserRepoPath(userId);
        Path folderPath = Paths.get(vaultRoot, "documents", subPath);
        try
        {
            if (!Files.exists(folderPath))
            {
                Files.createDirectories(folderPath);
                return 1;
            }
            return 0;
        }
        catch (IOException e)
        {
            log.error("创建文件夹失败: {}", folderPath, e);
            throw new RuntimeException("创建文件夹失败: " + e.getMessage());
        }
    }

    @Override
    @Transactional
    public Map<String, Integer> syncVault(Long userId)
    {
        Map<String, Integer> stats = new HashMap<>();
        stats.put("added", 0);
        stats.put("updated", 0);
        stats.put("deleted", 0);
        stats.put("duplicates", 0);

        String vaultRoot = userRepoService.getUserRepoPath(userId);
        Path documentsPath = Paths.get(vaultRoot, "documents");
        if (!Files.exists(documentsPath))
        {
            return stats;
        }
        
        try (Stream<Path> paths = Files.walk(documentsPath))
        {
            List<Path> filePaths = paths.filter(Files::isRegularFile).collect(Collectors.toList());
            
            // 获取数据库中该用户的所有文件
            LingdocFileIndex query = new LingdocFileIndex();
            query.setUserId(userId);
            List<LingdocFileIndex> dbFiles = fileIndexMapper.selectLingdocFileIndexList(query);
            Map<String, LingdocFileIndex> dbFileMap = dbFiles.stream()
                .collect(Collectors.toMap(LingdocFileIndex::getAbsPath, f -> f, (a, b) -> a));
            
            for (Path filePath : filePaths)
            {
                String absPath = filePath.toString();
                String checksum = calcFileSha256(filePath.toFile());
                
                if (dbFileMap.containsKey(absPath))
                {
                    LingdocFileIndex dbFile = dbFileMap.get(absPath);
                    if (!checksum.equals(dbFile.getChecksum()))
                    {
                        // 文件已变更，更新
                        dbFile.setChecksum(checksum);
                        dbFile.setFileSize(Files.size(filePath));
                        // 尝试读取文本内容
                        readFileContentIfText(dbFile, filePath);
                        fileIndexMapper.updateLingdocFileIndex(dbFile);
                        stats.put("updated", stats.get("updated") + 1);
                    }
                    dbFileMap.remove(absPath);
                }
                else
                {
                    // 新文件（按absPath判断），检查是否已有相同checksum
                    List<LingdocFileIndex> existingByChecksum = fileIndexMapper.selectLingdocFileIndexByChecksum(userId, checksum);
                    if (!existingByChecksum.isEmpty())
                    {
                        stats.put("duplicates", stats.get("duplicates") + 1);
                    }
                    // 正常插入所有文件（数据库唯一约束已移除，允许同一用户相同checksum的多条记录）
                    LingdocFileIndex newFile = new LingdocFileIndex();
                    newFile.setFileId(UUID.fastUUID().toString());
                    newFile.setUserId(userId);
                    newFile.setFileName(filePath.getFileName().toString());
                    newFile.setAbsPath(absPath);
                    String relPath = documentsPath.relativize(filePath).toString().replace("\\", "/");
                    newFile.setVaultPath(relPath);
                    newFile.setSubPath(relPath.contains("/") ? relPath.substring(0, relPath.lastIndexOf("/")) : "");
                    String ext = "";
                    String fileName = filePath.getFileName().toString();
                    if (fileName.contains("."))
                    {
                        ext = fileName.substring(fileName.lastIndexOf(".") + 1).toLowerCase();
                    }
                    newFile.setFileType(ext);
                    newFile.setFileSize(Files.size(filePath));
                    newFile.setChecksum(checksum);
                    newFile.setSourceType("0");
                    readFileContentIfText(newFile, filePath);
                    fileIndexMapper.insertLingdocFileIndex(newFile);
                    stats.put("added", stats.get("added") + 1);
                }
            }
            
            // 数据库中有但物理文件不存在的，删除
            for (LingdocFileIndex dbFile : dbFileMap.values())
            {
                fileIndexMapper.deleteLingdocFileIndexById(dbFile.getFileId());
                stats.put("deleted", stats.get("deleted") + 1);
            }
        }
        catch (IOException e)
        {
            log.error("Vault扫描同步失败", e);
            throw new RuntimeException("Vault扫描同步失败: " + e.getMessage());
        }
        
        return stats;
    }

    /**
     * 计算文件SHA256校验值
     */
    private String calcFileSha256(java.io.File file)
    {
        try (FileInputStream fis = new FileInputStream(file))
        {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] buffer = new byte[8192];
            int bytesRead;
            while ((bytesRead = fis.read(buffer)) != -1)
            {
                digest.update(buffer, 0, bytesRead);
            }
            byte[] hash = digest.digest();
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash)
            {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        }
        catch (Exception e)
        {
            log.error("计算文件SHA256失败: {}", file.getAbsolutePath(), e);
            return "";
        }
    }

    /**
     * 如果是文本文件，读取内容
     */
    private void readFileContentIfText(LingdocFileIndex file, Path filePath)
    {
        List<String> textTypes = List.of("txt", "md", "csv", "json", "xml", "yaml", "yml", "html", "htm", "js", "css", "java", "py", "c", "cpp", "h", "sql");
        if (textTypes.contains(file.getFileType().toLowerCase()))
        {
            try
            {
                String content = Files.readString(filePath);
                file.setFileContent(content);
                file.setContentSize((long) content.getBytes().length);
            }
            catch (IOException e)
            {
                log.warn("读取文件内容失败: {}", filePath, e);
            }
        }
    }

    @Override
    public List<Map<String, Object>> getDuplicateFiles(Long userId)
    {
        List<Map<String, Object>> result = new ArrayList<>();
        List<String> duplicateChecksums = fileIndexMapper.selectDuplicateChecksums(userId);
        
        for (String checksum : duplicateChecksums)
        {
            List<LingdocFileIndex> files = fileIndexMapper.selectLingdocFileIndexByChecksum(userId, checksum);
            Map<String, Object> group = new HashMap<>();
            group.put("checksum", checksum);
            group.put("count", files.size());
            group.put("files", files);
            result.add(group);
        }
        
        return result;
    }

    @Override
    public List<LingdocFileVersion> selectFileVersionList(String fileId)
    {
        LingdocFileVersion query = new LingdocFileVersion();
        query.setFileId(fileId);
        return fileVersionMapper.selectLingdocFileVersionList(query);
    }

    @Override
    @Transactional
    public LingdocFileIndex uploadFile(org.springframework.web.multipart.MultipartFile file, String subPath, Long userId) throws IOException
    {
        if (file == null || file.isEmpty())
        {
            throw new RuntimeException("上传文件不能为空");
        }

        String vaultRoot = userRepoService.getUserRepoPath(userId);
        Path targetDir = Paths.get(vaultRoot, "documents", StringUtils.isEmpty(subPath) ? "" : subPath);
        if (!Files.exists(targetDir))
        {
            Files.createDirectories(targetDir);
        }

        String originalFileName = file.getOriginalFilename();
        if (StringUtils.isEmpty(originalFileName))
        {
            originalFileName = "unnamed";
        }

        // 清理文件名中的非法字符
        String safeFileName = originalFileName.replaceAll("[\\\\/:*?\"<>|]", "_");
        Path targetPath = targetDir.resolve(safeFileName);

        // 如果文件已存在，添加序号
        int counter = 1;
        String baseName = safeFileName;
        String ext = "";
        int dotIndex = safeFileName.lastIndexOf('.');
        if (dotIndex > 0)
        {
            baseName = safeFileName.substring(0, dotIndex);
            ext = safeFileName.substring(dotIndex);
        }
        while (Files.exists(targetPath))
        {
            safeFileName = baseName + "_" + counter + ext;
            targetPath = targetDir.resolve(safeFileName);
            counter++;
        }

        // 保存物理文件
        file.transferTo(targetPath.toFile());

        // 计算SHA256
        String checksum = calcFileSha256(targetPath.toFile());

        // 检查重复checksum（同用户下）
        List<LingdocFileIndex> existingByChecksum = fileIndexMapper.selectLingdocFileIndexByChecksum(userId, checksum);
        if (!existingByChecksum.isEmpty())
        {
            // 删除刚保存的物理文件
            Files.deleteIfExists(targetPath);
            throw new RuntimeException("文件已存在（checksum重复）：" + existingByChecksum.get(0).getFileName());
        }

        // 构建文件索引
        LingdocFileIndex fileIndex = new LingdocFileIndex();
        fileIndex.setFileId(UUID.fastUUID().toString());
        fileIndex.setUserId(userId);
        fileIndex.setFileName(safeFileName);
        String relPath = Paths.get(vaultRoot, "documents").relativize(targetPath).toString().replace("\\", "/");
        fileIndex.setVaultPath(relPath);
        fileIndex.setAbsPath(targetPath.toString());
        fileIndex.setSubPath(StringUtils.isEmpty(subPath) ? "" : subPath);
        String fileExt = "";
        if (safeFileName.contains("."))
        {
            fileExt = safeFileName.substring(safeFileName.lastIndexOf(".") + 1).toLowerCase();
        }
        fileIndex.setFileType(fileExt);
        fileIndex.setFileSize(file.getSize());
        fileIndex.setChecksum(checksum);
        fileIndex.setSourceType("0"); // 手动上传
        readFileContentIfText(fileIndex, targetPath);

        fileIndexMapper.insertLingdocFileIndex(fileIndex);
        return fileIndex;
    }

    @Override
    public int recordFileVersion(String fileId, String snapshotPath, String operationType, Long operatorId)
    {
        Integer maxVersion = fileVersionMapper.selectMaxVersionNoByFileId(fileId);
        int nextVersion = (maxVersion == null) ? 1 : maxVersion + 1;
        
        LingdocFileVersion version = new LingdocFileVersion();
        version.setVersionId(UUID.fastUUID().toString());
        version.setFileId(fileId);
        version.setVersionNo(nextVersion);
        version.setSnapshotPath(snapshotPath);
        version.setOperationType(operationType);
        version.setOperatorId(operatorId);
        
        return fileVersionMapper.insertLingdocFileVersion(version);
    }
}
