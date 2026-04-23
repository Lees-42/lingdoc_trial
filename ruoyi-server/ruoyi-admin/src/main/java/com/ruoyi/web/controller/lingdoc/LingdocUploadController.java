package com.ruoyi.web.controller.lingdoc;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import com.ruoyi.common.annotation.Log;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.core.page.TableDataInfo;
import com.ruoyi.common.enums.BusinessType;
import com.ruoyi.system.domain.lingdoc.LingdocFileIndex;
import com.ruoyi.system.domain.lingdoc.LingdocInbox;
import com.ruoyi.system.domain.lingdoc.LingdocUploadConfirmRequest;
import com.ruoyi.system.service.lingdoc.ILingdocInboxService;

/**
 * 上传与自动规整 Controller
 * 
 * @author lingdoc
 */
@RestController
@RequestMapping("/lingdoc/upload")
public class LingdocUploadController extends BaseController
{
    @Autowired
    private ILingdocInboxService inboxService;

    /**
     * 查询收件箱文件列表
     */
    @PreAuthorize("@ss.hasPermi('lingdoc:upload:list')")
    @GetMapping("/list")
    public TableDataInfo list(LingdocInbox query)
    {
        query.setUserId(getUserId());
        startPage();
        List<LingdocInbox> list = inboxService.selectList(query);
        return getDataTable(list);
    }

    /**
     * 上传文件到 inbox
     */
    @PreAuthorize("@ss.hasPermi('lingdoc:upload:upload')")
    @Log(title = "上传临时文件", businessType = BusinessType.INSERT)
    @PostMapping("/upload")
    public AjaxResult upload(@RequestParam("file") MultipartFile file)
    {
        try
        {
            LingdocInbox inbox = inboxService.uploadToInbox(file, getUserId());
            return AjaxResult.success(inbox);
        }
        catch (Exception e)
        {
            return AjaxResult.error(e.getMessage());
        }
    }

    /**
     * 获取文件建议详情
     */
    @PreAuthorize("@ss.hasPermi('lingdoc:upload:list')")
    @GetMapping("/suggestion/{fileId}")
    public AjaxResult suggestion(@PathVariable("fileId") String fileId)
    {
        LingdocInbox inbox = inboxService.selectById(fileId);
        if (inbox == null || !getUserId().equals(inbox.getUserId()))
        {
            return AjaxResult.error("文件不存在或无权限");
        }
        return AjaxResult.success(inbox);
    }

    /**
     * 单文件自动规整
     */
    @PreAuthorize("@ss.hasPermi('lingdoc:upload:organize')")
    @Log(title = "自动规整", businessType = BusinessType.UPDATE)
    @PostMapping("/organize")
    public AjaxResult organize(@RequestBody Map<String, String> body)
    {
        String fileId = body.get("fileId");
        try
        {
            LingdocInbox result = inboxService.organize(fileId, getUserId());
            return AjaxResult.success(result);
        }
        catch (Exception e)
        {
            return AjaxResult.error(e.getMessage());
        }
    }

    /**
     * 批量自动规整
     */
    @PreAuthorize("@ss.hasPermi('lingdoc:upload:organize')")
    @Log(title = "批量自动规整", businessType = BusinessType.UPDATE)
    @PostMapping("/batchOrganize")
    public AjaxResult batchOrganize(@RequestBody List<String> fileIds)
    {
        List<LingdocInbox> results = new ArrayList<>();
        List<String> errors = new ArrayList<>();
        for (String fileId : fileIds)
        {
            try
            {
                results.add(inboxService.organize(fileId, getUserId()));
            }
            catch (Exception e)
            {
                errors.add(fileId + ": " + e.getMessage());
            }
        }
        AjaxResult ajax = AjaxResult.success();
        ajax.put("data", results);
        ajax.put("errors", errors);
        return ajax;
    }

    /**
     * 确认归档
     */
    @PreAuthorize("@ss.hasPermi('lingdoc:upload:confirm')")
    @Log(title = "确认归档", businessType = BusinessType.INSERT)
    @PostMapping("/confirm")
    public AjaxResult confirm(@RequestBody LingdocUploadConfirmRequest request)
    {
        try
        {
            LingdocFileIndex fileIndex = inboxService.confirmToVault(request, getUserId());
            return AjaxResult.success(fileIndex);
        }
        catch (Exception e)
        {
            return AjaxResult.error(e.getMessage());
        }
    }

    /**
     * 批量确认归档
     */
    @PreAuthorize("@ss.hasPermi('lingdoc:upload:confirm')")
    @Log(title = "批量确认归档", businessType = BusinessType.INSERT)
    @PostMapping("/batchConfirm")
    public AjaxResult batchConfirm(@RequestBody List<LingdocUploadConfirmRequest> requests)
    {
        List<LingdocFileIndex> success = new ArrayList<>();
        List<String> errors = new ArrayList<>();
        for (LingdocUploadConfirmRequest req : requests)
        {
            try
            {
                success.add(inboxService.confirmToVault(req, getUserId()));
            }
            catch (Exception e)
            {
                errors.add(req.getFileId() + ": " + e.getMessage());
            }
        }
        AjaxResult ajax = AjaxResult.success();
        ajax.put("success", success);
        ajax.put("errors", errors);
        return ajax;
    }

    /**
     * 删除临时文件
     */
    @PreAuthorize("@ss.hasPermi('lingdoc:upload:delete')")
    @Log(title = "删除临时文件", businessType = BusinessType.DELETE)
    @DeleteMapping("/{fileId}")
    public AjaxResult remove(@PathVariable String fileId)
    {
        return toAjax(inboxService.deleteById(fileId, getUserId()));
    }

    /**
     * 批量删除临时文件
     */
    @PreAuthorize("@ss.hasPermi('lingdoc:upload:delete')")
    @Log(title = "批量删除临时文件", businessType = BusinessType.DELETE)
    @DeleteMapping("/batch")
    public AjaxResult batchRemove(@RequestBody List<String> fileIds)
    {
        return toAjax(inboxService.deleteByIds(fileIds.toArray(new String[0]), getUserId()));
    }

    /**
     * 清空临时文件
     */
    @PreAuthorize("@ss.hasPermi('lingdoc:upload:delete')")
    @Log(title = "清空临时文件", businessType = BusinessType.DELETE)
    @DeleteMapping("/clean")
    public AjaxResult clean()
    {
        return toAjax(inboxService.cleanByUserId(getUserId()));
    }
}
