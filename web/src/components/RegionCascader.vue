<template>
  <el-cascader
    :model-value="modelValue"
    :options="options"
    :loading="loading"
    :placeholder="placeholder"
    :clearable="clearable"
    :style="{ width: width }"
    @update:model-value="handleChange"
  />
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { getProvinces, getCities, getDistricts, getTowns, getVillages } from '@/api/region'

const props = defineProps({
  modelValue: { type: Array, default: () => [] },
  placeholder: { type: String, default: '请选择地区（省/市/县/乡镇/村）' },
  clearable: { type: Boolean, default: true },
  width: { type: String, default: '100%' }
})

const emit = defineEmits(['update:modelValue', 'change'])

/**
 * 五级地区级联（省→市→县→乡镇→村）
 *
 * 实现说明：全量加载整棵地区树（模块级缓存，各页面复用），
 * 不使用 el-cascader 的 lazy 懒加载展开——部分环境下 lazy 展开
 * 点击父级节点不会触发 lazyLoad、下一级永远加载不出来；
 * 全量 options 方案彻底绕开该问题，且无下级的节点天然是叶子可直接选中。
 */
let treeCache = null
const loading = ref(false)
const options = ref([])

async function ensureTree() {
  if (treeCache) {
    options.value = treeCache
    return
  }
  loading.value = true
  try {
    const provinces = (await getProvinces()).data || []
    const tree = []
    await Promise.all(provinces.map(async (p) => {
      const cities = (await getCities(p)).data || []
      const pNode = { value: p, label: p, children: [] }
      await Promise.all(cities.map(async (c) => {
        const districts = (await getDistricts(p, c)).data || []
        const cNode = { value: c, label: c, children: [] }
        await Promise.all(districts.map(async (d) => {
          const towns = (await getTowns(p, c, d)).data || []
          const dNode = { value: d, label: d, children: [] }
          await Promise.all(towns.map(async (t) => {
            const villages = (await getVillages(p, c, d, t)).data || []
            dNode.children.push({
              value: t, label: t,
              children: villages.map(v => ({ value: v, label: v }))
            })
          }))
          cNode.children.push(dNode)
        }))
        pNode.children.push(cNode)
      }))
      tree.push(pNode)
    }))
    treeCache = tree
    options.value = tree
  } finally {
    loading.value = false
  }
}

function handleChange(val) {
  emit('update:modelValue', val)
  emit('change', val)
}

onMounted(ensureTree)
</script>
