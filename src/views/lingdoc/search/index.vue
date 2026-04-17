<template>
  <div class="app-container">
    <h2>智能检索</h2>
    <p class="text-secondary">用自然语言提问，AI 将从本地 Vault 中提取答案并定位源文件。</p>
    <el-card class="box-card mt-20">
      <el-empty description="MVP 开发中">
        <template #description>
          <p>自然语言检索与对话功能正在开发中</p>
          <p class="text-tip">基于本地全文索引与阿里千问 API 实现</p>
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
