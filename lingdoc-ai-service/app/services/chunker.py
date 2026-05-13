# -*- coding: utf-8 -*-
"""
=============================================================================
Semantic Chunker - 语义分块策略
=============================================================================
用途：将长文档按语义边界切分，提高向量检索精度

策略：
  1. 先按标题/章节切分（结构性分块）
  2. 大块再按句子边界切分（语义分块，目标 300-500 tokens）
  3. 块间保留 50 tokens 重叠（避免信息截断）

优势：
  - 相比固定长度分块，语义边界更准确
  - 重叠区域保证上下文连贯
  - 适合知识库问答和引用溯源
=============================================================================
"""

import re
from typing import List, Dict


class SemanticChunker:
    """语义分块器"""

    # 目标块大小（字符数，约 300-500 tokens）
    TARGET_CHUNK_SIZE = 1200
    MIN_CHUNK_SIZE = 400
    OVERLAP_SIZE = 200

    # 章节标题正则（匹配 Markdown/Word 常见标题格式）
    HEADING_PATTERNS = [
        r'^#{1,6}\s+(.+)$',           # Markdown 标题
        r'^\d+[\.、]\s*(.+)$',         # 数字标题 1. 一、
        r'^[（(]\d+[)）]\s*(.+)$',    # （1）标题
        r'^(第[一二三四五六七八九十\d]+章|第\d+节)\s*[、\.\s]*(.+)$',  # 第一章
    ]

    def __init__(self, target_size: int = None, overlap: int = None):
        self.target_size = target_size or self.TARGET_CHUNK_SIZE
        self.overlap = overlap or self.OVERLAP_SIZE

    def chunk(self, text: str, metadata: Dict = None) -> List[Dict]:
        """
        主分块入口

        Returns: [{"id": str, "text": str, "metadata": {}, "start": int, "end": int}]
        """
        if not text or len(text.strip()) == 0:
            return []

        # Step 1: 按章节切分
        sections = self._split_by_headings(text)

        # Step 2: 大块再按语义切分
        chunks = []
        chunk_id = 0
        for section in sections:
            section_chunks = self._split_section(section["text"], section["heading"])
            for sc in section_chunks:
                chunks.append({
                    "id": f"chunk_{chunk_id:04d}",
                    "text": sc,
                    "metadata": {
                        **(metadata or {}),
                        "section_heading": section["heading"],
                        "section_level": section["level"],
                    },
                    "start": 0,  # TODO: 计算实际偏移
                    "end": 0,
                })
                chunk_id += 1

        return chunks

    def _split_by_headings(self, text: str) -> List[Dict]:
        """按标题切分章节"""
        lines = text.split('\n')
        sections = []
        current_heading = ""
        current_level = 0
        current_lines = []

        for line in lines:
            heading_match = self._match_heading(line)
            if heading_match:
                # 保存上一个章节
                if current_lines:
                    sections.append({
                        "heading": current_heading,
                        "level": current_level,
                        "text": '\n'.join(current_lines).strip()
                    })
                current_heading = heading_match["text"]
                current_level = heading_match["level"]
                current_lines = []
            else:
                current_lines.append(line)

        # 保存最后一个章节
        if current_lines:
            sections.append({
                "heading": current_heading,
                "level": current_level,
                "text": '\n'.join(current_lines).strip()
            })

        # 如果没有标题，整体作为一个章节
        if not sections:
            sections.append({"heading": "", "level": 0, "text": text.strip()})

        return sections

    def _split_section(self, text: str, heading: str) -> List[str]:
        """按语义边界切分单个章节"""
        if len(text) <= self.target_size:
            return [text]

        # 按句子切分（支持中英文标点）
        sentences = re.split(r'(?<=[。\.\!\?\n])\s+', text)
        chunks = []
        current = []
        current_len = 0

        for sentence in sentences:
            sentence_len = len(sentence)
            if current_len + sentence_len > self.target_size and current_len >= self.MIN_CHUNK_SIZE:
                # 保存当前块
                chunk_text = '\n'.join(current)
                chunks.append(chunk_text)
                # 保留重叠
                overlap_text = self._get_overlap(current)
                current = [overlap_text, sentence] if overlap_text else [sentence]
                current_len = len(overlap_text) + sentence_len
            else:
                current.append(sentence)
                current_len += sentence_len

        # 保存最后一块
        if current:
            chunks.append('\n'.join(current))

        return chunks

    def _get_overlap(self, sentences: List[str]) -> str:
        """获取与上一块的重叠文本"""
        overlap = []
        overlap_len = 0
        for s in reversed(sentences):
            if overlap_len + len(s) > self.overlap:
                break
            overlap.insert(0, s)
            overlap_len += len(s)
        return '\n'.join(overlap)

    def _match_heading(self, line: str) -> Dict:
        """匹配标题行"""
        for pattern in self.HEADING_PATTERNS:
            match = re.match(pattern, line.strip())
            if match:
                return {"text": match.group(1), "level": 1}
        return None


# =============================================================================
# Embedding 缓存包装器
# =============================================================================

class CachedEmbeddingClient:
    """
    带缓存的 Embedding 客户端

    - 优先从 Redis 读取 embedding
    - 未命中则调用 LLM 生成并写入 Redis
    - 显著降低重复文本的 embedding 成本
    """

    def __init__(self, redis_client, llm_client, expire_hours: int = 24):
        self.redis = redis_client
        self.llm = llm_client
        self.expire = expire_hours * 3600

    def embed(self, text: str) -> List[float]:
        """获取文本的 embedding（带缓存）"""
        import hashlib
        text_hash = hashlib.md5(text.encode()).hexdigest()
        cache_key = f"lingdoc:kb:embedding:{text_hash}"

        # 1. 尝试从 Redis 读取
        cached = self.redis.get(cache_key)
        if cached:
            import json
            return json.loads(cached)

        # 2. 调用 LLM 生成
        embedding = self.llm.embed(text)

        # 3. 写入 Redis
        import json
        self.redis.setex(cache_key, self.expire, json.dumps(embedding))

        return embedding

    def embed_batch(self, texts: List[str]) -> List[List[float]]:
        """批量 embedding，优先用缓存，未命中批量调用 LLM"""
        import hashlib, json
        results = []
        missing = []  # (index, text, hash)

        for i, text in enumerate(texts):
            text_hash = hashlib.md5(text.encode()).hexdigest()
            cache_key = f"lingdoc:kb:embedding:{text_hash}"
            cached = self.redis.get(cache_key)
            if cached:
                results.append(json.loads(cached))
            else:
                results.append(None)
                missing.append((i, text, text_hash))

        # 批量调用 LLM（一次最多 10 条）
        if missing:
            batch_size = 10
            for batch_start in range(0, len(missing), batch_size):
                batch = missing[batch_start:batch_start + batch_size]
                batch_texts = [b[1] for b in batch]
                batch_embeddings = self.llm.embed_batch(batch_texts)

                for (i, text, text_hash), embedding in zip(batch, batch_embeddings):
                    results[i] = embedding
                    cache_key = f"lingdoc:kb:embedding:{text_hash}"
                    self.redis.setex(cache_key, self.expire, json.dumps(embedding))

        return results
