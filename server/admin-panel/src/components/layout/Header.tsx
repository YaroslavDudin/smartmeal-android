import { Menu, Sun, Moon, LogOut, ChefHat } from 'lucide-react'
import { useTheme } from '@/store/themeContext'
import { useAuth } from '@/store/authContext'
import { useNavigate } from 'react-router-dom'

interface HeaderProps {
  onMenuToggle: () => void
  title?: string
}

export function Header({ onMenuToggle, title }: HeaderProps) {
  const { theme, toggleTheme } = useTheme()
  const { user, logout } = useAuth()
  const navigate = useNavigate()

  const handleLogout = () => {
    logout()
    navigate('/login')
  }

  return (
    <header className="h-16 border-b border-[var(--border-color)] bg-[var(--bg-primary)] flex items-center px-4 gap-4 sticky top-0 z-20">
      <button className="btn-ghost p-2 lg:hidden" onClick={onMenuToggle}>
        <Menu className="w-5 h-5" />
      </button>

      <div className="flex items-center gap-2 lg:hidden">
        <div className="w-7 h-7 bg-primary-600 rounded-lg flex items-center justify-center">
          <ChefHat className="w-4 h-4 text-white" />
        </div>
        <span className="font-bold text-[var(--text-primary)]">SmartMeal</span>
      </div>

      {title && (
        <h1 className="hidden lg:block text-lg font-semibold text-[var(--text-primary)]">{title}</h1>
      )}

      <div className="ml-auto flex items-center gap-2">
        <button
          className="btn-ghost p-2"
          onClick={toggleTheme}
          title={theme === 'dark' ? 'Светлая тема' : 'Тёмная тема'}
        >
          {theme === 'dark' ? <Sun className="w-5 h-5" /> : <Moon className="w-5 h-5" />}
        </button>

        {user && (
          <div className="flex items-center gap-3 pl-2 border-l border-[var(--border-color)]">
            <div className="hidden sm:block text-right">
              <p className="text-sm font-medium text-[var(--text-primary)] leading-none">
                {user.first_name || user.username}
              </p>
              <p className="text-xs text-[var(--text-muted)] mt-0.5">Superuser</p>
            </div>
            <div className="w-8 h-8 rounded-full bg-primary-600 flex items-center justify-center text-white text-sm font-semibold">
              {(user.first_name || user.username).charAt(0).toUpperCase()}
            </div>
            <button className="btn-ghost p-2" onClick={handleLogout} title="Выйти">
              <LogOut className="w-4 h-4" />
            </button>
          </div>
        )}
      </div>
    </header>
  )
}
