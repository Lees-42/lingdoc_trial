package com.ruoyi.web.controller.lingdoc;

import java.io.IOException;
import java.util.List;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import com.ruoyi.common.annotation.Log;
import com.ruoyi.common.config.RuoYiConfig;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.core.page.TableDataInfo;
import com.ruoyi.common.enums.BusinessType;
import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.common.utils.file.FileUploadUtils;
import com.ruoyi.common.utils.uuid.UUID;
import com.ruoyi.system.domain.lingdoc.PaddleOcrTask;
import com.ruoyi.system.service.lingdoc.IPaddleOcrService;

/**
 * PaddleOCR 文档识别 Controller
 * 
 * 提供文件上传、OCR识别、结果查询、任务管理等功能。
 * 
 * @author lingdoc
 */
@RestController
@RequestMapping("/lingdoc/ocr")
public class PaddleOcrController extends BaseController
{
    private static final Logger log = LoggerFactory.getLogger(PaddleOcrController.class);

    @Autowired
    private IPaddleOcrService paddleOcrService;

    /** 支持上传的文件类型 */
    private static final String[] SUPPORTED_EXTS = {"pdf", "docx", "doc", "jpg", "jpeg", "png", "bmp"};

    /**
     * 查询OCR任务列表
     */
    @PreAuthorize("@ss.hasPermi('lingdoc:ocr:list')")
    @GetMapping("/list")
    public TableDataInfo list(PaddleOcrTask paddleOcrTask)
    {
        startPage();
        paddleOcrTask.setUserId(getUserId());
        List<PaddleOcrTask> list = paddleOcrService.selectPaddleOcrTaskList(paddleOcrTask);
        return getDataTable(list);
    }

    /**
     * 获取OCR任务详情（含识别结果）
     */
    @PreAuthorize("@ss.hasPermi('lingdoc:ocr:list')")
    @GetMapping(value = "/{taskId}")
    public AjaxResult getInfo(@PathVariable("taskId") String taskId)
    {
        PaddleOcrTask task = paddleOcrService.selectPaddleOcrTaskById(taskId);
        if (task == null)
        {
            return AjaxResult.error("任务不存在");
        }
        return AjaxResult.success(task);
    }

    /**
     * 上传文件并执行OCR识别
     * 
     * 支持格式：PDF、Word(docx/doc)、图片(jpg/png/bmp)
     */
    @PreAuthorize("@ss.hasPermi('lingdoc:ocr:add')")
    @Log(title = "PaddleOCR识别", businessType = BusinessType.INSERT)
    @PostMapping("/upload")
    public AjaxResult upload(@RequestParam("file") MultipartFile file,
                             @RequestParam(value = "taskName", required = false) String taskName,
                             @RequestParam(value = "async", required = false, defaultValue = "false") boolean async)
    {
        try
        {
            // 校验文件名
            String originalFilename = file.getOriginalFilename();
            if (StringUtils.isEmpty(originalFilename))
            {
                return AjaxResult.error("文件名不能为空");
            }

            // 校验文件类型
            String ext = originalFilename.substring(originalFilename.lastIndexOf(".") + 1).toLowerCase();
            boolean supported = false;
            for (String supportedExt : SUPPORTED_EXTS)
            {
                if (supportedExt.equals(ext))
                {
                    supported = true;
                    break;
                }
            }
            if (!supported)
            {
                return AjaxResult.error("不支持的文件格式，请上传 PDF、Word 或图片文件");
            }

            // 保存文件
            String filePath = RuoYiConfig.getUploadPath() + "/lingdoc/ocr";
            String fileName = FileUploadUtils.upload(filePath, file);
            String absolutePath = filePath + "/" + fileName;

            // 创建任务
            String taskId = UUID.fastUUID().toString();
            PaddleOcrTask task = new PaddleOcrTask();
            task.setTaskId(taskId);
            task.setUserId(getUserId());
            task.setTaskName(StringUtils.isNotEmpty(taskName) ? taskName : originalFilename);
            task.setFileUrl(fileName);
            task.setFileName(originalFilename);
            task.setFileType(ext);
            task.setStatus("0"); // 待处理
            task.setCreateBy(getUsername());
            paddleOcrService.insertPaddleOcrTask(task);

            // 执行OCR
            if (async)
            {
                paddleOcrService.executeOcrAsync(taskId, absolutePath);
                AjaxResult ajax = AjaxResult.success("文件已上传，异步识别中...");
                ajax.put("taskId", taskId);
                ajax.put("status", "1");
                ajax.put("async", true);
                return ajax;
            }
            else
            {
                String resultJson = paddleOcrService.executeOcr(taskId, absolutePath);
                AjaxResult ajax = AjaxResult.success("识别完成");
                ajax.put("taskId", taskId);
                ajax.put("status", "2");
                ajax.put("result", resultJson);
                return ajax;
            }
        }
        catch (Exception e)
        {
            log.error("OCR识别失败", e);
            return AjaxResult.error("识别失败：" + e.getMessage());
        }
    }

    /**
     * 对已有任务重新执行OCR
     */
    @PreAuthorize("@ss.hasPermi('lingdoc:ocr:edit')")
    @Log(title = "PaddleOCR识别", businessType = BusinessType.UPDATE)
    @PostMapping("/reprocess/{taskId}")
    public AjaxResult reprocess(@PathVariable("taskId") String taskId)
    {
        PaddleOcrTask task = paddleOcrService.selectPaddleOcrTaskById(taskId);
        if (task == null)
        {
            return AjaxResult.error("任务不存在");
        }

        String filePath = RuoYiConfig.getUploadPath() + "/lingdoc/ocr/" + task.getFileUrl();
        try
        {
            String resultJson = paddleOcrService.executeOcr(taskId, filePath);
            return AjaxResult.success("重新识别完成").put("result", resultJson);
        }
        catch (Exception e)
        {
            log.error("重新识别失败，任务ID: {}", taskId, e);
            return AjaxResult.error("重新识别失败：" + e.getMessage());
        }
    }

    /**
     * 删除OCR任务
     */
    @PreAuthorize("@ss.hasPermi('lingdoc:ocr:delete')")
    @Log(title = "PaddleOCR识别", businessType = BusinessType.DELETE)
    @DeleteMapping("/{taskIds}")
    public AjaxResult remove(@PathVariable String[] taskIds)
    {
        return toAjax(paddleOcrService.deletePaddleOcrTaskByIds(taskIds));
    }
}
