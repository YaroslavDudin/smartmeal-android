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

export interface ImageSearchResult {
  url: string
  thumbnail: string
  title: string
  width?: number
  height?: number
}

export async function searchImages(query: string, max = 12): Promise<ImageSearchResult[]> {
  const response = await api.get<{ images: ImageSearchResult[] }>('/search/images/', {
    params: { q: query, max },
  })
  return response.data.images
}

export async function uploadImage(file: File): Promise<{ url: string }> {
  const formData = new FormData()
  formData.append('image', file)
  const response = await api.post<{ url: string }>('/upload/image/', formData, {
    headers: { 'Content-Type': 'multipart/form-data' },
  })
  return response.data
}
