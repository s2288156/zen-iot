import request from '@/utils/request'
import type {Simulator} from '@/api/types'
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

