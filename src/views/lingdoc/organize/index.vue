<template>
  <div class="app-container">
    <h2>自动规整</h2>
    <p class="text-secondary">拖拽文件到此处，AI 将自动解析内容并推荐规范命名与分类路径。</p>
    <el-card class="box-card mt-20">
      <el-empty description="MVP 开发中">
        <template #description>
          <p>一键自动规整功能正在开发中</p>
          <p class="text-tip">支持图片、PDF、Word、Excel 等格式的智能归档</p>
        </template>
      </el-empty>
    </el-card>
  </div>
</template>

<script setup>
</script>

<style scoped>
.mt-20 {
  margin-top: 20px;
}
.text-secondary {
  color: #606266;
}
.text-tip {
  font-size: 12px;
  color: #909399;
}
</style>
