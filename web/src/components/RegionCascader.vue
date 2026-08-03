<template>
  <el-cascader
    :model-value="modelValue"
    :props="cascaderProps"
    :placeholder="placeholder"
    :clearable="clearable"
    :style="{ width: width }"
    @update:model-value="handleChange"
  />
</template>

<script setup>
import { getProvinces, getCities, getDistricts, getTowns, getVillages } from '@/api/region'

const props = defineProps({
  modelValue: { type: Array, default: () => [] },
  placeholder: { type: String, default: '请选择地区（省/市/县/乡镇/村）' },
  clearable: { type: Boolean, default: true },
  width: { type: String, default: '100%' }
})

const emit = defineEmits(['update:modelValue', 'change'])

// 五级级联：省 → 市 → 县(区) → 乡镇 → 村
const cascaderProps = {
  lazy: true,
  lazyLoad(node, resolve) {
    const { level, value } = node
    if (level === 0) {
      getProvinces().then((res) => {
        resolve((res.data || []).map((p) => ({ value: p, label: p, leaf: false })))
      }).catch(() => resolve([]))
    } else if (level === 1) {
      getCities(value).then((res) => {
        resolve((res.data || []).map((c) => ({ value: c, label: c, leaf: false })))
      }).catch(() => resolve([]))
    } else if (level === 2) {
      const [province, city] = node.pathValues
      getDistricts(province, city).then((res) => {
        resolve((res.data || []).map((d) => ({ value: d, label: d, leaf: false })))
      }).catch(() => resolve([]))
    } else if (level === 3) {
      const [province, city, district] = node.pathValues
      getTowns(province, city, district).then((res) => {
        resolve((res.data || []).map((t) => ({ value: t, label: t, leaf: false })))
      }).catch(() => resolve([]))
    } else if (level === 4) {
      const [province, city, district, town] = node.pathValues
      getVillages(province, city, district, town).then((res) => {
        resolve((res.data || []).map((v) => ({ value: v, label: v, leaf: true })))
      }).catch(() => resolve([]))
    } else {
      resolve([])
    }
  }
}

function handleChange(val) {
  emit('update:modelValue', val)
  emit('change', val)
}
</script>
