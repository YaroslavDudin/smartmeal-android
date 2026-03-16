import api from '@/lib/axios'
import type { Stats } from '@/types'

export async function getStats() {
  const response = await api.get<Stats>('/stats/')
  return response.data
}
