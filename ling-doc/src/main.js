// 导入 Vue.js 的 createApp 函数，用于创建应用实例
import { createApp } from 'vue'

// 导入 js-cookie 库，用于处理浏览器 Cookie
import Cookies from 'js-cookie'

// 导入 Element Plus UI 组件库
import ElementPlus from 'element-plus'
// 导入 Element Plus 的基础样式
import 'element-plus/dist/index.css'
// 导入 Element Plus 的暗色主题变量
import 'element-plus/theme-chalk/dark/css-vars.css'
// 导入 Element Plus 的中文语言包
import locale from 'element-plus/es/locale/lang/zh-cn'

// 导入全局样式文件
import '@/assets/styles/index.scss' // global css

// 导入根组件 App.vue
import App from './App'
// 导入状态管理 store
import store from './store'
// 导入路由配置
import router from './router'
// 导入自定义指令
import directive from './directive' // directive

// 注册指令 - 导入自定义插件
import plugins from './plugins' // plugins
// 导入下载工具函数
import { download } from '@/utils/request'

// svg图标 - 导入 SVG 图标注册器（Vite 插件）
import 'virtual:svg-icons-register'
// 导入 SVG 图标组件
import SvgIcon from '@/components/SvgIcon'
// 导入 Element Plus 图标集合
import elementIcons from '@/components/SvgIcon/svgicon'

// 导入权限控制模块
import './permission' // permission control

// 导入字典工具函数
import { useDict } from '@/utils/dict'
// 导入系统配置 API
import { getConfigKey } from "@/api/system/config"
// 导入通用工具函数
import { parseTime, resetForm, addDateRange, handleTree, selectDictLabel, selectDictLabels } from '@/utils/ruoyi'

// 分页组件 - 导入分页组件
import Pagination from '@/components/Pagination'
// 自定义表格工具组件 - 导入右侧工具栏组件
import RightToolbar from '@/components/RightToolbar'
// 富文本组件 - 导入富文本编辑器组件
import Editor from "@/components/Editor"
// 文件上传组件 - 导入文件上传组件
import FileUpload from "@/components/FileUpload"
// 图片上传组件 - 导入图片上传组件
import ImageUpload from "@/components/ImageUpload"
// 图片预览组件 - 导入图片预览组件
import ImagePreview from "@/components/ImagePreview"
// 字典标签组件 - 导入字典标签组件
import DictTag from '@/components/DictTag'

// 创建 Vue 应用实例，传入根组件 App
const app = createApp(App)

// 全局方法挂载 - 将字典工具函数挂载到全局属性
// 挂载后，你在项目的任何页面都可以直接调用这些方法，不用再重复引入。
app.config.globalProperties.useDict = useDict
// 将下载函数挂载到全局属性
app.config.globalProperties.download = download
// 将时间解析函数挂载到全局属性
app.config.globalProperties.parseTime = parseTime
// 将表单重置函数挂载到全局属性
app.config.globalProperties.resetForm = resetForm
// 将树形数据处理函数挂载到全局属性
app.config.globalProperties.handleTree = handleTree
// 将日期范围添加函数挂载到全局属性
app.config.globalProperties.addDateRange = addDateRange
// 将配置获取函数挂载到全局属性
app.config.globalProperties.getConfigKey = getConfigKey
// 将字典标签选择函数挂载到全局属性
app.config.globalProperties.selectDictLabel = selectDictLabel
// 将字典标签多选函数挂载到全局属性
app.config.globalProperties.selectDictLabels = selectDictLabels

// 全局组件挂载 - 注册字典标签组件
// 注册后，你可以在任何页面直接写 <Pagination /> 这种标签，就像写 HTML 原生标签一样简单。
app.component('DictTag', DictTag)
// 注册分页组件
app.component('Pagination', Pagination)
// 注册文件上传组件
app.component('FileUpload', FileUpload)
// 注册图片上传组件
app.component('ImageUpload', ImageUpload)
// 注册图片预览组件
app.component('ImagePreview', ImagePreview)
// 注册右侧工具栏组件
app.component('RightToolbar', RightToolbar)
// 注册富文本编辑器组件
app.component('Editor', Editor)

// 安装路由插件
app.use(router)
// 安装状态管理插件
app.use(store)
// 安装自定义插件集合
app.use(plugins)
// 安装 Element Plus 图标系统
app.use(elementIcons)
// 注册 SVG 图标组件
app.component('svg-icon', SvgIcon)

// 注册自定义指令
directive(app)

// 使用element-plus 并且设置全局的大小 - 安装 Element Plus UI 库
app.use(ElementPlus, {
  locale: locale,  // 设置中文本地化
  // 支持 large、default、small - 设置组件默认尺寸，从 Cookie 中读取或使用默认值
  size: Cookies.get('size') || 'default'
})

// 将应用挂载到 HTML 中的 #app 元素上
app.mount('#app')
