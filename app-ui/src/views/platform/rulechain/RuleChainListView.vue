<script setup lang="ts">
import { ref } from 'vue';
import { PageQuery, PageResponse } from '@/api/global-types';
import { RuleChain } from '@/api/data/types';
import { queryRuleChains } from '@/api/rule-chain-apis';
import router from '@/router';

const pageQuery: PageQuery = {
  page: 0,
  size: 10
};
const ruleChains = ref<PageResponse<RuleChain>>({
  data: [],
  size: 0,
  totalPages: 0
});
const loadList = () => {
  queryRuleChains(pageQuery).then(response => {
    ruleChains.value = response.data;
  });
};
loadList();
const handelDelete = (row: RuleChain) => {
  console.log('delete row: ', row);
};

const handelDetail = (row: RuleChain) => {
  router.push({ path: '/platform/rule-chain/dnd', query: { id: row.id } });
};

const toDnd = () => {
  router.push('/platform/rule-chain/dnd');
};

const currentPage = ref(1);
const prevClick = () => {
  pageQuery.page -= 1;
  loadList();
};
const nextClick = () => {
  pageQuery.page += 1;
  loadList();
};
const currentChange = () => {
  pageQuery.page = currentPage.value - 1;
  loadList();
};
</script>

<template>
  <div class="app-container">
    <div class="filter-container">
      <el-button class="filter-item" icon="EditPen" type="primary" @click="toDnd()"> 新增 </el-button>
      <el-button class="filter-item" icon="RefreshRight" type="info" @click="loadList()">刷新 </el-button>
    </div>

    <el-table :data="ruleChains.data" class="list-table">
      <el-table-column type="index" width="50" />
      <el-table-column label="id" prop="id" width="200" />
      <el-table-column label="name" prop="name" />
      <el-table-column label="updateTime" prop="updateTime" />
      <el-table-column fixed="right" label="Operations">
        <template #default="{ row }">
          <el-button link size="small" type="primary" @click="handelDelete(row)"> Delete </el-button>
          <el-button link size="small" type="primary" @click="handelDetail(row)">Detail</el-button>
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

<style scoped lang="scss"></style>
