# -*- coding: utf-8 -*-
"""
=============================================================================
LingDoc AI Service - Tag Generator Service
=============================================================================
用途：结合规则引擎 + LLM，为文档生成智能标签

设计原则：
  1. 双层生成：规则引擎（快速、确定）+ LLM（智能、深度）
  2. 去重合并：避免规则标签和 AI 标签重复
  3. 置信度排序：高置信度标签优先展示
  4. 颜色编码：按标签类别分配颜色，UI 层直接可用

标签体系（参考飞书标签设计）：
  - 内容标签（蓝）：文档主题，如"电路"、"操作系统"
  - 类型标签（绿）：文档格式，如"实验报告"、"课程笔记"
  - 时间标签（橙）：学期/年份，如"2026春"、"大三上"
  - 优先级标签（红）：紧急程度，如"期末复习"、"考试重点"
  - 来源标签（灰）：产生渠道，如"AI规整"、"表格生成"

对接说明（给主项目工程师）：
  - 主后端调用 /api/ai/v1/doc/process 后，自动标签在响应的 auto_tags 字段中
  - 主后端可以直接将这些标签存入 lingdoc_tag_binding 表
  - 标签颜色代码可直接用于前端展示，无需转换
=============================================================================
"""

import json
import re
from typing import List, Dict, Any, Optional
from datetime import datetime

from app.services.llm_client import generate_tags as llm_generate_tags
from app.utils.logger import logger


# 预设标签颜色映射（参考飞书 Element UI 色系）
TAG_COLORS = {
    "content": "#409EFF",    # 内容标签 - 蓝色
    "type": "#67C23A",      # 类型标签 - 绿色
    "time": "#E6A23C",      # 时间标签 - 橙色
    "priority": "#F56C6C",  # 优先级标签 - 红色
    "source": "#909399",    # 来源标签 - 灰色
    "custom": "#9254DE",    # 自定义标签 - 紫色
}


# 规则引擎：基于文件名快速推断标签
# 优点：速度快（毫秒级）、不消耗 LLM Token
# 缺点：只能处理简单规则
FILENAME_RULES = [
    # 类型标签
    {"pattern": r"实验报告|实验|报告", "tag": "实验报告", "category": "type", "confidence": 0.85},
    {"pattern": r"课程笔记|笔记|讲义", "tag": "课程笔记", "category": "type", "confidence": 0.80},
    {"pattern": r"参考答案|答案|解答", "tag": "参考答案", "category": "type", "confidence": 0.90},
    {"pattern": r"作业|习题|题目", "tag": "作业", "category": "type", "confidence": 0.75},
    {"pattern": r"试卷|试题|考试", "tag": "考试试卷", "category": "type", "confidence": 0.85},
    {"pattern": r"教材|课本|教科书", "tag": "教材", "category": "type", "confidence": 0.80},
    
    # 学科内容标签（常见课程）
    {"pattern": r"电路|电学|电子", "tag": "电路", "category": "content", "confidence": 0.80},
    {"pattern": r"操作系统|OS|进程|线程", "tag": "操作系统", "category": "content", "confidence": 0.80},
    {"pattern": r"数据结构|算法|链表|树|图", "tag": "数据结构", "category": "content", "confidence": 0.80},
    {"pattern": r"计算机网络|网络|协议|TCP|IP", "tag": "计算机网络", "category": "content", "confidence": 0.80},
    {"pattern": r"数据库|SQL|MySQL|Redis", "tag": "数据库", "category": "content", "confidence": 0.80},
    {"pattern": r"机器学习|ML|深度学习|AI", "tag": "人工智能", "category": "content", "confidence": 0.75},
    {"pattern": r"高数|微积分|线代|概率", "tag": "高等数学", "category": "content", "confidence": 0.80},
    {"pattern": r"物理|力学|电磁|光学", "tag": "大学物理", "category": "content", "confidence": 0.80},
    {"pattern": r"化学|有机|无机|分析", "tag": "大学化学", "category": "content", "confidence": 0.80},
    
    # 时间标签（从文件名推断学期）
    {"pattern": r"大一[上上下下]", "tag": None, "category": "time", "confidence": 0.90, "dynamic": True},
    {"pattern": r"大二[上上下下]", "tag": None, "category": "time", "confidence": 0.90, "dynamic": True},
    {"pattern": r"大三[上上下下]", "tag": None, "category": "time", "confidence": 0.90, "dynamic": True},
    {"pattern": r"大四[上上下下]", "tag": None, "category": "time", "confidence": 0.90, "dynamic": True},
    {"pattern": r"20\d{2}[春春秋秋冬冬][季]?", "tag": None, "category": "time", "confidence": 0.85, "dynamic": True},
]


class TagGenerator:
    """
    标签生成器
    
    用途：为文档生成结构化标签，支持规则和 AI 双层生成
    
    生成流程：
      1. 规则引擎：从文件名快速提取标签（毫秒级）
      2. LLM 分析：从内容深度分析生成标签（秒级）
      3. 去重合并：合并两层结果，去除重复
      4. 排序输出：按置信度排序，高置信度优先
    
    使用方式：
        ```python
        generator = TagGenerator()
        tags = await generator.generate(file_name="电路实验报告.pdf", ocr_text="...")
        print(tags)  # [{"name": "电路", "color": "#409EFF", ...}, ...]
        ```
    """
    
    async def generate(
        self,
        file_name: str,
        ocr_text: str = "",
        existing_tags: Optional[List[str]] = None,
        source_type: str = "0",  # 来源类型，影响 source 标签
        task_id: str = ""
    ) -> List[Dict[str, Any]]:
        """
        生成文档标签（完整流程）
        
        参数：
            file_name: 文件名（用于规则引擎）
            ocr_text: OCR 文本（用于 LLM 分析）
            existing_tags: 已有标签（避免重复）
            source_type: 文件来源（0=手动上传, 1=AI规整, 2=表格生成）
            task_id: 任务 ID
        
        返回：
            List[Dict]: 标签列表，每项包含 name/color/confidence/source/category
        
        示例：
            [
              {"name": "电路", "color": "#409EFF", "confidence": 0.95, "source": "ai_auto", "category": "content"},
              {"name": "实验报告", "color": "#67C23A", "confidence": 0.88, "source": "rule_based", "category": "type"},
              {"name": "大三上", "color": "#E6A23C", "confidence": 0.72, "source": "rule_based", "category": "time"}
            ]
        """
        all_tags = []
        existing_set = set(existing_tags or [])
        
        logger.info(f"[TAG:{task_id}] 开始生成标签 | file={file_name}")
        
        # Step 1: 规则引擎生成（快速）
        rule_tags = self._generate_by_rules(file_name, source_type)
        for tag in rule_tags:
            if tag["name"] not in existing_set:
                all_tags.append(tag)
                existing_set.add(tag["name"])
        
        logger.debug(f"[TAG:{task_id}] 规则标签: {[t['name'] for t in rule_tags]}")
        
        # Step 2: LLM 生成（智能，仅当 OCR 文本足够时触发）
        if len(ocr_text) >= 50:  # 至少 50 个字符才触发 LLM
            try:
                llm_result = await llm_generate_tags(
                    ocr_text=ocr_text,
                    file_name=file_name,
                    existing_tags=list(existing_set),
                    task_id=task_id
                )
                
                if llm_result.success:
                    llm_tags = self._parse_llm_tags(llm_result.content)
                    for tag in llm_tags:
                        if tag["name"] not in existing_set:
                            all_tags.append(tag)
                            existing_set.add(tag["name"])
                    
                    logger.debug(f"[TAG:{task_id}] LLM 标签: {[t['name'] for t in llm_tags]}")
                else:
                    logger.warning(f"[TAG:{task_id}] LLM 标签生成失败: {llm_result.error}")
                    
            except Exception as e:
                logger.error(f"[TAG:{task_id}] LLM 标签生成异常: {str(e)}")
        else:
            logger.info(f"[TAG:{task_id}] OCR 文本过短 ({len(ocr_text)} 字符)，跳过 LLM 标签")
        
        # Step 3: 排序（按置信度降序）
        all_tags.sort(key=lambda x: x["confidence"], reverse=True)
        
        # 限制数量（避免标签过多）
        max_tags = 10
        final_tags = all_tags[:max_tags]
        
        logger.info(
            f"[TAG:{task_id}] 标签生成完成 | 规则={len(rule_tags)} | "
            f"LLM={len(all_tags)-len(rule_tags)} | 最终={len(final_tags)}"
        )
        
        return final_tags
    
    # ========================================================================
    # 私有方法：规则引擎
    # ========================================================================
    
    def _generate_by_rules(
        self,
        file_name: str,
        source_type: str = "0"
    ) -> List[Dict[str, Any]]:
        """
        基于文件名规则生成标签
        
        说明：
          - 速度快（毫秒级），不消耗 LLM Token
          - 适合处理常见命名模式的文件
          - 置信度通常较高（0.7-0.95）
        """
        tags = []
        
        # 遍历所有规则
        for rule in FILENAME_RULES:
            if re.search(rule["pattern"], file_name, re.IGNORECASE):
                # 动态标签（从文件名提取）
                if rule.get("dynamic"):
                    match = re.search(rule["pattern"], file_name, re.IGNORECASE)
                    if match:
                        tag_name = match.group(0)
                    else:
                        continue
                else:
                    tag_name = rule["tag"]
                
                if not tag_name:
                    continue
                
                tags.append({
                    "name": tag_name,
                    "color": TAG_COLORS.get(rule["category"], TAG_COLORS["custom"]),
                    "confidence": rule["confidence"],
                    "source": "rule_based",
                    "category": rule["category"]
                })
        
        # 添加来源标签（基于 source_type）
        source_tag_map = {
            "0": ("手动上传", "source"),
            "1": ("AI 规整", "source"),
            "2": ("表格生成", "source"),
            "3": ("OCR 提取", "source"),
        }
        if source_type in source_tag_map:
            name, cat = source_tag_map[source_type]
            tags.append({
                "name": name,
                "color": TAG_COLORS["source"],
                "confidence": 0.99,  # 来源是确定的
                "source": "rule_based",
                "category": cat
            })
        
        return tags
    
    # ========================================================================
    # 私有方法：解析 LLM 标签结果
    # ========================================================================
    
    def _parse_llm_tags(self, llm_content: str) -> List[Dict[str, Any]]:
        """
        解析 LLM 返回的标签 JSON
        
        容错处理：
          - LLM 可能返回不完整 JSON，尝试修复
          - 缺失字段使用默认值
        """
        tags = []
        
        try:
            data = json.loads(llm_content)
            raw_tags = data.get("tags", [])
            
            for tag in raw_tags:
                if not isinstance(tag, dict):
                    continue
                
                name = tag.get("name", "").strip()
                if not name:
                    continue
                
                # 标准化字段
                category = tag.get("category", "content")
                if category not in TAG_COLORS:
                    category = "content"
                
                tags.append({
                    "name": name,
                    "color": tag.get("color", TAG_COLORS[category]),
                    "confidence": min(max(tag.get("confidence", 0.8), 0.0), 1.0),  # 限制 0-1
                    "source": tag.get("source", "ai_auto"),
                    "category": category
                })
                
        except json.JSONDecodeError:
            logger.warning("LLM 标签 JSON 解析失败，返回空列表")
        except Exception as e:
            logger.error(f"解析 LLM 标签异常: {str(e)}")
        
        return tags


# =============================================================================
# 快捷函数
# =============================================================================

async def generate_tags(
    file_name: str,
    ocr_text: str = "",
    existing_tags: Optional[List[str]] = None,
    source_type: str = "0",
    task_id: str = ""
) -> List[Dict[str, Any]]:
    """快捷函数：标签生成入口"""
    generator = TagGenerator()
    return await generator.generate(file_name, ocr_text, existing_tags, source_type, task_id)
