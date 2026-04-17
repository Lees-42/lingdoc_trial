<template>
  <div class="app-container home">
    <el-row :gutter="20">
      <el-col :sm="24" :lg="24" style="padding-left: 20px">
        <h2>欢迎使用 LingDoc</h2>
        <p>灵档：您的个人智能文档管家</p>
        <p>数据不出本地，算力按需借用。简洁、高效、安全。</p>
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
const version = '1.0.0'
</script>

<style scoped>
.home {
  padding: 20px;
}
</style>
