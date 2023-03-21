<template>
  <div class="app-container">
    <div class="filter-container">
      <el-button
          class="filter-item"
          icon="EditPen"
          type="primary"
          @click="openModal()"
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
      <el-table-column fixed="right" label="Operations" width="240">
        <template #default="{ row, $index }">
          <el-button size="small" type="primary" @click="handleSimulatorPowerSwitch(row)">
            {{ row.status === 'DISABLE' ? 'Enable' : 'Disable' }}
          </el-button>
          <el-button size="small" type="info" @click="handleDetail">Detail</el-button>
          <el-button size="small" type="danger" @click="handleDeleteSimulator(row)">Delete</el-button>
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
    <AddSimulator ref="addSimulator"/>
  </div>
</template>

<script lang="ts" setup>
import {ref} from 'vue'
import type {BaseDataPage, PageQuery} from '@/api/global-types'
import {ElMessage} from "element-plus";
import type {Simulator} from "@/api/data/types";
import {deleteSimulator, getSimulators, switchSimulatorStatus} from "@/api/simulator-apis";
import AddSimulator from "./AddSimulator.vue";


const pageQuery: PageQuery = {page: 0, size: 10}
const simulators = ref<BaseDataPage<Simulator>>({
  data: [],
  size: 0,
  totalPages: 0,
})
const addSimulator = ref<InstanceType<typeof AddSimulator> | null>(null)
const openModal = () => {
  addSimulator.value.open()
}

const loadSimulatorList = () => {
  getSimulators(pageQuery).then((response) => {
    simulators.value = response.data
  })
}

loadSimulatorList()

const handleDetail = () => {
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

const handleSimulatorPowerSwitch = (row: Simulator) => {
  switchSimulatorStatus(row.id).then((resp) => {
    if (resp.status === 200) {
      row.status = resp.data.data.status
      ElMessage({
        showClose: true,
        message: resp.data.data.status + ' Success!',
        type: 'success',
      })
    }
  })

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

</script>

<style lang="scss" scoped>
</style>
