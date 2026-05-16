<script setup lang="ts">
import { ref } from 'vue'
import { ElMessage } from 'element-plus'
import { useI18n } from 'vue-i18n'
import BpmnDesigner from '../components/BpmnDesigner.vue'
import { deployBpmn } from '../api'

const { t } = useI18n()

const xml = ref('')
const designerRef = ref<InstanceType<typeof BpmnDesigner>>()
const fileInputRef = ref<HTMLInputElement>()
const deployName = ref('')
const fileName = ref('process.bpmn')

async function downloadXml() {
  if (!designerRef.value) return
  const content = await designerRef.value.getXml()
  const blob = new Blob([content], { type: 'application/xml' })
  const url = URL.createObjectURL(blob)
  const a = document.createElement('a')
  a.href = url
  a.download = fileName.value || 'process.bpmn'
  document.body.appendChild(a)
  a.click()
  document.body.removeChild(a)
  URL.revokeObjectURL(url)
}

async function handleDeploy() {
  if (!designerRef.value) return
  const content = await designerRef.value.getXml()
  const blob = new Blob([content], { type: 'application/xml' })
  const file = new File([blob], fileName.value || 'process.bpmn', { type: 'application/xml' })
  await deployBpmn(file, deployName.value || undefined)
  ElMessage.success(t('workflow.designer.deployOk'))
}

async function onPickFile(e: Event) {
  const target = e.target as HTMLInputElement
  const file = target.files?.[0]
  if (!file) return
  const text = await file.text()
  xml.value = text
  fileName.value = file.name
}
</script>

<template>
  <div class="scaffold-page">
    <div class="scaffold-card">
      <el-form
        inline
        label-width="80px"
      >
        <el-form-item :label="t('workflow.designer.fileName')">
          <el-input
            v-model="fileName"
            style="width: 200px"
          />
        </el-form-item>
        <el-form-item :label="t('workflow.designer.deployName')">
          <el-input
            v-model="deployName"
            :placeholder="t('workflow.designer.deployNameHint')"
            style="width: 200px"
          />
        </el-form-item>
        <el-form-item>
          <input
            ref="fileInputRef"
            type="file"
            accept=".bpmn,.xml,.bpmn20.xml"
            style="display: none"
            @change="onPickFile"
          >
          <el-button @click="fileInputRef?.click()">
            {{ t('workflow.designer.importBtn') }}
          </el-button>
          <el-button
            type="primary"
            @click="downloadXml"
          >
            {{ t('workflow.designer.exportXml') }}
          </el-button>
          <el-button
            type="success"
            @click="handleDeploy"
          >
            {{ t('workflow.designer.deployToEngine') }}
          </el-button>
        </el-form-item>
      </el-form>

      <div class="designer-host">
        <BpmnDesigner
          ref="designerRef"
          v-model="xml"
        />
      </div>
    </div>
  </div>
</template>

<style scoped lang="scss">
.designer-host {
  height: calc(100vh - 240px);
  min-height: 480px;
  margin-top: 12px;
}
</style>
