package com.ruoyi.system.domain.lingdoc;

import java.util.Date;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

/**
 * 用户仓库配置表 lingdoc_user_repo
 * 
 * @author lingdoc
 */
public class LingdocUserRepo
{
    private static final long serialVersionUID = 1L;

    /** 仓库ID */
    private String repoId;

    /** 用户ID */
    private Long userId;

    /** 仓库绝对路径 */
    private String repoPath;

    /** 仓库名称 */
    private String repoName;

    /** 创建时间 */
    private Date createTime;

    /** 更新时间 */
    private Date updateTime;

    /** 备注 */
    private String remark;

    public String getRepoId()
    {
        return repoId;
    }

    public void setRepoId(String repoId)
    {
        this.repoId = repoId;
    }

    public Long getUserId()
    {
        return userId;
    }

    public void setUserId(Long userId)
    {
        this.userId = userId;
    }

    public String getRepoPath()
    {
        return repoPath;
    }

    public void setRepoPath(String repoPath)
    {
        this.repoPath = repoPath;
    }

    public String getRepoName()
    {
        return repoName;
    }

    public void setRepoName(String repoName)
    {
        this.repoName = repoName;
    }

    public Date getCreateTime()
    {
        return createTime;
    }

    public void setCreateTime(Date createTime)
    {
        this.createTime = createTime;
    }

    public Date getUpdateTime()
    {
        return updateTime;
    }

    public void setUpdateTime(Date updateTime)
    {
        this.updateTime = updateTime;
    }

    public String getRemark()
    {
        return remark;
    }

    public void setRemark(String remark)
    {
        this.remark = remark;
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this, ToStringStyle.MULTI_LINE_STYLE)
            .append("repoId", getRepoId())
            .append("userId", getUserId())
            .append("repoPath", getRepoPath())
            .append("repoName", getRepoName())
            .append("createTime", getCreateTime())
            .append("updateTime", getUpdateTime())
            .append("remark", getRemark())
            .toString();
    }
}
