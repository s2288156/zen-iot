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

export function switchSimulatorStatus(id: string) {
  return request({
    url: '/api/simulator/switch/' + id,
    method: 'post',
  })
}
