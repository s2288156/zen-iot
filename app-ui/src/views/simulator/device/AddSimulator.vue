<template>
  <el-dialog v-model="visible" title="Add Simulator">
    <el-form :disabled="formDisabled" label-width="140px" ref="ruleFormRef" :model="createSimulatorForm" :rules="rules" status-icon>
      <el-form-item label="Name" prop="name">
        <el-input v-model="createSimulatorForm.name" />
      </el-form-item>
      <el-form-item label="Transport Type" prop="transportType">
        <el-select v-model="createSimulatorForm.transportType" @change="selectTransportTypeChange" placeholder="Transport Type">
          <el-option v-for="item in deviceCommonData.transportTypes" :key="item" :label="item" :value="item" />
        </el-select>
      </el-form-item>
      <el-form-item label="Timeseries Topic" prop="saveTimeseriesTopic">
        <el-input v-model="createSimulatorForm.transportConfig.saveTimeseriesTopic" />
      </el-form-item>
      <el-form-item label="Period" prop="period">
        <el-input v-model="createSimulatorForm.transportConfig.period" />
      </el-form-item>
      <el-form-item label="Time Unit" prop="timeUnit">
        <el-select v-model="createSimulatorForm.transportConfig.timeUnit" placeholder="Time Unit">
          <el-option v-for="item in deviceCommonData.timeUnit" :key="item" :label="item" :value="item" />
        </el-select>
      </el-form-item>
      <el-form-item label="Timeseries Fields" prop="timeseriesFields">
        <el-table :data="createSimulatorForm.transportConfig.timeseriesFields" stripe style="width: 100%">
          <el-table-column prop="name" label="Name" width="140">
            <template #default="scope">
              <el-input v-model="scope.row.name"></el-input>
            </template>
          </el-table-column>
          <el-table-column prop="fieldType" label="Field Type" width="140">
            <template #default="scope">
              <el-select v-model="scope.row.fieldType">
                <el-option v-for="item in deviceCommonData.fieldTypes" :key="item" :label="item" :value="item" />
              </el-select>
            </template>
          </el-table-column>
          <el-table-column prop="valueOrigin" label="Value Origin">
            <template #default="scope">
              <el-input v-model="scope.row.valueOrigin"></el-input>
            </template>
          </el-table-column>
          <el-table-column prop="valueBound" label="Value Bound">
            <template #header="scope">
              Value Bound
              <el-link v-show="!formDisabled" type="primary" @click="add">+</el-link>
            </template>
            <template #default="scope">
              <el-input v-model="scope.row.valueBound"></el-input>
            </template>
          </el-table-column>
        </el-table>
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
import { reactive, ref } from 'vue'
import type { FormInstance, FormRules } from 'element-plus'
import type { DeviceCommon, Simulator } from '@/api/data/types'
import { TimeUnit, TransportType } from '@/api/data/enums'
import { defaultTransportConfig, saveSimulator } from '@/api/simulator-apis'
import { getDeviceCommon } from '@/api/device-apis'
import { SimulatorJsonFieldDefine } from '@/api/data/types'

const visible = ref(false)
const formDisabled = ref(false)
const deviceCommonData = ref<DeviceCommon>({})
const ruleFormRef = ref<FormInstance>()

let emit = defineEmits(['refresh'])

const loadDeviceCommon = () => {
  getDeviceCommon().then(resp => {
    deviceCommonData.value = resp.data
  })
}

let createSimulatorForm = reactive<Simulator>({
  transportType: TransportType[TransportType.MQTT],
  transportConfig: {
    type: TransportType[TransportType.MQTT],
    saveTimeseriesTopic: '',
    period: 1,
    timeUnit: TimeUnit[TimeUnit.SECONDS],
    timeseriesFields: []
  }
})

const defaultTimeseriesField: SimulatorJsonFieldDefine = {
  fieldType: '',
  name: '',
  valueBound: '',
  valueOrigin: ''
}

const selectTransportTypeChange = () => {
  queryDefaultTransportConfig(createSimulatorForm.transportType || '')
}

const queryDefaultTransportConfig = (transportType: string) => {
  defaultTransportConfig(transportType).then(resp => {
    createSimulatorForm.name = ''
    createSimulatorForm.transportConfig = resp.data
    defaultTimeseriesField.fieldType = createSimulatorForm.transportConfig.timeseriesFields[0].fieldType
    defaultTimeseriesField.name = createSimulatorForm.transportConfig.timeseriesFields[0].name
    defaultTimeseriesField.valueBound = createSimulatorForm.transportConfig.timeseriesFields[0].valueBound
    defaultTimeseriesField.valueOrigin = createSimulatorForm.transportConfig.timeseriesFields[0].valueOrigin
  })
}
const resetDeviceForm = (formEl: FormInstance) => {
  formEl.resetFields()
  createSimulatorForm.name = ''
  createSimulatorForm.transportType = TransportType[TransportType.MQTT]
  visible.value = false
}

const rules = reactive<FormRules>({
  name: [{ required: true, message: 'Please input name!', trigger: 'blur' }],
  transportType: [{ required: true, message: 'Please input transportType!', trigger: 'blur' }]
})

const handleCreateSimulator = async (formEl: FormInstance) => {
  if (!formEl) return
  await formEl.validate((valid, fields) => {
    if (valid) {
      createSimulatorForm.transportConfig.type = createSimulatorForm.transportType
      saveSimulator(createSimulatorForm).then(() => {
        resetDeviceForm(formEl)
        emit('refresh')
      })
    }
  })
}

const add = () => {
  let oneFieldDefine: SimulatorJsonFieldDefine = {
    fieldType: defaultTimeseriesField.fieldType,
    name: defaultTimeseriesField.name,
    valueBound: defaultTimeseriesField.valueBound,
    valueOrigin: defaultTimeseriesField.valueOrigin
  }
  createSimulatorForm.transportConfig.timeseriesFields.push(oneFieldDefine)
}

const open = () => {
  visible.value = true
  formDisabled.value = false
  loadDeviceCommon()
  queryDefaultTransportConfig(TransportType[TransportType.MQTT])
}

const openDetail = (simulator: Simulator) => {
  visible.value = true
  formDisabled.value = true
  loadDeviceCommon()
  Object.assign(createSimulatorForm, simulator)
}

defineExpose({
  open, openDetail
})
</script>

<style scoped lang="scss"></style>
