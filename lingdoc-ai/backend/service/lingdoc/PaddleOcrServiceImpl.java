package com.ruoyi.system.service.lingdoc.impl;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.system.domain.lingdoc.PaddleOcrTask;
import com.ruoyi.system.mapper.lingdoc.PaddleOcrTaskMapper;
import com.ruoyi.system.service.lingdoc.IPaddleOcrService;

/**
 * PaddleOCR 识别任务 Service实现
 * 
 * 支持两种调用模式：
 * 1. 进程模式：通过 ProcessBuilder 调用本地 Python 脚本
 * 2. HTTP模式：通过 REST API 调用 PaddleOCR 服务（预留）
 * 
 * @author lingdoc
 */
@Service
public class PaddleOcrServiceImpl implements IPaddleOcrService
{
    private static final Logger log = LoggerFactory.getLogger(PaddleOcrServiceImpl.class);

    @Autowired
    private PaddleOcrTaskMapper paddleOcrTaskMapper;

    /** PaddleOCR Python 脚本路径 */
    @Value("${lingdoc.ocr.python.script:lingdoc-ai/backend/python/ocr_service.py}")
    private String pythonScriptPath;

    /** Python 可执行文件路径 */
    @Value("${lingdoc.ocr.python.executable:python3}")
    private String pythonExecutable;

    /** 调用模式：process/http */
    @Value("${lingdoc.ocr.mode:process}")
    private String ocrMode;

    /** 置信度阈值 */
    @Value("${lingdoc.ocr.confidence.threshold:0.5}")
    private double confidenceThreshold;

    /** PDF转图片DPI */
    @Value("${lingdoc.ocr.pdf.dpi:150}")
    private int pdfDpi;

    @Override
    public PaddleOcrTask selectPaddleOcrTaskById(String taskId)
    {
        return paddleOcrTaskMapper.selectPaddleOcrTaskById(taskId);
    }

    @Override
    public List<PaddleOcrTask> selectPaddleOcrTaskList(PaddleOcrTask paddleOcrTask)
    {
        return paddleOcrTaskMapper.selectPaddleOcrTaskList(paddleOcrTask);
    }

    @Override
    public int insertPaddleOcrTask(PaddleOcrTask paddleOcrTask)
    {
        return paddleOcrTaskMapper.insertPaddleOcrTask(paddleOcrTask);
    }

    @Override
    public int updatePaddleOcrTask(PaddleOcrTask paddleOcrTask)
    {
        return paddleOcrTaskMapper.updatePaddleOcrTask(paddleOcrTask);
    }

    @Override
    public int deletePaddleOcrTaskByIds(String[] taskIds)
    {
        return paddleOcrTaskMapper.deletePaddleOcrTaskByIds(taskIds);
    }

    @Override
    public int deletePaddleOcrTaskById(String taskId)
    {
        return paddleOcrTaskMapper.deletePaddleOcrTaskById(taskId);
    }

    @Override
    public String executeOcr(String taskId, String filePath)
    {
        log.info("[OCR] 开始同步识别，任务ID: {}, 文件: {}", taskId, filePath);
        long startTime = System.currentTimeMillis();

        try
        {
            // 更新任务状态为处理中
            PaddleOcrTask task = new PaddleOcrTask();
            task.setTaskId(taskId);
            task.setStatus("1");
            paddleOcrTaskMapper.updatePaddleOcrTask(task);

            String resultJson;
            if ("http".equals(ocrMode))
            {
                resultJson = executeOcrViaHttp(filePath);
            }
            else
            {
                resultJson = executeOcrViaProcess(filePath);
            }

            long processTime = System.currentTimeMillis() - startTime;

            // 解析结果统计信息
            OcrStatistics stats = parseStatistics(resultJson);

            // 更新任务状态为成功
            task.setStatus("2");
            task.setResultJson(resultJson);
            task.setProcessTime(processTime);
            task.setPageCount(stats.pageCount);
            task.setCharCount(stats.charCount);
            if (stats.pageCount > 0)
            {
                task.setAvgPageTime(processTime / stats.pageCount);
            }
            paddleOcrTaskMapper.updatePaddleOcrTask(task);

            log.info("[OCR] 识别完成，任务ID: {}, 耗时: {}ms, 页数: {}, 字符数: {}",
                    taskId, processTime, stats.pageCount, stats.charCount);

            return resultJson;
        }
        catch (Exception e)
        {
            long processTime = System.currentTimeMillis() - startTime;
            log.error("[OCR] 识别失败，任务ID: {}, 耗时: {}ms, 原因: {}", taskId, processTime, e.getMessage(), e);

            PaddleOcrTask task = new PaddleOcrTask();
            task.setTaskId(taskId);
            task.setStatus("3");
            task.setErrorMsg(StringUtils.abbreviate(e.getMessage(), 500));
            task.setProcessTime(processTime);
            paddleOcrTaskMapper.updatePaddleOcrTask(task);

            throw new RuntimeException("OCR识别失败: " + e.getMessage(), e);
        }
    }

    @Override
    @Async
    public void executeOcrAsync(String taskId, String filePath)
    {
        CompletableFuture.runAsync(() -> executeOcr(taskId, filePath));
    }

    /**
     * 通过进程调用执行OCR
     */
    private String executeOcrViaProcess(String filePath) throws Exception
    {
        File scriptFile = new File(pythonScriptPath);
        if (!scriptFile.exists())
        {
            // 尝试从 classpath 查找
            String classPath = System.getProperty("user.dir");
            scriptFile = new File(classPath, pythonScriptPath);
        }

        if (!scriptFile.exists())
        {
            throw new RuntimeException("PaddleOCR Python脚本未找到: " + pythonScriptPath);
        }

        ProcessBuilder pb = new ProcessBuilder(
                pythonExecutable,
                scriptFile.getAbsolutePath(),
                "--file", filePath,
                "--dpi", String.valueOf(pdfDpi),
                "--threshold", String.valueOf(confidenceThreshold),
                "--format", "json"
        );

        pb.redirectErrorStream(true);
        Process process = pb.start();

        StringBuilder output = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8)))
        {
            String line;
            while ((line = reader.readLine()) != null)
            {
                output.append(line).append("\n");
            }
        }

        int exitCode = process.waitFor();
        if (exitCode != 0)
        {
            throw new RuntimeException("PaddleOCR进程退出码: " + exitCode + ", 输出: " + output);
        }

        // 提取 JSON 输出（Python 脚本最后输出 JSON 结果）
        String fullOutput = output.toString();
        int jsonStart = fullOutput.lastIndexOf("{");
        int jsonEnd = fullOutput.lastIndexOf("}");

        if (jsonStart >= 0 && jsonEnd > jsonStart)
        {
            return fullOutput.substring(jsonStart, jsonEnd + 1);
        }

        // 如果找不到 JSON，返回完整输出
        return fullOutput;
    }

    /**
     * 通过HTTP调用执行OCR（预留，用于后续接入独立OCR服务）
     */
    private String executeOcrViaHttp(String filePath)
    {
        throw new UnsupportedOperationException("HTTP模式尚未实现，请先使用process模式");
    }

    /**
     * 解析OCR结果统计信息
     */
    private OcrStatistics parseStatistics(String resultJson)
    {
        OcrStatistics stats = new OcrStatistics();
        stats.pageCount = 0;
        stats.charCount = 0;

        try
        {
            JSONObject root = JSON.parseObject(resultJson);
            JSONArray pages = root.getJSONArray("pages");
            if (pages != null)
            {
                stats.pageCount = pages.size();
                for (int i = 0; i < pages.size(); i++)
                {
                    JSONArray lines = pages.getJSONObject(i).getJSONArray("lines");
                    if (lines != null)
                    {
                        for (int j = 0; j < lines.size(); j++)
                        {
                            String text = lines.getJSONObject(j).getString("text");
                            if (text != null)
                            {
                                stats.charCount += text.length();
                            }
                        }
                    }
                }
            }
        }
        catch (Exception e)
        {
            log.warn("[OCR] 解析统计信息失败: {}", e.getMessage());
        }

        return stats;
    }

    /**
     * OCR统计信息内部类
     */
    private static class OcrStatistics
    {
        int pageCount;
        int charCount;
    }
}
