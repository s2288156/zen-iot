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
          @click="loadSimulatorList()"
      >刷新
      </el-button>
    </div>

    <el-table :data="simulators.data" style="width: 100%">
      <el-table-column type="index" width="50"/>
      <el-table-column label="name" prop="name" width="180"/>
      <el-table-column label="transportType" prop="transportType" width="180"/>
      <el-table-column label="status">
        <template #default="{ row }">
          <el-tag :type="getStatusTag(row)">{{ row.status }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column fixed="right" label="Operations" width="120">
        <template #default="{ row, $index }">
          <el-button link size="small" type="primary" @click="handleClick">Detail</el-button>
          <el-button link size="small" type="primary" @click="handleDeleteSimulator(row)">Delete</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-pagination
        v-model:current-page="currentPage"
        :page-count="simulators.totalPages"
        :page-size="simulators.size"
        background
        layout="prev, pager, next"
        @prev-click="prevClick"
        @next-click="nextClick"
        @current-change="currentChange"
    />

    <el-dialog v-model="dialogFormVisible" title="Add Device">
      <el-form ref="ruleFormRef" :model="createSimulatorForm" :rules="rules" status-icon>
        <el-form-item label="Name" label-width="140px" prop="name">
          <el-input v-model="createSimulatorForm.name"/>
        </el-form-item>
        <el-form-item label="Transport Type" label-width="140px" prop="transportType">
          <el-select v-model="createSimulatorForm.transportType" clearable placeholder="Transport Type">
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
          <el-button type="primary" @click="handleCreateSimulator(ruleFormRef)">Confirm</el-button>
        </span>
      </template>
    </el-dialog>
  </div>
</template>

<script lang="ts" setup>
import {reactive, ref} from 'vue'
import type {BaseDataPage, PageQuery} from '@/api/global-types'
import type {FormInstance, FormRules} from 'element-plus'
import {ElMessage} from "element-plus";
import type {Simulator} from "@/api/types";
import {deleteSimulator, getSimulators, saveSimulator} from "@/api/simulator-apis";

const pageQuery: PageQuery = {
  page: 0,
  size: 10,
}
const simulators = ref<BaseDataPage<Simulator>>({
  data: [],
  size: 0,
  totalPages: 0,
})


const loadSimulatorList = () => {
  getSimulators(pageQuery).then((response) => {
    simulators.value = response.data
  })
}
loadSimulatorList()

const handleClick = () => {
  ElMessage({
    showClose: true,
    message: 'Detail Click',
    type: 'info',
  })
}

const handleDeleteSimulator = (row: Simulator) => {
  if (row.id != null) {
    deleteSimulator(row.id).then((response) => {
      if (response.status === 200) {
        loadSimulatorList();
      }
    })
  }
}

const currentPage = ref(1)
const prevClick = () => {
  pageQuery.page -= 1
  loadSimulatorList()
}
const nextClick = () => {
  pageQuery.page += 1
  loadSimulatorList()
}
const currentChange = () => {
  pageQuery.page = currentPage.value - 1
  loadSimulatorList()
}

const getStatusTag = (row: Simulator) => {
  if (row.status === 'DISABLE') {
    return 'info'
  } else if (row.status === 'ENABLE') {
    return 'success'
  }
}

// dialog
const dialogFormVisible = ref(false)
const ruleFormRef = ref<FormInstance>()

let createSimulatorForm = reactive<Simulator>({
  name: "", transportType: ""
})

const resetDeviceForm = (formEl: FormInstance) => {
  formEl.resetFields()
  dialogFormVisible.value = false
  createSimulatorForm.name = ''
  createSimulatorForm.transportType = ''
}

const rules = reactive<FormRules>({
  name: [{required: true, message: 'Please input name!', trigger: 'blur'}],
  transportType: [{required: true, message: 'Please input transportType!', trigger: 'blur'}],
})

const handleCreateSimulator = async (formEl: FormInstance) => {
  if (!formEl) return
  await formEl.validate((valid, fields) => {
    if (valid) {
      saveSimulator(createSimulatorForm).then((resp) => {
        if (resp.status === 200) {
          resetDeviceForm(formEl)
          loadSimulatorList()
        }
      })
    }
  })

}
</script>

<style lang="scss" scoped>
</style>
