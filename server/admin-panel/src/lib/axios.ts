import axios from 'axios'

const api = axios.create({
  baseURL: '/api/admin',
  headers: {
    'Content-Type': 'application/json',
  },
})

api.interceptors.request.use((config) => {
  const token = localStorage.getItem('admin_access_token')
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

api.interceptors.response.use(
  (response) => response,
  async (error) => {
    const originalRequest = error.config
    if (error.response?.status === 401 && !originalRequest._retry) {
      originalRequest._retry = true
      const refreshToken = localStorage.getItem('admin_refresh_token')
      if (refreshToken) {
        try {
          const response = await axios.post('/api/accounts/token/refresh/', {
            refresh: refreshToken,
          })
          const newAccess = response.data.access
          localStorage.setItem('admin_access_token', newAccess)
          originalRequest.headers.Authorization = `Bearer ${newAccess}`
          return api(originalRequest)
        } catch {
          localStorage.removeItem('admin_access_token')
          localStorage.removeItem('admin_refresh_token')
          window.location.href = '/panel/login'
        }
      } else {
        window.location.href = '/panel/login'
      }
    }
    return Promise.reject(error)
  },
)

export default api
