import api from '@/lib/axios'
import type { AdminUser, DietType, Allergy, PaginatedResponse } from '@/types'

export async function getUsers(params: { search?: string; page?: number } = {}) {
  const query = new URLSearchParams()
  if (params.search) query.set('search', params.search)
  if (params.page) query.set('page', String(params.page))
  const response = await api.get<PaginatedResponse<AdminUser>>(`/users/?${query}`)
  return response.data
}

export async function getUser(id: number) {
  const response = await api.get<AdminUser>(`/users/${id}/`)
  return response.data
}

export async function updateUser(id: number, data: Partial<AdminUser>) {
  const response = await api.patch<AdminUser>(`/users/${id}/`, data)
  return response.data
}

export async function getDietTypes() {
  const response = await api.get<DietType[]>('/diet-types/')
  return response.data
}

export async function createDietType(data: { name: string }) {
  const response = await api.post<DietType>('/diet-types/', data)
  return response.data
}

export async function updateDietType(id: number, data: { name: string }) {
  const response = await api.patch<DietType>(`/diet-types/${id}/`, data)
  return response.data
}

export async function deleteDietType(id: number) {
  await api.delete(`/diet-types/${id}/`)
}

export async function getAllergies() {
  const response = await api.get<Allergy[]>('/allergies/')
  return response.data
}

export async function createAllergy(data: { name: string }) {
  const response = await api.post<Allergy>('/allergies/', data)
  return response.data
}

export async function updateAllergy(id: number, data: { name: string }) {
  const response = await api.patch<Allergy>(`/allergies/${id}/`, data)
  return response.data
}

export async function deleteAllergy(id: number) {
  await api.delete(`/allergies/${id}/`)
}
