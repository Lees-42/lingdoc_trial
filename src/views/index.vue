<template>
  <div class="dashboard-page">
    <section class="dashboard-hero panel-glass">
      <div class="hero-copy">
        <el-tag class="hero-tag" round>仪表盘 Demo</el-tag>
        <h1 class="brand-title">灵档</h1>
        <p class="brand-slogan">让文档回到本地，让每一次整理、检索与回溯都更清晰、更可控。</p>
        <div class="hero-actions">
          <el-button class="main-btn" type="primary" round @click="navigate('/lingdoc/vault')">进入文件浏览器</el-button>
          <el-button class="main-btn main-btn--ghost" round @click="navigate('/lingdoc/agent')">打开灵犀问答</el-button>
        </div>
      </div>
      <div class="hero-signal">
        <span class="signal-label">工作区状态</span>
        <strong><i class="pulse-dot"></i>系统正在运行</strong>
        <div class="signal-usage">
          <div class="signal-usage__head">
            <span>算力使用情况</span>
            <strong>68%</strong>
          </div>
          <el-progress :percentage="68" :show-text="false" :stroke-width="10" />
        </div>
      </div>
    </section>

    <section class="section-block">
      <div class="section-heading">
        <h2>仪表盘概览</h2>
        <span class="section-caption">1.0.5 · DEMO MODE</span>
      </div>
      <div class="overview-strip">
        <article
          v-for="card in overviewCards"
          :key="card.label"
          class="overview-card panel-glass"
          :class="`overview-card--${card.variant}`"
        >
          <span class="overview-label">{{ card.label }}</span>
          <strong class="overview-value">{{ card.value }}</strong>
          <p class="overview-trend">{{ card.trend }}</p>
        </article>
      </div>
      <div class="chart-grid">
        <article class="chart-card panel-glass">
          <div class="chart-head">
            <h3>近 7 天处理趋势</h3>
          </div>
          <div ref="lineChartRef" class="chart-canvas"></div>
        </article>
        <article class="chart-card chart-card--heatmap panel-glass">
          <div class="chart-head">
            <h3>当月使用强度热力图</h3>
          </div>
          <div ref="heatmapChartRef" class="chart-canvas chart-canvas--heatmap"></div>
        </article>
      </div>
    </section>

    <section class="section-block">
      <div class="section-heading">
        <h2>功能跳转</h2>
      </div>
      <div class="action-grid">
        <router-link
          v-for="item in quickActions"
          :key="item.path"
          :to="item.path"
          class="action-card panel-glass"
        >
          <div class="action-head">
            <span class="action-tag">{{ item.tag }}</span>
            <svg-icon :icon-class="item.icon" class="action-icon" />
          </div>
          <div class="action-ornaments">
            <i class="shape shape-circle"></i>
            <i class="shape shape-square"></i>
            <i class="shape shape-pill"></i>
          </div>
          <h3>{{ item.title }}</h3>
          <p>{{ item.description }}</p>
          <span class="action-link">立即进入</span>
        </router-link>
      </div>
    </section>

    <section class="section-block">
      <div class="section-heading">
        <h2>运行脉冲</h2>
      </div>
      <div class="wordcloud-card panel-glass">
        <div class="wordcloud-head">
          <h3>文档语义热点</h3>
          <p>基于上传文档词频生成的词云</p>
        </div>
        <div class="wordcloud-board">
          <span
            v-for="term in wordCloudTerms"
            :key="term.text"
            class="cloud-term"
            :style="{
              left: `${term.x}%`,
              top: `${term.y}%`,
              fontSize: `${term.size}px`,
              transform: `translate(-50%, -50%) rotate(${term.rotate}deg)`,
              color: term.color,
              fontWeight: term.weight
            }"
          >
            {{ term.text }}
          </span>
        </div>
      </div>
    </section>
  </div>
</template>

<script setup>
import * as echarts from 'echarts'

const router = useRouter()

const version = import.meta.env.VITE_APP_VERSION || '3.9.2'

const lineChartRef = ref(null)
const heatmapChartRef = ref(null)

let lineChart = null
let heatmapChart = null

const overviewCards = [
  {
    label: '文档库存',
    value: '1,286',
    trend: '较昨日 +18',
    variant: 'volume'
  },
  {
    label: '待处理条目',
    value: '32',
    trend: '预计 14 分钟完成',
    variant: 'queue'
  },
  {
    label: '检索命中率',
    value: '98.4%',
    trend: '近 7 天稳定',
    variant: 'quality'
  }
]

const quickActions = [
  {
    title: '文件浏览器',
    tag: '浏览',
    icon: 'folder',
    path: '/lingdoc/vault',
    description: '浏览、预览和管理 Vault 中的所有文件，支持上传与检索。'
  },
  {
    title: '灵犀问答',
    tag: '搜索',
    icon: 'search',
    path: '/lingdoc/agent',
    description: '按关键词、上下文和使用场景快速定位目标文档。'
  },
  {
    title: '版本溯源',
    tag: '追踪',
    icon: 'time',
    path: '/lingdoc/version',
    description: '查看文档变更脉络，保留整理前后的信息连续性。'
  },
  {
    title: '关系图谱',
    tag: '可视化',
    icon: 'form',
    path: '/lingdoc/graph',
    description: '探索本地文件间的隐性关联网络，发现数据孤岛与核心枢纽。'
  }
]

const wordCloudTerms = [
  // 核心大词 (Size 40+)
  { text: 'DDL', size: 45, x: 50, y: 45, rotate: 0, color: '#0b5ea6', weight: 800 },
  { text: '期末复习', size: 38, x: 25, y: 30, rotate: -5, color: '#3d4f9d', weight: 700 },
  { text: '实验报告', size: 36, x: 75, y: 25, rotate: 10, color: '#0a8a77', weight: 700 },

  // 中等频率词 (Size 25-35)
  { text: '课程论文', size: 32, x: 35, y: 65, rotate: -8, color: '#186baf', weight: 600 },
  { text: '社团策划', size: 30, x: 70, y: 55, rotate: 12, color: '#1c8968', weight: 600 },
  { text: '文献综述', size: 28, x: 55, y: 15, rotate: -15, color: '#4472c4', weight: 600 },
  { text: '实习简历', size: 26, x: 15, y: 55, rotate: 0, color: '#2f7f5f', weight: 600 },

  // 高频小词 (Size 18-24)
  { text: 'PPT大作业', size: 24, x: 82, y: 75, rotate: -10, color: '#4658a8', weight: 500 },
  { text: '考公/考研', size: 22, x: 20, y: 80, rotate: 5, color: '#0f5c7f', weight: 600 },
  { text: '证书/成绩单', size: 20, x: 45, y: 85, rotate: 0, color: '#166883', weight: 500 },
  { text: '奖学金申请', size: 18, x: 10, y: 15, rotate: 15, color: '#1c8968', weight: 500 }
]

function navigate(path) {
  router.push(path)
}

function initLineChart() {
  if (!lineChartRef.value) {
    return
  }
  lineChart = echarts.init(lineChartRef.value)
  lineChart.setOption({
    grid: { left: 24, right: 18, top: 30, bottom: 22 },
    xAxis: {
      type: 'category',
      boundaryGap: false,
      data: ['Mon', 'Tue', 'Wed', 'Thu', 'Fri', 'Sat', 'Sun'],
      axisLine: { lineStyle: { color: '#a8bbcc' } },
      axisLabel: { color: '#5f758b' }
    },
    yAxis: {
      type: 'value',
      splitLine: { lineStyle: { color: 'rgba(26, 82, 146, 0.12)' } },
      axisLabel: { color: '#5f758b' }
    },
    tooltip: { trigger: 'axis' },
    series: [
      {
        name: '处理量',
        type: 'line',
        smooth: true,
        data: [95, 132, 121, 154, 170, 146, 188],
        symbolSize: 8,
        lineStyle: { width: 4, color: '#0f69c8' },
        itemStyle: { color: '#0f69c8' },
        areaStyle: {
          color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
            { offset: 0, color: 'rgba(15, 105, 200, 0.35)' },
            { offset: 1, color: 'rgba(15, 105, 200, 0.02)' }
          ])
        }
      }
    ]
  })
}

function initHeatmapChart() {
  if (!heatmapChartRef.value) {
    return
  }
  heatmapChart = echarts.init(heatmapChartRef.value)
  const now = new Date()
  const year = now.getFullYear()
  const month = now.getMonth() + 1
  const monthText = String(month).padStart(2, '0')
  const monthRange = `${year}-${monthText}`
  const daysInMonth = new Date(year, month, 0).getDate()

  const heatData = Array.from({ length: daysInMonth }, (_, index) => {
    const day = index + 1
    const dayText = String(day).padStart(2, '0')
    const dateText = `${year}-${monthText}-${dayText}`
    // 模拟当月每日使用强度：工作日更高，周末更低。
    const weekDay = new Date(year, month - 1, day).getDay()
    const baseline = weekDay === 0 || weekDay === 6 ? 3 : 6
    const wave = Math.round((Math.sin(day * 0.55) + 1) * 2)
    const intensity = Math.min(10, baseline + wave)
    return [dateText, intensity]
  })

  heatmapChart.setOption({
    tooltip: {
      position: 'top',
      formatter: ({ data }) => {
        return `${data[0]} 使用强度：${data[1]}`
      }
    },
    calendar: {
      top: 40,
      left: 40,      
      right: 20,
      bottom: 40,    
      range: monthRange,
      cellSize: ['auto', 20], 
      splitLine: {
        show: true,
        lineStyle: { color: '#ffffff', width: 2 }
      },
      itemStyle: {
        borderWidth: 1,
        borderColor: 'rgba(255, 255, 255, 0.8)'
      },
      yearLabel: { show: false },
      monthLabel: {
        show: true,
        color: '#4f6980'
      },
      dayLabel: {
        firstDay: 1,
        nameMap: 'cn',
        color: '#5f758b'
      }
    },
    visualMap: {
      min: 0,
      max: 10,
      orient: 'horizontal',
      left: 'center',
      bottom: 10,
      text: ['高频', '低频'],
      calculable: false,
      inRange: {
        color: ['#edf6ff', '#a7d4ff', '#4f9ff0', '#1f6dc6']
      },
      textStyle: { color: '#5f758b' }
    },
    series: [
      {
        name: '当月每天使用强度',
        type: 'heatmap',
        coordinateSystem: 'calendar',
        data: heatData,
        label: { show: false },
        emphasis: {
          itemStyle: {
            shadowBlur: 10,
            shadowColor: 'rgba(12, 54, 105, 0.24)'
          }
        }
      }
    ]
  })
}

function disposeCharts() {
  if (lineChart) {
    lineChart.dispose()
    lineChart = null
  }
  if (heatmapChart) {
    heatmapChart.dispose()
    heatmapChart = null
  }
}

function handleResize() {
  lineChart?.resize()
  heatmapChart?.resize()
}

onMounted(async () => {
  await nextTick()
  initLineChart()
  initHeatmapChart()
  window.addEventListener('resize', handleResize)
})

onBeforeUnmount(() => {
  window.removeEventListener('resize', handleResize)
  disposeCharts()
})
</script>

<style scoped lang="scss">
.dashboard-page {
  position: relative;
  padding: 24px;
  min-height: calc(100vh - 120px);
  overflow: hidden;
  background:
    radial-gradient(circle at 12% 18%, rgba(8, 132, 154, 0.20), transparent 30%),
    radial-gradient(circle at 90% 8%, rgba(16, 73, 168, 0.16), transparent 28%),
    linear-gradient(165deg, #eef6ff 0%, #f5fbff 48%, #eef9f5 100%);

  &::before,
  &::after {
    content: '';
    position: absolute;
    border-radius: 50%;
    filter: blur(8px);
    pointer-events: none;
    z-index: 0;
  }

  &::before {
    width: 220px;
    height: 220px;
    right: -90px;
    top: 40px;
    background: rgba(15, 70, 155, 0.12);
  }

  &::after {
    width: 280px;
    height: 280px;
    left: -120px;
    bottom: -110px;
    background: rgba(11, 133, 109, 0.12);
  }
}

.dashboard-hero,
.section-block {
  position: relative;
  z-index: 1;
}

.panel-glass {
  border: 1px solid rgba(19, 76, 141, 0.10);
  background: rgba(255, 255, 255, 0.82);
  backdrop-filter: blur(18px);
  box-shadow: 0 20px 44px rgba(12, 55, 106, 0.10);
}

.dashboard-hero {
  display: grid;
  grid-template-columns: minmax(0, 1.4fr) minmax(280px, 0.9fr);
  gap: 24px;
  align-items: center;
  border-radius: 30px;
  padding: 30px;
}

.hero-copy {
  display: flex;
  flex-direction: column;
  justify-content: flex-start;
}

.brand-title {
  margin: 14px 0 10px;
  font-size: clamp(36px, 4.5vw, 58px);
  line-height: 1.05;
  letter-spacing: -0.05em;
  background: linear-gradient(135deg, #0f4da8 0%, #18a58d 48%, #0f2f4d 100%);
  -webkit-background-clip: text;
  background-clip: text;
  color: transparent;
  text-shadow: 0 8px 24px rgba(15, 77, 168, 0.12);
}

.brand-slogan {
  margin: 0;
  max-width: 720px;
  color: #4f6980;
  font-size: 15px;
  line-height: 1.9;
}

.hero-tag {
  width: fit-content;
  border: none;
  background: linear-gradient(140deg, rgba(16, 73, 168, 0.18), rgba(8, 132, 154, 0.16));
  color: #0d4a89;
}

.hero-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
  margin-top: 24px;
}

.main-btn {
  min-width: 152px;
  box-shadow: 0 12px 28px rgba(15, 77, 168, 0.18);
  transform: translateY(0);
  transition: transform 0.2s ease, box-shadow 0.2s ease, filter 0.2s ease;

  &:hover {
    transform: translateY(-2px);
    box-shadow: 0 16px 34px rgba(15, 77, 168, 0.22);
    filter: brightness(1.02);
  }
}

.main-btn--ghost {
  background: linear-gradient(180deg, rgba(255, 255, 255, 0.92), rgba(242, 249, 255, 0.92));
  border-color: rgba(15, 77, 168, 0.14);
  color: #0f4da8;
}

.hero-signal {
  display: flex;
  flex-direction: column;
  justify-content: center;
  min-height: 180px;
  border-radius: 24px;
  padding: 22px;
  color: #eff7ff;
  background: linear-gradient(160deg, rgba(11, 57, 116, 0.94), rgba(7, 127, 131, 0.90));

  strong {
    display: inline-flex;
    align-items: center;
    gap: 10px;
    margin: 10px 0 8px;
    font-size: 24px;
    line-height: 1.3;
  }

  p {
    margin: 0;
    color: rgba(239, 247, 255, 0.90);
    line-height: 1.7;
  }
}

.signal-usage {
  margin-top: 16px;

  :deep(.el-progress-bar__outer) {
    background: rgba(255, 255, 255, 0.14);
  }

  :deep(.el-progress-bar__inner) {
    background: linear-gradient(90deg, #4ed8b2 0%, #3ea0ff 100%);
  }
}

.signal-usage__head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 10px;
  color: rgba(239, 247, 255, 0.94);
  font-size: 13px;

  strong {
    margin: 0;
    font-size: 18px;
    line-height: 1;
  }
}

.signal-label {
  font-size: 12px;
  letter-spacing: 0.14em;
  text-transform: uppercase;
  opacity: 0.86;
}

.pulse-dot {
  position: relative;
  width: 10px;
  height: 10px;
  border-radius: 50%;
  background: #33d1a1;
  box-shadow: 0 0 0 0 rgba(51, 209, 161, 0.5);
  animation: pulseGlow 1.8s infinite;
}

@keyframes pulseGlow {
  0% {
    box-shadow: 0 0 0 0 rgba(51, 209, 161, 0.42);
  }

  70% {
    box-shadow: 0 0 0 12px rgba(51, 209, 161, 0);
  }

  100% {
    box-shadow: 0 0 0 0 rgba(51, 209, 161, 0);
  }
}

.section-block {
  margin-top: 24px;
}

.section-heading {
  display: flex;
  align-items: baseline;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 14px;

  h2 {
    margin: 0;
    color: #12324f;
    font-size: 22px;
  }

  p {
    margin: 0;
    color: #667a8f;
  }
}

.section-caption {
  font-size: 12px;
  font-weight: 700;
  letter-spacing: 0.12em;
  text-transform: uppercase;
  color: #0e4f8f;
}

.overview-strip,
.chart-grid,
.action-grid,
.pulse-grid {
  display: grid;
  gap: 16px;
}

.overview-strip {
  grid-template-columns: repeat(auto-fit, minmax(220px, 1fr));
}

.chart-grid {
  margin-top: 16px;
  grid-template-columns: minmax(0, 1.3fr) minmax(0, 0.85fr);
}

.action-grid {
  grid-template-columns: repeat(auto-fit, minmax(220px, 1fr));
}

.pulse-grid {
  grid-template-columns: repeat(auto-fit, minmax(230px, 1fr));
}

.overview-card {
  position: relative;
  border-radius: 22px;
  padding: 20px;
  overflow: hidden;

  &::after {
    content: '';
    position: absolute;
    width: 110px;
    height: 110px;
    right: -34px;
    top: -30px;
    border-radius: 26px;
    transform: rotate(18deg);
    opacity: 0.2;
  }
}

.overview-card--volume {
  background: linear-gradient(155deg, rgba(236, 246, 255, 0.92), rgba(255, 255, 255, 0.86));

  &::after {
    background: linear-gradient(160deg, #4897f0, #8ec5ff);
  }
}

.overview-card--queue {
  background: linear-gradient(155deg, rgba(241, 252, 249, 0.95), rgba(255, 255, 255, 0.86));

  &::after {
    border-radius: 50%;
    background: linear-gradient(160deg, #24b78f, #8ce5cb);
  }
}

.overview-card--quality {
  background: linear-gradient(155deg, rgba(242, 245, 255, 0.95), rgba(255, 255, 255, 0.86));

  &::after {
    transform: rotate(-16deg);
    background: linear-gradient(160deg, #5a6ed5, #a6b5ff);
  }
}

.overview-label {
  font-size: 13px;
  color: #5b7288;
}

.overview-value {
  display: block;
  margin-top: 8px;
  color: #0f3150;
  font-size: 34px;
  font-weight: 700;
  line-height: 1.1;
}

.overview-trend {
  margin: 8px 0 0;
  color: #0b6b68;
  font-size: 13px;
}

.chart-card {
  border-radius: 24px;
  padding: 16px 16px 10px;
}

.chart-card--heatmap {
  padding-bottom: 8px;
}

.chart-head {
  display: flex;
  align-items: baseline;
  justify-content: space-between;
  padding: 4px 4px 0;

  h3 {
    margin: 0;
    color: #103552;
    font-size: 16px;
  }

  span {
    color: #0f5d99;
    font-size: 12px;
    letter-spacing: 0.08em;
    text-transform: uppercase;
    font-weight: 700;
  }
}

.chart-canvas {
  width: 100%;
  height: 290px;
}

.chart-canvas--heatmap {
  width: 100%;       
  max-width: none;   
  height: 290px;     
  margin: 0;         
}

.action-card {
  position: relative;
  display: block;
  padding: 22px;
  border-radius: 24px;
  overflow: hidden;
  transition: transform 0.2s ease, box-shadow 0.2s ease, border-color 0.2s ease;
  color: #12324f;

  &::before {
    content: '';
    position: absolute;
    inset: 0;
    background: linear-gradient(145deg, rgba(14, 72, 140, 0.06), rgba(25, 108, 110, 0.02));
    opacity: 0;
    transition: opacity 0.2s ease;
  }

  &:hover {
    transform: translateY(-5px);
    box-shadow: 0 26px 46px rgba(16, 59, 107, 0.16);
    border-color: rgba(17, 86, 161, 0.20);

    &::before {
      opacity: 1;
    }
  }

  h3,
  p,
  .action-head,
  .action-ornaments,
  .action-link {
    position: relative;
    z-index: 1;
  }

  h3 {
    margin: 14px 0 8px;
    font-size: 18px;
  }

  p {
    margin: 0;
    color: #5e7388;
    line-height: 1.7;
  }
}

.action-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.action-icon {
  color: #0f5fa9;
  font-size: 20px;
}

.action-tag {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-width: 58px;
  padding: 6px 12px;
  border-radius: 999px;
  background: rgba(21, 69, 126, 0.08);
  color: #0f4c8c;
  font-size: 12px;
  font-weight: 600;
}

.action-ornaments {
  position: absolute;
  right: 16px;
  top: 56px;
  width: 68px;
  height: 52px;

  .shape {
    position: absolute;
    display: block;
    opacity: 0.5;
  }

  .shape-circle {
    width: 12px;
    height: 12px;
    border-radius: 50%;
    right: 0;
    top: 0;
    background: #36a5ea;
  }

  .shape-square {
    width: 12px;
    height: 12px;
    right: 24px;
    top: 16px;
    border-radius: 4px;
    background: #18b48a;
    transform: rotate(12deg);
  }

  .shape-pill {
    width: 30px;
    height: 10px;
    left: 0;
    bottom: 0;
    border-radius: 999px;
    background: #6681da;
    transform: rotate(-14deg);
  }
}

.action-link {
  display: inline-flex;
  align-items: center;
  margin-top: 18px;
  color: #0e488c;
  font-size: 13px;
  font-weight: 600;
}

.wordcloud-card {
  border-radius: 22px;
  padding: 20px;
}

.wordcloud-head {
  h3 {
    margin: 0;
    font-size: 18px;
    color: #12324f;
  }

  p {
    margin: 8px 0 0;
    color: #5c7187;
    line-height: 1.6;
  }
}

.wordcloud-board {
  position: relative;
  margin-top: 14px;
  min-height: 300px;
  border-radius: 18px;
  border: 1px solid rgba(17, 92, 164, 0.12);
  background:
    radial-gradient(circle at 10% 20%, rgba(48, 137, 228, 0.10), transparent 26%),
    radial-gradient(circle at 88% 70%, rgba(22, 173, 138, 0.12), transparent 28%),
    linear-gradient(165deg, rgba(248, 252, 255, 0.92), rgba(242, 250, 246, 0.9));
  overflow: hidden;

  &::before,
  &::after {
    content: '';
    position: absolute;
    border-radius: 50%;
    pointer-events: none;
  }

  &::before {
    width: 160px;
    height: 160px;
    left: -40px;
    top: -50px;
    background: rgba(81, 154, 230, 0.14);
  }

  &::after {
    width: 180px;
    height: 180px;
    right: -60px;
    bottom: -80px;
    background: rgba(19, 169, 136, 0.12);
  }
}

.cloud-term {
  position: absolute;
  white-space: nowrap;
  letter-spacing: 0.01em;
  line-height: 1;
  text-shadow: 0 2px 8px rgba(12, 57, 109, 0.10);
}

.pulse-card {
  border-radius: 22px;
  padding: 22px;

  h3 {
    margin: 0;
    font-size: 18px;
    color: #12324f;
  }

  p {
    margin: 0;
    color: #5c7187;
    line-height: 1.8;
  }

  span {
    display: inline-flex;
    align-items: center;
    margin-top: 10px;
    color: #0e4f8f;
    font-size: 12px;
    font-weight: 700;
    letter-spacing: 0.08em;
    text-transform: uppercase;
  }
}

@media (max-width: 1200px) {
  .dashboard-hero {
    grid-template-columns: 1fr;
  }

  .chart-grid {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 768px) {
  .dashboard-page {
    padding: 16px;
  }

  .dashboard-hero {
    padding: 22px;
  }

  .overview-strip,
  .chart-grid,
  .action-grid,
  .pulse-grid {
    grid-template-columns: 1fr;
  }

  .wordcloud-board {
    min-height: 240px;
  }

  .section-heading {
    align-items: start;
    flex-direction: column;
  }
}
</style>
