import axios from 'axios'
import api from '@/lib/axios'
import type { AdminUser } from '@/types'

export async function loginSuperuser(email: string, password: string) {
  const response = await axios.post<{ access: string; refresh: string; user: AdminUser }>(
    '/api/admin/auth/token/',
    { email, password },
  )
  return response.data
}

export async function getAdminMe() {
  const response = await api.get<AdminUser>('/auth/me/')
  return response.data
}

export async function uploadImage(file: File): Promise<{ url: string }> {
  const formData = new FormData()
  formData.append('image', file)
  const response = await api.post<{ url: string }>('/upload/image/', formData, {
    headers: { 'Content-Type': 'multipart/form-data' },
  })
  return response.data
}
