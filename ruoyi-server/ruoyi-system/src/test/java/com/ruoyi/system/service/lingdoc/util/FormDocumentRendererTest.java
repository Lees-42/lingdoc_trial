package com.ruoyi.system.service.lingdoc.util;

import static org.junit.jupiter.api.Assertions.*;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

import org.apache.poi.xssf.usermodel.XSSFCell;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFTable;
import org.apache.poi.xwpf.usermodel.XWPFTableCell;
import org.apache.poi.xwpf.usermodel.XWPFTableRow;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * 表格文档渲染工具单元测试
 * 
 * 覆盖：占位符替换、docx 渲染、xlsx 渲染、html 渲染、分派逻辑
 */
class FormDocumentRendererTest
{

    @TempDir
    Path tempDir;

    private FormDocumentRenderer renderer;

    @BeforeEach
    void setUp()
    {
        renderer = new FormDocumentRenderer();
    }

    // ========== replacePlaceholders ==========

    @Test
    void testReplacePlaceholders_SinglePlaceholder()
    {
        String text = "姓名：{{姓名}}";
        Map<String, String> values = Map.of("姓名", "张三");

        String result = renderer.replacePlaceholders(text, values);

        assertEquals("姓名：张三", result);
    }

    @Test
    void testReplacePlaceholders_MultiplePlaceholders()
    {
        String text = "姓名：{{姓名}}，学号：{{学号}}，GPA：{{GPA}}";
        Map<String, String> values = Map.of(
            "姓名", "张三",
            "学号", "2023001001",
            "GPA", "3.85");

        String result = renderer.replacePlaceholders(text, values);

        assertEquals("姓名：张三，学号：2023001001，GPA：3.85", result);
    }

    @Test
    void testReplacePlaceholders_MissingValue_KeepsPlaceholder()
    {
        String text = "姓名：{{姓名}}，年龄：{{年龄}}";
        Map<String, String> values = Map.of("姓名", "张三");

        String result = renderer.replacePlaceholders(text, values);

        assertEquals("姓名：张三，年龄：{{年龄}}", result);
    }

    @Test
    void testReplacePlaceholders_NullOrEmptyMap()
    {
        String text = "姓名：{{姓名}}";

        assertEquals(text, renderer.replacePlaceholders(text, null));
        assertEquals(text, renderer.replacePlaceholders(text, new HashMap<>()));
    }

    @Test
    void testReplacePlaceholders_NullValue_TreatedAsEmptyString()
    {
        String text = "姓名：{{姓名}}";
        Map<String, String> values = new HashMap<>();
        values.put("姓名", null);

        String result = renderer.replacePlaceholders(text, values);

        assertEquals("姓名：", result);
    }

    @Test
    void testReplacePlaceholders_SpecialCharactersInValue()
    {
        String text = "获奖情况：{{获奖情况}}";
        Map<String, String> values = Map.of("获奖情况", "国家奖学金、优秀毕业生<>&\"");

        String result = renderer.replacePlaceholders(text, values);

        assertEquals("获奖情况：国家奖学金、优秀毕业生<>&\"", result);
    }

    @Test
    void testReplacePlaceholders_ChineseFieldName()
    {
        String text = "{{家庭住址}}";
        Map<String, String> values = Map.of("家庭住址", "北京市海淀区");

        String result = renderer.replacePlaceholders(text, values);

        assertEquals("北京市海淀区", result);
    }

    // ========== renderHtml ==========

    @Test
    void testRenderHtml_BasicReplacement() throws IOException
    {
        // 创建模板 HTML
        String html = "<table><tr><th>姓名</th><td class=\"blank\"></td></tr><tr><th>学号</th><td class=\"blank\"></td></tr></table>";
        Path templatePath = tempDir.resolve("template.html");
        Files.writeString(templatePath, html);

        Path outputPath = tempDir.resolve("filled.html");
        Map<String, String> values = Map.of("姓名", "张三", "学号", "2023001001");

        renderer.renderHtml(templatePath.toString(), outputPath.toString(), values);

        String result = Files.readString(outputPath, StandardCharsets.UTF_8);
        assertTrue(result.contains("<td class=\"filled\">张三</td>"));
        assertTrue(result.contains("<td class=\"filled\">2023001001</td>"));
        assertFalse(result.contains("class=\"blank\"")); // 所有 blank 都被替换
    }

    @Test
    void testRenderHtml_NoMatchingField() throws IOException
    {
        String html = "<table><tr><th>姓名</th><td class=\"blank\"></td></tr></table>";
        Path templatePath = tempDir.resolve("template.html");
        Files.writeString(templatePath, html);

        Path outputPath = tempDir.resolve("filled.html");
        Map<String, String> values = Map.of("学号", "2023001001"); // 没有 "姓名"

        renderer.renderHtml(templatePath.toString(), outputPath.toString(), values);

        String result = Files.readString(outputPath, StandardCharsets.UTF_8);
        assertTrue(result.contains("class=\"blank\"")); // 姓名位置仍是 blank
    }

    @Test
    void testRenderHtml_NullValues() throws IOException
    {
        String html = "<table><tr><th>姓名</th><td class=\"blank\"></td></tr></table>";
        Path templatePath = tempDir.resolve("template.html");
        Files.writeString(templatePath, html);

        Path outputPath = tempDir.resolve("filled.html");

        renderer.renderHtml(templatePath.toString(), outputPath.toString(), null);

        // 不抛异常即可，内容应保持原样
        assertTrue(Files.exists(outputPath));
    }

    // ========== renderDocx ==========

    @Test
    void testRenderDocx_ParagraphPlaceholder() throws IOException
    {
        // 创建 docx 模板
        Path templatePath = createDocxTemplate("姓名：{{姓名}}\n学号：{{学号}}");
        Path outputPath = tempDir.resolve("filled.docx");

        Map<String, String> values = Map.of("姓名", "张三", "学号", "2023001001");

        int replaceCount = renderer.renderDocx(templatePath.toString(), outputPath.toString(), values);

        assertTrue(replaceCount >= 2);
        assertTrue(Files.exists(outputPath));
        assertTrue(Files.size(outputPath) > 0);

        // 验证内容：读取生成的 docx
        try (var doc = new org.apache.poi.xwpf.usermodel.XWPFDocument(Files.newInputStream(outputPath)))
        {
            String fullText = doc.getParagraphs().stream()
                .map(org.apache.poi.xwpf.usermodel.XWPFParagraph::getText)
                .reduce("", String::concat);
            assertTrue(fullText.contains("张三"));
            assertTrue(fullText.contains("2023001001"));
            assertFalse(fullText.contains("{{姓名}}"));
            assertFalse(fullText.contains("{{学号}}"));
        }
    }

    @Test
    void testRenderDocx_TableCellPlaceholder() throws IOException
    {
        // 创建带表格的 docx
        Path templatePath = tempDir.resolve("template.docx");
        try (XWPFDocument doc = new XWPFDocument();
             FileOutputStream fos = new FileOutputStream(templatePath.toFile()))
        {
            XWPFTable table = doc.createTable(2, 2);
            XWPFTableRow row0 = table.getRow(0);
            row0.getCell(0).setText("姓名");
            row0.getCell(1).setText("{{姓名}}");
            XWPFTableRow row1 = table.getRow(1);
            row1.getCell(0).setText("学号");
            row1.getCell(1).setText("{{学号}}");
            doc.write(fos);
        }

        Path outputPath = tempDir.resolve("filled.docx");
        Map<String, String> values = Map.of("姓名", "张三", "学号", "2023001001");

        int replaceCount = renderer.renderDocx(templatePath.toString(), outputPath.toString(), values);

        assertTrue(replaceCount >= 2);

        // 验证
        try (var doc = new org.apache.poi.xwpf.usermodel.XWPFDocument(Files.newInputStream(outputPath)))
        {
            String fullText = doc.getTables().stream()
                .flatMap(t -> t.getRows().stream())
                .flatMap(r -> r.getTableCells().stream())
                .flatMap(c -> c.getParagraphs().stream())
                .map(org.apache.poi.xwpf.usermodel.XWPFParagraph::getText)
                .reduce("", String::concat);
            assertTrue(fullText.contains("张三"));
            assertTrue(fullText.contains("2023001001"));
        }
    }

    @Test
    void testRenderDocx_NoPlaceholder() throws IOException
    {
        Path templatePath = createDocxTemplate("这是一段没有占位符的文本");
        Path outputPath = tempDir.resolve("filled.docx");

        int replaceCount = renderer.renderDocx(templatePath.toString(), outputPath.toString(), Map.of("姓名", "张三"));

        assertEquals(0, replaceCount);
        assertTrue(Files.exists(outputPath));
    }

    @Test
    void testRenderDocx_EmptyValues() throws IOException
    {
        Path templatePath = createDocxTemplate("姓名：{{姓名}}");
        Path outputPath = tempDir.resolve("filled.docx");

        int replaceCount = renderer.renderDocx(templatePath.toString(), outputPath.toString(), new HashMap<>());

        assertEquals(0, replaceCount);
    }

    // ========== renderXlsx ==========

    @Test
    void testRenderXlsx_BasicReplacement() throws IOException
    {
        Path templatePath = createXlsxTemplate(new String[][]{
            {"字段", "值"},
            {"姓名", "{{姓名}}"},
            {"学号", "{{学号}}"},
            {"GPA", "{{GPA}}"}
        });
        Path outputPath = tempDir.resolve("filled.xlsx");

        Map<String, String> values = Map.of(
            "姓名", "张三",
            "学号", "2023001001",
            "GPA", "3.85");

        int replaceCount = renderer.renderXlsx(templatePath.toString(), outputPath.toString(), values);

        assertTrue(replaceCount >= 3);
        assertTrue(Files.exists(outputPath));

        // 验证
        try (var wb = new XSSFWorkbook(Files.newInputStream(outputPath)))
        {
            XSSFSheet sheet = wb.getSheetAt(0);
            assertEquals("张三", sheet.getRow(1).getCell(1).getStringCellValue());
            assertEquals("2023001001", sheet.getRow(2).getCell(1).getStringCellValue());
            assertEquals("3.85", sheet.getRow(3).getCell(1).getStringCellValue());
        }
    }

    @Test
    void testRenderXlsx_MissingValue_KeepsPlaceholder() throws IOException
    {
        Path templatePath = createXlsxTemplate(new String[][]{
            {"字段", "值"},
            {"姓名", "{{姓名}}"},
            {"年龄", "{{年龄}}"}
        });
        Path outputPath = tempDir.resolve("filled.xlsx");

        Map<String, String> values = Map.of("姓名", "张三");

        renderer.renderXlsx(templatePath.toString(), outputPath.toString(), values);

        try (var wb = new XSSFWorkbook(Files.newInputStream(outputPath)))
        {
            XSSFSheet sheet = wb.getSheetAt(0);
            assertEquals("张三", sheet.getRow(1).getCell(1).getStringCellValue());
            assertEquals("{{年龄}}", sheet.getRow(2).getCell(1).getStringCellValue()); // 保留占位符
        }
    }

    @Test
    void testRenderXlsx_NoPlaceholder() throws IOException
    {
        Path templatePath = createXlsxTemplate(new String[][]{
            {"A", "B"},
            {"1", "2"}
        });
        Path outputPath = tempDir.resolve("filled.xlsx");

        int replaceCount = renderer.renderXlsx(templatePath.toString(), outputPath.toString(), Map.of("姓名", "张三"));

        assertEquals(0, replaceCount);
    }

    // ========== render 分派方法 ==========

    @Test
    void testRender_Docx() throws IOException
    {
        Path templatePath = createDocxTemplate("{{姓名}}");
        Path outputPath = tempDir.resolve("filled.docx");

        int replaceCount = renderer.render(templatePath.toString(), outputPath.toString(), Map.of("姓名", "张三"), "docx");

        assertTrue(replaceCount > 0);
    }

    @Test
    void testRender_Xlsx() throws IOException
    {
        Path templatePath = createXlsxTemplate(new String[][]{{"字段", "值"}, {"姓名", "{{姓名}}"}});
        Path outputPath = tempDir.resolve("filled.xlsx");

        int replaceCount = renderer.render(templatePath.toString(), outputPath.toString(), Map.of("姓名", "张三"), "xlsx");

        assertTrue(replaceCount > 0);
    }

    @Test
    void testRender_Html() throws IOException
    {
        String html = "<table><tr><th>姓名</th><td class=\"blank\"></td></tr></table>";
        Path templatePath = tempDir.resolve("template.html");
        Files.writeString(templatePath, html);
        Path outputPath = tempDir.resolve("filled.html");

        int result = renderer.render(templatePath.toString(), outputPath.toString(), Map.of("姓名", "张三"), "html");

        assertEquals(-1, result); // HTML 返回 -1
        String content = Files.readString(outputPath, StandardCharsets.UTF_8);
        assertTrue(content.contains("张三"));
    }

    @Test
    void testRender_UnsupportedExtension()
    {
        Path templatePath = tempDir.resolve("template.pdf");
        Path outputPath = tempDir.resolve("filled.pdf");

        assertThrows(UnsupportedOperationException.class, () -> {
            renderer.render(templatePath.toString(), outputPath.toString(), Map.of("姓名", "张三"), "pdf");
        });
    }

    @Test
    void testRender_CaseInsensitive()
    {
        Path templatePath = tempDir.resolve("template.DOCX");
        Path outputPath = tempDir.resolve("filled.DOCX");

        assertThrows(UnsupportedOperationException.class, () -> {
            renderer.render(templatePath.toString(), outputPath.toString(), Map.of(), "DOCX");
        });
    }

    // ========== 辅助方法 ==========

    /**
     * 创建简单 docx 模板文件
     */
    private Path createDocxTemplate(String text) throws IOException
    {
        Path path = tempDir.resolve("template.docx");
        try (XWPFDocument doc = new XWPFDocument();
             FileOutputStream fos = new FileOutputStream(path.toFile()))
        {
            XWPFParagraph para = doc.createParagraph();
            para.createRun().setText(text);
            doc.write(fos);
        }
        return path;
    }

    /**
     * 创建简单 xlsx 模板文件
     */
    private Path createXlsxTemplate(String[][] data) throws IOException
    {
        Path path = tempDir.resolve("template.xlsx");
        try (XSSFWorkbook wb = new XSSFWorkbook();
             FileOutputStream fos = new FileOutputStream(path.toFile()))
        {
            XSSFSheet sheet = wb.createSheet("Sheet1");
            for (int i = 0; i < data.length; i++)
            {
                XSSFRow row = sheet.createRow(i);
                for (int j = 0; j < data[i].length; j++)
                {
                    XSSFCell cell = row.createCell(j);
                    cell.setCellValue(data[i][j]);
                }
            }
            wb.write(fos);
        }
        return path;
    }
}
