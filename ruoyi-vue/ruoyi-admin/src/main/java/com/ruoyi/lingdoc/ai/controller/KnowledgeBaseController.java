package com.ruoyi.lingdoc.ai.controller;

import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.core.page.TableDataInfo;
import com.ruoyi.lingdoc.ai.domain.dto.KbCreateDTO;
import com.ruoyi.lingdoc.ai.domain.dto.KbUpdateDTO;
import com.ruoyi.lingdoc.ai.domain.vo.KbKnowledgeBaseVO;
import com.ruoyi.lingdoc.ai.service.IKnowledgeBaseService;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 知识库Controller
 */
@Slf4j
@Validated
@RestController
@RequestMapping("/ai/kb")
public class KnowledgeBaseController extends BaseController {

    @Autowired
    private IKnowledgeBaseService knowledgeBaseService;

    /**
     * 查询知识库列表
     */
    @GetMapping("/list")
    public TableDataInfo list() {
        startPage();
        List<KbKnowledgeBaseVO> list = knowledgeBaseService.listByCurrentUser();
        return getDataTable(list);
    }

    /**
     * 获取知识库详情
     */
    @GetMapping("/{kbId}")
    public AjaxResult getInfo(@PathVariable String kbId) {
        KbKnowledgeBaseVO kb = knowledgeBaseService.getByKbId(kbId);
        return success(kb);
    }

    /**
     * 创建知识库
     */
    @PostMapping("/create")
    public AjaxResult add(@Valid @RequestBody KbCreateDTO dto) {
        log.info("创建知识库请求: name={}", dto.getKbName());
        KbKnowledgeBaseVO created = knowledgeBaseService.createKnowledgeBase(dto);
        return success(created);
    }

    /**
     * 修改知识库
     */
    @PutMapping("/{kbId}")
    public AjaxResult edit(@PathVariable String kbId, @Valid @RequestBody KbUpdateDTO dto) {
        log.info("更新知识库请求: kbId={}", kbId);
        boolean result = knowledgeBaseService.updateKnowledgeBase(kbId, dto);
        return result ? success() : error("知识库不存在或无权修改");
    }

    /**
     * 删除知识库
     */
    @DeleteMapping("/{kbId}")
    public AjaxResult remove(@PathVariable String kbId) {
        log.info("删除知识库请求: kbId={}", kbId);
        boolean result = knowledgeBaseService.deleteKnowledgeBase(kbId);
        return result ? success() : error("知识库不存在或无权删除");
    }
}
