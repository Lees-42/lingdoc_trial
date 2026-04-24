#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
Excel 表格智能填表渲染器
支持 .xlsx / .xls 格式
"""

import sys
import json
import os
from openpyxl import load_workbook


def clean_text(text):
    if not text:
        return ""
    return str(text).strip().replace("：", "").replace(":", "").replace(" ", "").replace("\n", "").replace("\t", "").replace("[", "").replace("]", "")


def is_placeholder(text):
    if not text:
        return True
    t = str(text).strip()
    if t.startswith("[") and t.endswith("]"):
        return True
    if "请" in t and ("填写" in t or "输入" in t or "不少于" in t or "示例" in t):
        return True
    if t.replace("_", "").replace("—", "").replace("-", "").replace(" ", "") == "":
        return True
    return False


def fill_excel_document(input_path: str, output_path: str, filled_values: dict):
    wb = load_workbook(input_path)
    filled_count = 0
    unmatched = []
    matched = []
    filled_cells = set()  # (sheet, row, col) 去重

    for sheet_name in wb.sheetnames:
        ws = wb[sheet_name]

        # 获取合并单元格范围映射
        merged_ranges = {}
        for merged_range in ws.merged_cells.ranges:
            min_row, min_col, max_row, max_col = merged_range.min_row, merged_range.min_col, merged_range.max_row, merged_range.max_col
            for r in range(min_row, max_row + 1):
                for c in range(min_col, max_col + 1):
                    merged_ranges[(r, c)] = (min_row, min_col)

        # 遍历所有单元格
        for row in ws.iter_rows():
            for cell in row:
                cell_text = clean_text(cell.value)
                if not cell_text:
                    continue

                # 检查是否是字段名
                for field_name, field_value in filled_values.items():
                    clean_name = clean_text(field_name)
                    if not clean_name:
                        continue

                    # 字段名匹配
                    if cell_text == clean_name or clean_name in cell_text or cell_text in clean_name:
                        # 找到对应的值单元格
                        target = _find_target_cell(ws, cell, merged_ranges, filled_cells, sheet_name)
                        if target:
                            target.value = field_value
                            filled_cells.add((sheet_name, target.row, target.column))
                            filled_count += 1
                            matched.append({
                                "fieldName": field_name,
                                "value": field_value,
                                "sheet": sheet_name,
                                "cell": f"{target.column_letter}{target.row}"
                            })

        # 也检查直接匹配 [字段名] 占位符
        for row in ws.iter_rows():
            for cell in row:
                for field_name, field_value in filled_values.items():
                    if cell.value and f"[{field_name}]" in str(cell.value):
                        key = (sheet_name, cell.row, cell.column)
                        if key not in filled_cells:
                            cell.value = field_value
                            filled_cells.add(key)
                            filled_count += 1
                            matched.append({
                                "fieldName": field_name,
                                "value": field_value,
                                "sheet": sheet_name,
                                "cell": f"{cell.column_letter}{cell.row}",
                                "matchType": "direct_placeholder"
                            })

    wb.save(output_path)

    # 检查未匹配的字段
    matched_names = [m["fieldName"] for m in matched]
    for field_name in filled_values.keys():
        if field_name not in matched_names:
            unmatched.append({"fieldName": field_name, "reason": "未找到对应占位符"})

    return {
        "success": True,
        "filledCount": filled_count,
        "matchedFields": matched,
        "unmatchedFields": unmatched,
        "outputPath": output_path
    }


def _find_target_cell(ws, field_cell, merged_ranges, filled_cells, sheet_name):
    """
    找到字段名对应的值单元格
    策略：优先右侧，其次下方
    """
    r, c = field_cell.row, field_cell.column

    # 检查右侧单元格
    if c + 1 <= ws.max_column:
        right = ws.cell(row=r, column=c + 1)
        key = (sheet_name, right.row, right.column)
        if is_placeholder(right.value) and key not in filled_cells:
            return right

    # 检查下方单元格
    if r + 1 <= ws.max_row:
        down = ws.cell(row=r + 1, column=c)
        key = (sheet_name, down.row, down.column)
        if is_placeholder(down.value) and key not in filled_cells:
            return down

    # 检查右下（隔一列）
    if c + 2 <= ws.max_column:
        right2 = ws.cell(row=r, column=c + 2)
        key = (sheet_name, right2.row, right2.column)
        if is_placeholder(right2.value) and key not in filled_cells:
            return right2

    return None


if __name__ == "__main__":
    if len(sys.argv) < 4:
        print("Usage: python xlsx_renderer.py <input.xlsx> <output.xlsx> '{\"姓名\":\"张三\"}'")
        sys.exit(1)

    input_path = sys.argv[1]
    output_path = sys.argv[2]
    filled_values = json.loads(sys.argv[3])

    result = fill_excel_document(input_path, output_path, filled_values)
    print(json.dumps(result, ensure_ascii=False, indent=2))
