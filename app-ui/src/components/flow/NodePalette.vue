<script setup lang="ts">
import LogicFlow, { IBaseModel } from '@logicflow/core';
import { ref } from 'vue';

const props = defineProps({
  lf: {
    type: LogicFlow,
    required: true
  }
});

const startDrag = (item) => {
  const { lf } = props;
  lf.dnd.startDrag({
    type: item.type,
    text: item.text
  });
};

const baseNodes = ref([
  {
    type: 'fetch-node',
    text: 'fetch',
    label: '开始节点',
    icon: 'data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAABQAAAAUCAYAAAH6ji2bAAAABGdBTUEAALGPC/xhBQAAAnBJREFUOBGdVL1rU1EcPfdGBddmaZLiEhdx1MHZQXApraCzQ7GKLgoRBxMfcRELuihWKcXFRcEWF8HBf0DdDCKYRZpnl7p0svLe9Zzbd29eQhTbC8nv+9zf130AT63jvooOGS8Vf9Nt5zxba7sXQwODfkWpkbjTQfCGUd9gIp3uuPP8bZ946g56dYQvnBg+b1HB8VIQmMFrazKcKSvFW2dQTxJnJdQ77urmXWOMBCmXM2Rke4S7UAW+/8ywwFoewmBps2tu7mbTdp8VMOkIRAkKfrVawalJTtIliclFbaOBqa0M2xImHeVIfd/nKAfVq/LGnPss5Kh00VEdSzfwnBXPUpmykNss4lUI9C1ga+8PNrBD5YeqRY2Zz8PhjooIbfJXjowvQJBqkmEkVnktWhwu2SM7SMx7Cj0N9IC0oQXRo8xwAGzQms+xrB/nNSUWVveI48ayrFGyC2+E2C+aWrZHXvOuz+CiV6iycWe1Rd1Q6+QUG07nb5SbPrL4426d+9E1axKjY3AoRrlEeSQo2Eu0T6BWAAr6COhTcWjRaYfKG5csnvytvUr/WY4rrPMB53Uo7jZRjXaG6/CFfNMaXEu75nG47X+oepU7PKJvvzGDY1YLSKHJrK7vFUwXKkaxwhCW3u+sDFMVrIju54RYYbFKpALZAo7sB6wcKyyrd+aBMryMT2gPyD6GsQoRFkGHr14TthZni9ck0z+Pnmee460mHXbRAypKNy3nuMdrWgVKj8YVV8E7PSzp1BZ9SJnJAsXdryw/h5ctboUVi4AFiCd+lQaYMw5z3LGTBKjLQOeUF35k89f58Vv/tGh+l+PE/wG0rgfIUbZK5AAAAABJRU5ErkJggg=='
  }
  // {
  //   type: 'fetch-node',
  //   text: 'text',
  //   background: 'rgb(231, 231, 174)',
  //   icon: 'public/images/fetch.svg'
  // }
]);
</script>

<template>
  <div class="node-collapse">
    <el-collapse>
      <el-collapse-item title="基础节点">
        <div
          v-for="(item, index) in baseNodes"
          @mousedown="startDrag(item)"
          :key="index">
          <div>{{ item.text }}</div>
          <div :style="{ backgroundImage: `url(${item.icon})`}"></div>
        </div>
      </el-collapse-item>
    </el-collapse>
  </div>
</template>

<style scoped lang="scss"></style>
