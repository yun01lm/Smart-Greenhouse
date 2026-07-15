<template>
  <div class="corpus-page">
    <!-- 操作栏 -->
    <div class="toolbar">
      <div class="toolbar-left">
        <el-select
          v-model="filterDialect"
          placeholder="方言类型筛选"
          clearable
          style="width: 160px"
          @change="loadData"
        >
          <el-option
            v-for="d in dialects"
            :key="d"
            :label="dialectLabel(d)"
            :value="d"
          />
        </el-select>
        <el-input
          v-model="keyword"
          placeholder="搜索标注文本"
          clearable
          style="width: 240px; margin-left: 12px"
          :prefix-icon="Search"
          @clear="loadData"
          @keyup.enter="loadData"
        />
        <el-button type="primary" style="margin-left: 12px" @click="loadData">搜索</el-button>
      </div>
      <el-button type="primary" :icon="Upload" @click="openUploadDialog">上传语料</el-button>
    </div>

    <!-- 语料表格 -->
    <el-table v-loading="loading" :data="corpusList" stripe border>
      <el-table-column prop="id" label="ID" width="70" align="center" />
      <el-table-column label="方言类型" width="120" align="center">
        <template #default="{ row }">
          <el-tag :type="dialectTagType(row.dialect)" size="small">
            {{ dialectLabel(row.dialect) }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="音频文件" min-width="180">
        <template #default="{ row }">
          <div class="audio-cell">
            <span class="audio-name">{{ row.audioFilename }}</span>
            <span class="audio-size">{{ formatSize(row.audioSize) }}</span>
          </div>
        </template>
      </el-table-column>
      <el-table-column label="标注文本" min-width="200">
        <template #default="{ row }">
          <span class="text-ellipsis" :title="row.annotationText">
            {{ row.annotationText || '-' }}
          </span>
        </template>
      </el-table-column>
      <el-table-column label="方言原文" min-width="160">
        <template #default="{ row }">
          <span class="text-ellipsis" :title="row.dialectText">
            {{ row.dialectText || '-' }}
          </span>
        </template>
      </el-table-column>
      <el-table-column label="来源" width="100" align="center">
        <template #default="{ row }">
          <el-tag :type="row.source === 'MANUAL' ? '' : 'success'" size="small">
            {{ row.source === 'MANUAL' ? '手动上传' : '问答采集' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="备注" min-width="120">
        <template #default="{ row }">
          <span class="text-ellipsis" :title="row.remark">{{ row.remark || '-' }}</span>
        </template>
      </el-table-column>
      <el-table-column label="上传时间" width="170" align="center">
        <template #default="{ row }">
          {{ formatTime(row.createdAt) }}
        </template>
      </el-table-column>
      <el-table-column label="操作" width="80" align="center" fixed="right">
        <template #default="{ row }">
          <el-button type="danger" link size="small" @click="handleDelete(row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <!-- 分页 -->
    <div class="pagination-wrapper">
      <el-pagination
        v-model:current-page="currentPage"
        v-model:page-size="pageSize"
        :page-sizes="[10, 20, 50]"
        :total="total"
        layout="total, sizes, prev, pager, next"
        @size-change="loadData"
        @current-change="loadData"
      />
    </div>

    <!-- 上传对话框 -->
    <el-dialog
      v-model="uploadVisible"
      title="上传方言语料"
      width="560px"
      :close-on-click-modal="false"
      @closed="resetUploadForm"
    >
      <el-form ref="uploadFormRef" :model="uploadForm" :rules="uploadRules" label-width="100px">
        <el-form-item label="方言类型" prop="dialect">
          <el-select v-model="uploadForm.dialect" placeholder="请选择方言类型" style="width: 100%">
            <el-option label="河北话" value="hebei" />
            <el-option label="山东话" value="shandong" />
            <el-option label="东北话" value="dongbei" />
            <el-option label="河南话" value="henan" />
            <el-option label="四川话" value="sichuan" />
            <el-option label="广东话" value="guangdong" />
          </el-select>
        </el-form-item>
        <el-form-item label="音频文件" prop="audio">
          <el-upload
            ref="uploadRef"
            :auto-upload="false"
            :limit="1"
            :on-change="onAudioChange"
            :on-remove="onAudioRemove"
            :before-upload="() => false"
            accept=".wav,.mp3,.webm,.amr"
            drag
          >
            <el-icon size="36"><UploadFilled /></el-icon>
            <div class="upload-text">拖拽音频文件到此处，或<em>点击选择</em></div>
            <template #tip>
              <div class="upload-tip">支持 wav / mp3 / webm / amr，最大 30MB</div>
            </template>
          </el-upload>
        </el-form-item>
        <el-form-item label="标注文本" prop="annotationText">
          <el-input
            v-model="uploadForm.annotationText"
            type="textarea"
            :rows="3"
            placeholder="标准普通话转写内容"
          />
        </el-form-item>
        <el-form-item label="方言原文">
          <el-input
            v-model="uploadForm.dialectText"
            type="textarea"
            :rows="2"
            placeholder="方言口音下的原始文本（可选）"
          />
        </el-form-item>
        <el-form-item label="来源" prop="source">
          <el-radio-group v-model="uploadForm.source">
            <el-radio value="MANUAL">手动上传</el-radio>
            <el-radio value="QA_COLLECT">问答采集</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="uploadForm.remark" placeholder="备注信息（可选）" maxlength="500" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="uploadVisible = false">取消</el-button>
        <el-button type="primary" :loading="uploading" @click="submitUpload">确认上传</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { Search, Upload, UploadFilled } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getCorpusList, getDialects, uploadCorpus, deleteCorpus } from '@/api/corpus'

// ===== 数据 =====
const loading = ref(false)
const corpusList = ref([])
const total = ref(0)
const currentPage = ref(1)
const pageSize = ref(20)
const filterDialect = ref('')
const keyword = ref('')
const dialects = ref([])

// ===== 上传 =====
const uploadVisible = ref(false)
const uploading = ref(false)
const uploadFormRef = ref(null)
const uploadRef = ref(null)

const uploadForm = reactive({
  dialect: '',
  audio: null,
  annotationText: '',
  dialectText: '',
  source: 'MANUAL',
  remark: ''
})

const uploadRules = {
  dialect: [{ required: true, message: '请选择方言类型', trigger: 'change' }],
  audio: [{ required: true, message: '请选择音频文件', trigger: 'change' }],
  annotationText: [{ required: true, message: '请输入标注文本', trigger: 'blur' }]
}

// ===== 数据加载 =====
async function loadData() {
  loading.value = true
  try {
    const params = { page: currentPage.value - 1, size: pageSize.value }
    if (filterDialect.value) params.dialect = filterDialect.value
    if (keyword.value) params.keyword = keyword.value

    const res = await getCorpusList(params)
    corpusList.value = res.data?.list || []
    total.value = res.data?.total || 0
  } catch { /* handled by interceptor */ }
  finally { loading.value = false }
}

async function loadDialects() {
  try {
    const res = await getDialects()
    dialects.value = res.data || []
  } catch { /* handled */ }
}

// ===== 上传 =====
function openUploadDialog() {
  uploadVisible.value = true
}

function onAudioChange(file) {
  uploadForm.audio = file.raw
}

function onAudioRemove() {
  uploadForm.audio = null
}

function resetUploadForm() {
  uploadForm.dialect = ''
  uploadForm.audio = null
  uploadForm.annotationText = ''
  uploadForm.dialectText = ''
  uploadForm.source = 'MANUAL'
  uploadForm.remark = ''
  uploadRef.value?.clearFiles()
}

async function submitUpload() {
  try {
    await uploadFormRef.value.validate()
  } catch { return }

  if (!uploadForm.audio) {
    ElMessage.warning('请选择音频文件')
    return
  }

  uploading.value = true
  try {
    const fd = new FormData()
    fd.append('audio', uploadForm.audio)
    fd.append('dialect', uploadForm.dialect)
    fd.append('annotationText', uploadForm.annotationText)
    if (uploadForm.dialectText) fd.append('dialectText', uploadForm.dialectText)
    fd.append('source', uploadForm.source)
    if (uploadForm.remark) fd.append('remark', uploadForm.remark)

    await uploadCorpus(fd)
    ElMessage.success('语料上传成功')
    uploadVisible.value = false
    await loadDialects()
    await loadData()
  } catch { /* handled by interceptor */ }
  finally { uploading.value = false }
}

// ===== 删除 =====
async function handleDelete(row) {
  try {
    await ElMessageBox.confirm(
      `确认删除该语料吗？音频文件和标注文本将一并删除，不可恢复。`,
      '删除确认',
      { type: 'warning', confirmButtonText: '确认删除', cancelButtonText: '取消' }
    )
    await deleteCorpus(row.id)
    ElMessage.success('语料已删除')
    await loadData()
  } catch { /* cancelled or error */ }
}

// ===== 工具 =====
function dialectLabel(d) {
  const map = {
    hebei: '河北话', shandong: '山东话', dongbei: '东北话',
    henan: '河南话', sichuan: '四川话', guangdong: '广东话'
  }
  return map[d] || d
}

function dialectTagType(d) {
  const map = { hebei: '', shandong: 'warning', dongbei: 'info', henan: 'success', sichuan: 'danger', guangdong: '' }
  return map[d] || 'info'
}

function formatSize(bytes) {
  if (!bytes) return ''
  if (bytes < 1024) return bytes + ' B'
  if (bytes < 1024 * 1024) return (bytes / 1024).toFixed(1) + ' KB'
  return (bytes / (1024 * 1024)).toFixed(1) + ' MB'
}

function formatTime(dateStr) {
  if (!dateStr) return '-'
  const d = new Date(dateStr)
  const pad = n => String(n).padStart(2, '0')
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())} ${pad(d.getHours())}:${pad(d.getMinutes())}`
}

onMounted(() => {
  loadDialects()
  loadData()
})
</script>

<style scoped>
.corpus-page {
  background: #fff;
  border-radius: 8px;
  padding: 16px;
}

.toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
}

.toolbar-left {
  display: flex;
  align-items: center;
}

.audio-cell {
  display: flex;
  align-items: center;
  gap: 8px;
}

.audio-name {
  font-size: 13px;
  color: #303133;
  max-width: 200px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.audio-size {
  font-size: 12px;
  color: #909399;
  white-space: nowrap;
}

.text-ellipsis {
  display: block;
  max-width: 280px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  font-size: 13px;
}

.pagination-wrapper {
  display: flex;
  justify-content: flex-end;
  margin-top: 16px;
}

.upload-text {
  font-size: 13px;
  color: #606266;
  margin-top: 8px;
}

.upload-text em {
  color: #409EFF;
  font-style: normal;
}

.upload-tip {
  font-size: 12px;
  color: #909399;
  margin-top: 4px;
}
</style>
