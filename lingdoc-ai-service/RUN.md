# 灵档全链路本地运行指南

> 更新日期: 2026-05-09
> 适用分支: `fast`（完整前后端）+ `feature/ai-native`（AI 服务）

---

## 一、环境要求

| 组件 | 版本 | 说明 |
|---|---|---|
| Java | 21+ | Spring Boot 后端 |
| MySQL | 8.0 | 系统表 |
| Redis | 7.0+ | 缓存 + 验证码 |
| Node.js | 20+ | 前端 Vue3 |
| Python | 3.12 | AI 服务 |
| SQLite | 3.45+ | Vault 数据库 |

---

## 二、启动步骤

### 1. 启动基础服务

```bash
# MySQL（确认 ruoyi 数据库已初始化）
mysql -u root -p -e "USE ruoyi; SHOW TABLES;"

# Redis
redis-cli ping  # 应返回 PONG
```

### 2. 启动后端

```bash
cd ruoyi-server
java -jar ruoyi-admin/target/ruoyi-admin.jar --spring.profiles.active=druid
```

- 端口: 8080
- 等待日志出现 `灵档启动成功`
- 默认账号: admin / admin123

### 3. 启动前端

```bash
cd ruoyi-ui
npm install   # 首次
npm run dev
```

- 端口: 3000
- 访问: http://localhost:3000

### 4. 启动 AI 服务

```bash
cd lingdoc-ai-service  # 从 feature/ai-native 分支克隆
source venv/bin/activate
python3 -m uvicorn app.main:app --host 0.0.0.0 --port 8000
```

- 端口: 8000
- 健康检查: http://localhost:8000/api/ai/v1/health

---

## 三、核心配置说明

### application.yml（后端）

```yaml
lingdoc:
  ai:
    local:
      enabled: true
      base-url: http://localhost:8000
      api-key: lingdoc-ai-2026-a7f3e9d2b8c1e4f5
    dify:
      enabled: false
      base-url: http://localhost:8000  # 指向本地 AI 服务
      api-key: lingdoc-ai-2026-a7f3e9d2b8c1e4f5
```

### .env（AI 服务）

```
DASHSCOPE_API_KEY=sk-...
INTERNAL_API_KEY=lingdoc-ai-2026-a7f3e9d2b8c1e4f5
```

---

## 四、使用流程

1. **创建仓库**：首次使用需创建 Vault（系统会自动回退到默认仓库）
2. **上传参考文档**：上传成绩单、获奖证书等到 Vault
3. **上传空白表格**：进入"表格填写助手"，上传空白申请表
4. **AI 识别字段**：系统自动识别表格字段
5. **生成文档**：点击"生成填写文档"，AI 自动从参考文档提取值并填充
6. **下载结果**：生成的文档在任务详情中下载

---

## 五、注意事项

1. **SQLite 时间戳**：已修复，默认值为 NULL
2. **文件路径**：Linux 下使用 `/tmp/lingdoc`，Windows 需改为 `E:/lingdoc`
3. **验证码**：登录时需要验证码，可从 Redis 获取 `captcha_codes:<UUID>`
4. **模型消耗**：每次生成约消耗 1500-2000 tokens（参考文档越多消耗越大）

---

## 六、测试文件

测试用的空白表格和参考文档在 `downloads/` 目录：
- `参考-成绩单.docx`
- `参考-获奖证书.docx`
- `参考-申请理由.docx`
- `国家奖学金申请表_空白_.docx`
- `国家奖学金申请表_Excel测试模板_.xlsx`
