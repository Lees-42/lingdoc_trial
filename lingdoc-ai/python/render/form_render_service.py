#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
表格填表渲染服务 - 统一入口
支持 docx / xlsx / xls
"""

import sys
import json
import os
from docx_renderer import fill_word_document
from xlsx_renderer import fill_excel_document


def render(input_path: str, output_path: str, filled_values: dict):
    ext = os.path.splitext(input_path)[1].lower()

    if ext == ".docx":
        return fill_word_document(input_path, output_path, filled_values)
    elif ext in [".xlsx", ".xls"]:
        return fill_excel_document(input_path, output_path, filled_values)
    else:
        return {
            "success": False,
            "error": f"不支持的文件类型: {ext}",
            "supported": [".docx", ".xlsx", ".xls"]
        }


if __name__ == "__main__":
    if len(sys.argv) < 4:
        print("Usage: python form_render_service.py <input> <output> '<json>'")
        sys.exit(1)

    result = render(sys.argv[1], sys.argv[2], json.loads(sys.argv[3]))
    print(json.dumps(result, ensure_ascii=False, indent=2))
