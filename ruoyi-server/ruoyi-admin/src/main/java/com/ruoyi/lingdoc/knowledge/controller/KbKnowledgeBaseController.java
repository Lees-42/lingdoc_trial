package com.ruoyi.lingdoc.knowledge.controller;

import com.ruoyi.common.annotation.Log;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.core.page.TableDataInfo;
import com.ruoyi.common.enums.BusinessType;
import com.ruoyi.common.utils.SecurityUtils;
import com.ruoyi.lingdoc.knowledge.domain.dto.KbKnowledgeBaseDTO;
import com.ruoyi.lingdoc.knowledge.domain.vo.KbKnowledgeBaseVO;
import com.ruoyi.lingdoc.knowledge.service.IKbKnowledgeBaseService;
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
@RequestMapping("/lingdoc/knowledge")
public class KbKnowledgeBaseController extends BaseController {

    @Autowired
    private IKbKnowledgeBaseService kbService;

    /**
     * 查询知识库列表
     */
    @GetMapping("/list")
    public TableDataInfo list(KbKnowledgeBaseDTO dto) {
        return kbService.listKbKnowledgeBase(dto);
    }

    /**
     * 获取知识库详情
     */
    @GetMapping(value = "/{kbId}")
    public AjaxResult getInfo(@PathVariable String kbId) {
        KbKnowledgeBaseVO vo = kbService.getKbKnowledgeBase(kbId);
        return AjaxResult.success(vo);
    }

    /**
     * 新增知识库
     */
    @Log(title = "知识库", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@RequestBody KbKnowledgeBaseDTO dto) {
        return toAjax(kbService.addKbKnowledgeBase(dto));
    }

    /**
     * 修改知识库
     */
    @Log(title = "知识库", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@RequestBody KbKnowledgeBaseDTO dto) {
        return toAjax(kbService.updateKbKnowledgeBase(dto));
    }

    /**
     * 删除知识库
     */
    @Log(title = "知识库", businessType = BusinessType.DELETE)
    @DeleteMapping("/{kbId}")
    public AjaxResult remove(@PathVariable String kbId) {
        return toAjax(kbService.delKbKnowledgeBase(kbId));
    }

    /**
     * 获取用户可访问的知识库列表（用于下拉选择）
     */
    @GetMapping("/accessible")
    public AjaxResult listAccessible() {
        List<KbKnowledgeBaseVO> list = kbService.listAccessibleKb(SecurityUtils.getLoginUser().getUserId().toString());
        return AjaxResult.success(list);
    }
}
