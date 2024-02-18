import axios from 'axios';
import type { RestResponse } from '@/api/global-types';
import { ElMessage, ElMessageBox } from 'element-plus';
import { useUserStore } from '@/stores/user';

const service = axios.create({
  baseURL: 'http://localhost:8088',
  timeout: 5000
});

service.interceptors.request.use(
  config => {
    if (useUserStore().getToken() && config.url !== '/api/login') {
      config.headers.setAuthorization('Bearer ' + useUserStore().getToken());
    }
    return config;
  },
  error => {
    console.log(error);
    return Promise.reject(error);
  }
);

/**
 * 可选值：application/x-www-form-urlencoded multipart/form-data
 */
const defaultHeaders = 'application/json';

service.interceptors.response.use(response => {
  const rsp: RestResponse<object> = response.data;
  if (!rsp.success) {
    ElMessage({
      showClose: true,
      message: rsp.msg,
      type: 'error'
    });
    if (rsp.code === 40000 || rsp.code === 40001) {
      ElMessageBox.confirm('You have been logged out, you can cancel to stay on this page, or log in again', 'Confirm logout', {
        confirmButtonText: 'Re-Login',
        cancelButtonText: 'Cancel',
        type: 'warning'
      }).then(() => {
        useUserStore()
          .logout()
          .then(() => {
            location.reload();
          });
      });
    }
    return Promise.reject(new Error(rsp.msg || 'Error'));
  } else {
    return response.data;
  }
});

declare type AxiosMethod = 'get' | 'post' | 'delete' | 'put';

declare type AxiosResponseType = 'arraybuffer' | 'blob' | 'document' | 'json' | 'text' | 'stream';

declare interface AxiosConfig {
  params?: any;
  data?: any;
  url?: string;
  method?: AxiosMethod;
  headersType?: string;
  responseType?: AxiosResponseType;
}

const request = (option: AxiosConfig) => {
  const { url, method, params, data, headersType, responseType } = option;
  return service.request({
    url: url,
    method,
    params,
    data,
    responseType: responseType,
    headers: {
      'Content-Type': headersType || defaultHeaders
    }
  });
};

export default {
  get: <T = any>(option: AxiosConfig) => {
    return service.request({ method: 'get', ...option }) as Promise<RestResponse<T>>;
  },
  post: <T = any>(option: AxiosConfig) => {
    return service.request({ method: 'post', ...option }) as Promise<RestResponse<T>>;
  },
  delete: <T = any>(option: AxiosConfig) => {
    return service.request({ method: 'delete', ...option }) as Promise<RestResponse<T>>;
  },
  put: <T = any>(option: AxiosConfig) => {
    return service.request({ method: 'put', ...option }) as Promise<RestResponse<T>>;
  }
};
