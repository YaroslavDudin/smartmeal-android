import { NavLink } from 'react-router-dom'
import {
  LayoutDashboard,
  ChefHat,
  Apple,
  Tag,
  Ruler,
  Users,
  Leaf,
  ShieldAlert,
  CalendarDays,
  X,
} from 'lucide-react'
import { cn } from '@/lib/utils'

interface SidebarProps {
  isOpen: boolean
  onClose: () => void
}

const navItems = [
  { to: '/dashboard', icon: LayoutDashboard, label: 'Дашборд' },
  { to: '/recipes', icon: ChefHat, label: 'Рецепты' },
  { to: '/ingredients', icon: Apple, label: 'Ингредиенты' },
  { to: '/categories', icon: Tag, label: 'Категории' },
  { to: '/units', icon: Ruler, label: 'Единицы' },
  { to: '/users', icon: Users, label: 'Пользователи' },
  { to: '/diet-types', icon: Leaf, label: 'Типы диет' },
  { to: '/allergies', icon: ShieldAlert, label: 'Аллергены' },
  { to: '/menus', icon: CalendarDays, label: 'Меню' },
]

export function Sidebar({ isOpen, onClose }: SidebarProps) {
  return (
    <>
      {isOpen && (
        <div
          className="fixed inset-0 bg-black/50 z-30 lg:hidden"
          onClick={onClose}
        />
      )}

      <aside
        className={cn(
          'fixed top-0 left-0 h-full w-64 bg-[var(--sidebar-bg)] flex flex-col z-40 transition-transform duration-300',
          'lg:translate-x-0 lg:static lg:z-auto',
          isOpen ? 'translate-x-0' : '-translate-x-full',
        )}
      >
        <div className="h-16 flex items-center px-5 gap-3 border-b border-white/10">
          <div className="w-8 h-8 bg-primary-600 rounded-lg flex items-center justify-center flex-shrink-0">
            <ChefHat className="w-5 h-5 text-white" />
          </div>
          <div>
            <p className="text-white font-bold text-sm leading-none">SmartMeal</p>
            <p className="text-[var(--sidebar-text)] text-xs mt-0.5 opacity-70">Admin Panel</p>
          </div>
          <button
            className="ml-auto text-[var(--sidebar-text)] hover:text-white lg:hidden"
            onClick={onClose}
          >
            <X className="w-5 h-5" />
          </button>
        </div>

        <nav className="flex-1 overflow-y-auto py-4 px-3">
          <p className="text-xs font-semibold uppercase tracking-widest text-[var(--sidebar-text)] opacity-40 px-2 mb-2">
            Навигация
          </p>
          {navItems.map(({ to, icon: Icon, label }) => (
            <NavLink
              key={to}
              to={to}
              onClick={onClose}
              className={({ isActive }) =>
                cn(
                  'flex items-center gap-3 px-3 py-2.5 rounded-lg text-sm font-medium transition-colors mb-0.5',
                  isActive
                    ? 'bg-primary-600 text-white'
                    : 'text-[var(--sidebar-text)] hover:bg-[var(--sidebar-hover)] hover:text-white',
                )
              }
            >
              <Icon className="w-4 h-4 flex-shrink-0" />
              {label}
            </NavLink>
          ))}
        </nav>

        <div className="p-4 border-t border-white/10">
          <p className="text-xs text-[var(--sidebar-text)] opacity-40 text-center">
            SmartMeal Admin v1.0
          </p>
        </div>
      </aside>
    </>
  )
}
