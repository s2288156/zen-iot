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
      <el-table-column type="index" width="50"/>
      <el-table-column label="name" prop="name" width="180"/>
      <el-table-column label="transportType" prop="transportType" width="180"/>
      <el-table-column label="status" prop="status"/>
      <el-table-column fixed="right" label="Operations" width="120">
        <template #default="{ row, $index }">
          <el-button link size="small" type="primary" @click="handleClick">Detail</el-button>
          <el-button link size="small" type="primary" @click="handleDeleteDevice(row)">Delete</el-button>
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
      <el-form :model="createDeviceForm" :rules="rules" status-icon>
        <el-form-item label="Name" label-width="140px" prop="name">
          <el-input v-model="createDeviceForm.name" autocomplete="off"/>
        </el-form-item>
        <el-form-item label="Transport Type" label-width="140px" prop="transportType">
          <el-input v-model="createDeviceForm.transportType" autocomplete="off"/>
        </el-form-item>
        <el-form-item label="Status" label-width="140px" prop="status">
          <el-input v-model="createDeviceForm.status" autocomplete="off"/>
        </el-form-item>
      </el-form>
      <template #footer>
        <span class="dialog-footer">
          <el-button @click="resetDeviceForm">Cancel</el-button>
          <el-button type="primary" @click="handleCreateDevice">Confirm</el-button>
        </span>
      </template>
    </el-dialog>
  </div>
</template>

<script lang="ts" setup>
import {reactive, ref} from 'vue'
import type {Device} from '@/api/types'
import type {BaseDataPage, PageQuery} from '@/api/global-types'
import type {FormRules} from 'element-plus'
import {ElMessage} from "element-plus";
import {deleteDevice, getDevices, saveDevice} from "@/api/device-apis";

const pageQuery: PageQuery = {
  page: 0,
  size: 10,
}
const devices = ref<BaseDataPage<Device>>({
  data: [],
  size: 0,
  totalPages: 0,
})

const loadDeviceList = () => {
  getDevices(pageQuery).then((response) => {
    devices.value = response.data
  })
}
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
      if (response.status === 200) {
        loadDeviceList();
      }
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

// dialog
const dialogFormVisible = ref(false)
let createDeviceForm = reactive<Device>({
  name: "", status: "", transportType: ""
})

const resetDeviceForm = () => {
  dialogFormVisible.value = false
  createDeviceForm = reactive<Device>({
    name: "", status: "", transportType: ""
  })
}

const rules = reactive<FormRules>({
  name: [{required: true, message: 'Please input name!', trigger: 'blur'}],
  status: [{required: true, message: 'Please input status!', trigger: 'blur'}],
  transportType: [{required: true, message: 'Please input transportType!', trigger: 'blur'}],
})

const handleCreateDevice = () => {
  saveDevice(createDeviceForm).then((resp) => {
    if (resp.status === 200) {
      resetDeviceForm()
      loadDeviceList()
    }
  })
}
</script>

<style lang="scss" scoped>
</style>
