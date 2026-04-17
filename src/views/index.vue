<template>
  <div class="app-container home">
    <el-row :gutter="20">
      <el-col :sm="24" :lg="24" style="padding-left: 20px">
        <h2>欢迎使用 LingDoc</h2>
        <p>基于 RuoYi-Vue3 打造的专属客户端，简洁、高效。</p>
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
