package com.ruoyi.system.service.lingdoc;

import java.util.List;
import com.ruoyi.system.domain.lingdoc.PaddleOcrTask;

/**
 * PaddleOCR 识别任务 服务层
 * 
 * @author lingdoc
 */
public interface IPaddleOcrService
{
    /**
     * 查询OCR任务
     * 
     * @param taskId 任务ID
     * @return OCR任务
     */
    public PaddleOcrTask selectPaddleOcrTaskById(String taskId);

    /**
     * 查询OCR任务列表
     * 
     * @param paddleOcrTask 查询条件
     * @return OCR任务集合
     */
    public List<PaddleOcrTask> selectPaddleOcrTaskList(PaddleOcrTask paddleOcrTask);

    /**
     * 新增OCR任务
     * 
     * @param paddleOcrTask OCR任务
     * @return 结果
     */
    public int insertPaddleOcrTask(PaddleOcrTask paddleOcrTask);

    /**
     * 修改OCR任务
     * 
     * @param paddleOcrTask OCR任务
     * @return 结果
     */
    public int updatePaddleOcrTask(PaddleOcrTask paddleOcrTask);

    /**
     * 批量删除OCR任务
     * 
     * @param taskIds 需要删除的任务ID
     * @return 结果
     */
    public int deletePaddleOcrTaskByIds(String[] taskIds);

    /**
     * 删除OCR任务信息
     * 
     * @param taskId 任务ID
     * @return 结果
     */
    public int deletePaddleOcrTaskById(String taskId);

    /**
     * 执行OCR识别（同步）
     * 
     * @param taskId 任务ID
     * @param filePath 文件绝对路径
     * @return 识别结果JSON
     */
    public String executeOcr(String taskId, String filePath);

    /**
     * 执行OCR识别（异步）
     * 
     * @param taskId 任务ID
     * @param filePath 文件绝对路径
     */
    public void executeOcrAsync(String taskId, String filePath);
}
