import { useQuery } from '@tanstack/react-query'
import { getStats } from '@/api/stats'
import { ChefHat, Apple, Users, CalendarDays, TrendingUp, Clock } from 'lucide-react'
import { Skeleton } from '@/components/ui/Skeleton'
import { useNavigate } from 'react-router-dom'
import { formatDate, truncate } from '@/lib/utils'
import type { LucideIcon } from 'lucide-react'

function StatCard({ icon: Icon, label, value, color, loading }: {
  icon: LucideIcon
  label: string
  value: number | string
  color: string
  loading?: boolean
}) {
  return (
    <div className="card p-5 flex items-center gap-4">
      <div className={`w-12 h-12 rounded-xl flex items-center justify-center flex-shrink-0 ${color}`}>
        <Icon className="w-6 h-6 text-white" />
      </div>
      <div>
        <p className="text-sm text-[var(--text-secondary)]">{label}</p>
        {loading ? (
          <Skeleton className="h-7 w-16 mt-1" />
        ) : (
          <p className="text-2xl font-bold text-[var(--text-primary)]">{value}</p>
        )}
      </div>
    </div>
  )
}

export function DashboardPage() {
  const navigate = useNavigate()
  const { data: stats, isLoading } = useQuery({
    queryKey: ['stats'],
    queryFn: getStats,
  })

  return (
    <div className="space-y-6">
      <div>
        <h1 className="text-2xl font-bold text-[var(--text-primary)]">Дашборд</h1>
        <p className="text-[var(--text-secondary)] text-sm mt-1">Обзор данных приложения</p>
      </div>

      <div className="grid grid-cols-1 sm:grid-cols-2 xl:grid-cols-4 gap-4">
        <StatCard icon={ChefHat} label="Рецепты" value={stats?.recipes_count ?? 0} color="bg-primary-600" loading={isLoading} />
        <StatCard icon={Apple} label="Ингредиенты" value={stats?.ingredients_count ?? 0} color="bg-blue-500" loading={isLoading} />
        <StatCard icon={Users} label="Пользователи" value={stats?.users_count ?? 0} color="bg-violet-500" loading={isLoading} />
        <StatCard icon={CalendarDays} label="Меню" value={stats?.menus_count ?? 0} color="bg-amber-500" loading={isLoading} />
      </div>

      <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
        <div className="card">
          <div className="flex items-center justify-between px-5 py-4 border-b border-[var(--border-color)]">
            <h2 className="font-semibold text-[var(--text-primary)] flex items-center gap-2">
              <TrendingUp className="w-4 h-4 text-primary-600" />
              Последние рецепты
            </h2>
            <button className="text-sm text-primary-600 hover:text-primary-700 font-medium" onClick={() => navigate('/recipes')}>
              Все рецепты
            </button>
          </div>
          <div>
            {isLoading ? (
              <div className="p-4 space-y-3">
                {[...Array(5)].map((_, i) => <Skeleton key={i} className="h-12 w-full" />)}
              </div>
            ) : stats?.recent_recipes?.length ? (
              stats.recent_recipes.map((recipe) => (
                <div
                  key={recipe.id}
                  className="flex items-center gap-3 px-5 py-3 hover:bg-[var(--bg-secondary)] cursor-pointer border-b border-[var(--border-color)] last:border-0 transition-colors"
                  onClick={() => navigate(`/recipes/${recipe.id}`)}
                >
                  {recipe.image_url ? (
                    <img src={recipe.image_url} alt={recipe.title} className="w-10 h-10 rounded-lg object-cover flex-shrink-0" />
                  ) : (
                    <div className="w-10 h-10 rounded-lg bg-[var(--bg-tertiary)] flex items-center justify-center flex-shrink-0">
                      <ChefHat className="w-5 h-5 text-[var(--text-muted)]" />
                    </div>
                  )}
                  <div className="flex-1 min-w-0">
                    <p className="text-sm font-medium text-[var(--text-primary)] truncate">{recipe.title}</p>
                    <p className="text-xs text-[var(--text-secondary)] flex items-center gap-1">
                      <Clock className="w-3 h-3" />
                      {recipe.cook_time} мин · {recipe.total_calories} ккал
                    </p>
                  </div>
                </div>
              ))
            ) : (
              <p className="text-center text-[var(--text-muted)] py-8 text-sm">Нет рецептов</p>
            )}
          </div>
        </div>

        <div className="card">
          <div className="flex items-center justify-between px-5 py-4 border-b border-[var(--border-color)]">
            <h2 className="font-semibold text-[var(--text-primary)] flex items-center gap-2">
              <Users className="w-4 h-4 text-violet-500" />
              Последние пользователи
            </h2>
            <button className="text-sm text-primary-600 hover:text-primary-700 font-medium" onClick={() => navigate('/users')}>
              Все пользователи
            </button>
          </div>
          <div>
            {isLoading ? (
              <div className="p-4 space-y-3">
                {[...Array(5)].map((_, i) => <Skeleton key={i} className="h-12 w-full" />)}
              </div>
            ) : stats?.recent_users?.length ? (
              stats.recent_users.map((user) => (
                <div
                  key={user.id}
                  className="flex items-center gap-3 px-5 py-3 hover:bg-[var(--bg-secondary)] cursor-pointer border-b border-[var(--border-color)] last:border-0 transition-colors"
                  onClick={() => navigate(`/users/${user.id}`)}
                >
                  <div className="w-10 h-10 rounded-full bg-violet-500 flex items-center justify-center text-white text-sm font-semibold flex-shrink-0">
                    {(user.first_name || user.username || user.email).charAt(0).toUpperCase()}
                  </div>
                  <div className="flex-1 min-w-0">
                    <p className="text-sm font-medium text-[var(--text-primary)] truncate">
                      {user.first_name ? `${user.first_name} ${user.last_name}` : user.username || truncate(user.email, 20)}
                    </p>
                    <p className="text-xs text-[var(--text-secondary)] truncate">{user.email}</p>
                  </div>
                  <p className="text-xs text-[var(--text-muted)] flex-shrink-0">{formatDate(user.date_joined)}</p>
                </div>
              ))
            ) : (
              <p className="text-center text-[var(--text-muted)] py-8 text-sm">Нет пользователей</p>
            )}
          </div>
        </div>
      </div>
    </div>
  )
}
