import axios from 'axios'
import type {RestResponse} from "@/api/global-types";
import {ElMessage, ElMessageBox} from "element-plus";
import {useUserStore} from "@/stores/user";

const service = axios.create({
  baseURL: 'http://localhost:8088',
  timeout: 5000
})

service.interceptors.request.use(
  config => {
    if (useUserStore().getToken) {
      config.headers = {
        'Authorization': 'Bearer ' + useUserStore().getToken
      }
    }
    return config
  },
  error => {
    console.log(error)
    return Promise.reject(error)
  }
)

service.interceptors.response.use(
  response => {
    const rsp: RestResponse = response.data
    if (!rsp.success) {
      ElMessage({
        showClose: true,
        message: rsp.msg,
        type: 'error',
      })
      if (rsp.code === 40000 || rsp.code === 40001) {
        ElMessageBox.confirm('You have been logged out, you can cancel to stay on this page, or log in again', 'Confirm logout', {
          confirmButtonText: 'Re-Login',
          cancelButtonText: 'Cancel',
          type: 'warning'
        }).then(() => {
          alert(rsp.msg)
        })
      }
      return Promise.reject(new Error(rsp.msg || 'Error'));
    } else {
            return response
        }
    }
)

export default service
