import request from '@/utils/request'
import type {Simulator} from '@/api/data/types'
import type {PageQuery} from '@/api/global-types'

export function getSimulators(data: PageQuery) {
  return request({
    url: '/api/simulators',
    method: 'get',
    params: data,
  })
}

export function saveSimulator(data: Simulator) {
  return request({
    url: '/api/simulator',
    method: 'post',
    data: data,
  })
}

export function deleteSimulator(id: string) {
  return request({
    url: '/api/simulator/' + id,
    method: 'delete'
  })
}

export function switchSimulatorStatus(id: string, status: string) {
  return request({
    url: '/api/simulator/switch',
    method: 'post',
    params: {
      id: id,
      status: status
    }
  })
}

export function defaultTransportConfig(transportType: string) {
  return request({
    url: '/api/simulator/defaultConfig',
    method: 'get',
    params: {
      transportType: transportType
    }
  })
}
