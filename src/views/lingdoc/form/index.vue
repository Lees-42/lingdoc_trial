<template>
  <div class="app-container">
    <h2>表格助手</h2>
    <p class="text-secondary">上传空白表格，AI 将根据本地 Vault 中的已有信息自动生成填写建议。</p>
    <el-card class="box-card mt-20">
      <el-empty description="MVP 开发中">
        <template #description>
          <p>智能表格填写助手正在开发中</p>
          <p class="text-tip">自动识别表格字段并从本地资料中匹配关键信息</p>
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
