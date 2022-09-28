import axios from 'axios'

const service = axios.create({
  baseURL: 'http://localhost:8088',
})

export default service
