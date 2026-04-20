package com.ruoyi.lingdoc.ai.service.impl;

import com.ruoyi.common.utils.uuid.UUID;
import com.ruoyi.lingdoc.ai.domain.entity.KbDocumentChunk;
import com.ruoyi.lingdoc.ai.service.IChunkingService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 文本分块服务实现
 */
@Slf4j
@Service
public class ChunkingServiceImpl implements IChunkingService {

    // 段落分隔符正则
    private static final Pattern PARAGRAPH_PATTERN = Pattern.compile("\\n\\s*\\n|\\r\\n\\s*\\r\\n");
    // 句子结束符
    private static final Pattern SENTENCE_PATTERN = Pattern.compile("[.!?。！？]\\s+");

    @Override
    public List<KbDocumentChunk> chunkText(String text, int chunkSize, int chunkOverlap, String docId, String kbId) {
        List<KbDocumentChunk> chunks = new ArrayList<>();
        
        if (text == null || text.isEmpty()) {
            return chunks;
        }

        int textLength = text.length();
        int start = 0;
        int chunkIndex = 0;

        while (start < textLength) {
            int end = Math.min(start + chunkSize, textLength);
            
            // 如果不是最后一块，尝试在句子边界截断
            if (end < textLength) {
                end = findSentenceBoundary(text, end);
            }

            String chunkText = text.substring(start, end);
            KbDocumentChunk chunk = createChunk(chunkText, chunkIndex, start, end, docId, kbId);
            chunks.add(chunk);

            chunkIndex++;
            start = end - chunkOverlap; // 重叠部分
            
            // 避免死循环
            if (start >= end) {
                start = end;
            }
        }

        log.info("文本分块完成: docId={}, chunks={}", docId, chunks.size());
        return chunks;
    }

    @Override
    public List<KbDocumentChunk> smartChunk(String text, int chunkSize, int chunkOverlap, String docId, String kbId) {
        // 先按段落分割
        String[] paragraphs = PARAGRAPH_PATTERN.split(text);
        List<KbDocumentChunk> chunks = new ArrayList<>();
        
        StringBuilder currentChunk = new StringBuilder();
        int chunkIndex = 0;
        int charStart = 0;
        int currentLength = 0;

        for (String paragraph : paragraphs) {
            paragraph = paragraph.trim();
            if (paragraph.isEmpty()) continue;

            // 如果当前段落超过chunkSize，单独处理
            if (paragraph.length() > chunkSize) {
                // 先保存当前积累的内容
                if (currentChunk.length() > 0) {
                    KbDocumentChunk chunk = createChunk(
                        currentChunk.toString(), 
                        chunkIndex++, 
                        charStart, 
                        charStart + currentChunk.length(), 
                        docId, kbId
                    );
                    chunks.add(chunk);
                    charStart += currentChunk.length() - chunkOverlap;
                    currentChunk = new StringBuilder();
                }
                
                // 将大段落按固定长度分块
                List<KbDocumentChunk> paraChunks = chunkText(paragraph, chunkSize, chunkOverlap, docId, kbId);
                for (KbDocumentChunk ck : paraChunks) {
                    ck.setChunkIndex(chunkIndex++);
                }
                chunks.addAll(paraChunks);
                charStart += paragraph.length();
                continue;
            }

            // 检查加入当前段落后是否超过chunkSize
            int newLength = currentLength + (currentLength > 0 ? 1 : 0) + paragraph.length(); // +1 for newline
            
            if (newLength > chunkSize && currentChunk.length() > 0) {
                // 保存当前块
                KbDocumentChunk chunk = createChunk(
                    currentChunk.toString(), 
                    chunkIndex++, 
                    charStart, 
                    charStart + currentChunk.length(), 
                    docId, kbId
                );
                chunks.add(chunk);
                
                // 计算下一个块的起始位置（考虑重叠）
                int overlapLength = Math.min(chunkOverlap, currentChunk.length() / 2);
                String overlapText = currentChunk.substring(currentChunk.length() - overlapLength);
                charStart += currentChunk.length() - overlapLength;
                
                // 新块从重叠部分+当前段落开始
                currentChunk = new StringBuilder(overlapText);
                if (overlapText.length() > 0) {
                    currentChunk.append("\n\n");
                }
                currentChunk.append(paragraph);
                currentLength = currentChunk.length();
            } else {
                // 继续添加到当前块
                if (currentChunk.length() > 0) {
                    currentChunk.append("\n\n");
                }
                currentChunk.append(paragraph);
                currentLength = currentChunk.length();
            }
        }

        // 保存最后一个块
        if (currentChunk.length() > 0) {
            KbDocumentChunk chunk = createChunk(
                currentChunk.toString(), 
                chunkIndex, 
                charStart, 
                charStart + currentChunk.length(), 
                docId, kbId
            );
            chunks.add(chunk);
        }

        log.info("智能分块完成: docId={}, chunks={}", docId, chunks.size());
        return chunks;
    }

    /**
     * 查找句子边界
     */
    private int findSentenceBoundary(String text, int targetPos) {
        // 从targetPos向前搜索最近的句子结束符
        int searchStart = Math.max(0, targetPos - 100); // 最多向前搜索100字符
        String searchText = text.substring(searchStart, targetPos);
        
        Matcher matcher = SENTENCE_PATTERN.matcher(searchText);
        int lastEnd = -1;
        
        while (matcher.find()) {
            lastEnd = matcher.end();
        }
        
        if (lastEnd > 0) {
            return searchStart + lastEnd;
        }
        
        // 找不到句子边界，尝试在空白字符处截断
        int spacePos = searchText.lastIndexOf(' ');
        if (spacePos > 0) {
            return searchStart + spacePos;
        }
        
        // 都找不到，直接在targetPos截断
        return targetPos;
    }

    /**
     * 创建分块实体
     */
    private KbDocumentChunk createChunk(String text, int chunkIndex, int charStart, int charEnd, String docId, String kbId) {
        KbDocumentChunk chunk = new KbDocumentChunk();
        chunk.setChunkId("chunk_" + UUID.fastUUID().toString(true));
        chunk.setKbId(kbId);
        chunk.setFileId(docId);
        chunk.setChunkIndex(chunkIndex);
        chunk.setChunkText(text);
        chunk.setChunkTextLength(text.length());
        chunk.setCharStart(charStart);
        chunk.setCharEnd(charEnd);
        chunk.setStatus(1);
        chunk.setCreatedAt(LocalDateTime.now());
        chunk.setUpdatedAt(LocalDateTime.now());
        return chunk;
    }
}
