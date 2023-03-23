<template>
  <el-dialog v-model="visible" title="Add Device">
    <el-form label-width="140px" ref="ruleFormRef" :model="createSimulatorForm" :rules="rules" status-icon>
      <el-form-item label="Name" prop="name">
        <el-input v-model="createSimulatorForm.name"/>
      </el-form-item>
      <el-form-item label="Transport Type" prop="transportType">
        <el-select v-model="createSimulatorForm.transportType" clearable placeholder="Transport Type">
          <el-option
              v-for="item in deviceCommonData.transportTypes"
              :key="item"
              :label="item"
              :value="item"
          />
        </el-select>
      </el-form-item>
      <el-form-item label="Timeseries Topic" prop="saveTimeseriesTopic">
        <el-input v-model="createSimulatorForm.transportConfig.saveTimeseriesTopic"/>
      </el-form-item>
      <el-form-item label="Period" prop="period">
        <el-input v-model="createSimulatorForm.transportConfig.period"/>
      </el-form-item>
      <el-form-item label="Time Unit" prop="timeUnit">
        <el-select v-model="createSimulatorForm.transportConfig.timeUnit" placeholder="Time Unit">
          <el-option
              v-for="item in deviceCommonData.timeUnit"
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
          <el-button type="primary" @click="handleCreateSimulator(ruleFormRef)">Confirm</el-button>
        </span>
    </template>
  </el-dialog>
</template>

<script lang="ts" setup>
import {reactive, ref} from "vue";
import type {FormInstance, FormRules} from "element-plus";
import type {DeviceCommon, Simulator} from "@/api/data/types";
import {TimeUnit, TransportType} from "@/api/data/enums";
import {saveSimulator} from "@/api/simulator-apis";
import {getDeviceCommon} from "@/api/device-apis";

const visible = ref(false)
const deviceCommonData = ref<DeviceCommon>({});
const ruleFormRef = ref<FormInstance>()

const loadDeviceCommon = () => {
  getDeviceCommon().then((resp) => {
    deviceCommonData.value = resp.data
  })
}

let createSimulatorForm = reactive<Simulator>({
  transportType: TransportType[TransportType.MQTT],
  transportConfig: {
    type: TransportType[TransportType.MQTT],
    saveTimeseriesTopic: '',
    period: 1,
    timeUnit: TimeUnit[TimeUnit.SECONDS]
  }
})

const resetDeviceForm = (formEl: FormInstance) => {
  formEl.resetFields()
  createSimulatorForm.name = ''
  createSimulatorForm.transportType = TransportType[TransportType.DEFAULT]
}

const rules = reactive<FormRules>({
  name: [{required: true, message: 'Please input name!', trigger: 'blur'}],
  transportType: [{required: true, message: 'Please input transportType!', trigger: 'blur'}],
})

const handleCreateSimulator = async (formEl: FormInstance) => {
  if (!formEl) return
  await formEl.validate((valid, fields) => {
    if (valid) {
      createSimulatorForm.transportConfig.type = createSimulatorForm.transportType
      saveSimulator(createSimulatorForm).then(() => {
        resetDeviceForm(formEl)
      })
    }
  })
}

const open = () => {
  visible.value = true
  loadDeviceCommon()
}

defineExpose({
  open
})
</script>

<style scoped lang="scss">

</style>
