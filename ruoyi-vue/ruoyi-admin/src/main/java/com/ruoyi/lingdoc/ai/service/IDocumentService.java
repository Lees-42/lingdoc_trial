package com.ruoyi.lingdoc.ai.service;

import com.ruoyi.lingdoc.ai.domain.dto.DocumentUploadDTO;
import com.ruoyi.lingdoc.ai.domain.vo.KbDocumentVO;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * 文档管理服务接口
 */
public interface IDocumentService {

    /**
     * 上传文档到知识库
     */
    KbDocumentVO uploadDocument(String kbId, MultipartFile file, Boolean overwrite);

    /**
     * 获取知识库文档列表
     */
    List<KbDocumentVO> listByKbId(String kbId);

    /**
     * 获取文档详情
     */
    KbDocumentVO getByDocId(String docId);

    /**
     * 删除文档
     */
    boolean deleteDocument(String docId);

    /**
     * 重新处理文档
     */
    boolean reprocessDocument(String docId);
}
