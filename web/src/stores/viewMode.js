import { defineStore } from 'pinia'
import { ref, computed } from 'vue'

/**
 * 棚主视角切换状态（R10）
 * 管理员在棚主管理页点击"进入管理"后，前端切换为棚主视角查看其系统，
 * 可一键切回管理员视角。数据接口通过 ownerId 代查（ADMIN 后端旁路）。
 */
export const useViewModeStore = defineStore('viewMode', () => {
  const active = ref(false)
  const ownerId = ref(null)
  const ownerName = ref('')
  const ownerUsername = ref('')
  const greenhouseId = ref(null)
  const greenhouses = ref([])

  /** 视角模式下的有效角色：路由守卫与菜单据此放行 OWNER 页面 */
  const effectiveRole = computed(() => (active.value ? 'OWNER' : ''))

  function enterOwnerView(owner, ghList) {
    active.value = true
    ownerId.value = owner.id
    ownerName.value = owner.realName || owner.username
    ownerUsername.value = owner.username
    greenhouses.value = ghList || []
    greenhouseId.value = ghList && ghList.length > 0 ? ghList[0].id : null
  }

  function setGreenhouse(id) {
    greenhouseId.value = id
  }

  function exitOwnerView() {
    active.value = false
    ownerId.value = null
    ownerName.value = ''
    ownerUsername.value = ''
    greenhouseId.value = null
    greenhouses.value = []
  }

  return {
    active, ownerId, ownerName, ownerUsername, greenhouseId, greenhouses,
    effectiveRole, enterOwnerView, setGreenhouse, exitOwnerView
  }
})
