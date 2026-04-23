package com.ruoyi.system.service.lingdoc;

import java.io.IOException;
import java.util.List;
import org.springframework.web.multipart.MultipartFile;
import com.ruoyi.system.domain.lingdoc.LingdocFileIndex;
import com.ruoyi.system.domain.lingdoc.LingdocInbox;
import com.ruoyi.system.domain.lingdoc.LingdocUploadConfirmRequest;

/**
 * 收件箱文件管理服务层接口
 * 
 * @author lingdoc
 */
public interface ILingdocInboxService
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
     * 上传文件到 inbox
     * 
     * @param file 上传的文件
     * @param userId 用户ID
     * @return 收件箱文件信息
     * @throws IOException 文件操作异常
     */
    public LingdocInbox uploadToInbox(MultipartFile file, Long userId) throws IOException;

    /**
     * 触发 AI 自动规整
     * 
     * @param inboxId 收件箱记录ID
     * @param userId 用户ID
     * @return 更新后的收件箱文件
     * @throws IOException 文件操作异常
     */
    public LingdocInbox organize(String inboxId, Long userId) throws IOException;

    /**
     * 确认归档：inbox → Vault 正式目录 + lingdoc_file_index
     * 
     * @param request 确认请求
     * @param userId 用户ID
     * @return 正式文件索引
     * @throws IOException 文件操作异常
     */
    public LingdocFileIndex confirmToVault(LingdocUploadConfirmRequest request, Long userId) throws IOException;

    /**
     * 删除收件箱文件
     * 
     * @param inboxId 收件箱记录ID
     * @param userId 用户ID
     * @return 结果
     */
    public int deleteById(String inboxId, Long userId);

    /**
     * 批量删除收件箱文件
     * 
     * @param inboxIds 需要删除的数据ID数组
     * @param userId 用户ID
     * @return 结果
     */
    public int deleteByIds(String[] inboxIds, Long userId);

    /**
     * 清空当前用户收件箱
     * 
     * @param userId 用户ID
     * @return 结果
     */
    public int cleanByUserId(Long userId);
}
