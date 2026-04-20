package com.ruoyi.lingdoc.ai.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.ruoyi.common.core.domain.model.LoginUser;
import com.ruoyi.common.utils.SecurityUtils;
import com.ruoyi.common.utils.bean.BeanUtils;
import com.ruoyi.common.utils.file.FileUtils;
import com.ruoyi.common.utils.uuid.UUID;
import com.ruoyi.lingdoc.ai.domain.entity.KbDocument;
import com.ruoyi.lingdoc.ai.domain.entity.KbDocumentChunk;
import com.ruoyi.lingdoc.ai.domain.entity.KbKnowledgeBase;
import com.ruoyi.lingdoc.ai.domain.vo.KbDocumentVO;
import com.ruoyi.lingdoc.ai.mapper.KbDocumentChunkMapper;
import com.ruoyi.lingdoc.ai.mapper.KbDocumentMapper;
import com.ruoyi.lingdoc.ai.mapper.KbEmbeddingMapper;
import com.ruoyi.lingdoc.ai.mapper.KbKnowledgeBaseMapper;
import com.ruoyi.lingdoc.ai.service.IChunkingService;
import com.ruoyi.lingdoc.ai.service.IDocumentService;
import com.ruoyi.lingdoc.ai.service.ITextExtractionService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 文档管理服务实现
 */
@Slf4j
@Service
public class DocumentServiceImpl extends ServiceImpl<KbDocumentMapper, KbDocument> implements IDocumentService {

    @Autowired
    private KbKnowledgeBaseMapper kbMapper;

    @Autowired
    private KbDocumentChunkMapper chunkMapper;

    @Autowired
    private KbEmbeddingMapper embeddingMapper;

    @Autowired
    private ITextExtractionService textExtractionService;

    @Autowired
    private IChunkingService chunkingService;

    @Value("${lingdoc.ai.upload.temp-dir:${java.io.tmpdir}/lingdoc/upload}")
    private String uploadBasePath;

    // 支持的文件类型
    private static final String[] ALLOWED_EXTENSIONS = {"pdf", "doc", "docx", "xls", "xlsx", "txt", "png", "jpg", "jpeg"};
    // 最大文件大小 50MB
    private static final long MAX_FILE_SIZE = 50 * 1024 * 1024;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public KbDocumentVO uploadDocument(String kbId, MultipartFile file, Boolean overwrite) {
        LoginUser loginUser = SecurityUtils.getLoginUser();
        if (loginUser == null) {
            throw new SecurityException("用户未登录");
        }

        // 验证知识库
        KbKnowledgeBase kb = kbMapper.selectByKbId(kbId);
        if (kb == null) {
            throw new IllegalArgumentException("知识库不存在");
        }
        if (!kb.getUserUuid().equals(loginUser.getUserId().toString())) {
            throw new SecurityException("无权访问该知识库");
        }

        // 验证文件
        validateFile(file);

        String originalFilename = file.getOriginalFilename();
        String extension = getFileExtension(originalFilename);
        String fileType = getFileType(extension);

        // 生成存储文件名
        String storageName = UUID.fastUUID().toString(true) + "." + extension;

        // 创建存储目录
        String userDir = Paths.get(uploadBasePath, loginUser.getUserId().toString(), kbId).toString();
        File dir = new File(userDir);
        if (!dir.exists()) {
            dir.mkdirs();
        }

        // 保存文件
        String storagePath = Paths.get(userDir, storageName).toString();
        try {
            file.transferTo(new File(storagePath));
        } catch (IOException e) {
            log.error("文件保存失败: {}", e.getMessage(), e);
            throw new RuntimeException("文件保存失败");
        }

        // 创建文档记录
        KbDocument doc = new KbDocument();
        doc.setDocId("doc_" + UUID.fastUUID().toString(true));
        doc.setKbId(kbId);
        doc.setUserUuid(loginUser.getUserId().toString());
        doc.setOriginalName(originalFilename);
        doc.setStorageName(storageName);
        doc.setFileType(fileType);
        doc.setMimeType(file.getContentType());
        doc.setFileSize(file.getSize());
        doc.setStoragePath(storagePath);
        doc.setFileHash(""); // TODO: 计算MD5
        doc.setProcessStatus(0); // 待处理
        doc.setChunkCount(0);
        doc.setCreatedAt(LocalDateTime.now());
        doc.setUpdatedAt(LocalDateTime.now());

        this.save(doc);

        log.info("文档上传成功: docId={}, name={}", doc.getDocId(), originalFilename);

        // 异步处理文档（提取文本+分块）
        processDocumentAsync(doc, kb);

        return convertToVO(doc);
    }

    @Override
    public List<KbDocumentVO> listByKbId(String kbId) {
        LoginUser loginUser = SecurityUtils.getLoginUser();
        if (loginUser == null) {
            throw new SecurityException("用户未登录");
        }

        List<KbDocument> list = baseMapper.selectByKbId(kbId);
        return list.stream()
                .filter(doc -> doc.getProcessStatus() != 3) // 排除处理失败的
                .map(this::convertToVO)
                .collect(Collectors.toList());
    }

    @Override
    public KbDocumentVO getByDocId(String docId) {
        LoginUser loginUser = SecurityUtils.getLoginUser();
        if (loginUser == null) {
            throw new SecurityException("用户未登录");
        }

        KbDocument doc = baseMapper.selectByDocId(docId);
        if (doc == null) {
            return null;
        }

        // 验证权限
        if (!doc.getUserUuid().equals(loginUser.getUserId().toString())) {
            throw new SecurityException("无权访问该文档");
        }

        return convertToVO(doc);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean deleteDocument(String docId) {
        LoginUser loginUser = SecurityUtils.getLoginUser();
        if (loginUser == null) {
            throw new SecurityException("用户未登录");
        }

        KbDocument doc = baseMapper.selectByDocId(docId);
        if (doc == null) {
            return false;
        }

        if (!doc.getUserUuid().equals(loginUser.getUserId().toString())) {
            throw new SecurityException("无权删除该文档");
        }

        // 删除物理文件
        try {
            File file = new File(doc.getStoragePath());
            if (file.exists()) {
                file.delete();
            }
        } catch (Exception e) {
            log.warn("删除物理文件失败: {}", e.getMessage());
        }

        // 删除分块
        chunkMapper.deleteByFileId(docId);

        // 删除数据库记录
        this.removeById(doc.getId());

        log.info("文档删除成功: docId={}", docId);
        return true;
    }

    @Override
    public boolean reprocessDocument(String docId) {
        LoginUser loginUser = SecurityUtils.getLoginUser();
        if (loginUser == null) {
            throw new SecurityException("用户未登录");
        }

        KbDocument doc = baseMapper.selectByDocId(docId);
        if (doc == null) {
            return false;
        }

        if (!doc.getUserUuid().equals(loginUser.getUserId().toString())) {
            throw new SecurityException("无权操作该文档");
        }

        // 删除旧分块
        chunkMapper.deleteByFileId(docId);

        // 重置状态
        doc.setProcessStatus(0);
        doc.setChunkCount(0);
        doc.setErrorMsg(null);
        this.updateById(doc);

        // 重新处理
        KbKnowledgeBase kb = kbMapper.selectByKbId(doc.getKbId());
        processDocumentAsync(doc, kb);

        log.info("文档重新处理: docId={}", docId);
        return true;
    }

    /**
     * 异步处理文档
     */
    private void processDocumentAsync(KbDocument doc, KbKnowledgeBase kb) {
        // TODO: 使用Spring @Async或线程池实现真正的异步
        // 这里先用同步处理，后续优化
        try {
            processDocument(doc, kb);
        } catch (Exception e) {
            log.error("文档处理失败: docId={}, error={}", doc.getDocId(), e.getMessage(), e);
            doc.setProcessStatus(3); // 失败
            doc.setErrorMsg(e.getMessage());
            this.updateById(doc);
        }
    }

    /**
     * 处理文档：提取文本 -> 分块 -> 保存
     */
    private void processDocument(KbDocument doc, KbKnowledgeBase kb) {
        doc.setProcessStatus(1); // 处理中
        baseMapper.updateStatus(doc.getDocId(), 1);

        // 1. 提取文本
        String text = textExtractionService.extractText(doc.getStoragePath(), doc.getFileType());
        if (text.isEmpty()) {
            throw new RuntimeException("未能从文档中提取文本");
        }

        doc.setCharCount(text.length());
        doc.setPageCount(estimatePageCount(text));

        // 2. 分块
        int chunkSize = kb.getChunkSize() != null ? kb.getChunkSize() : 512;
        int chunkOverlap = kb.getChunkOverlap() != null ? kb.getChunkOverlap() : 50;

        List<KbDocumentChunk> chunks = chunkingService.smartChunk(text, chunkSize, chunkOverlap, doc.getDocId(), doc.getKbId());

        // 3. 保存分块
        if (!chunks.isEmpty()) {
            for (KbDocumentChunk chunk : chunks) {
                chunkMapper.insert(chunk);
            }
        }

        // 4. 更新文档状态
        doc.setProcessStatus(2); // 已完成
        doc.setChunkCount(chunks.size());
        doc.setUpdatedAt(LocalDateTime.now());
        baseMapper.updateProcessResult(doc.getDocId(), 2, text.length(), chunks.size());

        // 5. 更新知识库文档计数
        kbMapper.updateDocCount(kb.getKbId(), 
            baseMapper.selectByKbId(kb.getKbId()).size(), 
            chunks.size()
        );

        log.info("文档处理完成: docId={}, chunks={}", doc.getDocId(), chunks.size());
    }

    /**
     * 验证文件
     */
    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("文件不能为空");
        }

        if (file.getSize() > MAX_FILE_SIZE) {
            throw new IllegalArgumentException("文件大小超过50MB限制");
        }

        String originalFilename = file.getOriginalFilename();
        String extension = getFileExtension(originalFilename);

        boolean allowed = Arrays.stream(ALLOWED_EXTENSIONS)
                .anyMatch(ext -> ext.equalsIgnoreCase(extension));

        if (!allowed) {
            throw new IllegalArgumentException("不支持的文件类型: " + extension);
        }
    }

    /**
     * 获取文件扩展名
     */
    private String getFileExtension(String filename) {
        if (filename == null || filename.lastIndexOf(".") == -1) {
            return "";
        }
        return filename.substring(filename.lastIndexOf(".") + 1).toLowerCase();
    }

    /**
     * 获取文件类型
     */
    private String getFileType(String extension) {
        switch (extension.toLowerCase()) {
            case "pdf": return "pdf";
            case "doc":
            case "docx": return "word";
            case "xls":
            case "xlsx": return "excel";
            case "txt": return "text";
            case "png":
            case "jpg":
            case "jpeg": return "image";
            default: return "unknown";
        }
    }

    /**
     * 估算页数
     */
    private Integer estimatePageCount(String text) {
        // 粗略估算：每页约2000字符
        return Math.max(1, text.length() / 2000);
    }

    /**
     * 转换为VO
     */
    private KbDocumentVO convertToVO(KbDocument doc) {
        KbDocumentVO vo = new KbDocumentVO();
        BeanUtils.copyProperties(doc, vo);

        // 格式化文件大小
        vo.setFileSizeFormatted(formatFileSize(doc.getFileSize()));

        // 状态名称
        switch (doc.getProcessStatus()) {
            case 0: vo.setProcessStatusName("待处理"); break;
            case 1: vo.setProcessStatusName("处理中"); break;
            case 2: vo.setProcessStatusName("已完成"); break;
            case 3: vo.setProcessStatusName("处理失败"); break;
            default: vo.setProcessStatusName("未知"); break;
        }

        return vo;
    }

    /**
     * 格式化文件大小
     */
    private String formatFileSize(Long size) {
        if (size == null) return "0 B";
        if (size < 1024) return size + " B";
        if (size < 1024 * 1024) return String.format("%.2f KB", size / 1024.0);
        if (size < 1024 * 1024 * 1024) return String.format("%.2f MB", size / (1024.0 * 1024));
        return String.format("%.2f GB", size / (1024.0 * 1024 * 1024));
    }
}
