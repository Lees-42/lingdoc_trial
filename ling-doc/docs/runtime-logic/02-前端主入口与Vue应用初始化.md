# 前端主入口与 Vue 应用初始化

本文档逐层解读 `src/main.js`，这是前端应用的唯一入口文件，负责创建 Vue 应用实例并加载所有全局依赖。

---

## 1. 入口文件位置

```
src/main.js
```

---

## 2. 执行流程图

```
导入第三方库（Vue、Element Plus、Cookies 等）
    ↓
导入全局样式（index.scss、Element Plus 主题）
    ↓
导入根组件 App.vue
    ↓
导入核心模块（router、store、directive、plugins）
    ↓
导入并注册全局组件（Pagination、DictTag、Editor 等）
    ↓
导入权限控制（permission.js）
    ↓
挂载全局属性到 app.config.globalProperties
    ↓
安装插件（router、store、plugins、elementIcons）
    ↓
注册自定义指令
    ↓
安装 Element Plus UI 库
    ↓
app.mount('#app') → 挂载到 DOM
```

---

## 3. 逐段解读

### 3.1 第三方框架导入

```javascript
import { createApp } from 'vue'
import Cookies from 'js-cookie'
import ElementPlus from 'element-plus'
import 'element-plus/dist/index.css'
import 'element-plus/theme-chalk/dark/css-vars.css'
import locale from 'element-plus/es/locale/lang/zh-cn'
```

| 导入项 | 作用 |
|--------|------|
| `createApp` | Vue 3 创建应用实例的工厂函数 |
| `js-cookie` | 操作浏览器 Cookie（存储 Token、尺寸偏好等） |
| `element-plus` | UI 组件库 |
| `zh-cn` | Element Plus 中文语言包 |

### 3.2 全局样式导入

```javascript
import '@/assets/styles/index.scss'
```

加载项目自定义的全局 SCSS 样式，包括变量、混入（mixin）、基础重置样式。

### 3.3 核心模块导入

```javascript
import App from './App'
import store from './store'
import router from './router'
import directive from './directive'
import plugins from './plugins'
```

| 模块 | 文件路径 | 职责 |
|------|---------|------|
| `App` | `src/App.vue` | 根组件，仅包含 `<router-view />` |
| `store` | `src/store/index.js` | Pinia 状态管理入口 |
| `router` | `src/router/index.js` | Vue Router 路由配置 |
| `directive` | `src/directive/index.js` | 自定义指令注册（如 `v-hasPermi` 权限指令） |
| `plugins` | `src/plugins/index.js` | 自定义插件集合（session 缓存等） |

### 3.4 权限控制导入

```javascript
import './permission'
```

**注意**：这不是一个导出具名成员的模块，而是**直接执行** `src/permission.js` 中的副作用代码。该文件注册了全局路由守卫（`router.beforeEach` 和 `router.afterEach`），是整个前端权限校验的入口点。

### 3.5 全局组件注册

```javascript
import Pagination from '@/components/Pagination'
import RightToolbar from '@/components/RightToolbar'
import Editor from "@/components/Editor"
// ... 其他组件

app.component('DictTag', DictTag)
app.component('Pagination', Pagination)
// ...
```

注册后，在任何 Vue 单文件组件中可直接使用 `<Pagination />`、`<DictTag />` 等标签，无需重复导入。

### 3.6 全局属性挂载

```javascript
app.config.globalProperties.useDict = useDict
app.config.globalProperties.download = download
app.config.globalProperties.parseTime = parseTime
// ...
```

在组件中通过 `this.useDict()`、`this.download()` 调用。在 `<script setup>` 中通过 `getCurrentInstance().appContext.config.globalProperties` 访问。

### 3.7 插件安装顺序

```javascript
app.use(router)      // 1. 路由
app.use(store)       // 2. 状态管理
app.use(plugins)     // 3. 自定义插件
app.use(elementIcons)// 4. Element Plus 图标系统
app.component('svg-icon', SvgIcon)
directive(app)       // 5. 注册指令
app.use(ElementPlus, { locale, size: Cookies.get('size') || 'default' })
                     // 6. UI 库（最后安装，确保依赖已就绪）
```

### 3.8 应用挂载

```javascript
app.mount('#app')
```

将 Vue 应用挂载到 `index.html` 中 id 为 `app` 的 DOM 节点上，页面开始渲染。

---

## 4. 关键依赖关系

```
main.js
├── App.vue（根组件）
│   └── <router-view />（根据路由渲染不同页面）
├── router（路由系统）
│   └── permission.js（全局守卫，依赖 store/modules/user、store/modules/permission）
├── store（Pinia 状态管理）
│   ├── modules/user.js（用户、Token）
│   ├── modules/permission.js（动态路由生成）
│   ├── modules/dict.js（字典缓存）
│   └── ...
├── plugins（插件封装）
│   └── cache.js（session/local 缓存工具）
└── directive（自定义指令）
    └── permission/hasPermi.js（按钮级权限控制）
```
