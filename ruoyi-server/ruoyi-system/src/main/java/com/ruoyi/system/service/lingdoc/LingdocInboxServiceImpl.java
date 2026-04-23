package com.ruoyi.system.service.lingdoc;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.common.utils.file.FileUploadUtils;
import com.ruoyi.common.utils.file.FileUtils;
import com.ruoyi.common.utils.uuid.UUID;
import com.ruoyi.system.domain.lingdoc.LingdocFileIndex;
import com.ruoyi.system.domain.lingdoc.LingdocInbox;
import com.ruoyi.system.domain.lingdoc.LingdocTagBinding;
import com.ruoyi.system.domain.lingdoc.LingdocUploadConfirmRequest;
import com.ruoyi.system.mapper.lingdoc.LingdocFileIndexMapper;
import com.ruoyi.system.mapper.lingdoc.LingdocInboxMapper;
import com.ruoyi.system.service.lingdoc.ai.IAiOrganizeService;
import com.ruoyi.system.service.lingdoc.ai.result.AiOrganizeResult;
import com.ruoyi.system.service.lingdoc.ai.result.AiTagSuggestion;

/**
 * 收件箱文件管理服务层实现
 * 
 * @author lingdoc
 */
@Service
public class LingdocInboxServiceImpl implements ILingdocInboxService
{
    private static final Logger log = LoggerFactory.getLogger(LingdocInboxServiceImpl.class);

    @Autowired
    private LingdocInboxMapper inboxMapper;

    @Autowired
    private LingdocFileIndexMapper fileIndexMapper;

    @Autowired
    private ILingdocUserRepoService userRepoService;

    @Autowired
    private ILingdocTagService tagService;

    @Autowired
    private IAiOrganizeService aiOrganizeService;

    @Override
    public LingdocInbox selectById(String inboxId)
    {
        return inboxMapper.selectById(inboxId);
    }

    @Override
    public List<LingdocInbox> selectList(LingdocInbox query)
    {
        return inboxMapper.selectList(query);
    }

    /**
     * 上传文件到 Vault inbox 目录（与 documents 并列）
     */
    @Override
    @Transactional
    public LingdocInbox uploadToInbox(MultipartFile file, Long userId) throws IOException
    {
        String vaultRoot = userRepoService.getUserRepoPath(userId);
        Path inboxDir = Paths.get(vaultRoot, "inbox");
        if (!Files.exists(inboxDir))
        {
            Files.createDirectories(inboxDir);
        }

        // 使用 FileUploadUtils 保存（生成带日期前缀的唯一文件名）
        String savedFileName = FileUploadUtils.upload(inboxDir.toString(), file);
        Path absPath = inboxDir.resolve(savedFileName);

        LingdocInbox inbox = new LingdocInbox();
        inbox.setInboxId(UUID.fastUUID().toString());
        inbox.setUserId(userId);
        inbox.setOriginalName(file.getOriginalFilename());
        inbox.setFileType(getExtension(file.getOriginalFilename()));
        inbox.setFileSize(file.getSize());
        inbox.setAbsPath(absPath.toString());
        inbox.setStatus("uploaded");
        inboxMapper.insert(inbox);

        return inbox;
    }

    /**
     * 触发 AI 自动规整
     */
    @Override
    @Transactional
    public LingdocInbox organize(String inboxId, Long userId) throws IOException
    {
        LingdocInbox inbox = inboxMapper.selectById(inboxId);
        if (inbox == null || !userId.equals(inbox.getUserId()))
        {
            throw new RuntimeException("文件不存在或无权限");
        }

        if (!("uploaded".equals(inbox.getStatus()) || "failed".equals(inbox.getStatus())))
        {
            throw new RuntimeException("当前状态不允许规整: " + inbox.getStatus());
        }

        inbox.setStatus("organizing");
        inboxMapper.update(inbox);

        try
        {
            String content = readTextContentIfPossible(inbox.getAbsPath(), inbox.getFileType());

            AiOrganizeResult result = aiOrganizeService.organize(
                inboxId, inbox.getAbsPath(), inbox.getOriginalName(), content, userId);

            // 应用建议
            if (result.getRename() != null && StringUtils.isNotEmpty(result.getRename().getSuggestedName()))
            {
                inbox.setSuggestedName(result.getRename().getSuggestedName());
            }
            else
            {
                inbox.setSuggestedName(inbox.getOriginalName());
            }

            if (result.getCategory() != null && StringUtils.isNotEmpty(result.getCategory().getSuggestedSubPath()))
            {
                inbox.setSuggestedPath(result.getCategory().getSuggestedSubPath());
            }
            else
            {
                inbox.setSuggestedPath("/");
            }

            if (result.getTags() != null && !result.getTags().isEmpty())
            {
                List<String> tagIds = resolveTagIds(result.getTags());
                inbox.setTagIds(String.join(",", tagIds));
            }

            inbox.setAiSummary(result.getSummary());
            inbox.setAiKeywords(result.getKeywords() != null ? String.join(",", result.getKeywords()) : null);
            inbox.setConfidence(result.getConfidence());
            inbox.setTokenCost(result.getTokenCost());
            inbox.setStatus("pending");
            inboxMapper.update(inbox);
        }
        catch (Exception e)
        {
            inbox.setStatus("failed");
            inbox.setErrorMsg(e.getMessage());
            inboxMapper.update(inbox);
            throw new RuntimeException("自动规整失败: " + e.getMessage(), e);
        }

        return inbox;
    }

    /**
     * 确认归档：inbox → Vault 正式目录
     */
    @Override
    @Transactional
    public LingdocFileIndex confirmToVault(LingdocUploadConfirmRequest request, Long userId) throws IOException
    {
        LingdocInbox inbox = inboxMapper.selectById(request.getFileId());
        if (inbox == null || !userId.equals(inbox.getUserId()))
        {
            throw new RuntimeException("文件不存在或无权限");
        }

        String vaultRoot = userRepoService.getUserRepoPath(userId);
        Path sourcePath = Paths.get(inbox.getAbsPath());

        // 目标目录（documents/ 下）
        String targetSubPath = request.getSuggestedPath();
        if ("/".equals(targetSubPath) || StringUtils.isEmpty(targetSubPath))
        {
            targetSubPath = "";
        }
        Path targetDir = Paths.get(vaultRoot, "documents", targetSubPath.replace("/", java.io.File.separator));
        if (!Files.exists(targetDir))
        {
            Files.createDirectories(targetDir);
        }

        // 目标文件名
        String targetFileName = StringUtils.isNotEmpty(request.getSuggestedName())
            ? request.getSuggestedName() : inbox.getOriginalName();
        Path targetPath = targetDir.resolve(targetFileName);

        // 处理重名
        int counter = 1;
        String baseName = targetFileName;
        String ext = "";
        int dot = targetFileName.lastIndexOf('.');
        if (dot > 0)
        {
            baseName = targetFileName.substring(0, dot);
            ext = targetFileName.substring(dot);
        }
        while (Files.exists(targetPath))
        {
            targetFileName = baseName + "_" + counter + ext;
            targetPath = targetDir.resolve(targetFileName);
            counter++;
        }

        // 物理 move：inbox/ → documents/
        Files.move(sourcePath, targetPath, StandardCopyOption.REPLACE_EXISTING);

        // 计算 SHA256
        String checksum = calcFileSha256(targetPath.toFile());

        // 写入正式索引表 lingdoc_file_index
        // relPath 是相对于 documents/ 的路径
        String relPath = Paths.get(vaultRoot, "documents").relativize(targetPath)
            .toString().replace("\\", "/");

        LingdocFileIndex fileIndex = new LingdocFileIndex();
        fileIndex.setFileId(UUID.fastUUID().toString());
        fileIndex.setUserId(userId);
        fileIndex.setFileName(targetFileName);
        fileIndex.setVaultPath(relPath);
        fileIndex.setAbsPath(targetPath.toString());
        fileIndex.setSubPath(targetSubPath);
        fileIndex.setFileType(getExtension(targetFileName));
        fileIndex.setFileSize(Files.size(targetPath));
        fileIndex.setChecksum(checksum);
        fileIndex.setSourceType("1");   // 自动规整来源
        fileIndexMapper.insertLingdocFileIndex(fileIndex);

        // 绑定标签
        if (request.getTagIds() != null && !request.getTagIds().isEmpty())
        {
            for (String tagId : request.getTagIds())
            {
                LingdocTagBinding binding = new LingdocTagBinding();
                binding.setBindingId(UUID.fastUUID().toString());
                binding.setTargetType("F");
                binding.setTargetId(fileIndex.getFileId());
                binding.setTagId(tagId);
                tagService.insertLingdocTagBinding(binding);
            }
        }

        // 删除 inbox 记录
        inboxMapper.deleteById(inbox.getInboxId());

        return fileIndex;
    }

    @Override
    @Transactional
    public int deleteById(String inboxId, Long userId)
    {
        LingdocInbox inbox = inboxMapper.selectById(inboxId);
        if (inbox == null || !userId.equals(inbox.getUserId()))
        {
            return 0;
        }
        // 删除物理文件
        FileUtils.deleteFile(inbox.getAbsPath());
        return inboxMapper.deleteById(inboxId);
    }

    @Override
    @Transactional
    public int deleteByIds(String[] inboxIds, Long userId)
    {
        int count = 0;
        for (String id : inboxIds)
        {
            count += deleteById(id, userId);
        }
        return count;
    }

    @Override
    @Transactional
    public int cleanByUserId(Long userId)
    {
        LingdocInbox query = new LingdocInbox();
        query.setUserId(userId);
        List<LingdocInbox> list = inboxMapper.selectList(query);
        for (LingdocInbox inbox : list)
        {
            FileUtils.deleteFile(inbox.getAbsPath());
        }
        return inboxMapper.deleteByUserId(userId);
    }

    // ---------- 私有工具方法 ----------

    private String getExtension(String fileName)
    {
        if (StringUtils.isEmpty(fileName) || fileName.lastIndexOf('.') < 0)
        {
            return "";
        }
        return fileName.substring(fileName.lastIndexOf('.') + 1).toLowerCase();
    }

    private String readTextContentIfPossible(String absPath, String fileType)
    {
        List<String> textTypes = List.of("txt", "md", "csv", "json", "xml", "yaml", "yml", "html", "htm", "js", "css", "java", "py");
        if (!textTypes.contains(fileType.toLowerCase()))
        {
            return null;
        }
        try
        {
            return Files.readString(Paths.get(absPath));
        }
        catch (IOException e)
        {
            log.warn("读取文本内容失败: {}", absPath);
            return null;
        }
    }

    private List<String> resolveTagIds(List<AiTagSuggestion> suggestions)
    {
        List<String> tagIds = new ArrayList<>();
        for (AiTagSuggestion s : suggestions)
        {
            // TODO: 根据 tagName 查询系统中是否已有该标签
            // 若有则取 tagId，若无则跳过（或自动创建）
        }
        return tagIds;
    }

    private String calcFileSha256(File file)
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
            StringBuilder hex = new StringBuilder();
            for (byte b : hash)
            {
                String h = Integer.toHexString(0xff & b);
                if (h.length() == 1)
                {
                    hex.append('0');
                }
                hex.append(h);
            }
            return hex.toString();
        }
        catch (Exception e)
        {
            log.error("计算 SHA256 失败", e);
            return "";
        }
    }
}
