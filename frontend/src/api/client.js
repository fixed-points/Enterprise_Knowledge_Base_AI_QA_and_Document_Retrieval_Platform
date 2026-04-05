import axios from 'axios'

const request = axios.create({
  baseURL: 'http://localhost:8081',
  timeout: 60000
})

request.interceptors.response.use(
  (response) => {
    const payload = response.data
    if (payload?.success === false) {
      return Promise.reject(new Error(payload.message || '请求失败'))
    }
    return payload
  },
  (error) => Promise.reject(new Error(error.response?.data?.message || error.message || '网络异常'))
)

export const getOverview = () => request.get('/api/dashboard/overview')
export const getDocuments = () => request.get('/api/documents')
export const getDocumentDetail = (id) => request.get(`/api/documents/${id}`)
export const deleteDocument = (id) => request.delete(`/api/documents/${id}`)
export const uploadDocument = (file) => {
  const formData = new FormData()
  formData.append('file', file)
  return request.post('/api/documents/upload', formData, {
    headers: {
      'Content-Type': 'multipart/form-data'
    }
  })
}
export const askQuestion = (payload) => request.post('/api/qa/ask', payload)
export const getRecords = () => request.get('/api/qa/records')
export const submitFeedback = (payload) => request.post('/api/feedback', payload)
