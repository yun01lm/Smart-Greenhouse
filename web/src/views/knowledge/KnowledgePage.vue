<template>
  <div class="knowledge-page">
    <el-tabs v-model="activeTab" type="border-card">
      <!-- Tab 1: 文档管理 -->
      <el-tab-pane label="文档管理" name="documents">
        <div class="tab-content">
          <!-- 操作栏 -->
          <div class="toolbar">
            <div class="toolbar-left">
              <el-select
                v-model="filterCategory"
                placeholder="全部分类"
                clearable
                style="width: 160px"
                @change="loadDocuments"
              >
                <el-option
                  v-for="cat in categories"
                  :key="cat"
                  :label="cat"
                  :value="cat"
                />
              </el-select>
              <el-input
                v-model="keyword"
                placeholder="搜索文档名称"
                clearable
                style="width: 220px; margin-left: 12px"
                @clear="loadDocuments"
                @keyup.enter="loadDocuments"
              >
                <template #prefix>
                  <el-icon><Search /></el-icon>
                </template>
              </el-input>
              <el-button type="primary" style="margin-left: 12px" @click="loadDocuments">
                搜索
              </el-button>
            </div>
            <div v-if="!readOnly" class="toolbar-right">
              <el-button @click="openCategoryDialog">
                <el-icon><Collection /></el-icon>
                分类管理
              </el-button>
              <el-button type="primary" @click="showUploadDialog = true">
                <el-icon><Upload /></el-icon>
                上传文档
              </el-button>
            </div>
          </div>

          <!-- 文档表格 -->
          <el-table
            :data="documents"
            v-loading="loading"
            stripe
            style="width: 100%; margin-top: 16px"
            empty-text="暂无知识库文档"
          >
            <el-table-column prop="docNo" label="编号" width="110">
              <template #default="{ row }">
                <span class="doc-no">{{ row.docNo || ('DOC-' + String(row.id).padStart(4, '0')) }}</span>
              </template>
            </el-table-column>
            <el-table-column prop="title" label="文档标题" min-width="200">
              <template #default="{ row }">
                <span class="doc-title">{{ row.title }}</span>
              </template>
            </el-table-column>
            <el-table-column prop="category" label="分类" width="130">
              <template #default="{ row }">
                <el-tag v-if="row.category" type="info" size="small">
                  {{ row.category }}
                </el-tag>
                <span v-else style="color: #999">未分类</span>
              </template>
            </el-table-column>

            <el-table-column prop="description" label="简介" min-width="180" show-overflow-tooltip>
              <template #default="{ row }">
                <span v-if="row.description" class="doc-desc">{{ row.description }}</span>
                <span v-else style="color: #999">-</span>
              </template>
            </el-table-column>
            <el-table-column prop="fileType" label="格式" width="70">
              <template #default="{ row }">
                <el-tag :type="fileTypeColor(row.fileType)" size="small">
                  {{ row.fileType?.toUpperCase() }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="fileSizeFormatted" label="大小" width="90" />
            <el-table-column label="向量化" width="110" align="center">
              <template #default="{ row }">
                <el-tag v-if="row.vectorIndexed" type="success" size="small">
                  已完成 {{ row.chunkCount }}块
                </el-tag>
                <el-tag v-else type="warning" size="small">
                  待处理
                  <el-button
                    v-if="!readOnly"
                    type="warning"
                    size="small"
                    link
                    style="margin-left: 4px"
                    @click="triggerIndex(row.id)"
                    :loading="indexingId === row.id"
                  >
                    重试
                  </el-button>
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="createdAt" label="上传时间" width="170">
              <template #default="{ row }">
                {{ formatTime(row.createdAt) }}
              </template>
            </el-table-column>
            <el-table-column label="操作" width="150" fixed="right">
              <template #default="{ row }">
                <template v-if="readOnly">
                  <el-button type="primary" size="small" link @click="openPreview(row)">查看</el-button>
                </template>
                <template v-else>
                <el-button type="primary" size="small" link @click="openEditDialog(row)">编辑</el-button>
                <el-popconfirm
                  title="删除后向量数据也将被清理，确认删除？"
                  @confirm="handleDelete(row.id)"
                >
                  <template #reference>
                    <el-button type="danger" size="small" link>删除</el-button>
                  </template>
                </el-popconfirm>
                </template>
              </template>
            </el-table-column>
          </el-table>

          <!-- 分页 -->
          <div class="pagination-wrap" v-if="total > 0">
            <el-pagination
              v-model:current-page="currentPage"
              v-model:page-size="pageSize"
              :total="total"
              :page-sizes="[10, 20, 50]"
              layout="total, sizes, prev, pager, next"
              @size-change="loadDocuments"
              @current-change="loadDocuments"
            />
          </div>
        </div>
      </el-tab-pane>

      <!-- Tab 2: 问答测试（仅管理员） -->
      <el-tab-pane v-if="!readOnly" label="问答测试" name="test">
        <div class="tab-content test-tab">
          <div class="test-input-area">
            <el-input
              v-model="testQuestion"
              type="textarea"
              :rows="3"
              placeholder="输入农业技术问题，测试知识库 RAG 问答效果。例如：番茄晚疫病怎么防治？"
              maxlength="500"
              show-word-limit
            />
            <el-button
              type="primary"
              style="margin-top: 12px"
              @click="runTest"
              :loading="testing"
            >
              <el-icon><ChatDotRound /></el-icon>
              测试问答
            </el-button>
            <span v-if="testResponseTime" class="response-time">
              响应时间: {{ testResponseTime }}ms
            </span>
          </div>

          <!-- 回答结果 -->
          <div v-if="testAnswer" class="test-result">
            <el-card shadow="never">
              <template #header>
                <span class="result-header">🤖 AI 回答</span>
              </template>
              <div class="answer-text">{{ testAnswer }}</div>
            </el-card>
          </div>

          <!-- 检索片段 -->
          <div v-if="testChunks.length > 0" class="retrieved-chunks">
            <h4>📚 检索到的知识库片段</h4>
            <el-card
              v-for="(chunk, index) in testChunks"
              :key="index"
              shadow="never"
              class="chunk-card"
            >
              <div class="chunk-header">
                <el-tag size="small" type="info">{{ chunk.documentTitle || '知识库文档' }}</el-tag>
                <span class="chunk-score" v-if="chunk.score">
                  相似度: {{ (chunk.score * 100).toFixed(1) }}%
                </span>
              </div>
              <div class="chunk-content">{{ chunk.content }}</div>
            </el-card>
          </div>

          <!-- 空状态 -->
          <el-empty
            v-if="!testAnswer && !testing"
            description="输入问题，测试知识库问答效果"
          />
        </div>
      </el-tab-pane>
    </el-tabs>
    <!-- 文档内容预览（R27 只读） -->
    <el-dialog
      v-model="previewVisible"
      :title="previewDoc ? previewDoc.title : '文档预览'"
      width="720px"
      top="6vh"
    >
      <div v-if="previewDoc" class="preview-meta">
        <el-tag size="small" type="info">{{ previewDoc.docNo || ('DOC-' + String(previewDoc.id).padStart(4, '0')) }}</el-tag>
        <el-tag v-if="previewDoc.category" size="small">{{ previewDoc.category }}</el-tag>
        <span class="preview-desc">{{ previewDoc.description || '' }}</span>
      </div>
      <div v-loading="previewLoading" class="preview-body">
        <pre class="preview-text">{{ previewText }}</pre>
      </div>
    </el-dialog>

    <!-- 上传对话框 -->
    <el-dialog
      v-model="showUploadDialog"
      title="上传知识库文档"
      width="500px"
      :close-on-click-modal="false"
    >
      <el-form :model="uploadForm" label-width="80px">
        <el-form-item label="文档标题">
          <el-input v-model="uploadForm.title" placeholder="可选，默认使用文件名" />
        </el-form-item>
        <el-form-item label="分类">
          <el-select
            v-model="uploadForm.category"
            placeholder="选择或输入分类"
            clearable
            filterable
            allow-create
            style="width: 100%"
          >
            <el-option
              v-for="cat in categories"
              :key="cat"
              :label="cat"
              :value="cat"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="文件">
          <el-upload
            ref="uploadRef"
            :auto-upload="false"
            :limit="1"
            :on-change="handleFileChange"
            :on-remove="handleFileRemove"
            accept=".md,.txt"
            drag
          >
            <el-icon class="el-icon--upload"><UploadFilled /></el-icon>
            <div class="el-upload__text">
              拖拽文件到此处，或 <em>点击选择</em>
            </div>
            <template #tip>
              <div class="el-upload__tip">
                支持 .md / .txt 格式，单文件最大 20MB。
                PDF/DOCX 将在后续版本中支持。
              </div>
            </template>
          </el-upload>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showUploadDialog = false">取消</el-button>
        <el-button
          type="primary"
          @click="handleUpload"
          :loading="uploading"
          :disabled="!uploadFile"
        >
          {{ uploading ? '上传中...' : '上传并向量化' }}
        </el-button>
      </template>
    </el-dialog>

    <!-- 分类管理对话框 -->
    <el-dialog
      v-model="showCategoryDialog"
      title="分类管理"
      width="680px"
      :close-on-click-modal="false"
    >
      <div class="cat-toolbar">
        <el-input
          v-model="catForm.name"
          placeholder="分类名称（唯一）"
          maxlength="100"
          style="width: 200px"
        />
        <el-input
          v-model="catForm.description"
          placeholder="分类说明（可选）"
          maxlength="255"
          style="width: 200px; margin-left: 8px"
        />
        <el-button
          type="primary"
          style="margin-left: 8px"
          :loading="catSaving"
          @click="handleCategorySave"
        >
          {{ catEditingId ? '保存修改' : '新增分类' }}
        </el-button>
        <el-button v-if="catEditingId" @click="resetCategoryForm">取消编辑</el-button>
      </div>
      <el-table
        :data="managedCategories"
        v-loading="catLoading"
        stripe
        style="width: 100%; margin-top: 12px"
      >
        <el-table-column prop="id" label="ID" width="60" />
        <el-table-column prop="name" label="分类名称" min-width="140" />
        <el-table-column prop="description" label="说明" min-width="180" show-overflow-tooltip>
          <template #default="{ row }">
            <span v-if="row.description">{{ row.description }}</span>
            <span v-else style="color: #999">-</span>
          </template>
        </el-table-column>
        <el-table-column prop="docCount" label="文档数" width="80" align="center" />
        <el-table-column label="操作" width="140" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" size="small" link @click="startEditCategory(row)">编辑</el-button>
            <el-popconfirm
              title="删除后不可恢复，确认删除该分类？"
              @confirm="handleCategoryDelete(row)"
            >
              <template #reference>
                <el-button type="danger" size="small" link :disabled="row.docCount > 0">删除</el-button>
              </template>
            </el-popconfirm>
          </template>
        </el-table-column>
      </el-table>
      <div class="cat-tip">分类已被文档使用时不可删除；重命名会自动更新文档与问答引用来源。</div>
      <template #footer>
        <el-button @click="showCategoryDialog = false">关闭</el-button>
      </template>
    </el-dialog>

    <!-- 编辑对话框 -->
    <el-dialog
      v-model="showEditDialog"
      title="编辑文档信息"
      width="540px"
      :close-on-click-modal="false"
    >
      <el-form :model="editForm" label-width="80px">
        <el-form-item label="系统ID">
          <el-input :model-value="editForm.id" disabled />
        </el-form-item>
        <el-form-item label="文档编号">
          <el-input
            v-model="editForm.docNo"
            placeholder="如 DOC-0001 或自定义编号（唯一）"
            maxlength="64"
          />
        </el-form-item>
        <el-form-item label="文档标题">
          <el-input v-model="editForm.title" placeholder="文档标题" maxlength="200" />
        </el-form-item>
        <el-form-item label="分类">
          <el-select
            v-model="editForm.category"
            placeholder="选择或输入分类"
            clearable
            filterable
            allow-create
            style="width: 100%"
          >
            <el-option
              v-for="cat in categories"
              :key="cat"
              :label="cat"
              :value="cat"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="简介">
          <el-input
            v-model="editForm.description"
            type="textarea"
            :rows="4"
            maxlength="2000"
            show-word-limit
            placeholder="文档内容简介/摘要（可选）"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showEditDialog = false">取消</el-button>
        <el-button type="primary" :loading="editing" @click="handleEditSave">
          保存
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useAuthStore } from '@/stores/auth'
import { ElMessage } from 'element-plus'
import { Search, Upload, UploadFilled, ChatDotRound, Collection } from '@element-plus/icons-vue'
import {
  getDocuments,
  getCategories,
  uploadDocument,
  indexDocument,
  deleteDocument,
  updateDocument,
  testQa,
  getDocumentContent,
  getManagedCategories,
  createCategory,
  updateCategory,
  deleteCategory
} from '@/api/knowledge'

// ===== 文档管理 =====

const activeTab = ref('documents')
const loading = ref(false)
const documents = ref([])
const categories = ref([])
const total = ref(0)
const currentPage = ref(1)
const pageSize = ref(10)
const filterCategory = ref('')
const keyword = ref('')
const authStore = useAuthStore()
// R27：非管理员为只读模式（查看文档内容），写操作按钮隐藏
const readOnly = computed(() => !authStore.isAdmin())

// ===== 文档内容预览（只读） =====
const previewVisible = ref(false)
const previewLoading = ref(false)
const previewDoc = ref(null)
const previewText = ref('')

async function openPreview(row) {
  previewDoc.value = row
  previewVisible.value = true
  previewLoading.value = true
  previewText.value = ''
  try {
    const blob = await getDocumentContent(row.id)
    previewText.value = await blob.text()
  } catch (e) {
    previewText.value = '文档内容读取失败或格式暂不支持预览'
  } finally {
    previewLoading.value = false
  }
}

async function loadDocuments() {
  loading.value = true
  try {
    const params = {
      page: currentPage.value,
      size: pageSize.value
    }
    if (filterCategory.value) params.category = filterCategory.value
    if (keyword.value) params.keyword = keyword.value

    const res = await getDocuments(params)
    documents.value = res.data?.list || []
    total.value = res.data?.total || 0
  } catch (e) {
    // 错误由拦截器统一处理
  } finally {
    loading.value = false
  }
}

async function loadCategories() {
  try {
    const res = await getCategories()
    categories.value = res.data || []
  } catch (e) {
    // 忽略
  }
}

// ===== 上传 =====

const showUploadDialog = ref(false)
const uploading = ref(false)
const uploadFile = ref(null)
const uploadForm = ref({ title: '', category: '' })

function handleFileChange(file) {
  uploadFile.value = file.raw
}
function handleFileRemove() {
  uploadFile.value = null
}

async function handleUpload() {
  if (!uploadFile.value) {
    ElMessage.warning('请选择文件')
    return
  }

  uploading.value = true
  try {
    const formData = new FormData()
    formData.append('file', uploadFile.value)
    if (uploadForm.value.title) formData.append('title', uploadForm.value.title)
    if (uploadForm.value.category) formData.append('category', uploadForm.value.category)

    await uploadDocument(formData)
    ElMessage.success('文档上传成功，向量化处理中')
    showUploadDialog.value = false
    uploadFile.value = null
    uploadForm.value = { title: '', category: '' }
    loadDocuments()
    loadCategories()
  } catch (e) {
    // 拦截器已处理
  } finally {
    uploading.value = false
  }
}

// ===== 索引 =====

const indexingId = ref(null)

async function triggerIndex(docId) {
  indexingId.value = docId
  try {
    const res = await indexDocument(docId)
    ElMessage.success(res.message || '向量化完成')
    loadDocuments()
  } catch (e) {
    // 拦截器已处理
  } finally {
    indexingId.value = null
  }
}

// ===== 删除 =====

async function handleDelete(id) {
  try {
    await deleteDocument(id)
    ElMessage.success('文档已删除')
    loadDocuments()
    loadCategories()
  } catch (e) {
    // 拦截器已处理
  }
}

// ===== 分类管理 =====

const showCategoryDialog = ref(false)
const catLoading = ref(false)
const catSaving = ref(false)
const managedCategories = ref([])
const catForm = ref({ name: '', description: '' })
const catEditingId = ref(null)

function openCategoryDialog() {
  showCategoryDialog.value = true
  loadManagedCategories()
}

async function loadManagedCategories() {
  catLoading.value = true
  try {
    const res = await getManagedCategories()
    managedCategories.value = res.data || []
  } catch (e) {
    // 拦截器已处理
  } finally {
    catLoading.value = false
  }
}

function resetCategoryForm() {
  catForm.value = { name: '', description: '' }
  catEditingId.value = null
}

async function handleCategorySave() {
  if (!catForm.value.name.trim()) {
    ElMessage.warning('请输入分类名称')
    return
  }
  catSaving.value = true
  try {
    if (catEditingId.value) {
      await updateCategory(catEditingId.value, {
        name: catForm.value.name.trim(),
        description: catForm.value.description || ''
      })
      ElMessage.success('分类已更新')
    } else {
      await createCategory({
        name: catForm.value.name.trim(),
        description: catForm.value.description || ''
      })
      ElMessage.success('分类已创建')
    }
    resetCategoryForm()
    loadManagedCategories()
    loadCategories()
    loadDocuments()
  } catch (e) {
    // 拦截器已处理
  } finally {
    catSaving.value = false
  }
}

function startEditCategory(row) {
  catEditingId.value = row.id
  catForm.value = { name: row.name, description: row.description || '' }
}

async function handleCategoryDelete(row) {
  try {
    await deleteCategory(row.id)
    ElMessage.success('分类已删除')
    loadManagedCategories()
    loadCategories()
  } catch (e) {
    // 拦截器已处理
  }
}

// ===== 编辑 =====

const showEditDialog = ref(false)
const editing = ref(false)
const editForm = ref({ id: null, docNo: '', title: '', category: '', description: '' })

function openEditDialog(row) {
  editForm.value = {
    id: row.id,
    docNo: row.docNo || '',
    title: row.title || '',
    category: row.category || '',
    description: row.description || ''
  }
  showEditDialog.value = true
}

async function handleEditSave() {
  if (!editForm.value.docNo.trim()) {
    ElMessage.warning('请填写文档编号')
    return
  }
  if (!editForm.value.title.trim()) {
    ElMessage.warning('请填写文档标题')
    return
  }
  editing.value = true
  try {
    await updateDocument(editForm.value.id, {
      docNo: editForm.value.docNo.trim(),
      title: editForm.value.title.trim(),
      category: editForm.value.category || '',
      description: editForm.value.description || ''
    })
    ElMessage.success('文档信息已更新')
    showEditDialog.value = false
    loadDocuments()
    loadCategories()
  } catch (e) {
    // 拦截器已处理
  } finally {
    editing.value = false
  }
}

// ===== 问答测试 =====

const testing = ref(false)
const testQuestion = ref('')
const testAnswer = ref('')
const testChunks = ref([])
const testResponseTime = ref(null)

async function runTest() {
  if (!testQuestion.value.trim()) {
    ElMessage.warning('请输入测试问题')
    return
  }

  testing.value = true
  testAnswer.value = ''
  testChunks.value = []
  testResponseTime.value = null

  try {
    const res = await testQa({
      question: testQuestion.value,
      topK: 5
    })
    testAnswer.value = res.data?.answer || ''
    testChunks.value = res.data?.retrievedChunks || []
    testResponseTime.value = res.data?.responseTime || null
  } catch (e) {
    // 拦截器已处理
  } finally {
    testing.value = false
  }
}

// ===== 工具方法 =====

function fileTypeColor(type) {
  const map = { md: '', txt: 'success', pdf: 'danger', docx: 'warning' }
  return map[type] || 'info'
}

function formatTime(time) {
  if (!time) return '-'
  return new Date(time).toLocaleString('zh-CN')
}

// ===== 初始化 =====

onMounted(() => {
  loadDocuments()
  loadCategories()
})
</script>

<style scoped>
.knowledge-page {
  height: 100%;
}

.tab-content {
  padding: 4px 0;
}

.toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.toolbar-left {
  display: flex;
  align-items: center;
}

.doc-title {
  font-weight: 500;
  color: #303133;
}

.pagination-wrap {
  display: flex;
  justify-content: flex-end;
  margin-top: 16px;
}

.doc-no {
  font-weight: 600;
  color: #409eff;
}

.doc-desc {
  color: #606266;
}

.cat-toolbar {
  display: flex;
  align-items: center;
}

.cat-tip {
  margin-top: 10px;
  color: #909399;
  font-size: 12px;
}

/* 问答测试 */
.test-tab {
  max-width: 900px;
}

.test-input-area {
  margin-bottom: 24px;
}

.response-time {
  margin-left: 16px;
  color: #909399;
  font-size: 13px;
}

.test-result {
  margin-bottom: 24px;
}

.result-header {
  font-weight: 600;
  font-size: 15px;
}

.answer-text {
  white-space: pre-wrap;
  line-height: 1.8;
  color: #303133;
  font-size: 14px;
}

.retrieved-chunks h4 {
  margin: 0 0 12px 0;
  font-size: 15px;
  color: #303133;
}

.chunk-card {
  margin-bottom: 12px;
}

.chunk-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 8px;
}

.chunk-score {
  color: #909399;
  font-size: 12px;
}

.chunk-content {
  color: #606266;
  font-size: 13px;
  line-height: 1.7;
  white-space: pre-wrap;
}
.preview-meta {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 12px;
}
.preview-desc {
  font-size: 13px;
  color: #909399;
  margin-left: 4px;
}
.preview-body {
  max-height: 60vh;
  overflow: auto;
  background: #f7f9fc;
  border-radius: 8px;
  padding: 16px;
}
.preview-text {
  margin: 0;
  font-size: 13px;
  line-height: 1.8;
  color: #303133;
  white-space: pre-wrap;
  word-break: break-word;
  font-family: 'Segoe UI', 'PingFang SC', 'Microsoft YaHei', sans-serif;
}
</style>
