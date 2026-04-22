package com.ruoyi.system.mapper.lingdoc;

import java.util.List;
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
     * 根据用户ID查询所有仓库配置
     * 
     * @param userId 用户ID
     * @return 仓库配置列表
     */
    public List<LingdocUserRepo> selectByUserId(Long userId);

    /**
     * 根据用户ID查询默认仓库配置
     * 
     * @param userId 用户ID
     * @return 默认仓库配置
     */
    public LingdocUserRepo selectDefaultByUserId(Long userId);

    /**
     * 根据仓库ID查询仓库配置
     * 
     * @param repoId 仓库ID
     * @return 仓库配置
     */
    public LingdocUserRepo selectByRepoId(String repoId);

    /**
     * 新增仓库配置
     * 
     * @param repo 仓库配置
     * @return 结果
     */
    public int insert(LingdocUserRepo repo);

    /**
     * 更新仓库路径
     * 
     * @param repoId 仓库ID
     * @param repoPath 新仓库路径
     * @return 结果
     */
    public int updateRepoPath(@Param("repoId") String repoId, @Param("repoPath") String repoPath);

    /**
     * 更新仓库默认标记
     * 
     * @param repoId 仓库ID
     * @param isDefault 是否默认
     * @return 结果
     */
    public int updateDefaultByRepoId(@Param("repoId") String repoId, @Param("isDefault") String isDefault);

    /**
     * 清除用户所有仓库的默认标记
     * 
     * @param userId 用户ID
     * @return 结果
     */
    public int clearDefaultByUserId(Long userId);

    /**
     * 根据仓库ID删除仓库配置
     * 
     * @param repoId 仓库ID
     * @return 结果
     */
    public int deleteByRepoId(String repoId);

    /**
     * 根据用户ID删除仓库配置
     * 
     * @param userId 用户ID
     * @return 结果
     */
    public int deleteByUserId(Long userId);
}
