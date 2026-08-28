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

/** 给预查请求加超时兜底：失败或超时都按"无下级"处理，避免级联卡在加载态 */
function peek(promise) {
  return Promise.race([
    Promise.resolve(promise).then(r => (r?.data || []).length > 0, () => false),
    new Promise(res => setTimeout(() => res(false), 3000))
  ])
}

/**
 * 五级级联：省 → 市 → 县(区) → 乡镇 → 村
 * 懒加载时预查下一级：下一级无数据的节点直接标记为 leaf，
 * 否则会出现"点击只展开空列表、永远选不中"的假筛选问题。
 */
const cascaderProps = {
  lazy: true,
  lazyLoad(node, resolve) {
    const { level, value } = node
    if (level === 0) {
      // 省：预查该省是否有市，无则视为叶子（可直接选中）
      getProvinces().then(async (res) => {
        const provinces = res.data || []
        const nodes = await Promise.all(provinces.map(async (p) => {
          const hasChildren = await peek(getCities(p))
          return { value: p, label: p, leaf: !hasChildren }
        }))
        resolve(nodes)
      }).catch(() => resolve([]))
    } else if (level === 1) {
      // 市：预查该市是否有区县，无则视为叶子
      getCities(value).then(async (res) => {
        const cities = res.data || []
        const nodes = await Promise.all(cities.map(async (c) => {
          const hasChildren = await peek(getDistricts(value, c))
          return { value: c, label: c, leaf: !hasChildren }
        }))
        resolve(nodes)
      }).catch(() => resolve([]))
    } else if (level === 2) {
      // 区县：预查是否有乡镇，无则视为叶子
      const [province, city] = node.pathValues
      getDistricts(province, city).then(async (res) => {
        const districts = res.data || []
        const nodes = await Promise.all(districts.map(async (d) => {
          const hasChildren = await peek(getTowns(province, city, d))
          return { value: d, label: d, leaf: !hasChildren }
        }))
        resolve(nodes)
      }).catch(() => resolve([]))
    } else if (level === 3) {
      // 乡镇：预查是否有村，无则视为叶子
      const [province, city, district] = node.pathValues
      getTowns(province, city, district).then(async (res) => {
        const towns = res.data || []
        const nodes = await Promise.all(towns.map(async (t) => {
          const hasChildren = await peek(getVillages(province, city, district, t))
          return { value: t, label: t, leaf: !hasChildren }
        }))
        resolve(nodes)
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
