<template>
  <div class="app-container">
    <h2 class="page-title">版本溯源</h2>
    <p class="page-desc">查看文件的历史版本时间线，支持一键预览与回滚。</p>
    <el-card class="box-card">
      <el-empty description="MVP 开发中">
        <template #description>
          <p>历史版本溯源功能正在开发中</p>
          <p class="empty-tip">自动保存文档快照，完整记录每一次变更</p>
        </template>
      </el-empty>
    </el-card>
  </div>
</template>

<script setup name="Version">
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
