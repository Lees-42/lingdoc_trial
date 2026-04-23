package com.ruoyi.system.mapper.lingdoc;

import java.util.List;
import com.ruoyi.system.domain.lingdoc.LingdocInbox;

/**
 * 收件箱文件表 Mapper接口
 * 
 * @author lingdoc
 */
public interface LingdocInboxMapper
{
    /**
     * 查询收件箱文件
     * 
     * @param inboxId 收件箱记录ID
     * @return 收件箱文件
     */
    public LingdocInbox selectById(String inboxId);

    /**
     * 查询收件箱文件列表
     * 
     * @param query 查询条件
     * @return 收件箱文件集合
     */
    public List<LingdocInbox> selectList(LingdocInbox query);

    /**
     * 新增收件箱文件
     * 
     * @param inbox 收件箱文件
     * @return 结果
     */
    public int insert(LingdocInbox inbox);

    /**
     * 修改收件箱文件
     * 
     * @param inbox 收件箱文件
     * @return 结果
     */
    public int update(LingdocInbox inbox);

    /**
     * 删除收件箱文件
     * 
     * @param inboxId 收件箱记录ID
     * @return 结果
     */
    public int deleteById(String inboxId);

    /**
     * 批量删除收件箱文件
     * 
     * @param inboxIds 需要删除的数据ID数组
     * @return 结果
     */
    public int deleteByIds(String[] inboxIds);

    /**
     * 根据用户ID删除收件箱文件
     * 
     * @param userId 用户ID
     * @return 结果
     */
    public int deleteByUserId(Long userId);
}
