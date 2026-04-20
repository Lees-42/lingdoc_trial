package com.ruoyi.lingdoc.ai.controller;

import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.core.page.TableDataInfo;
import com.ruoyi.lingdoc.ai.domain.vo.KbDocumentVO;
import com.ruoyi.lingdoc.ai.service.IDocumentService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * 文档管理Controller
 */
@Slf4j
@Validated
@RestController
@RequestMapping("/ai/doc")
public class DocumentController extends BaseController {

    @Autowired
    private IDocumentService documentService;

    /**
     * 查询知识库文档列表
     */
    @GetMapping("/list/{kbId}")
    public TableDataInfo list(@PathVariable String kbId) {
        startPage();
        List<KbDocumentVO> list = documentService.listByKbId(kbId);
        return getDataTable(list);
    }

    /**
     * 获取文档详情
     */
    @GetMapping("/{docId}")
    public AjaxResult getInfo(@PathVariable String docId) {
        KbDocumentVO doc = documentService.getByDocId(docId);
        return success(doc);
    }

    /**
     * 上传文档
     */
    @PostMapping("/upload")
    public AjaxResult upload(@RequestParam("kbId") String kbId,
                             @RequestParam("file") MultipartFile file,
                             @RequestParam(value = "overwrite", defaultValue = "false") Boolean overwrite) {
        log.info("上传文档请求: kbId={}, filename={}, size={}", kbId, file.getOriginalFilename(), file.getSize());
        KbDocumentVO doc = documentService.uploadDocument(kbId, file, overwrite);
        return success(doc);
    }

    /**
     * 删除文档
     */
    @DeleteMapping("/{docId}")
    public AjaxResult remove(@PathVariable String docId) {
        log.info("删除文档请求: docId={}", docId);
        boolean result = documentService.deleteDocument(docId);
        return result ? success() : error("文档不存在或无权删除");
    }

    /**
     * 重新处理文档
     */
    @PostMapping("/reprocess/{docId}")
    public AjaxResult reprocess(@PathVariable String docId) {
        log.info("重新处理文档请求: docId={}", docId);
        boolean result = documentService.reprocessDocument(docId);
        return result ? success() : error("文档不存在或无权操作");
    }
}
