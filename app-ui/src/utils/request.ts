import axios from 'axios'
import type {RestResponse} from "@/api/global-types";
import {ElMessage} from "element-plus";

const service = axios.create({
    baseURL: 'http://localhost:8088',
    timeout: 5000
})

service.interceptors.response.use(
    response => {
        const rsp: RestResponse = response.data
        if (!rsp.success) {
            console.log(rsp, '@@@')
            ElMessage({
                showClose: true,
                message: rsp.msg,
                type: 'error',
            })
        } else {
            return rsp
        }
    }
)

export default service
