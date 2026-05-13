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
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
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
import com.ruoyi.common.constant.Constants;
import com.ruoyi.common.utils.file.FileUtils;
import com.ruoyi.common.utils.uuid.UUID;
import com.ruoyi.system.domain.lingdoc.LingdocFileIndex;
import com.ruoyi.system.domain.lingdoc.LingdocFormField;
import com.ruoyi.system.domain.lingdoc.FormFieldUpdateRequest;
import com.ruoyi.system.domain.lingdoc.LingdocFormReference;
import com.ruoyi.system.domain.lingdoc.LingdocFormTask;
import com.ruoyi.system.mapper.lingdoc.LingdocFileIndexMapper;
import com.ruoyi.common.enums.FormTaskStatusEnum;
import com.ruoyi.system.service.lingdoc.FormTaskAsyncService;
import com.ruoyi.system.service.lingdoc.ILingdocFormTaskService;

/**
 * 表格填写助手 Controller
 * 
 * @author lingdoc
 */
@RestController
@RequestMapping("/lingdoc/form")
public class LingdocFormController extends BaseController
{
    private static final Logger log = LoggerFactory.getLogger(LingdocFormController.class);

    @Autowired
    private ILingdocFormTaskService formTaskService;

    @Autowired
    private FormTaskAsyncService formTaskAsyncService;

    @Autowired
    private LingdocFileIndexMapper lingdocFileIndexMapper;

    /**
     * 查询表格填写任务列表
     */
    @PreAuthorize("@ss.hasPermi('lingdoc:form:list')")
    @GetMapping("/list")
    public TableDataInfo list(LingdocFormTask lingdocFormTask)
    {
        startPage();
        List<LingdocFormTask> list = formTaskService.selectLingdocFormTaskList(lingdocFormTask);
        return getDataTable(list);
    }

    /**
     * 获取表格填写任务详细信息
     */
    @PreAuthorize("@ss.hasPermi('lingdoc:form:list')")
    @GetMapping(value = "/{taskId}")
    public AjaxResult getInfo(@PathVariable("taskId") String taskId)
    {
        LingdocFormTask task = formTaskService.selectLingdocFormTaskById(taskId);
        if (task != null)
        {
            task.setParams(null);
        }
        AjaxResult ajax = AjaxResult.success();
        ajax.put("data", task);
        ajax.put("fields", formTaskService.selectFormFieldsByTaskId(taskId));
        ajax.put("references", formTaskService.selectFormReferencesByTaskId(taskId));
        return ajax;
    }

    /**
     * 上传表格文件并创建任务
     */
    @PreAuthorize("@ss.hasPermi('lingdoc:form:upload')")
    @Log(title = "表格填写助手", businessType = BusinessType.INSERT)
    @PostMapping("/upload")
    public AjaxResult upload(@RequestParam("file") MultipartFile file,
                             @RequestParam(value = "taskName", required = false) String taskName)
    {
        try
        {
            // 校验文件类型
            String originalFilename = file.getOriginalFilename();
            if (StringUtils.isEmpty(originalFilename))
            {
                return AjaxResult.error("文件名不能为空");
            }
            String ext = originalFilename.substring(originalFilename.lastIndexOf(".") + 1).toLowerCase();
            if (!("pdf".equals(ext) || "docx".equals(ext) || "xlsx".equals(ext) || "doc".equals(ext) || "xls".equals(ext) || "html".equals(ext) || "htm".equals(ext)))
            {
                return AjaxResult.error("不支持的文件格式，请上传 PDF、Word、Excel 或 HTML 文件");
            }

            // 保存文件
            String filePath = RuoYiConfig.getUploadPath() + "/lingdoc/form";
            String fileName = FileUploadUtils.upload(filePath, file);
            String fileId = UUID.fastUUID().toString();

            // 创建任务
            LingdocFormTask task = new LingdocFormTask();
            task.setTaskId(UUID.fastUUID().toString());
            task.setUserId(getUserId());
            task.setTaskName(StringUtils.isNotEmpty(taskName) ? taskName : originalFilename);
            task.setOriginalFileId(fileId);
            task.setOriginalFileUrl(fileName);
            task.setOriginalFileName(originalFilename);
            task.setStatus(FormTaskStatusEnum.EXTRACTING.getCode()); // 识别中
            task.setFieldCount(0);
            task.setConfirmedCount(0);
            task.setTokenCost(0);
            task.setCreateBy(getUsername());
            formTaskService.insertLingdocFormTask(task);

            // 调用 AI 字段识别（异步，不阻塞接口）
            formTaskAsyncService.extractFieldsAsync(task.getTaskId());

            AjaxResult ajax = AjaxResult.success("上传成功，表格字段识别中");
            ajax.put("taskId", task.getTaskId());
            ajax.put("taskName", task.getTaskName());
            ajax.put("originalFileName", originalFilename);
            ajax.put("status", task.getStatus());
            return ajax;
        }
        catch (Exception e)
        {
            log.error("上传表格文件失败", e);
            return AjaxResult.error("上传失败：" + e.getMessage());
        }
    }

    /**
     * 修改表格填写任务
     */
    @PreAuthorize("@ss.hasPermi('lingdoc:form:edit')")
    @Log(title = "表格填写助手", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@RequestBody LingdocFormTask lingdocFormTask)
    {
        return toAjax(formTaskService.updateLingdocFormTask(lingdocFormTask));
    }

    /**
     * 批量更新字段值
     */
    @PreAuthorize("@ss.hasPermi('lingdoc:form:edit')")
    @Log(title = "表格填写助手", businessType = BusinessType.UPDATE)
    @PutMapping("/fields")
    public AjaxResult updateFields(@RequestBody FormFieldUpdateRequest request)
    {
        int count = formTaskService.batchUpdateFormFields(request.getFields());
        return AjaxResult.success("更新成功").put("updatedCount", count);
    }

    /**
     * 触发 AI 生成填写后文档
     */
    @PreAuthorize("@ss.hasPermi('lingdoc:form:generate')")
    @Log(title = "表格填写助手", businessType = BusinessType.UPDATE)
    @PostMapping("/generate")
    public AjaxResult generate(@RequestBody LingdocFormTask taskParam)
    {
        String taskId = taskParam.getTaskId();
        LingdocFormTask task = formTaskService.selectLingdocFormTaskById(taskId);
        if (task == null)
        {
            return AjaxResult.error("任务不存在");
        }

        // 检查状态：只有待确认状态才能生成
        if (!FormTaskStatusEnum.AWAIT_CONFIRM.getCode().equals(task.getStatus()))
        {
            return AjaxResult.error("当前状态不允许生成，请等待识别完成或检查是否已生成");
        }

        // 更新状态为生成中（前端轮询时可见）
        task.setStatus(FormTaskStatusEnum.COMPLETED.getCode());
        formTaskService.updateLingdocFormTask(task);

        // 调用文档生成（异步，不阻塞接口）
        formTaskAsyncService.generateDocumentAsync(taskId);

        return AjaxResult.success("文档生成已提交，正在处理中")
                .put("taskId", taskId)
                .put("status", FormTaskStatusEnum.AI_PROCESSING.getCode());
    }

    /**
     * 查询任务进度（前端轮询用）
     */
    @PreAuthorize("@ss.hasPermi('lingdoc:form:list')")
    @GetMapping("/progress/{taskId}")
    public AjaxResult progress(@PathVariable("taskId") String taskId)
    {
        LingdocFormTask task = formTaskService.selectLingdocFormTaskById(taskId);
        if (task == null)
        {
            return AjaxResult.error("任务不存在");
        }

        AjaxResult ajax = AjaxResult.success();
        ajax.put("taskId", task.getTaskId());
        ajax.put("status", task.getStatus());
        ajax.put("statusName", FormTaskStatusEnum.fromCode(task.getStatus()) != null
                ? FormTaskStatusEnum.fromCode(task.getStatus()).getDesc() : "未知");
        ajax.put("fieldCount", task.getFieldCount());
        ajax.put("confirmedCount", task.getConfirmedCount());
        ajax.put("filledFileUrl", task.getFilledFileUrl());
        ajax.put("filledFileName", task.getFilledFileName());
        ajax.put("errorMsg", task.getErrorMsg());
        ajax.put("tokenCost", task.getTokenCost());
        return ajax;
    }

    /**
     * 获取参考文档列表
     */
    @PreAuthorize("@ss.hasPermi('lingdoc:form:list')")
    @GetMapping("/references/{taskId}")
    public AjaxResult references(@PathVariable("taskId") String taskId)
    {
        List<LingdocFormReference> list = formTaskService.selectFormReferencesByTaskId(taskId);
        return AjaxResult.success(list);
    }

    /**
     * 保存填写后文档到 Vault
     */
    @PreAuthorize("@ss.hasPermi('lingdoc:form:edit')")
    @Log(title = "表格填写助手", businessType = BusinessType.INSERT)
    @PostMapping("/saveToVault")
    public AjaxResult saveToVault(@RequestBody LingdocFormTask taskParam)
    {
        String taskId = taskParam.getTaskId();
        LingdocFormTask task = formTaskService.selectLingdocFormTaskById(taskId);
        if (task == null)
        {
            return AjaxResult.error("任务不存在");
        }
        if (StringUtils.isEmpty(task.getFilledFileUrl()))
        {
            return AjaxResult.error("尚未生成填写后文档，无法保存");
        }

        try
        {
            LingdocFileIndex fileIndex = formTaskService.saveToVault(task, getUserId());

            return AjaxResult.success("已保存到 Vault")
                    .put("taskId", taskId)
                    .put("vaultFileId", fileIndex.getFileId())
                    .put("vaultFileName", fileIndex.getFileName())
                    .put("vaultPath", fileIndex.getVaultPath());
        }
        catch (Exception e)
        {
            log.error("保存到 Vault 失败", e);
            return AjaxResult.error("保存失败：" + e.getMessage());
        }
    }

    /**
     * 下载填写后文档
     */
    @PreAuthorize("@ss.hasPermi('lingdoc:form:download')")
    @GetMapping("/download/{taskId}")
    public void download(@PathVariable("taskId") String taskId, HttpServletResponse response)
    {
        try
        {
            LingdocFormTask task = formTaskService.selectLingdocFormTaskById(taskId);
            if (task == null)
            {
                response.sendError(404, "任务不存在");
                return;
            }

            String filePath = null;
            String downloadName = null;

            // 优先级 1：Vault 归档文件
            if (StringUtils.isNotEmpty(task.getFilledFileId()))
            {
                LingdocFileIndex fileIndex = lingdocFileIndexMapper.selectLingdocFileIndexById(task.getFilledFileId());
                if (fileIndex != null)
                {
                    filePath = fileIndex.getAbsPath();
                    downloadName = fileIndex.getFileName();
                }
            }

            // 优先级 2：临时生成文件
            if (filePath == null && StringUtils.isNotEmpty(task.getFilledFileUrl()))
            {
                filePath = task.getFilledFileUrl().replace(Constants.RESOURCE_PREFIX, RuoYiConfig.getProfile());
                downloadName = StringUtils.isNotEmpty(task.getFilledFileName())
                        ? task.getFilledFileName()
                        : task.getOriginalFileName();
            }

            if (filePath == null)
            {
                response.sendError(404, "文件不存在");
                return;
            }

            response.setContentType("application/octet-stream");
            FileUtils.setAttachmentResponseHeader(response, downloadName);
            FileUtils.writeBytes(filePath, response.getOutputStream());
        }
        catch (IOException e)
        {
            log.error("下载文件失败", e);
        }
    }

    /**
     * 删除表格填写任务
     */
    @PreAuthorize("@ss.hasPermi('lingdoc:form:delete')")
    @Log(title = "表格填写助手", businessType = BusinessType.DELETE)
    @DeleteMapping("/{taskIds}")
    public AjaxResult remove(@PathVariable String[] taskIds)
    {
        return toAjax(formTaskService.deleteLingdocFormTaskByIds(taskIds));
    }
}
