<script setup lang="ts">
import { computed } from 'vue'
import { statusLabel, statusType } from '@/utils/order'

const props = defineProps<{ status: string }>()

const label = computed(() => statusLabel(props.status))
const type = computed(() => statusType(props.status))
</script>

<template>
  <el-tag :type="type" effect="light" round>{{ label }}</el-tag>
</template>
