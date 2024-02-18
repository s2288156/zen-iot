import request from '@/utils/request';
import type { Device } from '@/api/data/types';
import type { PageQuery } from '@/api/global-types';

export function getDeviceCommon() {
  return request.get({
    url: '/api/device/common'
  });
}

export function getDevices(data: PageQuery) {
  return request.get({
    url: '/api/devices',
    params: data,
  })
}

export function saveDevice(data: Device) {
  return request.post({
    url: '/api/device',
    data: data,
  })
}

export function deleteDevice(id: string) {
  return request.delete({
    url: '/api/device/' + id,
  })
}

