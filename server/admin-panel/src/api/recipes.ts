import api from '@/lib/axios'
import type { Recipe, RecipeShort, PaginatedResponse } from '@/types'

export interface RecipeFilters {
  search?: string
  diet_type?: number
  min_calories?: number
  max_calories?: number
  ordering?: string
  page?: number
  page_size?: number
}

export async function getRecipes(filters: RecipeFilters = {}) {
  const params = new URLSearchParams()
  if (filters.search) params.set('search', filters.search)
  if (filters.diet_type) params.set('diet_type', String(filters.diet_type))
  if (filters.min_calories !== undefined) params.set('min_calories', String(filters.min_calories))
  if (filters.max_calories !== undefined) params.set('max_calories', String(filters.max_calories))
  if (filters.ordering) params.set('ordering', filters.ordering)
  if (filters.page) params.set('page', String(filters.page))
  if (filters.page_size) params.set('page_size', String(filters.page_size ?? 20))
  const response = await api.get<PaginatedResponse<RecipeShort>>(`/recipes/?${params}`)
  return response.data
}

export async function getRecipe(id: number) {
  const response = await api.get<Recipe>(`/recipes/${id}/`)
  return response.data
}

export async function createRecipe(data: FormData) {
  const response = await api.post('/recipes/', data, {
    headers: { 'Content-Type': 'multipart/form-data' }
  })
  return response.data
}

export async function updateRecipe(id: number, data: FormData) {
  const response = await api.patch<Recipe>(`/recipes/${id}/`, data, {
    headers: { 'Content-Type': 'multipart/form-data' }
  })
  return response.data
}

export async function deleteRecipe(id: number) {
  await api.delete(`/recipes/${id}/`)
}

export async function addRecipeIngredient(recipeId: number, data: object) {
  const response = await api.post(`/recipes/${recipeId}/ingredients/`, data)
  return response.data
}

export async function updateRecipeIngredient(recipeId: number, ingredientId: number, data: object) {
  const response = await api.patch(`/recipes/${recipeId}/ingredients/${ingredientId}/`, data)
  return response.data
}

export async function deleteRecipeIngredient(recipeId: number, ingredientId: number) {
  await api.delete(`/recipes/${recipeId}/ingredients/${ingredientId}/`)
}

export async function addRecipeStep(recipeId: number, data: FormData) {
  const response = await api.post(`/recipes/${recipeId}/steps/`, data, {
    headers: { 'Content-Type': 'multipart/form-data' }
  })
  return response.data
}

export async function updateRecipeStep(recipeId: number, stepId: number, data: FormData) {
  const response = await api.patch<Recipe>(`/recipes/${recipeId}/steps/${stepId}/`, data, {
    headers: { 'Content-Type': 'multipart/form-data' }
  })
  return response.data
}

export async function deleteRecipeStep(recipeId: number, stepId: number) {
  await api.delete(`/recipes/${recipeId}/steps/${stepId}/`)
}
