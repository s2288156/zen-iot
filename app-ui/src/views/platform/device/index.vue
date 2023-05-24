<template>
  <div class="app-container">
    <div class="filter-container">
      <el-button
        class="filter-item"
        icon="EditPen"
        type="primary"
        @click="dialogFormVisible = !dialogFormVisible"
      >
        新增
      </el-button>
      <el-button
        class="filter-item"
        icon="RefreshRight"
        type="info"
        @click="loadDeviceList()"
        >刷新
      </el-button>
    </div>

    <el-table :data="devices.data" style="width: 100%">
      <el-table-column type="index" width="50" />
      <el-table-column label="name" prop="name" width="180" />
      <el-table-column label="transportType" prop="transportType" width="180" />
      <el-table-column label="status">
        <template #default="{ row }">
          <el-tag :type="getStatusTag(row)">{{ row.status }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column fixed="right" label="Operations" width="120">
        <template #default="{ row, $index }">
          <el-button link size="small" type="primary" @click="handleClick"
            >Detail</el-button
          >
          <el-button
            link
            size="small"
            type="primary"
            @click="handleDeleteDevice(row)"
            >Delete</el-button
          >
        </template>
      </el-table-column>
    </el-table>

    <el-pagination
      v-model:current-page="currentPage"
      :page-count="devices.totalPages"
      :page-size="devices.size"
      background
      layout="prev, pager, next"
      @prev-click="prevClick"
      @next-click="nextClick"
      @current-change="currentChange"
    />

    <el-dialog v-model="dialogFormVisible" title="Add Device">
      <el-form
        ref="ruleFormRef"
        :model="createDeviceForm"
        :rules="rules"
        status-icon
      >
        <el-form-item label="Name" label-width="140px" prop="name">
          <el-input v-model="createDeviceForm.name" />
        </el-form-item>
        <el-form-item
          label="Transport Type"
          label-width="140px"
          prop="transportType"
        >
          <el-select
            v-model="createDeviceForm.transportType"
            clearable
            placeholder="Transport Type"
          >
            <el-option
              v-for="item in deviceCommonData.transportTypes"
              :key="item"
              :label="item"
              :value="item"
            />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <span class="dialog-footer">
          <el-button @click="resetDeviceForm(ruleFormRef)">Cancel</el-button>
          <el-button type="primary" @click="handleCreateDevice(ruleFormRef)"
            >Confirm</el-button
          >
        </span>
      </template>
    </el-dialog>
  </div>
</template>

<script lang="ts" setup>
import { reactive, ref } from 'vue'
import type { Device, DeviceCommon } from '@/api/data/types'
import type { BaseDataPage, PageQuery, RestResponse } from '@/api/global-types'
import type { FormInstance, FormRules } from 'element-plus'
import { ElMessage } from 'element-plus'
import {
  deleteDevice,
  getDeviceCommon,
  getDevices,
  saveDevice,
} from '@/api/device-apis'

const pageQuery: PageQuery = {
  page: 0,
  size: 10,
}
const devices = ref<BaseDataPage<Device> | any>({
  data: [],
  size: 0,
  totalPages: 0,
})

const deviceCommonData = ref<DeviceCommon>({ statuses: [], transportTypes: [] })

const loadDeviceList = () => {
  getDevices(pageQuery).then((response) => {
    devices.value = response
  })
}
const loadDeviceCommon = () => {
  getDeviceCommon().then((resp) => {
    deviceCommonData.value = resp.data
  })
}

loadDeviceCommon()
loadDeviceList()

const handleClick = () => {
  ElMessage({
    showClose: true,
    message: 'Detail Click',
    type: 'info',
  })
}

const handleDeleteDevice = (row: Device) => {
  if (row.id != null) {
    deleteDevice(row.id).then((response) => {
      loadDeviceList()
    })
  }
}

const currentPage = ref(1)
const prevClick = () => {
  pageQuery.page -= 1
  loadDeviceList()
}
const nextClick = () => {
  pageQuery.page += 1
  loadDeviceList()
}
const currentChange = () => {
  pageQuery.page = currentPage.value - 1
  loadDeviceList()
}

const getStatusTag = (row: Device) => {
  if (row.status === 'DISABLE') {
    return 'info'
  } else if (row.status === 'ENABLE') {
    return 'success'
  }
}

// dialog
const dialogFormVisible = ref(false)
const ruleFormRef = ref<FormInstance>()

let createDeviceForm = reactive<Device>({
  name: '',
  transportType: '',
})

const resetDeviceForm = (formEl: FormInstance) => {
  formEl.resetFields()
  dialogFormVisible.value = false
  createDeviceForm.name = ''
  createDeviceForm.transportType = ''
}

const rules = reactive<FormRules>({
  name: [{ required: true, message: 'Please input name!', trigger: 'blur' }],
  transportType: [
    { required: true, message: 'Please input transportType!', trigger: 'blur' },
  ],
})

const handleCreateDevice = async (formEl: FormInstance) => {
  if (!formEl) return
  await formEl.validate((valid, fields) => {
    if (valid) {
      saveDevice(createDeviceForm).then((resp: any) => {
        if (resp.success) {
          resetDeviceForm(formEl)
          loadDeviceList()
        }
      })
    }
  })
}
</script>

<style lang="scss" scoped></style>
