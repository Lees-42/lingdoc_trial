package com.ruoyi.lingdoc.ai.service.impl;

import com.ruoyi.lingdoc.ai.service.ITextExtractionService;
import lombok.extern.slf4j.Slf4j;
import org.apache.tika.Tika;
import org.apache.tika.parser.AutoDetectParser;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.parser.ParseContext;
import org.apache.tika.parser.Parser;
import org.apache.tika.sax.BodyContentHandler;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * 文本提取服务实现（基于Apache Tika）
 */
@Slf4j
@Service
public class TextExtractionServiceImpl implements ITextExtractionService {

    private final Tika tika = new Tika();

    @Override
    public String extractText(String filePath, String fileType) {
        Path path = Paths.get(filePath);
        if (!Files.exists(path)) {
            log.error("文件不存在: {}", filePath);
            return "";
        }

        try (InputStream stream = new FileInputStream(path.toFile())) {
            Parser parser = new AutoDetectParser();
            BodyContentHandler handler = new BodyContentHandler(-1); // -1表示无限制
            ParseContext context = new ParseContext();
            
            parser.parse(stream, handler, new Metadata(), context);
            
            String text = handler.toString().trim();
            log.info("文本提取成功: path={}, length={}", filePath, text.length());
            return text;
        } catch (Exception e) {
            log.error("文本提取失败: path={}, error={}", filePath, e.getMessage(), e);
            return "";
        }
    }

    @Override
    public String detectMimeType(String filePath) {
        try {
            return tika.detect(new File(filePath));
        } catch (Exception e) {
            log.error("MIME类型检测失败: {}, error={}", filePath, e.getMessage());
            return "application/octet-stream";
        }
    }
}
