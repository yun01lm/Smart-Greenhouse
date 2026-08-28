<template>
  <div class="blocked-page">
    <el-result icon="warning" title="请使用手机 APP 登录" sub-title="普通员工账号仅支持通过手机 APP 使用本系统，请下载/打开 APP 登录。">
      <template #extra>
        <el-button type="primary" @click="logout">返回登录页</el-button>
      </template>
    </el-result>
  </div>
</template>

<script setup>
import { useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'

const router = useRouter()
const authStore = useAuthStore()

function logout() {
  authStore.logout()
  router.push('/login')
}
</script>

<style scoped>
.blocked-page {
  height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #0a1628 0%, #16263f 100%);
}
</style>