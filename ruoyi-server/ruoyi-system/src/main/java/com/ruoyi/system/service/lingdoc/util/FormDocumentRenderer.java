package com.ruoyi.system.service.lingdoc.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ruoyi.common.utils.StringUtils;

/**
 * 表格文档渲染工具类
 * <p>
 * 负责将 AI 返回的 filledValues 键值对写入模板文件，
 * 支持 docx / xlsx / html 三种格式。
 * 
 * @author lingdoc
 */
public class FormDocumentRenderer
{
    private static final Logger log = LoggerFactory.getLogger(FormDocumentRenderer.class);

    /**
     * 根据文件扩展名选择渲染器
     * 
     * @param originalPath 原始模板文件路径
     * @param filledPath 输出文件路径
     * @param filledValues 字段值映射表
     * @param ext 文件扩展名（不含点）
     * @return 替换占位符的数量
     */
    public int render(String originalPath, String filledPath, Map<String, String> filledValues, String ext)
    {
        switch (ext.toLowerCase())
        {
            case "html":
            case "htm":
                renderHtml(originalPath, filledPath, filledValues);
                return -1; // HTML 不统计替换数量
            case "docx":
                return renderDocx(originalPath, filledPath, filledValues);
            case "xlsx":
            case "xls":
                return renderXlsx(originalPath, filledPath, filledValues);
            default:
                throw new UnsupportedOperationException("不支持的文件格式进行本地渲染: " + ext);
        }
    }

    /**
     * HTML 渲染：将 .blank 占位符替换为 .filled 实际值
     */
    public void renderHtml(String originalPath, String filledPath, Map<String, String> filledValues)
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
            log.info("HTML 渲染完成, path={}", filledPath);
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
     */
    public int renderDocx(String originalPath, String filledPath, Map<String, String> filledValues)
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
            return replaceCount;
        }
        catch (IOException e)
        {
            throw new RuntimeException("DOCX 渲染失败：" + e.getMessage(), e);
        }
    }

    /**
     * Excel (xlsx) 渲染：将 {{字段名}} 占位符替换为实际值
     */
    public int renderXlsx(String originalPath, String filledPath, Map<String, String> filledValues)
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
            return replaceCount;
        }
        catch (IOException e)
        {
            throw new RuntimeException("XLSX 渲染失败：" + e.getMessage(), e);
        }
    }

    /**
     * 在 Word 段落中替换占位符
     */
    public int replaceInParagraph(XWPFParagraph para, Map<String, String> filledValues)
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
    public String replacePlaceholders(String text, Map<String, String> filledValues)
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
}
