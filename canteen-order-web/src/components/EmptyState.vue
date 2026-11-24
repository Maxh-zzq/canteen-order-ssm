<template>
  <div class="empty-state">
    <div class="empty-icon">
      <el-icon :size="26"><Bowl /></el-icon>
    </div>
    <h3>{{ title || '暂无数据' }}</h3>
    <p v-if="description">{{ description }}</p>
    <slot />
  </div>
</template>

<script setup lang="ts">
import { Bowl } from '@element-plus/icons-vue'

defineProps<{
  title?: string
  description?: string
}>()
</script>

<style scoped>
.empty-state {
  padding: 54px 20px 48px;
  text-align: center;
  color: var(--muted);
}

.empty-icon {
  width: 66px;
  height: 66px;
  margin: 0 auto 16px;
  border-radius: 50%;
  display: grid;
  place-items: center;
  color: var(--brand);
  background: var(--brand-soft);
}

h3 {
  margin: 0 0 6px;
  font-size: 16px;
  font-weight: 750;
  color: var(--ink);
}

p {
  margin: 0;
  font-size: 13px;
  line-height: 1.7;
}
</style>
