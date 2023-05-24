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
        type="info"
        icon="RefreshRight"
        class="filter-item"
        @click="loadAccountList()"
        >刷新
      </el-button>
    </div>

    <el-table :data="accountDataPage.data" style="width: 100%">
      <el-table-column type="index" width="50" />
      <el-table-column prop="username" label="Username" width="180" />
      <el-table-column prop="createTime" label="CreateDate" width="180" />
      <el-table-column prop="updateTime" label="UpdateDate" />
      <el-table-column fixed="right" label="Operations" width="120">
        <template #default="{ row, $index }">
          <el-button link size="small" type="primary" @click="handleClick"
            >Detail</el-button
          >
          <el-button
            link
            size="small"
            type="primary"
            @click="handleDeleteAccount(row)"
            >Delete</el-button
          >
        </template>
      </el-table-column>
    </el-table>

    <el-pagination
      v-model:current-page="currentPage"
      :page-count="accountDataPage.totalPages"
      :page-size="accountDataPage.size"
      background
      layout="prev, pager, next"
      @prev-click="prevClick"
      @next-click="nextClick"
      @current-change="currentChange"
    />

    <el-dialog v-model="dialogFormVisible" title="Add Account">
      <el-form :model="createAccountForm" :rules="rules" status-icon>
        <el-form-item label="Username" label-width="140px" prop="username">
          <el-input v-model="createAccountForm.username" autocomplete="off" />
        </el-form-item>
        <el-form-item label="Password" label-width="140px" prop="password">
          <el-input
            v-model="createAccountForm.password"
            autocomplete="off"
            type="password"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <span class="dialog-footer">
          <el-button @click="resetAccountForm">Cancel</el-button>
          <el-button type="primary" @click="handleCreateAccount"
            >Confirm</el-button
          >
        </span>
      </template>
    </el-dialog>
  </div>
</template>

<script lang="ts" setup>
import { deleteAccount, getAccounts, registerAccount } from '@/api/account-apis'
import { reactive, ref } from 'vue'
import type { Account } from '@/api/data/types'
import type { BaseDataPage, PageQuery } from '@/api/global-types'
import type { FormRules } from 'element-plus'
import { ElMessage } from 'element-plus'

const pageQuery: PageQuery = {
  page: 0,
  size: 10,
}
const accountDataPage = ref<BaseDataPage<Account> | any>({
  data: [],
  size: 0,
  totalPages: 0,
})

const loadAccountList = () => {
  getAccounts(pageQuery).then((response) => {
    accountDataPage.value = response
  })
}
loadAccountList()

const handleClick = () => {
  ElMessage({
    showClose: true,
    message: 'Detail Click',
    type: 'info',
  })
}

const handleDeleteAccount = (row: Account) => {
  if (row.id != null) {
    deleteAccount(row.id).then(() => {
      loadAccountList()
    })
  }
}

const currentPage = ref(1)
const prevClick = () => {
  pageQuery.page -= 1
  loadAccountList()
}
const nextClick = () => {
  pageQuery.page += 1
  loadAccountList()
}
const currentChange = () => {
  pageQuery.page = currentPage.value - 1
  loadAccountList()
}

// dialog
const dialogFormVisible = ref(false)
const createAccountForm = reactive<Account>({
  username: '',
  password: '',
})

const resetAccountForm = () => {
  dialogFormVisible.value = false
  createAccountForm.username = ''
  createAccountForm.password = ''
}

const rules = reactive<FormRules>({
  username: [
    { required: true, message: 'Please input username!', trigger: 'blur' },
  ],
  password: [
    { required: true, message: 'Please input password!', trigger: 'blur' },
  ],
})

const handleCreateAccount = () => {
  registerAccount(createAccountForm).then((resp) => {
    if (resp.status === 200) {
      resetAccountForm()
      loadAccountList()
    }
  })
}
</script>

<style scoped lang="scss"></style>
