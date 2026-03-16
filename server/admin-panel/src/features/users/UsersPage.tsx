import { useState } from 'react'
import { useQuery } from '@tanstack/react-query'
import { useNavigate } from 'react-router-dom'
import { Users, Eye } from 'lucide-react'
import { getUsers } from '@/api/users'
import { SearchInput } from '@/components/ui/SearchInput'
import { Pagination } from '@/components/ui/Pagination'
import { TableSkeleton } from '@/components/ui/Skeleton'
import { EmptyState } from '@/components/ui/EmptyState'
import { formatDate, truncate } from '@/lib/utils'

const PAGE_SIZE = 20

export function UsersPage() {
  const navigate = useNavigate()
  const [search, setSearch] = useState('')
  const [page, setPage] = useState(1)

  const { data, isLoading } = useQuery({
    queryKey: ['users', { search, page }],
    queryFn: () => getUsers({ search, page }),
  })

  const totalPages = data ? Math.ceil(data.count / PAGE_SIZE) : 1

  const handleSearch = (val: string) => {
    setSearch(val)
    setPage(1)
  }

  const getUserDisplayName = (user: { first_name: string; last_name: string; username: string; email: string }) => {
    if (user.first_name || user.last_name) {
      return `${user.first_name} ${user.last_name}`.trim()
    }
    return user.username || truncate(user.email, 25)
  }

  return (
    <div className="space-y-5">
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-2xl font-bold text-[var(--text-primary)]">Пользователи</h1>
          <p className="text-sm text-[var(--text-secondary)] mt-0.5">
            {data ? `Всего: ${data.count}` : 'Загрузка...'}
          </p>
        </div>
      </div>

      <div className="flex flex-wrap gap-3">
        <SearchInput value={search} onChange={handleSearch} placeholder="Поиск пользователей..." />
      </div>

      <div className="card overflow-hidden">
        <div className="overflow-x-auto">
          <table className="w-full">
            <thead>
              <tr className="border-b border-[var(--border-color)]">
                <th className="table-header text-left">Пользователь</th>
                <th className="table-header text-left hidden sm:table-cell">Email</th>
                <th className="table-header text-center hidden md:table-cell">Диета</th>
                <th className="table-header text-center hidden lg:table-cell">Статус</th>
                <th className="table-header text-center hidden lg:table-cell">Дата регистрации</th>
                <th className="table-header text-right">Действия</th>
              </tr>
            </thead>
            <tbody>
              {isLoading ? (
                <tr>
                  <td colSpan={6}>
                    <TableSkeleton rows={8} cols={6} />
                  </td>
                </tr>
              ) : !data?.results?.length ? (
                <tr>
                  <td colSpan={6}>
                    <EmptyState
                      icon={Users}
                      title="Пользователи не найдены"
                      description="Попробуйте изменить параметры поиска"
                    />
                  </td>
                </tr>
              ) : (
                data.results.map((user) => (
                  <tr
                    key={user.id}
                    className="border-b border-[var(--border-color)] last:border-0 hover:bg-[var(--bg-secondary)] transition-colors cursor-pointer"
                    onClick={() => navigate(`/users/${user.id}`)}
                  >
                    <td className="table-cell">
                      <div className="flex items-center gap-3">
                        <div className="w-9 h-9 rounded-full bg-violet-500 flex items-center justify-center text-white text-sm font-semibold flex-shrink-0">
                          {(user.first_name || user.username || user.email).charAt(0).toUpperCase()}
                        </div>
                        <div>
                          <p className="font-medium text-[var(--text-primary)]">
                            {getUserDisplayName(user)}
                          </p>
                          {user.is_superuser && (
                            <span className="badge-blue text-xs">Superuser</span>
                          )}
                        </div>
                      </div>
                    </td>
                    <td className="table-cell hidden sm:table-cell text-[var(--text-secondary)]">
                      {user.email}
                    </td>
                    <td className="table-cell text-center hidden md:table-cell">
                      {user.diet_type_name ? (
                        <span className="badge-green">{user.diet_type_name}</span>
                      ) : (
                        <span className="badge-gray">Не указано</span>
                      )}
                    </td>
                    <td className="table-cell text-center hidden lg:table-cell">
                      {user.is_active ? (
                        <span className="badge-green">Активен</span>
                      ) : (
                        <span className="badge-red">Неактивен</span>
                      )}
                    </td>
                    <td className="table-cell text-center hidden lg:table-cell text-[var(--text-secondary)]">
                      {formatDate(user.date_joined)}
                    </td>
                    <td className="table-cell text-right" onClick={(e) => e.stopPropagation()}>
                      <button
                        className="btn-ghost p-2"
                        onClick={() => navigate(`/users/${user.id}`)}
                        title="Просмотр"
                      >
                        <Eye className="w-4 h-4" />
                      </button>
                    </td>
                  </tr>
                ))
              )}
            </tbody>
          </table>
        </div>

        {data && data.count > PAGE_SIZE && (
          <div className="px-4 py-3 border-t border-[var(--border-color)] flex justify-between items-center">
            <p className="text-xs text-[var(--text-muted)]">
              Показано {Math.min((page - 1) * PAGE_SIZE + 1, data.count)}–{Math.min(page * PAGE_SIZE, data.count)} из {data.count}
            </p>
            <Pagination page={page} totalPages={totalPages} onPageChange={setPage} />
          </div>
        )}
      </div>
    </div>
  )
}
