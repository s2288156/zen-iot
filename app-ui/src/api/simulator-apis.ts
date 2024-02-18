import request from '@/utils/request';
import type { Simulator } from '@/api/data/types';
import type { PageQuery } from '@/api/global-types';

export function getSimulators(data: PageQuery) {
  return request.get({
    url: '/api/simulators',
    params: data
  });
}

export function saveSimulator(data: Simulator) {
  return request.post({
    url: '/api/simulator',
    data: data,
  })
}

export function deleteSimulator(id: string) {
  return request.delete({
    url: '/api/simulator/' + id,
  })
}

export function switchSimulatorStatus(id: string, status: string) {
  return request.post({
    url: '/api/simulator/switch',
    params: {
      id: id,
      status: status
    }
  })
}

export function defaultTransportConfig(transportType: string) {
  return request.get({
    url: '/api/simulator/defaultConfig',
    params: {
      transportType: transportType
    }
  })
}
