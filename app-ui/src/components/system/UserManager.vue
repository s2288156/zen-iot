<template>
  <div class="app-container">
    <div class="filter-container">
      <el-button type="primary" icon="EditPen" class="filter-item">新增</el-button>
    </div>

    <el-table :data="tableData.pages" style="width: 100%">
      <el-table-column prop="date" label="Date" width="180"/>
      <el-table-column prop="name" label="Name" width="180"/>
      <el-table-column prop="address" label="Address"/>
    </el-table>

    <el-pagination background layout="prev, pager, next" :page-size="tableData.pageSize" :page-count="tableData.pageCount"/>
  </div>
</template>

<script lang="ts" setup>

const tableData = {
  'pageSize': 2,
  'pageCount': 1,
  'pages': [
    {
      date: '2016-05-03',
      name: 'Tom',
      address: 'No. 189, Grove St, Los Angeles',
    },
    {
      date: '2016-05-02',
      name: 'Tom',
      address: 'No. 189, Grove St, Los Angeles',
    }
  ]
}

</script>

<style scoped lang="scss">

</style>
