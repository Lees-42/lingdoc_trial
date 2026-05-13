#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
=============================================================================
Load Test Script - AI 服务压测
=============================================================================
用途：测试 AI 服务在高并发下的表现，定位性能瓶颈

测试场景:
  1. 并发填表请求（模拟 10 用户同时上传）
  2. 轮询压力测试（模拟前端高频轮询）
  3. Vault 文件列表缓存测试（验证 Redis 缓存命中率）

运行方式:
  python3 load_test.py --host http://localhost:8000 --token lingdoc-ai-2026-a7f3e9d2b8c1e4f5

依赖:
  pip install requests
=============================================================================
"""

import argparse
import concurrent.futures
import time
import statistics
from typing import List, Dict

try:
    import requests
except ImportError:
    print("请先安装 requests: pip install requests")
    raise


class LoadTester:
    def __init__(self, host: str, token: str):
        self.host = host.rstrip("/")
        self.headers = {"X-Internal-Token": token}
        self.results: List[Dict] = []

    def _post(self, path: str, json_data: dict) -> dict:
        url = f"{self.host}{path}"
        start = time.time()
        try:
            resp = requests.post(url, json=json_data, headers=self.headers, timeout=30)
            duration = time.time() - start
            return {
                "status": resp.status_code,
                "duration_ms": int(duration * 1000),
                "success": resp.status_code == 200,
                "data": resp.json() if resp.status_code == 200 else None,
            }
        except Exception as e:
            return {"status": 0, "duration_ms": int((time.time() - start) * 1000), "success": False, "error": str(e)}

    def _get(self, path: str) -> dict:
        url = f"{self.host}{path}"
        start = time.time()
        try:
            resp = requests.get(url, headers=self.headers, timeout=10)
            duration = time.time() - start
            return {
                "status": resp.status_code,
                "duration_ms": int(duration * 1000),
                "success": resp.status_code == 200,
            }
        except Exception as e:
            return {"status": 0, "duration_ms": int((time.time() - start) * 1000), "success": False, "error": str(e)}

    # ==================== 场景1: 并发填表 ====================

    def test_fill_async_concurrent(self, concurrency: int = 5):
        """测试并发提交填表任务"""
        print(f"\n[场景1] 并发填表测试 | 并发数={concurrency}")

        def worker(i: int):
            return self._post("/api/ai/v1/form/fill/async", {
                "task_id": f"load_test_{i}_{int(time.time() * 1000)}",
                "reference_paths": ["/tmp/lingdoc/test_doc.docx"],
                "template_path": "/tmp/lingdoc/test_form.docx",
                "output_path": f"/tmp/lingdoc/output/load_test_{i}.docx",
            })

        start = time.time()
        with concurrent.futures.ThreadPoolExecutor(max_workers=concurrency) as ex:
            futures = [ex.submit(worker, i) for i in range(concurrency)]
            self.results = [f.result() for f in concurrent.futures.as_completed(futures)]

        total_time = time.time() - start
        successes = [r for r in self.results if r["success"]]
        durations = [r["duration_ms"] for r in self.results]

        print(f"  总耗时: {total_time:.1f}s")
        print(f"  成功: {len(successes)}/{concurrency}")
        print(f"  平均响应: {statistics.mean(durations):.0f}ms")
        print(f"  P95 响应: {self._percentile(durations, 95):.0f}ms")
        print(f"  最大响应: {max(durations):.0f}ms")

    # ==================== 场景2: 轮询压力 ====================

    def test_poll_progress(self, task_id: str, requests_count: int = 50):
        """测试高频轮询"""
        print(f"\n[场景2] 轮询压力测试 | 请求数={requests_count}")

        durations = []
        for i in range(requests_count):
            r = self._get(f"/api/ai/v1/form/status/{task_id}")
            durations.append(r["duration_ms"])
            time.sleep(0.1)  # 模拟 100ms 轮询间隔

        print(f"  平均响应: {statistics.mean(durations):.0f}ms")
        print(f"  P95 响应: {self._percentile(durations, 95):.0f}ms")

    # ==================== 场景3: Vault 缓存 ====================

    def test_vault_cache(self, requests_count: int = 30):
        """测试 Vault 文件列表缓存"""
        print(f"\n[场景3] Vault 缓存测试 | 请求数={requests_count}")
        # 通过 Java 后端测试
        # TODO: 配置 Java 后端地址后启用
        print("  （需配置 Java 后端地址，跳过）")

    # ==================== 工具 ====================

    @staticmethod
    def _percentile(data: List[float], p: float) -> float:
        sorted_data = sorted(data)
        k = (len(sorted_data) - 1) * p / 100
        f = int(k)
        c = f + 1 if f + 1 < len(sorted_data) else f
        return sorted_data[f] + (k - f) * (sorted_data[c] - sorted_data[f])

    def run_all(self):
        """运行所有测试场景"""
        print("=" * 60)
        print("LingDoc AI 服务压测")
        print(f"目标: {self.host}")
        print("=" * 60)

        self.test_fill_async_concurrent(concurrency=5)
        self.test_poll_progress("test_task_123", requests_count=20)
        self.test_vault_cache()

        print("\n" + "=" * 60)
        print("压测完成")
        print("=" * 60)


def main():
    parser = argparse.ArgumentParser(description="LingDoc AI 服务压测脚本")
    parser.add_argument("--host", default="http://localhost:8000", help="AI 服务地址")
    parser.add_argument("--token", default="lingdoc-ai-2026-a7f3e9d2b8c1e4f5", help="内部认证 Token")
    parser.add_argument("--concurrency", type=int, default=5, help="并发数")
    args = parser.parse_args()

    tester = LoadTester(args.host, args.token)
    tester.test_fill_async_concurrent(concurrency=args.concurrency)


if __name__ == "__main__":
    main()
