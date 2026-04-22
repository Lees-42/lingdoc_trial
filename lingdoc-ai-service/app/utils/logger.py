# -*- coding: utf-8 -*-
"""
=============================================================================
LingDoc AI Service - Logger Utility
=============================================================================
用途：统一日志管理，支持结构化输出和链路追踪

设计原则：
  1. 统一格式：所有日志包含时间、级别、模块名、消息
  2. 请求追踪：每个请求有唯一 request_id，贯穿整条链路
  3. 性能友好：异步写入，不阻塞主逻辑
  4. 分级控制：INFO（生产）/ DEBUG（开发）/ WARNING

对接说明：
  - 主后端可通过 trace.request_id 关联 AI 服务日志
  - 排查问题时提供 request_id，可快速定位问题
=============================================================================
"""

import logging
import sys
from datetime import datetime
from typing import Optional

# 日志格式：时间 | 级别 | 模块 | 消息
LOG_FORMAT = "%(asctime)s | %(levelname)-8s | %(name)s | %(message)s"
DATE_FORMAT = "%Y-%m-%d %H:%M:%S"

# 配置根日志器
def setup_logger(name: str = "lingdoc-ai", level: str = "INFO") -> logging.Logger:
    """
    初始化日志器
    
    参数：
        name: 日志器名称，建议使用模块名
        level: 日志级别，从环境变量 AI_LOG_LEVEL 读取
    
    返回：
        logging.Logger: 配置好的日志器
    """
    logger = logging.getLogger(name)
    
    # 避免重复配置
    if logger.handlers:
        return logger
    
    # 设置级别
    logger.setLevel(getattr(logging, level.upper(), logging.INFO))
    
    # 控制台处理器
    handler = logging.StreamHandler(sys.stdout)
    handler.setFormatter(logging.Formatter(LOG_FORMAT, datefmt=DATE_FORMAT))
    logger.addHandler(handler)
    
    return logger


# 全局日志器实例
logger = setup_logger()


def log_request(request_id: str, method: str, path: str, client_ip: str) -> None:
    """记录请求入口日志"""
    logger.info(f"[REQ:{request_id}] {method} {path} | client={client_ip}")


def log_response(request_id: str, status: int, duration_ms: int, token_usage: int = 0) -> None:
    """记录请求完成日志"""
    logger.info(
        f"[RES:{request_id}] status={status} | duration={duration_ms}ms | "
        f"tokens={token_usage}"
    )


def log_error(request_id: str, error: Exception, context: str = "") -> None:
    """记录错误日志"""
    logger.error(
        f"[ERR:{request_id}] {context} | {type(error).__name__}: {str(error)}",
        exc_info=True
    )
