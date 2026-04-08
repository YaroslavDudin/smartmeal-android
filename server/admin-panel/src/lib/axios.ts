import axios from 'axios'

let isRefreshing = false
let failedQueue: any[] = []

const processQueue = (error: any, token: string | null = null) => {
  failedQueue.forEach((prom) => {
    if (error) {
      prom.reject(error)
    } else {
      prom.resolve(token)
    }
  })

  failedQueue = []
}

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
      if (isRefreshing) {
        return new Promise(function (resolve, reject) {
          failedQueue.push({ resolve, reject })
        })
          .then((token) => {
            originalRequest.headers.Authorization = 'Bearer ' + token
            return api(originalRequest)
          })
          .catch((err) => {
            return Promise.reject(err)
          })
      }

      originalRequest._retry = true
      isRefreshing = true

      const refreshToken = localStorage.getItem('admin_refresh_token')
      const currentAccessToken = localStorage.getItem('admin_access_token')
      const requestAccessToken = originalRequest.headers.Authorization?.replace('Bearer ', '')

      // Double-check: if token has been updated while this request was flying, just retry
      if (currentAccessToken && currentAccessToken !== requestAccessToken) {
        isRefreshing = false
        originalRequest.headers.Authorization = 'Bearer ' + currentAccessToken
        return api(originalRequest)
      }

      if (refreshToken) {
        try {
          const response = await axios.post('/api/accounts/token/refresh/', {
            refresh: refreshToken,
          })
          const newAccess = response.data.access
          const newRefresh = response.data.refresh || refreshToken // Поддержка ротации
          
          localStorage.setItem('admin_access_token', newAccess)
          localStorage.setItem('admin_refresh_token', newRefresh)
          
          originalRequest.headers.Authorization = `Bearer ${newAccess}`
          processQueue(null, newAccess)
          return api(originalRequest)
        } catch (refreshError) {
          processQueue(refreshError, null)
          localStorage.removeItem('admin_access_token')
          localStorage.removeItem('admin_refresh_token')
          window.location.href = '/panel/login'
          return Promise.reject(refreshError)
        } finally {
          isRefreshing = false
        }
      } else {
        isRefreshing = false
        window.location.href = '/panel/login'
      }
    }
    return Promise.reject(error)
  },
)

export default api
