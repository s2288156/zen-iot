import request from '@/utils/request'
import type {Device} from '@/api/types'
import type {PageQuery} from '@/api/global-types'

export function getDeviceCommon() {
  return request({
    url: '/api/device/common',
    method: 'get'
  })
}

export function getDevices(data: PageQuery) {
  return request({
    url: '/api/devices',
    method: 'get',
    params: data,
  })
}

export function saveDevice(data: Device) {
  return request({
    url: '/api/device',
    method: 'post',
    data: data,
  })
}

export function deleteDevice(id: number) {
  return request({
    url: '/api/device/' + id,
    method: 'delete'
  })
}

