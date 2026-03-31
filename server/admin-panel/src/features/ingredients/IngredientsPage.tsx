import { useState } from 'react'
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { useNavigate } from 'react-router-dom'
import { Plus, Apple, Pencil, Trash2 } from 'lucide-react'
import { getIngredients, deleteIngredient, getCategories } from '@/api/ingredients'
import { SearchInput } from '@/components/ui/SearchInput'
import { Pagination } from '@/components/ui/Pagination'
import { ConfirmDialog } from '@/components/ui/ConfirmDialog'
import { TableSkeleton } from '@/components/ui/Skeleton'
import { EmptyState } from '@/components/ui/EmptyState'
import { toast } from '@/components/ui/Toaster'
import type { Ingredient } from '@/types'

const PAGE_SIZE = 20

export function IngredientsPage() {
  const navigate = useNavigate()
  const queryClient = useQueryClient()
  const [search, setSearch] = useState('')
  const [page, setPage] = useState(1)
  const [categoryFilter, setCategoryFilter] = useState<number | undefined>()
  const [deleteTarget, setDeleteTarget] = useState<Ingredient | null>(null)

  const { data, isLoading } = useQuery({
    queryKey: ['ingredients', { search, page, categoryFilter }],
    queryFn: () => getIngredients({ search, page, category: categoryFilter }),
  })

  const { data: categories } = useQuery({
    queryKey: ['categories'],
    queryFn: getCategories,
  })

  const deleteMutation = useMutation({
    mutationFn: (id: number) => deleteIngredient(id),
    onSuccess: () => {
      toast.success('Ингредиент удалён')
      setDeleteTarget(null)
      void queryClient.invalidateQueries({ queryKey: ['ingredients'] })
    },
    onError: () => {
      toast.error('Не удалось удалить ингредиент')
    },
  })

  const totalPages = data ? Math.ceil(data.count / PAGE_SIZE) : 1

  const handleSearch = (val: string) => {
    setSearch(val)
    setPage(1)
  }

  return (
    <div className="space-y-5">
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-2xl font-bold text-[var(--text-primary)]">Ингредиенты</h1>
          <p className="text-sm text-[var(--text-secondary)] mt-0.5">
            {data ? `Всего: ${data.count}` : 'Загрузка...'}
          </p>
        </div>
        <button className="btn-primary" onClick={() => navigate('/ingredients/new')}>
          <Plus className="w-4 h-4" />
          Добавить ингредиент
        </button>
      </div>

      <div className="flex flex-wrap gap-3">
        <SearchInput value={search} onChange={handleSearch} placeholder="Поиск ингредиентов..." />
        <select
          className="input w-auto"
          value={categoryFilter ?? ''}
          onChange={(e) => {
            setCategoryFilter(e.target.value ? Number(e.target.value) : undefined)
            setPage(1)
          }}
        >
          <option value="">Все категории</option>
          {categories?.map((cat) => (
            <option key={cat.id} value={cat.id}>{cat.name}</option>
          ))}
        </select>
      </div>

      <div className="card overflow-hidden">
        <div className="overflow-x-auto">
          <table className="w-full">
            <thead>
              <tr className="border-b border-[var(--border-color)]">
                <th className="table-header text-left">Название</th>
                <th className="table-header text-left hidden sm:table-cell">Категория</th>
                <th className="table-header text-center hidden md:table-cell">Калории</th>
                <th className="table-header text-center hidden md:table-cell">Белки/Жиры/Углеводы</th>
                <th className="table-header text-right">Действия</th>
              </tr>
            </thead>
            <tbody>
              {isLoading ? (
                <tr>
                  <td colSpan={5}>
                    <TableSkeleton rows={8} cols={5} />
                  </td>
                </tr>
              ) : !data?.results?.length ? (
                <tr>
                  <td colSpan={5}>
                    <EmptyState
                      icon={Apple}
                      title="Ингредиенты не найдены"
                      description="Попробуйте изменить фильтры или добавьте первый ингредиент"
                      action={
                        <button className="btn-primary" onClick={() => navigate('/ingredients/new')}>
                          <Plus className="w-4 h-4" />
                          Добавить ингредиент
                        </button>
                      }
                    />
                  </td>
                </tr>
              ) : (
                data.results.map((ingredient) => (
                  <tr
                    key={ingredient.id}
                    className="border-b border-[var(--border-color)] last:border-0 hover:bg-[var(--bg-secondary)] transition-colors"
                  >
                    <td className="table-cell">
                      <div className="flex items-center gap-3">
                        <div className="w-8 h-8 rounded-lg bg-green-100 dark:bg-green-900/30 flex items-center justify-center flex-shrink-0">
                          <Apple className="w-4 h-4 text-green-600" />
                        </div>
                        <div>
                          <p className="font-medium text-[var(--text-primary)]">{ingredient.name}</p>
                          <p className="text-xs text-[var(--text-muted)]">#{ingredient.id}</p>
                        </div>
                      </div>
                    </td>
                    <td className="table-cell hidden sm:table-cell">
                      {ingredient.category_name ? (
                        <span className="badge-blue">{ingredient.category_name}</span>
                      ) : (
                        <span className="badge-gray">Без категории</span>
                      )}
                    </td>
                    <td className="table-cell text-center hidden md:table-cell">
                      {ingredient.nutrition ? (
                        <span className="font-medium">{ingredient.nutrition.calories} ккал</span>
                      ) : (
                        <span className="text-[var(--text-muted)]">—</span>
                      )}
                    </td>
                    <td className="table-cell text-center hidden md:table-cell">
                      {ingredient.nutrition ? (
                        <span className="text-xs text-[var(--text-secondary)]">
                          {ingredient.nutrition.protein} / {ingredient.nutrition.fat} / {ingredient.nutrition.carbs}
                        </span>
                      ) : (
                        <span className="text-[var(--text-muted)]">—</span>
                      )}
                    </td>
                    <td className="table-cell text-right">
                      <div className="flex items-center justify-end gap-1">
                        <button
                          className="btn-ghost p-2"
                          onClick={() => navigate(`/ingredients/${ingredient.id}`)}
                          title="Редактировать"
                        >
                          <Pencil className="w-4 h-4" />
                        </button>
                        <button
                          className="btn-ghost p-2 text-red-500 hover:text-red-600"
                          onClick={() => setDeleteTarget(ingredient)}
                          title="Удалить"
                        >
                          <Trash2 className="w-4 h-4" />
                        </button>
                      </div>
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

      <ConfirmDialog
        open={!!deleteTarget}
        title="Удалить ингредиент?"
        description={`Ингредиент "${deleteTarget?.name}" будет удалён безвозвратно.`}
        onConfirm={() => deleteTarget && deleteMutation.mutate(deleteTarget.id)}
        onCancel={() => setDeleteTarget(null)}
        loading={deleteMutation.isPending}
      />
    </div>
  )
}
