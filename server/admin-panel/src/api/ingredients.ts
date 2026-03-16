import api from '@/lib/axios'
import type { Ingredient, IngredientCategory, Unit, PaginatedResponse } from '@/types'

export async function getIngredients(params: { search?: string; category?: number; page?: number } = {}) {
  const query = new URLSearchParams()
  if (params.search) query.set('search', params.search)
  if (params.category) query.set('category', String(params.category))
  if (params.page) query.set('page', String(params.page))
  const response = await api.get<PaginatedResponse<Ingredient>>(`/ingredients/?${query}`)
  return response.data
}

export async function getIngredient(id: number) {
  const response = await api.get<Ingredient>(`/ingredients/${id}/`)
  return response.data
}

export async function createIngredient(data: Partial<Ingredient>) {
  const response = await api.post<Ingredient>('/ingredients/', data)
  return response.data
}

export async function updateIngredient(id: number, data: Partial<Ingredient>) {
  const response = await api.patch<Ingredient>(`/ingredients/${id}/`, data)
  return response.data
}

export async function deleteIngredient(id: number) {
  await api.delete(`/ingredients/${id}/`)
}

export async function getCategories() {
  const response = await api.get<IngredientCategory[]>('/categories/')
  return response.data
}

export async function createCategory(data: { name: string }) {
  const response = await api.post<IngredientCategory>('/categories/', data)
  return response.data
}

export async function updateCategory(id: number, data: { name: string }) {
  const response = await api.patch<IngredientCategory>(`/categories/${id}/`, data)
  return response.data
}

export async function deleteCategory(id: number) {
  await api.delete(`/categories/${id}/`)
}

export async function getUnits() {
  const response = await api.get<Unit[]>('/units/')
  return response.data
}

export async function createUnit(data: { name: string; is_base: boolean }) {
  const response = await api.post<Unit>('/units/', data)
  return response.data
}

export async function updateUnit(id: number, data: { name: string; is_base: boolean }) {
  const response = await api.patch<Unit>(`/units/${id}/`, data)
  return response.data
}

export async function deleteUnit(id: number) {
  await api.delete(`/units/${id}/`)
}
