<template>
  <div class="app-container">
    <h2>版本溯源</h2>
    <p class="text-secondary">查看文件的历史版本时间线，支持一键预览与回滚。</p>
    <el-card class="box-card mt-20">
      <el-empty description="MVP 开发中">
        <template #description>
          <p>历史版本溯源功能正在开发中</p>
          <p class="text-tip">自动保存文档快照，完整记录每一次变更</p>
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
