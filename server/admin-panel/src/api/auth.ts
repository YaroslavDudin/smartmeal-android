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

export interface ImageSearchResponse {
  images: ImageSearchResult[]
  source?: 'pixabay' | 'ddgs'
}

export class ImageSearchRateLimitError extends Error {
  constructor(message: string) {
    super(message)
    this.name = 'ImageSearchRateLimitError'
  }
}

export async function searchImages(query: string, max = 12): Promise<ImageSearchResponse> {
  try {
    const response = await api.get<ImageSearchResponse>('/search/images/', {
      params: { q: query, max },
    })
    return response.data
  } catch (err: unknown) {
    const e = err as { response?: { status?: number; data?: { detail?: string; rate_limited?: boolean } } }
    if (e.response?.status === 503 || e.response?.data?.rate_limited) {
      throw new ImageSearchRateLimitError(
        e.response?.data?.detail ?? 'Поиск временно недоступен. Подождите 1-2 минуты.'
      )
    }
    throw err
  }
}

export async function uploadImage(file: File): Promise<{ url: string }> {
  const formData = new FormData()
  formData.append('image', file)
  const response = await api.post<{ url: string }>('/upload/image/', formData, {
    headers: { 'Content-Type': 'multipart/form-data' },
  })
  return response.data
}
