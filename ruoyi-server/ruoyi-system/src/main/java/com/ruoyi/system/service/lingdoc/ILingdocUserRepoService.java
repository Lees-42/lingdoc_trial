package com.ruoyi.system.service.lingdoc;

import java.io.IOException;
import com.ruoyi.system.domain.lingdoc.LingdocUserRepo;

/**
 * 用户仓库配置服务层接口
 * 
 * @author lingdoc
 */
public interface ILingdocUserRepoService
{
    /**
     * 获取或初始化用户仓库
     * 若用户尚无仓库配置，则在默认位置自动创建并记录
     * 
     * @param userId 用户ID
     * @return 仓库配置
     */
    public LingdocUserRepo getOrInitUserRepo(Long userId);

    /**
     * 创建新仓库（切换到新路径）
     * 旧仓库文件不自动迁移，仅切换路径配置
     * 
     * @param userId 用户ID
     * @param repoPath 新仓库绝对路径
     * @param repoName 仓库名称
     * @return 仓库配置
     */
    public LingdocUserRepo createRepo(Long userId, String repoPath, String repoName);

    /**
     * 迁移仓库
     * 将旧仓库中的全部文件复制到新路径，更新数据库路径记录，切换至新仓库
     * 
     * @param userId 用户ID
     * @param newRepoPath 新仓库绝对路径
     * @throws IOException 文件操作异常
     */
    public void migrateRepo(Long userId, String newRepoPath) throws IOException;

    /**
     * 获取用户仓库路径（便捷方法）
     * 若不存在则自动初始化
     * 
     * @param userId 用户ID
     * @return 仓库绝对路径
     */
    public String getUserRepoPath(Long userId);
}
