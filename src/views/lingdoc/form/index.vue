<template>
  <div class="app-container">
    <h2 class="page-title">表格助手</h2>
    <p class="page-desc">上传空白表格，AI 将根据本地 Vault 中的已有信息自动生成填写建议。</p>
    <el-card class="box-card">
      <el-empty description="MVP 开发中">
        <template #description>
          <p>智能表格填写助手正在开发中</p>
          <p class="empty-tip">自动识别表格字段并从本地资料中匹配关键信息</p>
        </template>
      </el-empty>
    </el-card>
  </div>
</template>

<script setup name="Form">
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
