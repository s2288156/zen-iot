<template>
  <div class="app-container">
    <div class="filter-container">
      <el-button type="primary" icon="EditPen" class="filter-item" @click="dialogFormVisible = !dialogFormVisible">
        新增
      </el-button>
      <el-button type="info" icon="RefreshRight" class="filter-item" @click="getAccountList()">刷新</el-button>
    </div>

    <el-table :data="accountDataPage.data" style="width: 100%">
      <el-table-column prop="createTime" label="CreateDate" width="180"/>
      <el-table-column prop="updateTime" label="UpdateDate" width="180"/>
      <el-table-column prop="username" label="Username" width="180"/>
    </el-table>

    <el-pagination background layout="prev, pager, next" :page-size="accountDataPage.size"
                   :page-count="accountDataPage.totalPages"/>

    <el-dialog v-model="dialogFormVisible" title="Shipping address">
      <el-form :model="createAccountForm">
        <el-form-item label="Username" :label-width="formLabelWidth">
          <el-input v-model="createAccountForm.username" autocomplete="off"/>
        </el-form-item>
      </el-form>
      <template #footer>
      <span class="dialog-footer">
        <el-button @click="dialogFormVisible = false">Cancel</el-button>
        <el-button type="primary" @click="dialogFormVisible = false">Confirm</el-button>
      </span>
      </template>
    </el-dialog>
  </div>
</template>

<script lang="ts" setup>
import { getAccounts } from '@/api/system/account'
import { PageQuery } from '@/utils/datas'
import { ref, reactive } from 'vue'
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

// dialog
const dialogFormVisible = ref(false)
const formLabelWidth = '140px'

const createAccountForm = reactive<AccountData>({ username: '' })

</script>

<style scoped lang="scss">
.my-header {
  display: flex;
  flex-direction: row;
  justify-content: space-between;
}
</style>
