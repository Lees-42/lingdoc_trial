<template>
  <div class="app-container">
    <h2 class="page-title">智能检索</h2>
    <p class="page-desc">用自然语言提问，AI 将从本地 Vault 中提取答案并定位源文件。</p>
    <el-card class="box-card">
      <el-empty description="MVP 开发中">
        <template #description>
          <p>自然语言检索与对话功能正在开发中</p>
          <p class="empty-tip">基于本地全文索引与阿里千问 API 实现</p>
        </template>
      </el-empty>
    </el-card>
  </div>
</template>

<script setup name="Search">
</script>

<style scoped>
.page-title {
  margin: 0 0 8px;
  font-size: 22px;
  color: var(--el-text-color-primary);
}

.page-desc {
  margin: 0 0 20px;
  font-size: 14px;
  color: var(--el-text-color-regular);
}

.box-card {
  margin-top: 20px;
}

.empty-tip {
  font-size: 12px;
  color: var(--el-text-color-secondary);
  margin-top: 4px;
}
</style>
