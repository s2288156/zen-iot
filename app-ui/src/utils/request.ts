import axios from 'axios'

const service = axios.create({
    baseURL: 'http://localhost:8088',
    timeout: 5000
})

service.interceptors.response.use(
    response => {

    }
)

export default service
