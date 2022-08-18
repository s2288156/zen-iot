<template>
  <div class="app-container">
    <div class="filter-container">
      <el-button type="primary" icon="EditPen" class="filter-item" @click="dialogFormVisible = !dialogFormVisible">
        新增
      </el-button>
      <el-button type="info" icon="RefreshRight" class="filter-item" @click="getAccountList()">刷新</el-button>
    </div>

    <el-table :data="accountDataPage.data" style="width: 100%" height="445px">
      <el-table-column type="index" width="50" />
      <el-table-column prop="username" label="Username" width="180"/>
      <el-table-column prop="createTime" label="CreateDate" width="180"/>
      <el-table-column prop="updateTime" label="UpdateDate" width="180"/>
    </el-table>

    <el-pagination background layout="prev, pager, next"
                   @prev-click="prevClick"
                   @next-click="nextClick"
                   @current-change="currentChange"
                   v-model:current-page="currentPage"
                   :page-size="accountDataPage.size"
                   :page-count="accountDataPage.totalPages"/>

    <el-dialog v-model="dialogFormVisible" title="Add Account">
      <el-form :model="createAccountForm" :rules="rules" status-icon>
        <el-form-item label="Username" :label-width="formLabelWidth" prop="username">
          <el-input v-model="createAccountForm.username" autocomplete="off"/>
        </el-form-item>
        <el-form-item label="Password" :label-width="formLabelWidth" prop="password">
          <el-input type="password" v-model="createAccountForm.password" autocomplete="off"/>
        </el-form-item>
      </el-form>
      <template #footer>
      <span class="dialog-footer">
        <el-button @click="resetAccountForm">Cancel</el-button>
        <el-button type="primary" @click="createAccount">Confirm</el-button>
      </span>
      </template>
    </el-dialog>
  </div>
</template>

<script lang="ts" setup>
import { getAccounts, registerAccount } from '@/api/system/account'
import { PageQuery } from '@/utils/datas'
import { reactive, ref } from 'vue'
import { Account } from '@/api/system/types'
import { BaseDataPage } from '@/api/global-types'
import { FormRules } from 'element-plus'

const pageQuery = reactive(new PageQuery(0, 10))
const accountDataPage = ref<BaseDataPage<Account>>({
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

const currentPage = ref(1)
const prevClick = () => {
  pageQuery.page -= 1
  getAccountList()
}
const nextClick = () => {
  pageQuery.page += 1
  getAccountList()
}
const currentChange = () => {
  pageQuery.page = currentPage.value - 1
  getAccountList()
}

// dialog
const dialogFormVisible = ref(false)
const createAccountForm = reactive<Account>({
  username: '',
  password: ''
})

const resetAccountForm = () => {
  dialogFormVisible.value = false
  createAccountForm.username = ''
  createAccountForm.password = ''
}

const rules = reactive<FormRules>({
  username: [
    { required: true, message: 'Please input username!', trigger: 'blur' }
  ],
  password: [
    { required: true, message: 'Please input password!', trigger: 'blur' }
  ]
})
const formLabelWidth = '140px'

const createAccount = () => {
  registerAccount(createAccountForm).then(resp => {
    if (resp.status === 200) {
      resetAccountForm()
      getAccountList()
    }
  })
}

</script>

<style scoped lang="scss">
.my-header {
  display: flex;
  flex-direction: row;
  justify-content: space-between;
}
</style>
