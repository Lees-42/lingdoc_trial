# -*- coding: utf-8 -*-
"""
=============================================================================
OCR Engine Optimized Config - PP-OCRv4 + 超分增强
=============================================================================
用途：提升手写表格和模糊文档的 OCR 识别率

配置项：
  - det_model_dir: 文本检测模型（PP-OCRv4）
  - rec_model_dir: 文本识别模型（PP-OCRv4）
  - enable_mkldnn: 启用 Intel MKL-DNN 加速（CPU 上提速 2-3x）
  - use_tensorrt: 是否启用 TensorRT（GPU 环境）
  - drop_score: 置信度阈值（低于此值的识别结果丢弃）

超分增强（可选）：
  - 当图片 DPI < 200 时，先使用 Real-ESRGAN 超分
  - 超分后再送入 OCR，显著提升手写体识别率
=============================================================================
"""

import os
from typing import Optional
from paddleocr import PaddleOCR


class OcrEngineOptimized:
    """优化版 OCR 引擎"""

    _instance = None

    def __new__(cls):
        if cls._instance is None:
            cls._instance = super().__new__(cls)
            cls._instance._initialized = False
        return cls._instance

    def __init__(self):
        if self._initialized:
            return

        # PP-OCRv4 模型路径（自动下载或本地指定）
        model_dir = os.environ.get("PADDLEOCR_MODEL_DIR", "~/.paddleocr")

        self.ocr = PaddleOCR(
            use_angle_cls=True,           # 启用方向分类器
            lang='ch',                     # 中文模型
            det_model_dir=f"{model_dir}/Whl/det/ch/ch_PP-OCRv4_det_infer",
            rec_model_dir=f"{model_dir}/Whl/rec/ch/ch_PP-OCRv4_rec_infer",
            cls_model_dir=f"{model_dir}/Whl/cls/ch_ppocr_mobile_v2.0_cls_infer",
            # 性能优化
            enable_mkldnn=True,            # Intel CPU 加速
            use_tensorrt=False,            # 如需 GPU 改为 True
            use_gpu=False,                 # CPU 模式
            # 质量参数
            drop_score=0.5,                # 置信度阈值
            det_db_thresh=0.3,             # 检测阈值
            det_db_box_thresh=0.5,         # 检测框阈值
            det_db_unclip_ratio=1.6,       # 文本框扩张比例
            max_text_length=50,            # 最大文本长度
            # 批量处理
            batch_size=1,                  # 单图处理（如需批量可改）
            # 日志
            show_log=False,
        )

        self._initialized = True

    def recognize(self, image_path: str, enhance_dpi: bool = True) -> list:
        """
        识别图片中的文本

        Args:
            image_path: 图片路径
            enhance_dpi: 是否启用 DPI 检测+超分增强

        Returns:
            [{"text": str, "confidence": float, "box": [[x1,y1],[x2,y2],[x3,y3],[x4,y4]]}]
        """
        # TODO: 当 enhance_dpi=True 且 DPI < 200 时，先超分
        # from app.services.super_resolution import enhance_image
        # if needs_enhancement(image_path):
        #     image_path = enhance_image(image_path)

        result = self.ocr.ocr(image_path, cls=True)

        if not result or not result[0]:
            return []

        items = []
        for line in result[0]:
            box, (text, confidence) = line
            items.append({
                "text": text,
                "confidence": float(confidence),
                "box": box,
            })
        return items

    def recognize_pdf(self, pdf_path: str, dpi: int = 300) -> dict:
        """
        识别 PDF 中的所有页面

        Returns:
            {
                "page_count": int,
                "pages": [{"page": int, "items": [...]}],
                "avg_confidence": float
            }
        """
        import fitz  # PyMuPDF

        doc = fitz.open(pdf_path)
        pages = []
        all_confidences = []

        for page_num in range(len(doc)):
            page = doc[page_num]
            # 提高 DPI 获取更清晰图片
            mat = fitz.Matrix(dpi/72, dpi/72)
            pix = page.get_pixmap(matrix=mat)
            img_path = f"/tmp/lingdoc_ocr_page_{page_num}.png"
            pix.save(img_path)

            items = self.recognize(img_path)
            pages.append({"page": page_num + 1, "items": items})
            all_confidences.extend([i["confidence"] for i in items])

            # 清理临时文件
            os.remove(img_path)

        doc.close()

        avg_conf = sum(all_confidences) / len(all_confidences) if all_confidences else 0
        return {
            "page_count": len(pages),
            "pages": pages,
            "avg_confidence": round(avg_conf, 2)
        }
