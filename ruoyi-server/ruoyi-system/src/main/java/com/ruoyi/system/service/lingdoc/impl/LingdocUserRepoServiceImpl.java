package com.ruoyi.system.service.lingdoc.impl;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Date;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.ruoyi.common.datasource.VaultContextHolder;
import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.common.utils.uuid.UUID;
import com.ruoyi.system.domain.lingdoc.LingdocFileIndex;
import com.ruoyi.system.domain.lingdoc.LingdocUserRepo;
import com.ruoyi.system.mapper.lingdoc.LingdocFileIndexMapper;
import com.ruoyi.system.mapper.lingdoc.LingdocUserRepoMapper;
import com.ruoyi.common.config.SQLiteVaultConfig;
import com.ruoyi.system.service.lingdoc.ILingdocUserRepoService;

/**
 * 用户仓库配置服务层实现
 * 
 * @author lingdoc
 */
@Service
public class LingdocUserRepoServiceImpl implements ILingdocUserRepoService
{
    private static final Logger log = LoggerFactory.getLogger(LingdocUserRepoServiceImpl.class);

    private static final String DEFAULT_REPO_NAME = "默认仓库";

    @Autowired
    private LingdocUserRepoMapper userRepoMapper;

    @Autowired
    private LingdocFileIndexMapper fileIndexMapper;

    @Autowired
    private VaultDbInitializer vaultDbInitializer;

    @Override
    public LingdocUserRepo getOrInitUserRepo(Long userId)
    {
        // 先查找默认仓库
        LingdocUserRepo repo = userRepoMapper.selectDefaultByUserId(userId);
        if (repo != null)
        {
            return repo;
        }

        // 查找该用户的任意仓库
        List<LingdocUserRepo> repos = userRepoMapper.selectByUserId(userId);
        if (!repos.isEmpty())
        {
            return repos.get(0);
        }

        // 自动初始化默认仓库
        String defaultPath = getDefaultRepoPath();
        Path documentsPath = Paths.get(defaultPath, "documents");
        try
        {
            if (!Files.exists(documentsPath))
            {
                Files.createDirectories(documentsPath);
                log.info("为用户 {} 创建默认仓库目录: {}", userId, documentsPath);
            }
        }
        catch (IOException e)
        {
            log.error("创建默认仓库目录失败: {}", documentsPath, e);
            throw new RuntimeException("创建默认仓库失败: " + e.getMessage());
        }

        repo = new LingdocUserRepo();
        repo.setRepoId(UUID.fastUUID().toString());
        repo.setUserId(userId);
        repo.setRepoPath(defaultPath);
        repo.setRepoName(DEFAULT_REPO_NAME);
        repo.setIsDefault("1");
        repo.setIsActive("1");
        repo.setCreateTime(new Date());
        repo.setUpdateTime(new Date());
        userRepoMapper.insert(repo);

        // 初始化 .lingdoc 目录和 SQLite 数据库
        vaultDbInitializer.initializeVaultDatabase(defaultPath);

        log.info("为用户 {} 初始化默认仓库配置: {}", userId, defaultPath);
        return repo;
    }

    @Override
    public List<LingdocUserRepo> listUserRepos(Long userId)
    {
        return userRepoMapper.selectByUserId(userId);
    }

    @Override
    public LingdocUserRepo createRepo(Long userId, String repoPath, String repoName)
    {
        if (StringUtils.isEmpty(repoPath))
        {
            throw new RuntimeException("仓库路径不能为空");
        }

        Path path = Paths.get(repoPath);
        try
        {
            Path documentsPath = path.resolve("documents");
            if (!Files.exists(documentsPath))
            {
                Files.createDirectories(documentsPath);
            }
        }
        catch (IOException e)
        {
            log.error("创建仓库目录失败: {}", repoPath, e);
            throw new RuntimeException("创建仓库目录失败: " + e.getMessage());
        }

        LingdocUserRepo repo = new LingdocUserRepo();
        repo.setRepoId(UUID.fastUUID().toString());
        repo.setUserId(userId);
        repo.setRepoPath(repoPath);
        repo.setRepoName(StringUtils.isNotEmpty(repoName) ? repoName : DEFAULT_REPO_NAME);
        repo.setIsDefault("0");
        repo.setIsActive("1");
        repo.setCreateTime(new Date());
        repo.setUpdateTime(new Date());
        userRepoMapper.insert(repo);

        // 初始化 .lingdoc 目录和 SQLite 数据库
        vaultDbInitializer.initializeVaultDatabase(repoPath);

        log.info("用户 {} 创建新仓库: {} -> {}", userId, repoName, repoPath);
        return repo;
    }

    @Override
    @Transactional
    public void deleteRepo(Long userId, String repoId)
    {
        LingdocUserRepo repo = userRepoMapper.selectByRepoId(repoId);
        if (repo == null || !userId.equals(repo.getUserId()))
        {
            throw new RuntimeException("仓库不存在或无权限");
        }

        // 如果删除的是默认仓库，需要重新指定默认仓库
        boolean wasDefault = "1".equals(repo.getIsDefault());

        userRepoMapper.deleteByRepoId(repoId);

        // 清理 SQLite 数据库文件（可选：保留物理文件）
        try
        {
            Path dbFile = vaultDbInitializer.getVaultDatabasePath(repo.getRepoPath());
            if (Files.exists(dbFile))
            {
                Files.delete(dbFile);
                log.info("删除仓库 SQLite 数据库: {}", dbFile);
            }
        }
        catch (IOException e)
        {
            log.warn("删除仓库 SQLite 数据库失败: {}", repo.getRepoPath(), e);
        }

        // 如果删除的是默认仓库，将剩余的第一个仓库设为默认
        if (wasDefault)
        {
            List<LingdocUserRepo> remaining = userRepoMapper.selectByUserId(userId);
            if (!remaining.isEmpty())
            {
                userRepoMapper.updateDefaultByRepoId(remaining.get(0).getRepoId(), "1");
            }
        }

        log.info("用户 {} 删除仓库: {}", userId, repoId);
    }

    @Override
    @Transactional
    public LingdocUserRepo setDefaultRepo(Long userId, String repoId)
    {
        LingdocUserRepo repo = userRepoMapper.selectByRepoId(repoId);
        if (repo == null || !userId.equals(repo.getUserId()))
        {
            throw new RuntimeException("仓库不存在或无权限");
        }

        // 清除该用户所有仓库的默认标记
        userRepoMapper.clearDefaultByUserId(userId);

        // 设置指定仓库为默认
        userRepoMapper.updateDefaultByRepoId(repoId, "1");
        repo.setIsDefault("1");

        log.info("用户 {} 设置默认仓库: {}", userId, repoId);
        return repo;
    }

    @Override
    @Transactional
    public void migrateRepo(Long userId, String repoId, String newRepoPath) throws IOException
    {
        if (StringUtils.isEmpty(newRepoPath))
        {
            throw new RuntimeException("新仓库路径不能为空");
        }

        LingdocUserRepo currentRepo = userRepoMapper.selectByRepoId(repoId);
        if (currentRepo == null || !userId.equals(currentRepo.getUserId()))
        {
            throw new RuntimeException("仓库不存在或无权限");
        }

        String oldPath = currentRepo.getRepoPath();
        if (oldPath.equals(newRepoPath))
        {
            throw new RuntimeException("新路径与当前路径相同，无需迁移");
        }

        // 校验新路径不是当前路径的子目录
        Path oldPathObj = Paths.get(oldPath).toAbsolutePath().normalize();
        Path newPathObj = Paths.get(newRepoPath).toAbsolutePath().normalize();
        if (newPathObj.startsWith(oldPathObj))
        {
            throw new RuntimeException("新路径不能是当前仓库路径的子目录");
        }

        // 创建新仓库目录结构
        Path newDocumentsPath = newPathObj.resolve("documents");
        if (!Files.exists(newDocumentsPath))
        {
            Files.createDirectories(newDocumentsPath);
        }

        // 获取该用户在数据库中的所有文件
        LingdocFileIndex query = new LingdocFileIndex();
        query.setUserId(userId);
        List<LingdocFileIndex> userFiles = fileIndexMapper.selectLingdocFileIndexList(query);

        int migratedCount = 0;
        for (LingdocFileIndex file : userFiles)
        {
            String oldAbsPath = file.getAbsPath();
            if (StringUtils.isEmpty(oldAbsPath))
            {
                continue;
            }

            Path oldFilePath = Paths.get(oldAbsPath);
            if (!Files.exists(oldFilePath))
            {
                log.warn("迁移时物理文件不存在，跳过: {}", oldAbsPath);
                continue;
            }

            // 计算相对路径（基于旧仓库的 documents 目录）
            Path oldDocumentsPath = oldPathObj.resolve("documents");
            String relPath;
            if (oldAbsPath.startsWith(oldDocumentsPath.toString()))
            {
                relPath = oldDocumentsPath.relativize(oldFilePath).toString();
            }
            else
            {
                //  fallback：仅用文件名
                relPath = oldFilePath.getFileName().toString();
            }

            Path newFilePath = newDocumentsPath.resolve(relPath);
            // 确保目标目录存在
            Path parentDir = newFilePath.getParent();
            if (parentDir != null && !Files.exists(parentDir))
            {
                Files.createDirectories(parentDir);
            }

            // 复制文件（保留原文件作为备份，迁移成功后可手动清理）
            Files.copy(oldFilePath, newFilePath, StandardCopyOption.REPLACE_EXISTING);

            // 更新数据库路径
            String newAbsPath = newFilePath.toString();
            String newVaultPath = relPath.replace("\\", "/");
            String newSubPath = "";
            if (newVaultPath.contains("/"))
            {
                newSubPath = newVaultPath.substring(0, newVaultPath.lastIndexOf("/"));
            }

            file.setAbsPath(newAbsPath);
            file.setVaultPath(newVaultPath);
            file.setSubPath(newSubPath);
            fileIndexMapper.updateLingdocFileIndex(file);

            migratedCount++;
        }

        // 迁移 SQLite 数据库文件
        Path oldDbFile = vaultDbInitializer.getVaultDatabasePath(oldPath);
        Path newLingdocDir = newPathObj.resolve(SQLiteVaultConfig.LINGDOC_DIR);
        if (!Files.exists(newLingdocDir))
        {
            Files.createDirectories(newLingdocDir);
        }
        Path newDbFile = newLingdocDir.resolve(SQLiteVaultConfig.DB_FILE_NAME);
        if (Files.exists(oldDbFile))
        {
            Files.copy(oldDbFile, newDbFile, StandardCopyOption.REPLACE_EXISTING);
            log.info("迁移 SQLite 数据库: {} -> {}", oldDbFile, newDbFile);
        }
        else
        {
            // 如果旧数据库不存在，在新位置初始化
            vaultDbInitializer.initializeVaultDatabase(newRepoPath);
        }

        // 更新仓库配置
        userRepoMapper.updateRepoPath(repoId, newRepoPath);

        log.info("用户 {} 仓库迁移完成: {} -> {}, 迁移文件数: {}", userId, oldPath, newRepoPath, migratedCount);
    }

    @Override
    public String getUserRepoPath(Long userId)
    {
        // 优先使用 Vault 上下文中的路径（前端通过 X-Vault-Path 传递）
        String vaultPath = VaultContextHolder.getCurrentVaultPath();
        if (StringUtils.isNotEmpty(vaultPath))
        {
            return vaultPath;
        }
        LingdocUserRepo repo = getOrInitUserRepo(userId);
        return repo != null ? repo.getRepoPath() : null;
    }

    @Override
    public LingdocUserRepo getDefaultUserRepo(Long userId)
    {
        // 先查找默认仓库
        LingdocUserRepo repo = userRepoMapper.selectDefaultByUserId(userId);
        if (repo != null)
        {
            return repo;
        }
        // 查找该用户的任意仓库
        List<LingdocUserRepo> repos = userRepoMapper.selectByUserId(userId);
        if (!repos.isEmpty())
        {
            return repos.get(0);
        }
        // 不自动创建，返回 null
        return null;
    }

    /**
     * 获取默认仓库路径
     * Windows: C:\Users\xxx\LingDoc\Vault
     * Linux/macOS: ~/LingDoc/Vault
     */
    private String getDefaultRepoPath()
    {
        String userHome = System.getProperty("user.home");
        return Paths.get(userHome, "LingDoc", "Vault").toString();
    }
}
