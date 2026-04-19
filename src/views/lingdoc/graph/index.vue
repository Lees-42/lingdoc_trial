<template>
  <div class="app-container graph-page">
    <!-- 页面标题 -->
    <div class="graph-header">
      <div class="graph-header__left">
        <h2 class="page-title">关系图谱</h2>
        <p class="page-desc">以标签为中心，可视化展示本地 Vault 文件的归属与分布</p>
      </div>
      <div class="graph-header__right">
        <el-input
          v-model="searchKeyword"
          placeholder="搜索标签或文件名…"
          clearable
          :prefix-icon="Search"
          class="graph-search"
          @keyup.enter="handleSearch"
        />
        <el-button type="primary" :icon="Refresh" @click="resetGraph">重置视图</el-button>
        <el-button :icon="Setting" @click="settingsOpen = true">设置</el-button>
      </div>
    </div>

    <!-- 统计栏 -->
    <div class="graph-stats">
      <el-tag v-for="stat in stats" :key="stat.label" :type="stat.type" effect="plain" size="large">
        {{ stat.label }}：{{ stat.value }}
      </el-tag>
    </div>

    <!-- 图谱主体 -->
    <div class="graph-body" :class="`graph-body--${settings.bgTheme}`">
      <div ref="chartRef" class="graph-chart"></div>

      <!-- 节点详情面板 -->
      <el-drawer
        v-model="detailOpen"
        :title="selectedNode?.nodeType === 'tag' ? '标签详情' : '文件详情'"
        direction="rtl"
        size="340px"
        :with-header="true"
      >
        <!-- 标签详情 -->
        <div v-if="selectedNode?.nodeType === 'tag'" class="node-detail">
          <div class="node-detail__icon tag-icon">
            <el-icon :size="48" color="var(--el-color-primary)"><Collection /></el-icon>
          </div>
          <h3 class="node-detail__name">{{ selectedNode.name }}</h3>
          <el-descriptions :column="1" border size="small">
            <el-descriptions-item label="节点类型">标签</el-descriptions-item>
            <el-descriptions-item label="关联文件">{{ selectedNode.linkCount }} 个</el-descriptions-item>
          </el-descriptions>

          <div class="node-detail__links">
            <h4>下属文件</h4>
            <div class="link-list">
              <div
                v-for="link in selectedNode.links"
                :key="link.id"
                class="link-item"
                @click="highlightNode(link.id)"
              >
                <svg-icon :icon-class="getFileIcon(link.type)" class="link-icon" />
                <span>{{ link.name }}</span>
                <el-tag size="small" :style="{ backgroundColor: link.color + '20', color: link.color, borderColor: link.color + '40' }">
                  {{ link.type?.toUpperCase() }}
                </el-tag>
              </div>
            </div>
          </div>
        </div>

        <!-- 文件详情 -->
        <div v-else-if="selectedNode?.nodeType === 'file'" class="node-detail">
          <div class="node-detail__icon">
            <svg-icon :icon-class="getFileIcon(selectedNode.type)" class="detail-file-icon" />
          </div>
          <h3 class="node-detail__name">{{ selectedNode.name }}</h3>
          <el-descriptions :column="1" border size="small">
            <el-descriptions-item label="文件类型">{{ selectedNode.type?.toUpperCase() }}</el-descriptions-item>
            <el-descriptions-item label="存储路径">{{ selectedNode.path }}</el-descriptions-item>
            <el-descriptions-item label="文件大小">{{ formatFileSize(selectedNode.value) }}</el-descriptions-item>
            <el-descriptions-item label="所属标签">
              <el-tag
                v-for="tag in selectedNode.tags"
                :key="tag"
                size="small"
                effect="plain"
                class="tag-badge"
              >{{ tag }}</el-tag>
            </el-descriptions-item>
          </el-descriptions>

          <div class="node-detail__links" v-if="selectedNode.links?.length">
            <h4>同标签文件</h4>
            <div class="link-list">
              <div
                v-for="link in selectedNode.links"
                :key="link.id"
                class="link-item"
                @click="highlightNode(link.id)"
              >
                <svg-icon :icon-class="getFileIcon(link.type)" class="link-icon" />
                <span>{{ link.name }}</span>
                <el-tag size="small" type="info" effect="plain">{{ link.tag }}</el-tag>
              </div>
            </div>
          </div>
        </div>
      </el-drawer>

      <!-- 设置面板 -->
      <el-drawer
        v-model="settingsOpen"
        title="图谱设置"
        direction="rtl"
        size="340px"
        :with-header="true"
      >
        <div class="settings-body">
          <!-- 显示设置 -->
          <div class="settings-group">
            <h4 class="settings-group__title">显示设置</h4>
            <div class="settings-row">
              <span>显示文件名</span>
              <el-switch v-model="settings.showLabels" />
            </div>
            <div class="settings-row">
              <span>显示标签文字</span>
              <el-switch v-model="settings.showTagLabels" />
            </div>
            <div class="settings-row settings-row--slider">
              <span>节点大小缩放</span>
              <el-slider v-model="settings.nodeScale" :min="0.5" :max="2.0" :step="0.1" show-stops />
              <span class="slider-value">{{ settings.nodeScale.toFixed(1) }}x</span>
            </div>
          </div>

          <!-- 外观设置 -->
          <div class="settings-group">
            <h4 class="settings-group__title">外观设置</h4>
            <div class="settings-row settings-row--slider">
              <span>边透明度</span>
              <el-slider v-model="settings.edgeOpacity" :min="0.1" :max="1.0" :step="0.1" show-stops />
              <span class="slider-value">{{ Math.round(settings.edgeOpacity * 100) }}%</span>
            </div>
            <div class="settings-row settings-row--slider">
              <span>边粗细</span>
              <el-slider v-model="settings.edgeWidth" :min="0.5" :max="4.0" :step="0.5" show-stops />
              <span class="slider-value">{{ settings.edgeWidth.toFixed(1) }}px</span>
            </div>
            <div class="settings-row">
              <span>画布背景</span>
              <el-radio-group v-model="settings.bgTheme" size="small">
                <el-radio-button label="light">浅色</el-radio-button>
                <el-radio-button label="dark">深色</el-radio-button>
                <el-radio-button label="warm">暖色</el-radio-button>
              </el-radio-group>
            </div>
          </div>

          <!-- 力度设置 -->
          <div class="settings-group">
            <h4 class="settings-group__title">力度设置</h4>
            <div class="settings-row settings-row--slider">
              <span>斥力强度</span>
              <el-slider v-model="settings.repulsion" :min="100" :max="2000" :step="50" show-stops />
              <span class="slider-value">{{ settings.repulsion }}</span>
            </div>
            <div class="settings-row settings-row--slider">
              <span>引力强度</span>
              <el-slider v-model="settings.gravity" :min="0" :max="0.3" :step="0.01" show-stops />
              <span class="slider-value">{{ settings.gravity.toFixed(2) }}</span>
            </div>
            <div class="settings-row settings-row--slider">
              <span>边长度</span>
              <el-slider v-model="settings.edgeLength" :min="40" :max="250" :step="10" show-stops />
              <span class="slider-value">{{ settings.edgeLength }}</span>
            </div>
          </div>

          <div class="settings-footer">
            <el-button text type="primary" @click="resetSettings">恢复默认</el-button>
          </div>
        </div>
      </el-drawer>
    </div>
  </div>
</template>

<script setup name="Graph">
import { ref, computed, onMounted, onBeforeUnmount, watch } from 'vue'
import * as echarts from 'echarts'
import { Search, Refresh, Setting, Collection } from '@element-plus/icons-vue'

const chartRef = ref(null)
let chartInstance = null

const searchKeyword = ref('')
const detailOpen = ref(false)
const selectedNode = ref(null)
const settingsOpen = ref(false)

// ========== 默认设置 ==========
const defaultSettings = {
  showLabels: true,
  showTagLabels: true,
  nodeScale: 1.0,
  edgeOpacity: 0.6,
  edgeWidth: 1.5,
  bgTheme: 'light',
  repulsion: 750,
  gravity: 0.06,
  edgeLength: 105
}

const settings = ref({ ...defaultSettings })

// ========== 配色方案（莫兰迪低饱和）==========
const COLOR_TAG = '#2B2D42'
const COLOR_PDF = '#D4A373'
const COLOR_WORD = '#5B8DEF'
const COLOR_EXCEL = '#6CAE75'
const COLOR_IMAGE = '#E9AFAF'
const COLOR_TEXT = '#8D99AE'

const typeColorMap = {
  pdf: COLOR_PDF, docx: COLOR_WORD, doc: COLOR_WORD,
  xlsx: COLOR_EXCEL, xls: COLOR_EXCEL,
  png: COLOR_IMAGE, jpg: COLOR_IMAGE, jpeg: COLOR_IMAGE,
  txt: COLOR_TEXT, md: COLOR_TEXT
}

const typeNameMap = {
  pdf: 'PDF', docx: 'Word', doc: 'Word',
  xlsx: 'Excel', xls: 'Excel',
  png: '图片', jpg: '图片', jpeg: '图片',
  txt: '文本', md: '文本'
}

// ========== 标签定义 ==========
const tagDefs = [
  { id: 'tag-学习资料', name: '学习资料', color: COLOR_TAG },
  { id: 'tag-申请材料', name: '申请材料', color: COLOR_TAG },
  { id: 'tag-工作文档', name: '工作文档', color: COLOR_TAG }
]

// ========== Mock 文件数据 ==========
const rawFiles = [
  { id: 'n1', name: '实验报告要求_20260501.pdf', type: 'pdf', path: '/学习资料/大三下/计算机网络', value: 256000, tag: '学习资料' },
  { id: 'n2', name: '国家奖学金申请表_20260901.docx', type: 'docx', path: '/申请材料/奖学金', value: 128000, tag: '申请材料' },
  { id: 'n3', name: '个人简历_2026版.docx', type: 'docx', path: '/工作文档/实习材料', value: 64000, tag: '工作文档' },
  { id: 'n4', name: 'OS大作业要求_20260315.pdf', type: 'pdf', path: '/学习资料/大三下/操作系统', value: 512000, tag: '学习资料' },
  { id: 'n5', name: '课程论文初稿_20260410.docx', type: 'docx', path: '/学习资料/大三下/文献综述', value: 320000, tag: '学习资料' },
  { id: 'n6', name: '社团活动策划书_20260520.docx', type: 'docx', path: '/申请材料/社团', value: 180000, tag: '申请材料' },
  { id: 'n7', name: '成绩单_大三下.pdf', type: 'pdf', path: '/申请材料/成绩单', value: 1024000, tag: '申请材料' },
  { id: 'n8', name: '实习证明_2026.pdf', type: 'pdf', path: '/工作文档/实习材料', value: 200000, tag: '工作文档' },
  { id: 'n9', name: '奖学金申请通知_20260901.pdf', type: 'pdf', path: '/申请材料/奖学金', value: 150000, tag: '申请材料' },
  { id: 'n10', name: '实验报告模板.docx', type: 'docx', path: '/学习资料/大三下/计算机网络', value: 80000, tag: '学习资料' },
  { id: 'n11', name: 'OS课程大纲.pdf', type: 'pdf', path: '/学习资料/大三下/操作系统', value: 300000, tag: '学习资料' },
  { id: 'n12', name: '社会实践证明_2024.pdf', type: 'pdf', path: '/申请材料/奖学金', value: 120000, tag: '申请材料' },
  { id: 'n13', name: '获奖证书扫描件.png', type: 'png', path: '/申请材料/奖学金', value: 500000, tag: '申请材料' },
  { id: 'n14', name: '实习offer_2026.pdf', type: 'pdf', path: '/工作文档/实习材料', value: 100000, tag: '工作文档' },
  { id: 'n15', name: '文献综述参考文献.xlsx', type: 'xlsx', path: '/学习资料/大三下/文献综述', value: 45000, tag: '学习资料' },
  { id: 'n16', name: '课程表_大三下.xlsx', type: 'xlsx', path: '/学习资料/大三下', value: 32000, tag: '学习资料' },
  { id: 'n17', name: '读书笔记_操作系统.txt', type: 'txt', path: '/学习资料/大三下/操作系统', value: 15000, tag: '学习资料' },
  { id: 'n18', name: '面试准备笔记.md', type: 'md', path: '/工作文档/实习材料', value: 22000, tag: '工作文档' },
  { id: 'n19', name: '项目截图_1.png', type: 'png', path: '/工作文档/实习材料', value: 800000, tag: '工作文档' },
  { id: 'n20', name: '项目截图_2.png', type: 'png', path: '/工作文档/实习材料', value: 750000, tag: '工作文档' },
  { id: 'n21', name: '报销单模板.xlsx', type: 'xlsx', path: '/申请材料/报销', value: 28000, tag: '申请材料' },
  { id: 'n22', name: '社团成员名单.xlsx', type: 'xlsx', path: '/申请材料/社团', value: 18000, tag: '申请材料' },
  { id: 'n23', name: '毕业论文大纲.docx', type: 'docx', path: '/学习资料/大四上', value: 45000, tag: '学习资料' },
  { id: 'n24', name: '导师联系方式.txt', type: 'txt', path: '/学习资料/大四上', value: 5000, tag: '学习资料' },
  { id: 'n25', name: '图书馆借阅记录.xlsx', type: 'xlsx', path: '/学习资料', value: 25000, tag: '学习资料' }
]

// ========== 构建节点与边 ==========

// 标签 → 文件映射
const tagFileMap = {}
tagDefs.forEach(t => { tagFileMap[t.id] = [] })
rawFiles.forEach(f => {
  const tagId = `tag-${f.tag}`
  if (tagFileMap[tagId]) tagFileMap[tagId].push(f)
})

// 计算文件节点的关联信息（同标签的其他文件）
const fileLinkMap = {}
rawFiles.forEach(f => {
  const sameTagFiles = tagFileMap[`tag-${f.tag}`].filter(x => x.id !== f.id)
  fileLinkMap[f.id] = sameTagFiles.map(x => ({
    id: x.id, name: x.name, type: x.type, tag: f.tag
  }))
})

function getCategoryIndex(fileType) {
  const map = { pdf: 1, docx: 2, doc: 2, xlsx: 3, xls: 3, png: 4, jpg: 4, jpeg: 4, txt: 5, md: 5 }
  return map[fileType] || 1
}

// ECharts categories（图例）
const categories = [
  { name: '标签', itemStyle: { color: COLOR_TAG } },
  { name: 'PDF', itemStyle: { color: COLOR_PDF } },
  { name: 'Word', itemStyle: { color: COLOR_WORD } },
  { name: 'Excel', itemStyle: { color: COLOR_EXCEL } },
  { name: '图片', itemStyle: { color: COLOR_IMAGE } },
  { name: '文本', itemStyle: { color: COLOR_TEXT } }
]

// ========== 根据设置生成节点和边 ==========
function buildGraphData(s) {
  const nodes = []

  // 标签节点
  nodes.push(...tagDefs.map(t => ({
    id: t.id,
    name: t.name,
    value: tagFileMap[t.id].length,
    symbolSize: 72 * s.nodeScale,
    symbol: 'circle',
    category: 0,
    nodeType: 'tag',
    color: t.color,
    linkCount: tagFileMap[t.id].length,
    links: tagFileMap[t.id].map(f => ({
      id: f.id, name: f.name, type: f.type, color: typeColorMap[f.type]
    })),
    itemStyle: {
      color: t.color,
      shadowBlur: 20,
      shadowColor: t.color + '40'
    },
    label: {
      show: s.showTagLabels,
      fontSize: 15,
      fontWeight: 'bold',
      color: '#fff',
      formatter: '{b}\n({c} 个文件)'
    }
  })))

  // 文件节点
  nodes.push(...rawFiles.map(f => ({
    id: f.id,
    name: f.name,
    value: f.value,
    symbolSize: 22 * s.nodeScale,
    category: getCategoryIndex(f.type),
    nodeType: 'file',
    type: f.type,
    path: f.path,
    tags: [f.tag],
    color: typeColorMap[f.type],
    links: fileLinkMap[f.id],
    itemStyle: {
      color: typeColorMap[f.type],
      borderColor: '#fff',
      borderWidth: 2
    },
    label: {
      show: s.showLabels,
      fontSize: 11,
      color: '#4a4a4a'
    }
  })))

  // 边数据
  const links = rawFiles.map(f => ({
    source: `tag-${f.tag}`,
    target: f.id,
    lineStyle: {
      color: typeColorMap[f.type] + '60',
      width: s.edgeWidth,
      curveness: 0.05
    }
  }))

  return { nodes, links }
}

// ========== 统计 ==========
const stats = computed(() => [
  { label: '标签数', value: tagDefs.length, type: 'primary' },
  { label: '文件数', value: rawFiles.length, type: 'success' },
  { label: 'PDF', value: rawFiles.filter(n => n.type === 'pdf').length, type: '' },
  { label: 'Word', value: rawFiles.filter(n => n.type === 'docx').length, type: '' },
  { label: 'Excel', value: rawFiles.filter(n => n.type === 'xlsx').length, type: '' }
])

// ========== 初始化图表 ==========
function initChart() {
  if (!chartRef.value) return
  chartInstance = echarts.init(chartRef.value)
  applySettings(true)

  chartInstance.on('click', (params) => {
    if (params.dataType === 'node') {
      selectedNode.value = params.data
      detailOpen.value = true
    }
  })

  window.addEventListener('resize', handleResize)
}

function handleResize() {
  chartInstance?.resize()
}

function resetGraph() {
  chartInstance?.dispatchAction({ type: 'restore' })
}

function applySettings(isInit = false) {
  if (!chartInstance) return
  const s = settings.value
  const { nodes, links } = buildGraphData(s)

  chartInstance.setOption({
    tooltip: {
      trigger: 'item',
      formatter: (params) => {
        if (params.dataType === 'node') {
          const d = params.data
          if (d.nodeType === 'tag') {
            return `<b>${d.name}</b><br/>类型：标签<br/>下属文件：${d.value} 个`
          }
          return `<b>${d.name}</b><br/>类型：${d.type?.toUpperCase()}<br/>路径：${d.path}<br/>标签：${d.tags?.join('、')}`
        }
        return ''
      }
    },
    legend: {
      data: categories.map(c => c.name),
      top: 12,
      left: 16,
      textStyle: { fontSize: 12, color: '#555' },
      itemWidth: 14,
      itemHeight: 14,
      itemGap: 16
    },
    series: [{
      type: 'graph',
      layout: 'force',
      data: nodes,
      links: links,
      categories: categories,
      roam: true,
      draggable: true,
      focusNodeAdjacency: true,
      force: {
        repulsion: s.repulsion,
        edgeLength: s.edgeLength,
        gravity: s.gravity,
        layoutAnimation: true
      },
      label: {
        show: s.showLabels,
        position: 'right',
        formatter: '{b}',
        fontSize: 11
      },
      lineStyle: {
        opacity: s.edgeOpacity,
        width: s.edgeWidth
      },
      emphasis: {
        focus: 'adjacency',
        lineStyle: { width: 2.5, opacity: 0.9 }
      },
      animationDuration: isInit ? 1500 : 500,
      animationEasingUpdate: 'quinticInOut'
    }]
  }, true) // true = not merge, replace
}

// 监听设置变化实时更新图表
watch(settings, () => {
  applySettings(false)
}, { deep: true })

function handleSearch() {
  const kw = searchKeyword.value.trim()
  if (!kw || !chartInstance) return

  const { nodes } = buildGraphData(settings.value)
  const found = nodes.find(n =>
    n.name.toLowerCase().includes(kw.toLowerCase()) ||
    (n.nodeType === 'file' && n.path.toLowerCase().includes(kw.toLowerCase()))
  )
  if (found) {
    const idx = nodes.findIndex(n => n.id === found.id)
    chartInstance.dispatchAction({ type: 'focusNodeAdjacency', seriesIndex: 0, dataIndex: idx })
    chartInstance.dispatchAction({ type: 'highlight', seriesIndex: 0, dataIndex: idx })
    selectedNode.value = found
    detailOpen.value = true
  }
}

function highlightNode(nodeId) {
  if (!chartInstance) return
  const { nodes } = buildGraphData(settings.value)
  const idx = nodes.findIndex(n => n.id === nodeId)
  if (idx !== -1) {
    chartInstance.dispatchAction({ type: 'focusNodeAdjacency', seriesIndex: 0, dataIndex: idx })
    chartInstance.dispatchAction({ type: 'highlight', seriesIndex: 0, dataIndex: idx })
  }
}

function resetSettings() {
  settings.value = { ...defaultSettings }
}

// ========== 工具函数 ==========
function getFileIcon(fileType) {
  const iconMap = {
    pdf: 'pdf', doc: 'word', docx: 'word',
    xls: 'excel', xlsx: 'excel',
    png: 'image', jpg: 'image', jpeg: 'image',
    txt: 'txt', md: 'txt'
  }
  return iconMap[fileType?.toLowerCase()] || 'file'
}

function formatFileSize(size) {
  if (!size) return '-'
  if (size < 1024) return size + ' B'
  if (size < 1024 * 1024) return (size / 1024).toFixed(1) + ' KB'
  if (size < 1024 * 1024 * 1024) return (size / (1024 * 1024)).toFixed(1) + ' MB'
  return (size / (1024 * 1024 * 1024)).toFixed(1) + ' GB'
}

onMounted(() => {
  initChart()
})

onBeforeUnmount(() => {
  window.removeEventListener('resize', handleResize)
  chartInstance?.dispose()
})
</script>

<style scoped>
.graph-page {
  display: flex;
  flex-direction: column;
  height: calc(100vh - 84px);
  padding: 20px 24px;
  overflow: hidden;
}

.graph-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 12px;
  flex-shrink: 0;
}

.graph-header__left {
  flex: 1;
}

.page-title {
  margin: 0 0 6px;
  font-size: 22px;
  color: var(--el-text-color-primary);
}

.page-desc {
  margin: 0;
  font-size: 14px;
  color: var(--el-text-color-regular);
}

.graph-header__right {
  display: flex;
  align-items: center;
  gap: 10px;
}

.graph-search {
  width: 260px;
}

.graph-stats {
  display: flex;
  gap: 10px;
  margin-bottom: 12px;
  flex-shrink: 0;
}

.graph-body {
  flex: 1;
  min-height: 0;
  border-radius: 12px;
  border: 1px solid var(--el-border-color-light);
  background: #fff;
  overflow: hidden;
  position: relative;
  transition: background 0.3s;
}

.graph-body--dark {
  background: #1a1a2e;
}

.graph-body--warm {
  background: #faf6f1;
}

.graph-chart {
  width: 100%;
  height: 100%;
}

/* ========== 节点详情面板 ========== */
.node-detail {
  padding: 8px 4px;
}

.node-detail__icon {
  display: flex;
  justify-content: center;
  margin-bottom: 12px;
}

.node-detail__icon.tag-icon {
  padding: 16px;
  border-radius: 50%;
  background: #f0f4ff;
  width: fit-content;
  margin: 0 auto 12px;
}

.detail-file-icon {
  font-size: 48px;
  color: var(--el-color-primary);
}

.node-detail__name {
  margin: 0 0 16px;
  font-size: 16px;
  font-weight: 600;
  text-align: center;
  color: var(--el-text-color-primary);
  word-break: break-all;
}

.node-detail__links {
  margin-top: 20px;
}

.node-detail__links h4 {
  margin: 0 0 10px;
  font-size: 14px;
  color: var(--el-text-color-primary);
}

.link-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.link-item {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px 10px;
  border-radius: 8px;
  background: #f5f7fa;
  cursor: pointer;
  transition: background 0.2s;
}

.link-item:hover {
  background: #e6f0ff;
}

.link-item span {
  flex: 1;
  font-size: 13px;
  color: var(--el-text-color-primary);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.link-icon {
  font-size: 16px;
  color: var(--el-color-primary);
  flex-shrink: 0;
}

.tag-badge {
  margin-right: 6px;
}

/* ========== 设置面板 ========== */
.settings-body {
  padding: 4px 8px;
}

.settings-group {
  margin-bottom: 24px;
}

.settings-group__title {
  margin: 0 0 14px;
  font-size: 14px;
  font-weight: 600;
  color: var(--el-text-color-primary);
  padding-bottom: 8px;
  border-bottom: 1px solid var(--el-border-color-lighter);
}

.settings-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 14px;
  font-size: 13px;
  color: var(--el-text-color-regular);
}

.settings-row--slider {
  flex-wrap: wrap;
  gap: 6px;
}

.settings-row--slider > span:first-child {
  width: 100%;
}

.settings-row--slider .el-slider {
  flex: 1;
  min-width: 0;
}

.slider-value {
  min-width: 44px;
  text-align: right;
  font-size: 12px;
  color: var(--el-text-color-secondary);
  font-variant-numeric: tabular-nums;
}

.settings-footer {
  display: flex;
  justify-content: center;
  padding-top: 8px;
  border-top: 1px solid var(--el-border-color-lighter);
}
</style>
