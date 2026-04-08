import { createContext, useContext, useState, useCallback } from 'react'
import { logoutAdmin } from '@/api/auth'

interface AdminUser {
  id: number
  username: string
  email: string
  is_superuser: boolean
  first_name: string
  last_name: string
}

interface AuthContextValue {
  user: AdminUser | null
  isAuthenticated: boolean
  login: (user: AdminUser, access: string, refresh: string) => void
  logout: () => Promise<void>
}

const AuthContext = createContext<AuthContextValue | null>(null)

export function AuthProvider({ children }: { children: React.ReactNode }) {
  const [user, setUser] = useState<AdminUser | null>(() => {
    const stored = localStorage.getItem('admin_user')
    if (stored) {
      try {
        return JSON.parse(stored) as AdminUser
      } catch {
        return null
      }
    }
    return null
  })

  const login = useCallback((user: AdminUser, access: string, refresh: string) => {
    setUser(user)
    localStorage.setItem('admin_user', JSON.stringify(user))
    localStorage.setItem('admin_access_token', access)
    localStorage.setItem('admin_refresh_token', refresh)
  }, [])

  const logout = useCallback(async () => {
    const refresh = localStorage.getItem('admin_refresh_token')
    if (refresh) {
      await logoutAdmin(refresh)
    }
    setUser(null)
    localStorage.removeItem('admin_user')
    localStorage.removeItem('admin_access_token')
    localStorage.removeItem('admin_refresh_token')
  }, [])

  return (
    <AuthContext.Provider value={{ user, isAuthenticated: !!user, login, logout }}>
      {children}
    </AuthContext.Provider>
  )
}

export function useAuth() {
  const ctx = useContext(AuthContext)
  if (!ctx) throw new Error('useAuth must be used within AuthProvider')
  return ctx
}
