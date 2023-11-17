<script setup lang="ts">
import {ref} from "vue";
import {PageQuery} from "@/api/global-types";
import {RuleChain} from "@/api/data/types";

const dialogFormVisible = ref(false)
const pageQuery: PageQuery = {
  page: 0,
  size: 10,
}
const ruleChains = ref<RuleChain>()
const loadList = () => {
  getDevices(pageQuery).then((response) => {
    devices.value = response
  })
}
</script>

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
          @click="loadList()"
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

<style scoped lang="scss">

</style>
