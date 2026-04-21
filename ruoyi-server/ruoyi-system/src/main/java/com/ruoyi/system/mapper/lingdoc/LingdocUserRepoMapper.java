package com.ruoyi.system.mapper.lingdoc;

import org.apache.ibatis.annotations.Param;
import com.ruoyi.system.domain.lingdoc.LingdocUserRepo;

/**
 * 用户仓库配置Mapper接口
 * 
 * @author lingdoc
 */
public interface LingdocUserRepoMapper
{
    /**
     * 根据用户ID查询仓库配置
     * 
     * @param userId 用户ID
     * @return 仓库配置
     */
    public LingdocUserRepo selectByUserId(Long userId);

    /**
     * 新增仓库配置
     * 
     * @param repo 仓库配置
     * @return 结果
     */
    public int insert(LingdocUserRepo repo);

    /**
     * 更新仓库路径和名称
     * 
     * @param userId 用户ID
     * @param repoPath 新仓库路径
     * @param repoName 新仓库名称
     * @return 结果
     */
    public int updateRepoPath(@Param("userId") Long userId, @Param("repoPath") String repoPath, @Param("repoName") String repoName);

    /**
     * 根据用户ID删除仓库配置
     * 
     * @param userId 用户ID
     * @return 结果
     */
    public int deleteByUserId(Long userId);
}
