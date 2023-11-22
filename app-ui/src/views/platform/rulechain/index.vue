<script setup lang="ts">
import {ref} from "vue";
import {BaseDataPage, PageQuery} from "@/api/global-types";
import {RuleChain} from "@/api/data/types";
import {queryRuleChains} from "@/api/rule-chain-apis";

const dialogFormVisible = ref(false)
const pageQuery: PageQuery = {
  page: 0,
  size: 10,
}
const ruleChains = ref<BaseDataPage<RuleChain> | any>({
  data: [],
  size: 0,
  totalPages: 0,
})
const loadList = () => {
  queryRuleChains(pageQuery).then((response) => {
    ruleChains.value = response
  })
}
loadList()
const handelDelete = (row: RuleChain) => {
  console.log("delete row: ", row)
}

const currentPage = ref(1)
const prevClick = () => {
  pageQuery.page -= 1
  loadList()
}
const nextClick = () => {
  pageQuery.page += 1
  loadList()
}
const currentChange = () => {
  pageQuery.page = currentPage.value - 1
  loadList()
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

    <el-table :data="ruleChains.data" style="width: 100%">
      <el-table-column type="index" width="50"/>
      <el-table-column label="id" prop="id" width="250"/>
      <el-table-column label="name" prop="name" width="750"/>
      <el-table-column label="updateTime" prop="updateTime"/>
      <el-table-column fixed="right" label="Operations" width="250" >
        <template #default="{ row, $index }">
          <el-button
              link
              size="small"
              type="primary"
              @click="handelDelete(row)">
            Delete
          </el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-pagination
        v-model:current-page="currentPage"
        :page-count="ruleChains.totalPages"
        :page-size="ruleChains.size"
        background
        layout="prev, pager, next"
        @prev-click="prevClick"
        @next-click="nextClick"
        @current-change="currentChange"
    />
  </div>
</template>

<style scoped lang="scss">

</style>
