package com.ruoyi.system.service.lingdoc;

import com.ruoyi.common.enums.FormTaskStatusEnum;
import com.ruoyi.system.domain.lingdoc.LingdocFormTask;
import com.ruoyi.system.mapper.lingdoc.LingdocFormTaskMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

/**
 * 表格填写任务异步处理服务
 *
 * 用途：将 AI 耗时操作（字段识别、文档生成）放到独立线程池执行，
 * 不占用 Tomcat 线程，支持前端轮询获取进度。
 *
 * @author lingdoc
 */
@Service
public class FormTaskAsyncService
{
    private static final Logger log = LoggerFactory.getLogger(FormTaskAsyncService.class);

    @Autowired
    private ILingdocFormTaskService formTaskService;

    @Autowired
    private LingdocFormTaskMapper formTaskMapper;

    /**
     * 异步执行字段识别
     *
     * 调用线程池：aiTaskExecutor（core=8, max=32, queue=100）
     */
    @Async("aiTaskExecutor")
    public void extractFieldsAsync(String taskId)
    {
        log.info("[AI-Async] 开始字段识别, taskId={}", taskId);
        long start = System.currentTimeMillis();

        try
        {
            // 更新状态为 AI 处理中
            updateTaskStatus(taskId, FormTaskStatusEnum.AI_PROCESSING);

            // 调用同步 Service 中的 AI 识别逻辑
            formTaskService.extractFields(taskId);

            long duration = System.currentTimeMillis() - start;
            log.info("[AI-Async] 字段识别完成, taskId={}, duration={}ms", taskId, duration);
        }
        catch (Exception e)
        {
            log.error("[AI-Async] 字段识别失败, taskId={}", taskId, e);
            // 更新任务状态为失败，记录错误信息
            markTaskFailed(taskId, "字段识别失败：" + e.getMessage());
        }
    }

    /**
     * 异步执行文档生成
     *
     * 调用线程池：aiTaskExecutor
     */
    @Async("aiTaskExecutor")
    public void generateDocumentAsync(String taskId)
    {
        log.info("[AI-Async] 开始文档生成, taskId={}", taskId);
        long start = System.currentTimeMillis();

        try
        {
            // 更新状态为 AI 处理中
            updateTaskStatus(taskId, FormTaskStatusEnum.AI_PROCESSING);

            // 调用同步 Service 中的 AI 生成逻辑
            formTaskService.generateDocument(taskId);

            long duration = System.currentTimeMillis() - start;
            log.info("[AI-Async] 文档生成完成, taskId={}, duration={}ms", taskId, duration);
        }
        catch (Exception e)
        {
            log.error("[AI-Async] 文档生成失败, taskId={}", taskId, e);
            markTaskFailed(taskId, "文档生成失败：" + e.getMessage());
        }
    }

    /**
     * 更新任务状态
     */
    private void updateTaskStatus(String taskId, FormTaskStatusEnum status)
    {
        LingdocFormTask task = new LingdocFormTask();
        task.setTaskId(taskId);
        task.setStatus(status.getCode());
        formTaskMapper.updateLingdocFormTask(task);
    }

    /**
     * 标记任务失败
     */
    private void markTaskFailed(String taskId, String errorMsg)
    {
        LingdocFormTask task = new LingdocFormTask();
        task.setTaskId(taskId);
        task.setStatus(FormTaskStatusEnum.FAILED.getCode());
        task.setErrorMsg(errorMsg);
        formTaskMapper.updateLingdocFormTask(task);
    }
}
