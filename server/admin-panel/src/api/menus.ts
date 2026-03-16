import api from '@/lib/axios'
import type { Menu, PaginatedResponse } from '@/types'

export async function getMenus(params: { page?: number } = {}) {
  const query = new URLSearchParams()
  if (params.page) query.set('page', String(params.page))
  const response = await api.get<PaginatedResponse<Menu>>(`/menus/?${query}`)
  return response.data
}

export async function deleteMenu(id: number) {
  await api.delete(`/menus/${id}/`)
}
