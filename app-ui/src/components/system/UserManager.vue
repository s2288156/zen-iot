<template>
  <div class="app-container">
    <div class="filter-container">
      <el-button type="primary" icon="EditPen" class="filter-item">新增</el-button>
    </div>

    <el-table :data="tableData.data" style="width: 100%">
      <el-table-column prop="createTime" label="CreateDate" width="180"/>
      <el-table-column prop="updateTime" label="UpdateDate" width="180"/>
      <el-table-column prop="username" label="Username" width="180"/>
    </el-table>

<!--    <el-pagination background layout="prev, pager, next" :page-size="tableData.size" :page-count="tableData.totalPages"/>-->
  </div>
</template>

<script lang="ts" setup>
import { getAccounts } from '@/api/account'
import { PageQuery } from '@/utils/datas'
import { ref } from 'vue'

const pageQuery = new PageQuery(0, 10)
// const tableData = reactive(getAccounts(pageQuery))
let tableData = ref()
getAccounts(pageQuery).then(function (response) {
  tableData = response.data
})
console.log(tableData)
</script>

<style scoped lang="scss">

</style>
