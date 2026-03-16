import { useState } from 'react'
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { CalendarDays, Trash2, ChevronDown, ChevronUp } from 'lucide-react'
import { getMenus, deleteMenu } from '@/api/menus'
import { Pagination } from '@/components/ui/Pagination'
import { ConfirmDialog } from '@/components/ui/ConfirmDialog'
import { TableSkeleton } from '@/components/ui/Skeleton'
import { EmptyState } from '@/components/ui/EmptyState'
import { toast } from '@/components/ui/Toaster'
import { formatDate, formatDateTime } from '@/lib/utils'
import type { Menu } from '@/types'

const PAGE_SIZE = 20

const MEAL_TYPE_LABELS: Record<string, string> = {
  breakfast: 'Завтрак',
  lunch: 'Обед',
  dinner: 'Ужин',
  snack: 'Перекус',
  drink: 'Напиток',
}

const PERIOD_LABELS: Record<string, string> = {
  day: 'День',
  week: 'Неделя',
}

export function MenusPage() {
  const queryClient = useQueryClient()
  const [page, setPage] = useState(1)
  const [deleteTarget, setDeleteTarget] = useState<Menu | null>(null)
  const [expandedId, setExpandedId] = useState<number | null>(null)

  const { data, isLoading } = useQuery({
    queryKey: ['menus', { page }],
    queryFn: () => getMenus({ page }),
  })

  const deleteMutation = useMutation({
    mutationFn: (id: number) => deleteMenu(id),
    onSuccess: () => {
      toast.success('Меню удалено')
      setDeleteTarget(null)
      void queryClient.invalidateQueries({ queryKey: ['menus'] })
    },
    onError: () => toast.error('Не удалось удалить меню'),
  })

  const totalPages = data ? Math.ceil(data.count / PAGE_SIZE) : 1

  const toggleExpand = (id: number) => {
    setExpandedId(expandedId === id ? null : id)
  }

  return (
    <div className="space-y-5">
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-2xl font-bold text-[var(--text-primary)]">Меню пользователей</h1>
          <p className="text-sm text-[var(--text-secondary)] mt-0.5">
            {data ? `Всего: ${data.count}` : 'Загрузка...'}
          </p>
        </div>
      </div>

      <div className="card overflow-hidden">
        <div className="overflow-x-auto">
          <table className="w-full">
            <thead>
              <tr className="border-b border-[var(--border-color)]">
                <th className="table-header text-left">ID</th>
                <th className="table-header text-left">Пользователь</th>
                <th className="table-header text-center hidden sm:table-cell">Период</th>
                <th className="table-header text-center hidden md:table-cell">Дата начала</th>
                <th className="table-header text-center hidden md:table-cell">Создано</th>
                <th className="table-header text-center hidden lg:table-cell">Блюд</th>
                <th className="table-header text-right">Действия</th>
              </tr>
            </thead>
            <tbody>
              {isLoading ? (
                <tr>
                  <td colSpan={7}>
                    <TableSkeleton rows={8} cols={7} />
                  </td>
                </tr>
              ) : !data?.results?.length ? (
                <tr>
                  <td colSpan={7}>
                    <EmptyState
                      icon={CalendarDays}
                      title="Меню не найдены"
                      description="Пользователи ещё не создавали меню"
                    />
                  </td>
                </tr>
              ) : (
                data.results.map((menu) => (
                  <>
                    <tr
                      key={menu.id}
                      className="border-b border-[var(--border-color)] hover:bg-[var(--bg-secondary)] transition-colors"
                    >
                      <td className="table-cell text-[var(--text-muted)] w-14">#{menu.id}</td>
                      <td className="table-cell">
                        <div>
                          <p className="font-medium text-[var(--text-primary)]">{menu.user_email}</p>
                          <p className="text-xs text-[var(--text-muted)]">User #{menu.user}</p>
                        </div>
                      </td>
                      <td className="table-cell text-center hidden sm:table-cell">
                        <span className={menu.period === 'week' ? 'badge-blue' : 'badge-gray'}>
                          {PERIOD_LABELS[menu.period] ?? menu.period}
                        </span>
                      </td>
                      <td className="table-cell text-center hidden md:table-cell text-[var(--text-secondary)]">
                        {formatDate(menu.start_date)}
                      </td>
                      <td className="table-cell text-center hidden md:table-cell text-[var(--text-secondary)]">
                        {formatDateTime(menu.created_at)}
                      </td>
                      <td className="table-cell text-center hidden lg:table-cell">
                        <span className="font-medium">{menu.items?.length ?? 0}</span>
                      </td>
                      <td className="table-cell text-right">
                        <div className="flex items-center justify-end gap-1">
                          <button
                            className="btn-ghost p-2"
                            onClick={() => toggleExpand(menu.id)}
                            title="Показать блюда"
                          >
                            {expandedId === menu.id ? (
                              <ChevronUp className="w-4 h-4" />
                            ) : (
                              <ChevronDown className="w-4 h-4" />
                            )}
                          </button>
                          <button
                            className="btn-ghost p-2 text-red-500 hover:text-red-600"
                            onClick={() => setDeleteTarget(menu)}
                            title="Удалить"
                          >
                            <Trash2 className="w-4 h-4" />
                          </button>
                        </div>
                      </td>
                    </tr>
                    {expandedId === menu.id && menu.items?.length > 0 && (
                      <tr key={`${menu.id}-items`} className="border-b border-[var(--border-color)] bg-[var(--bg-secondary)]">
                        <td colSpan={7} className="px-6 py-3">
                          <p className="text-xs font-semibold uppercase tracking-wider text-[var(--text-muted)] mb-2">
                            Блюда в меню
                          </p>
                          <div className="grid grid-cols-1 sm:grid-cols-2 md:grid-cols-3 lg:grid-cols-4 gap-2">
                            {menu.items.map((item) => (
                              <div
                                key={item.id}
                                className="bg-[var(--bg-primary)] rounded-lg px-3 py-2 border border-[var(--border-color)]"
                              >
                                <p className="text-xs font-medium text-[var(--text-primary)] truncate">{item.recipe_title}</p>
                                <div className="flex items-center justify-between mt-1">
                                  <span className="text-xs text-[var(--text-muted)]">
                                    {MEAL_TYPE_LABELS[item.meal_type] ?? item.meal_type}
                                  </span>
                                  <span className="text-xs text-[var(--text-muted)]">
                                    {item.actual_date ? formatDate(item.actual_date) : `День +${item.day_offset}`}
                                  </span>
                                </div>
                              </div>
                            ))}
                          </div>
                        </td>
                      </tr>
                    )}
                  </>
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

      <ConfirmDialog
        open={!!deleteTarget}
        title="Удалить меню?"
        description={`Меню пользователя ${deleteTarget?.user_email} (${PERIOD_LABELS[deleteTarget?.period ?? ''] ?? deleteTarget?.period}, с ${deleteTarget ? formatDate(deleteTarget.start_date) : ''}) будет удалено безвозвратно.`}
        onConfirm={() => deleteTarget && deleteMutation.mutate(deleteTarget.id)}
        onCancel={() => setDeleteTarget(null)}
        loading={deleteMutation.isPending}
      />
    </div>
  )
}
