<p align="center">
	<img alt="logo" src="src/assets/logo/logo.png" width="120">
</p>
<h1 align="center" style="margin: 30px 0 30px; font-weight: bold;">LingDoc 灵档</h1>
<h4 align="center">灵档：您的个人智能文档管家</h4>

> **本项目说明**：当前仓库为前后端统一管理的项目结构。
> - 根目录：前端项目（基于 LingDoc 个人版 UI）
> - `ruoyi-server/`：后端项目（基于 RuoYi-Vue 3.9.2 后端完整代码）
> - 前后端联调地址：`http://localhost:80` → 代理到 `http://localhost:8080`
>
> 详细文档请查看 `docs/` 目录。

## 平台简介

* 前端技术栈 [Vue3](https://v3.cn.vuejs.org) + [Element Plus](https://element-plus.org/zh-CN) + [Vite](https://cn.vitejs.dev)。
* 后端技术栈 Spring Boot + MyBatis + MySQL + Redis。
* 核心理念：**数据不出本地，算力按需借用**。

## 前端运行

```bash
# 安装依赖
npm install

# 启动服务
npm run dev

# 构建测试环境 npm run build:stage
# 构建生产环境 npm run build:prod
# 前端访问地址 http://localhost:80
```

## 后端运行

后端代码位于 `ruoyi-server/` 目录下，详细启动方式请参考 `docs/ruoyi/05-后端迁移与启动指南.md`。

```bash
# 方式一：IDEA 直接运行
# 打开 ruoyi-server/ruoyi-admin/src/main/java/com/ruoyi/RuoYiApplication.java 并执行 main 方法

# 方式二：Maven 打包后运行
cd ruoyi-server
mvn clean package -DskipTests
java -jar ruoyi-admin/target/ruoyi-admin.jar

# 方式三：使用脚本（Windows）
cd ruoyi-server
bin/package.bat
ry.bat
```

### 环境要求
- JDK 17+
- MySQL 8.0+
- Redis 5.0+
- Node.js 18+

## 核心功能

1. **一键自动规整**：拖拽文件即可由 AI 自动命名并归类到相应文件夹。
2. **自然语言检索与对话**：无需记忆文件名，直接以自然语言提问，AI 从文档中提取答案。
3. **数据本地化**：断网也能看，数据仅存储在本地设备。
4. **历史版本溯源**：对同一份文档的每一次修改自动存档，操作可查、可一键撤回。
5. **智能表格填写助手**：上传空白表格，AI 根据本地 Vault 中的已有信息自动生成填写建议。
6. **算力计费**：精确计量每次 AI 操作消耗的 Token。
7. **审计溯源**：不可篡改的操作日志，支持完整行为追溯。

## 默认账号

- admin / admin123
