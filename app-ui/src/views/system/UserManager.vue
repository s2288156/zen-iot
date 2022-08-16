<template>
  <div class="app-container">
    <div class="filter-container">
      <el-button type="primary" icon="EditPen" class="filter-item">新增</el-button>
      <el-button type="info" icon="RefreshRight" class="filter-item" @click="getAccountList()">刷新</el-button>
    </div>

    <el-table :data="accountDataPage.data" style="width: 100%">
      <el-table-column prop="createTime" label="CreateDate" width="180"/>
      <el-table-column prop="updateTime" label="UpdateDate" width="180"/>
      <el-table-column prop="username" label="Username" width="180"/>
    </el-table>

    <!--    <el-pagination background layout="prev, pager, next" :page-size="tableData.size" :page-count="tableData.totalPages"/>-->
  </div>
</template>

<script lang="ts" setup>
import { getAccounts } from '@/api/system/account'
import { PageQuery } from '@/utils/datas'
import { ref } from 'vue'
import { AccountData } from '@/api/system/types'
import { BaseDataPage } from '@/api/global-types'

const pageQuery = new PageQuery(0, 10)

const accountDataPage = ref<BaseDataPage<AccountData>>({
  data: [],
  size: 0,
  totalPages: 0
})

const getAccountList = () => {
  getAccounts(pageQuery).then(response => {
    accountDataPage.value = response.data
  })
}
getAccountList()
</script>

<style scoped lang="scss">

</style>
