package com.ruoyi.system.service.lingdoc;

import java.io.IOException;
import java.util.List;
import com.ruoyi.system.domain.lingdoc.LingdocUserRepo;

/**
 * 用户仓库配置服务层接口
 * 
 * @author lingdoc
 */
public interface ILingdocUserRepoService
{
    /**
     * 获取或初始化用户默认仓库
     * 若用户尚无仓库配置，则在默认位置自动创建并记录
     * 
     * @param userId 用户ID
     * @return 仓库配置
     */
    public LingdocUserRepo getOrInitUserRepo(Long userId);

    /**
     * 获取用户所有仓库列表
     * 
     * @param userId 用户ID
     * @return 仓库配置列表
     */
    public List<LingdocUserRepo> listUserRepos(Long userId);

    /**
     * 创建新仓库
     * 支持一个用户拥有多个仓库
     * 
     * @param userId 用户ID
     * @param repoPath 新仓库绝对路径
     * @param repoName 仓库名称
     * @return 仓库配置
     */
    public LingdocUserRepo createRepo(Long userId, String repoPath, String repoName);

    /**
     * 删除仓库配置
     * 仅删除数据库配置记录和 SQLite 文件，保留物理文件
     * 
     * @param userId 用户ID
     * @param repoId 仓库ID
     */
    public void deleteRepo(Long userId, String repoId);

    /**
     * 设置默认仓库
     * 
     * @param userId 用户ID
     * @param repoId 仓库ID
     * @return 更新后的仓库配置
     */
    public LingdocUserRepo setDefaultRepo(Long userId, String repoId);

    /**
     * 迁移仓库
     * 将旧仓库中的全部文件复制到新路径，更新数据库路径记录
     * 
     * @param userId 用户ID
     * @param repoId 仓库ID
     * @param newRepoPath 新仓库绝对路径
     * @throws IOException 文件操作异常
     */
    public void migrateRepo(Long userId, String repoId, String newRepoPath) throws IOException;

    /**
     * 获取用户仓库路径（便捷方法）
     * 返回默认仓库路径，若不存在则自动初始化
     * 
     * @param userId 用户ID
     * @return 仓库绝对路径
     */
    public String getUserRepoPath(Long userId);

    /**
     * 获取用户默认仓库（只查询，不自动创建）
     * 
     * @param userId 用户ID
     * @return 仓库配置，无仓库返回 null
     */
    public LingdocUserRepo getDefaultUserRepo(Long userId);
}
