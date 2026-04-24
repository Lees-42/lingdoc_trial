package com.ruoyi.system.service.lingdoc;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.apache.poi.xssf.usermodel.XSSFCell;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFRun;
import org.apache.poi.xwpf.usermodel.XWPFTable;
import org.apache.poi.xwpf.usermodel.XWPFTableCell;
import org.apache.poi.xwpf.usermodel.XWPFTableRow;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTP;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.ruoyi.common.config.RuoYiConfig;
import com.ruoyi.common.constant.Constants;
import com.ruoyi.common.utils.DateUtils;
import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.common.utils.uuid.UUID;
import com.ruoyi.system.domain.lingdoc.LingdocFileIndex;
import com.ruoyi.system.domain.lingdoc.LingdocFormField;
import com.ruoyi.system.domain.lingdoc.LingdocFormReference;
import com.ruoyi.system.domain.lingdoc.LingdocFormTask;
import com.ruoyi.system.domain.lingdoc.ai.AiExtractResult;
import com.ruoyi.system.domain.lingdoc.ai.AiField;
import com.ruoyi.system.domain.lingdoc.ai.AiGenerateResult;
import com.ruoyi.system.domain.lingdoc.ai.AiReference;
import com.ruoyi.system.mapper.lingdoc.LingdocFileIndexMapper;
import com.ruoyi.system.mapper.lingdoc.LingdocFormFieldMapper;
import com.ruoyi.system.mapper.lingdoc.LingdocFormReferenceMapper;
import com.ruoyi.system.mapper.lingdoc.LingdocFormTaskMapper;
import com.ruoyi.system.service.lingdoc.ai.IAiFormService;

/**
 * 表格填写任务 服务层实现
 * 
 * @author lingdoc
 */
@Service
public class LingdocFormTaskServiceImpl implements ILingdocFormTaskService
{
    @Autowired
    private LingdocFormTaskMapper formTaskMapper;

    @Autowired
    private LingdocFormFieldMapper formFieldMapper;

    @Autowired
    private LingdocFormReferenceMapper formReferenceMapper;

    @Autowired
    private LingdocFileIndexMapper lingdocFileIndexMapper;

    @Autowired
    private IAiFormService aiFormService;

    /**
     * 查询表格填写任务
     * 
     * @param taskId 任务ID
     * @return 表格填写任务
     */
    @Override
    public LingdocFormTask selectLingdocFormTaskById(String taskId)
    {
        return formTaskMapper.selectLingdocFormTaskById(taskId);
    }

    /**
     * 查询表格填写任务列表
     * 
     * @param lingdocFormTask 表格填写任务
     * @return 表格填写任务集合
     */
    @Override
    public List<LingdocFormTask> selectLingdocFormTaskList(LingdocFormTask lingdocFormTask)
    {
        return formTaskMapper.selectLingdocFormTaskList(lingdocFormTask);
    }

    /**
     * 新增表格填写任务
     * 
     * @param lingdocFormTask 表格填写任务
     * @return 结果
     */
    @Override
    public int insertLingdocFormTask(LingdocFormTask lingdocFormTask)
    {
        return formTaskMapper.insertLingdocFormTask(lingdocFormTask);
    }

    /**
     * 修改表格填写任务
     * 
     * @param lingdocFormTask 表格填写任务
     * @return 结果
     */
    @Override
    public int updateLingdocFormTask(LingdocFormTask lingdocFormTask)
    {
        return formTaskMapper.updateLingdocFormTask(lingdocFormTask);
    }

    /**
     * 批量删除表格填写任务
     * 
     * @param taskIds 需要删除的任务ID
     * @return 结果
     */
    @Override
    @Transactional
    public int deleteLingdocFormTaskByIds(String[] taskIds)
    {
        for (String taskId : taskIds)
        {
            formFieldMapper.deleteLingdocFormFieldByTaskId(taskId);
            formReferenceMapper.deleteLingdocFormReferenceByTaskId(taskId);
        }
        return formTaskMapper.deleteLingdocFormTaskByIds(taskIds);
    }

    /**
     * 删除表格填写任务信息
     * 
     * @param taskId 任务ID
     * @return 结果
     */
    @Override
    @Transactional
    public int deleteLingdocFormTaskById(String taskId)
    {
        formFieldMapper.deleteLingdocFormFieldByTaskId(taskId);
        formReferenceMapper.deleteLingdocFormReferenceByTaskId(taskId);
        return formTaskMapper.deleteLingdocFormTaskById(taskId);
    }

    /**
     * 查询任务的字段列表
     * 
     * @param taskId 任务ID
     * @return 字段列表
     */
    @Override
    public List<LingdocFormField> selectFormFieldsByTaskId(String taskId)
    {
        return formFieldMapper.selectLingdocFormFieldByTaskId(taskId);
    }

    /**
     * 批量更新字段值
     * 
     * @param fields 字段列表
     * @return 结果
     */
    @Override
    public int batchUpdateFormFields(List<LingdocFormField> fields)
    {
        if (fields == null || fields.isEmpty())
        {
            return 0;
        }
        return formFieldMapper.batchUpdateLingdocFormField(fields);
    }

    /**
     * 查询任务的参考文档列表
     * 
     * @param taskId 任务ID
     * @return 参考文档列表
     */
    @Override
    public List<LingdocFormReference> selectFormReferencesByTaskId(String taskId)
    {
        return formReferenceMapper.selectLingdocFormReferenceByTaskId(taskId);
    }

    /**
     * 字段识别：调用 AI 提取表格字段
     * <p>
     * AI 调用部分由另一位开发者实现（{@link IAiFormService#extract}）。
     * 当前框架负责：读取文件 → 调用 AI → 解析结果 → 写入数据库 → 更新任务状态。
     */
    @Override
    @Transactional
    public void extractFields(String taskId)
    {
        // 1. 根据 taskId 读取原始文件路径
        LingdocFormTask task = formTaskMapper.selectLingdocFormTaskById(taskId);
        if (task == null)
        {
            throw new RuntimeException("任务不存在：" + taskId);
        }
        String filePath = task.getOriginalFileUrl().replace(Constants.RESOURCE_PREFIX, RuoYiConfig.getProfile());

        // 2. 调用 Dify AI 工作流识别表格字段
        //    由 DifyAiFormServiceImpl 实现，调用 form-extract 工作流
        AiExtractResult result = aiFormService.extract(filePath, task.getOriginalFileName());

        // 3. 清空该 task 已有的 fields 和 references（防重复）
        formFieldMapper.deleteLingdocFormFieldByTaskId(taskId);
        formReferenceMapper.deleteLingdocFormReferenceByTaskId(taskId);

        // 4. 将 AI 返回的 JSON 中的 fields 写入 lingdoc_form_field
        if (result.getFields() != null)
        {
            for (AiField field : result.getFields())
            {
                LingdocFormField formField = new LingdocFormField();
                formField.setFieldId(UUID.fastUUID().toString());
                formField.setTaskId(taskId);
                formField.setFieldName(field.getFieldName());
                formField.setFieldType(field.getFieldType());
                formField.setFieldLabel(field.getFieldLabel());
                formField.setAiValue(field.getSuggestedValue());
                formField.setConfidence(field.getConfidence() != null ? field.getConfidence() : BigDecimal.ZERO);
                formField.setSourceDocId(field.getSourceDocId());
                formField.setSourceDocName(field.getSourceDocName());
                formField.setSortOrder(field.getSortOrder());
                formField.setIsConfirmed("0");
                formFieldMapper.insertLingdocFormField(formField);
            }
        }

        // 5. 将 references 写入 lingdoc_form_reference
        if (result.getReferences() != null)
        {
            for (AiReference ref : result.getReferences())
            {
                LingdocFormReference formRef = new LingdocFormReference();
                formRef.setRefId(UUID.fastUUID().toString());
                formRef.setTaskId(taskId);
                formRef.setDocId(ref.getDocId());
                formRef.setDocName(ref.getDocName());
                formRef.setDocPath(ref.getDocPath());
                formRef.setDocType(ref.getDocType());
                formRef.setRelevance(ref.getRelevance() != null ? ref.getRelevance() : BigDecimal.ZERO);
                formRef.setIsSelected("1");
                formReferenceMapper.insertLingdocFormReference(formRef);
            }
        }

        // 6. 更新任务状态为「待确认:2」
        task.setStatus("2");
        task.setFieldCount(result.getFields() != null ? result.getFields().size() : 0);
        task.setTokenCost(result.getTokenCost() != null ? result.getTokenCost() : 0);
        formTaskMapper.updateLingdocFormTask(task);
    }

    /**
     * 文档生成：调用 AI 生成填写后的文档
     * <p>
     * 采用方式 A：AI 直接生成填写后的文件，返回文件路径。
     * 后端负责：读取文件 → 调用 AI → 接收 AI 生成的文件 → 复制到标准目录 → 更新任务状态。
     * <p>
     * 对于 HTML 格式，保留方式 B 的 Jsoup 渲染作为备用路径。
     */
    @Override
    @Transactional
    public String generateDocument(String taskId)
    {
        // 1. 读取原始文件
        LingdocFormTask task = formTaskMapper.selectLingdocFormTaskById(taskId);
        if (task == null)
        {
            throw new RuntimeException("任务不存在：" + taskId);
        }
        String originalPath = task.getOriginalFileUrl().replace(Constants.RESOURCE_PREFIX, RuoYiConfig.getProfile());

        // 2. 获取所有已确认的字段
        List<LingdocFormField> allFields = formFieldMapper.selectLingdocFormFieldByTaskId(taskId);
        List<LingdocFormField> confirmedFields = new ArrayList<>();
        if (allFields != null)
        {
            for (LingdocFormField field : allFields)
            {
                if ("1".equals(field.getIsConfirmed()))
                {
                    confirmedFields.add(field);
                }
            }
        }

        // 3. 调用 AI 接口生成文档
        AiGenerateResult result = aiFormService.generate(taskId, originalPath, confirmedFields);

        // 4. 准备输出目录和文件名
        String originalFileName = task.getOriginalFileName();
        String filledFileName = generateFilledFileName(originalFileName);
        String filledDir = RuoYiConfig.getUploadPath() + "/lingdoc/form/filled/" + DateUtils.datePath();
        File filledDirFile = new File(filledDir);
        if (!filledDirFile.exists())
        {
            filledDirFile.mkdirs();
        }
        String filledPath = filledDir + "/" + filledFileName;

        // 5. 优先使用方式 A：AI 直接生成的文件
        if (StringUtils.isNotEmpty(result.getFilledFilePath()))
        {
            try
            {
                org.apache.commons.io.FileUtils.copyFile(new File(result.getFilledFilePath()), new File(filledPath));
            }
            catch (IOException e)
            {
                throw new RuntimeException("复制 AI 生成文件失败：" + e.getMessage(), e);
            }
        }
        else
        {
            // 方式 B：后端根据 filledValues 本地渲染
            String ext = "";
            if (StringUtils.isNotEmpty(originalFileName) && originalFileName.lastIndexOf(".") > 0)
            {
                ext = originalFileName.substring(originalFileName.lastIndexOf(".") + 1).toLowerCase();
            }

            switch (ext)
            {
                case "html":
                case "htm":
                    renderHtml(originalPath, filledPath, result.getFilledValues());
                    break;
                case "docx":
                    renderDocx(originalPath, filledPath, result.getFilledValues());
                    break;
                case "xlsx":
                case "xls":
                    renderXlsx(originalPath, filledPath, result.getFilledValues());
                    break;
                default:
                    // 未支持格式：复制原文件并记录日志
                    log.warn("不支持的文件格式进行本地渲染: ext={}, 仅复制原文件", ext);
                    try
                    {
                        org.apache.commons.io.FileUtils.copyFile(new File(originalPath), new File(filledPath));
                    }
                    catch (IOException e)
                    {
                        throw new RuntimeException("复制文件失败：" + e.getMessage(), e);
                    }
                    break;
            }
        }

        // 6. 更新任务状态
        // 生成与 FileUploadUtils.getPathFileName 一致的 URL 格式：/profile/upload/lingdoc/form/filled/...
        String filledFileUrl = Constants.RESOURCE_PREFIX + "/upload/lingdoc/form/filled/" + DateUtils.datePath() + "/" + filledFileName;
        task.setStatus("3");
        task.setFilledFileUrl(filledFileUrl);
        task.setFilledFileName(filledFileName);
        int totalTokenCost = (task.getTokenCost() != null ? task.getTokenCost() : 0)
                + (result.getTokenCost() != null ? result.getTokenCost() : 0);
        task.setTokenCost(totalTokenCost);
        formTaskMapper.updateLingdocFormTask(task);

        return filledPath;
    }

    /**
     * 生成填写后的文件名
     */
    private String generateFilledFileName(String originalFileName)
    {
        if (StringUtils.isEmpty(originalFileName))
        {
            return "filled_" + DateUtils.dateTimeNow() + ".html";
        }
        return originalFileName.replaceAll("\\.(?=[^\\.]+$)", "_已填写_" + DateUtils.dateTimeNow() + ".$0");
    }

    /**
     * HTML 渲染：将 .blank 占位符替换为 .filled 实际值
     */
    private void renderHtml(String originalPath, String filledPath, Map<String, String> filledValues)
    {
        try
        {
            String htmlContent = org.apache.commons.io.FileUtils.readFileToString(new File(originalPath), StandardCharsets.UTF_8);
            Document doc = Jsoup.parse(htmlContent);
            Elements thElements = doc.select("th");
            for (Element th : thElements)
            {
                String fieldName = th.text().trim();
                if (filledValues != null && filledValues.containsKey(fieldName))
                {
                    Element td = th.parent().select("td.blank").first();
                    if (td != null)
                    {
                        td.removeClass("blank").addClass("filled");
                        td.text(filledValues.get(fieldName));
                    }
                }
            }
            org.apache.commons.io.FileUtils.writeStringToFile(new File(filledPath), doc.html(), StandardCharsets.UTF_8);
        }
        catch (IOException e)
        {
            throw new RuntimeException("HTML 渲染失败：" + e.getMessage(), e);
        }
    }

    /**
     * Word (docx) 渲染：将 {{字段名}} 占位符替换为实际值
     * <p>
     * 模板中需要填写值的位置请使用占位符格式：{{字段名}}
     * 例如：姓名：{{姓名}}  或  单元格内直接写 {{姓名}}
     */
    private void renderDocx(String originalPath, String filledPath, Map<String, String> filledValues)
    {
        try (FileInputStream fis = new FileInputStream(originalPath);
             XWPFDocument doc = new XWPFDocument(fis);
             FileOutputStream fos = new FileOutputStream(filledPath))
        {
            int replaceCount = 0;

            // 1. 处理所有段落中的占位符
            for (XWPFParagraph para : doc.getParagraphs())
            {
                replaceCount += replaceInParagraph(para, filledValues);
            }

            // 2. 处理所有表格中的占位符
            for (XWPFTable table : doc.getTables())
            {
                for (XWPFTableRow row : table.getRows())
                {
                    for (XWPFTableCell cell : row.getTableCells())
                    {
                        for (XWPFParagraph para : cell.getParagraphs())
                        {
                            replaceCount += replaceInParagraph(para, filledValues);
                        }
                    }
                }
            }

            doc.write(fos);
            log.info("DOCX 渲染完成, path={}, 替换了 {} 处占位符", filledPath, replaceCount);
        }
        catch (IOException e)
        {
            throw new RuntimeException("DOCX 渲染失败：" + e.getMessage(), e);
        }
    }

    /**
     * Excel (xlsx) 渲染：将 {{字段名}} 占位符替换为实际值
     * <p>
     * 模板中需要填写值的位置请使用占位符格式：{{字段名}}
     */
    private void renderXlsx(String originalPath, String filledPath, Map<String, String> filledValues)
    {
        try (FileInputStream fis = new FileInputStream(originalPath);
             XSSFWorkbook wb = new XSSFWorkbook(fis);
             FileOutputStream fos = new FileOutputStream(filledPath))
        {
            int replaceCount = 0;

            for (int i = 0; i < wb.getNumberOfSheets(); i++)
            {
                XSSFSheet sheet = wb.getSheetAt(i);
                for (XSSFRow row : sheet)
                {
                    if (row == null) continue;
                    for (XSSFCell cell : row)
                    {
                        if (cell == null) continue;
                        String cellValue = cell.getStringCellValue();
                        if (StringUtils.isEmpty(cellValue)) continue;

                        String newValue = replacePlaceholders(cellValue, filledValues);
                        if (!cellValue.equals(newValue))
                        {
                            cell.setCellValue(newValue);
                            replaceCount++;
                        }
                    }
                }
            }

            wb.write(fos);
            log.info("XLSX 渲染完成, path={}, 替换了 {} 处占位符", filledPath, replaceCount);
        }
        catch (IOException e)
        {
            throw new RuntimeException("XLSX 渲染失败：" + e.getMessage(), e);
        }
    }

    /**
     * 在 Word 段落中替换占位符
     * <p>
     * 策略：获取段落完整文本 → 替换占位符 → 如果变化则清空段落并重新写入
     */
    private int replaceInParagraph(XWPFParagraph para, Map<String, String> filledValues)
    {
        String fullText = para.getText();
        if (StringUtils.isEmpty(fullText))
        {
            return 0;
        }

        String newText = replacePlaceholders(fullText, filledValues);
        if (fullText.equals(newText))
        {
            return 0;
        }

        // 清空所有 Run
        for (int i = para.getRuns().size() - 1; i >= 0; i--)
        {
            para.removeRun(i);
        }

        // 重新写入文本
        XWPFRun run = para.createRun();
        run.setText(newText);

        return 1;
    }

    /**
     * 通用占位符替换
     * <p>
     * 占位符格式：{{字段名}}
     * 如果 filledValues 中找不到对应字段，保留原占位符
     */
    private String replacePlaceholders(String text, Map<String, String> filledValues)
    {
        if (StringUtils.isEmpty(text) || filledValues == null || filledValues.isEmpty())
        {
            return text;
        }

        String result = text;
        for (Map.Entry<String, String> entry : filledValues.entrySet())
        {
            String placeholder = "{{" + entry.getKey() + "}}";
            String value = entry.getValue() != null ? entry.getValue() : "";
            result = result.replace(placeholder, value);
        }
        return result;
    }

    /**
     * 保存填写后的文档到 Vault
     * 
     * @param task 任务对象
     * @param userId 当前用户ID
     * @return 创建的 Vault 文件索引对象
     */
    @Transactional
    public LingdocFileIndex saveToVault(LingdocFormTask task, Long userId)
    {
        // 1. 确定源文件（临时生成文件）
        String srcPath = task.getFilledFileUrl().replace(Constants.RESOURCE_PREFIX, RuoYiConfig.getProfile());
        File srcFile = new File(srcPath);
        if (!srcFile.exists())
        {
            throw new RuntimeException("生成文件已失效，请重新生成");
        }

        // 2. 确定 Vault 目标路径
        String vaultRoot = RuoYiConfig.getProfile() + "/vault/" + userId + "/documents/表格助手/";
        File vaultDir = new File(vaultRoot);
        if (!vaultDir.exists())
        {
            vaultDir.mkdirs();
        }

        String vaultFileName = task.getFilledFileName();
        String vaultAbsPath = vaultRoot + vaultFileName;

        // 3. 复制文件到 Vault（避免移动导致临时预览失效）
        try
        {
            org.apache.commons.io.FileUtils.copyFile(srcFile, new File(vaultAbsPath));
        }
        catch (IOException e)
        {
            throw new RuntimeException("复制文件到 Vault 失败：" + e.getMessage(), e);
        }

        // 4. 计算 SHA256 checksum
        String checksum = calculateSha256(vaultAbsPath);

        // 5. 写入 lingdoc_file_index
        LingdocFileIndex fileIndex = new LingdocFileIndex();
        String fileId = UUID.fastUUID().toString();
        fileIndex.setFileId(fileId);
        fileIndex.setUserId(userId);
        fileIndex.setFileName(vaultFileName);
        fileIndex.setVaultPath("表格助手/" + vaultFileName);
        fileIndex.setAbsPath(vaultAbsPath);
        fileIndex.setFileType(getFileExtension(vaultFileName));
        fileIndex.setFileSize(srcFile.length());
        fileIndex.setChecksum(checksum);
        fileIndex.setSourceType("2");
        fileIndex.setCreateTime(DateUtils.getNowDate());
        lingdocFileIndexMapper.insertLingdocFileIndex(fileIndex);

        // 6. 更新任务表的关联字段
        task.setFilledFileId(fileId);
        formTaskMapper.updateLingdocFormTask(task);

        // 7. TODO: 异步触发文本解析（写入 lingdoc_file_ai_meta）
        // 使该文件可被 Vault 搜索和其他 AI 功能引用
        // lingdocFileAiMetaService.initMeta(fileId);

        return fileIndex;
    }

    /**
     * 计算文件 SHA-256 校验值
     */
    private String calculateSha256(String filePath)
    {
        try
        {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] fileBytes = org.apache.commons.io.FileUtils.readFileToByteArray(new File(filePath));
            byte[] hashBytes = digest.digest(fileBytes);
            StringBuilder sb = new StringBuilder();
            for (byte b : hashBytes)
            {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        }
        catch (NoSuchAlgorithmException | IOException e)
        {
            throw new RuntimeException("计算 SHA-256 失败：" + e.getMessage(), e);
        }
    }

    /**
     * 获取文件扩展名
     */
    private String getFileExtension(String fileName)
    {
        if (StringUtils.isEmpty(fileName) || fileName.lastIndexOf(".") < 0)
        {
            return "";
        }
        return fileName.substring(fileName.lastIndexOf(".") + 1).toLowerCase();
    }
}
