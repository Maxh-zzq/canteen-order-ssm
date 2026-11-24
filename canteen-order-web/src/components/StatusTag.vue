<template>
  <el-tag :type="orderStatusType(status)" effect="light" round size="small">
    {{ orderStatusText(status) }}
  </el-tag>
</template>

<script setup lang="ts">
import { orderStatusText, orderStatusType } from '@/utils/format'

defineProps<{ status: number }>()
</script>
