import { useState } from 'react'
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { useNavigate } from 'react-router-dom'
import { Plus, ChefHat, Clock, Flame, Pencil, Trash2, Eye } from 'lucide-react'
import { getRecipes, deleteRecipe, getRecipe } from '@/api/recipes'
import { getDietTypes } from '@/api/users'
import { SearchInput } from '@/components/ui/SearchInput'
import { Pagination } from '@/components/ui/Pagination'
import { ConfirmDialog } from '@/components/ui/ConfirmDialog'
import { TableSkeleton } from '@/components/ui/Skeleton'
import { EmptyState } from '@/components/ui/EmptyState'
import { toast } from '@/components/ui/Toaster'
import { RecipePreviewModal } from '@/components/ui/RecipePreviewModal'
import { truncate } from '@/lib/utils'
import type { RecipeShort } from '@/types'

const PAGE_SIZE = 20

export function RecipesPage() {
  const navigate = useNavigate()
  const queryClient = useQueryClient()
  const [search, setSearch] = useState('')
  const [page, setPage] = useState(1)
  const [dietFilter, setDietFilter] = useState<number | undefined>()
  const [minCalories, setMinCalories] = useState<number | undefined>()
  const [maxCalories, setMaxCalories] = useState<number | undefined>()
  const [deleteTarget, setDeleteTarget] = useState<RecipeShort | null>(null)
  const [previewId, setPreviewId] = useState<number | null>(null)

  const { data: previewRecipe, isFetching: previewLoading } = useQuery({
    queryKey: ['recipe', previewId],
    queryFn: () => getRecipe(previewId!),
    enabled: !!previewId,
    staleTime: 60_000,
  })

  const { data, isLoading } = useQuery({
    queryKey: ['recipes', { search, page, dietFilter, minCalories, maxCalories }],
    queryFn: () => getRecipes({ 
      search, 
      page, 
      diet_type: dietFilter, 
      min_calories: minCalories,
      max_calories: maxCalories,
      page_size: PAGE_SIZE 
    }),
  })

  const { data: dietTypes } = useQuery({
    queryKey: ['diet-types'],
    queryFn: getDietTypes,
  })

  const deleteMutation = useMutation({
    mutationFn: (id: number) => deleteRecipe(id),
    onSuccess: () => {
      toast.success('Рецепт удалён')
      setDeleteTarget(null)
      void queryClient.invalidateQueries({ queryKey: ['recipes'] })
    },
    onError: () => {
      toast.error('Не удалось удалить рецепт')
    },
  })

  const totalPages = data ? Math.ceil(data.count / PAGE_SIZE) : 1

  const handleSearch = (val: string) => {
    setSearch(val)
    setPage(1)
  }

  const handleDietFilter = (val: string) => {
    setDietFilter(val ? Number(val) : undefined)
    setPage(1)
  }

  return (
    <div className="space-y-5">
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-2xl font-bold text-[var(--text-primary)]">Рецепты</h1>
          <p className="text-sm text-[var(--text-secondary)] mt-0.5">
            {data ? `Всего: ${data.count}` : 'Загрузка...'}
          </p>
        </div>
        <button className="btn-primary" onClick={() => navigate('/recipes/new')}>
          <Plus className="w-4 h-4" />
          Добавить рецепт
        </button>
      </div>

      <div className="flex flex-wrap gap-3">
        <SearchInput value={search} onChange={handleSearch} placeholder="Поиск рецептов..." />
        <select
          className="input w-auto"
          value={dietFilter ?? ''}
          onChange={(e) => handleDietFilter(e.target.value)}
        >
          <option value="">Все типы диет</option>
          {dietTypes?.map((dt) => (
            <option key={dt.id} value={dt.id}>{dt.name}</option>
          ))}
        </select>
        <div className="flex items-center gap-2">
          <input
            type="number"
            placeholder="Мин. ккал"
            className="input w-24"
            value={minCalories ?? ''}
            onChange={(e) => {
              setMinCalories(e.target.value ? Number(e.target.value) : undefined)
              setPage(1)
            }}
          />
          <span className="text-[var(--text-muted)]">—</span>
          <input
            type="number"
            placeholder="Макс. ккал"
            className="input w-24"
            value={maxCalories ?? ''}
            onChange={(e) => {
              setMaxCalories(e.target.value ? Number(e.target.value) : undefined)
              setPage(1)
            }}
          />
        </div>
        {(search || dietFilter || minCalories || maxCalories) && (
          <button 
            className="btn-ghost text-xs"
            onClick={() => {
              setSearch('')
              setDietFilter(undefined)
              setMinCalories(undefined)
              setMaxCalories(undefined)
              setPage(1)
            }}
          >
            Сбросить
          </button>
        )}
      </div>

      <div className="card overflow-hidden">
        <div className="overflow-x-auto">
          <table className="w-full">
            <thead>
              <tr className="border-b border-[var(--border-color)]">
                <th className="table-header text-left">Рецепт</th>
                <th className="table-header text-left hidden sm:table-cell">Диеты</th>
                <th className="table-header text-center hidden md:table-cell">Время</th>
                <th className="table-header text-center hidden md:table-cell">Калории</th>
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
                      icon={ChefHat}
                      title="Рецепты не найдены"
                      description="Попробуйте изменить фильтры или добавьте первый рецепт"
                      action={
                        <button className="btn-primary" onClick={() => navigate('/recipes/new')}>
                          <Plus className="w-4 h-4" />
                          Добавить рецепт
                        </button>
                      }
                    />
                  </td>
                </tr>
              ) : (
                data.results.map((recipe, index) => (
                  <tr
                    key={recipe.id}
                    className="border-b border-[var(--border-color)] last:border-0 hover:bg-[var(--bg-secondary)] transition-colors"
                  >
                    <td className="table-cell text-[var(--text-muted)] text-sm">
                      {(page - 1) * PAGE_SIZE + index + 1}
                    </td>
                    <td className="table-cell">
                      <div className="flex items-center gap-3">
                        <button
                          className="flex-shrink-0 focus:outline-none"
                          onClick={() => setPreviewId(recipe.id)}
                          title="Предпросмотр"
                        >
                          {recipe.image_url ? (
                            <img
                              src={recipe.image_url}
                              alt={recipe.title}
                              className="w-10 h-10 rounded-lg object-cover hover:opacity-80 transition-opacity"
                            />
                          ) : (
                            <div className="w-10 h-10 rounded-lg bg-[var(--bg-tertiary)] flex items-center justify-center hover:opacity-80 transition-opacity">
                              <ChefHat className="w-5 h-5 text-[var(--text-muted)]" />
                            </div>
                          )}
                        </button>
                        <div>
                          <button
                            className="font-medium text-[var(--text-primary)] hover:text-primary-500 transition-colors text-left"
                            onClick={() => setPreviewId(recipe.id)}
                          >
                            {truncate(recipe.title, 40)}
                          </button>
                          <p className="text-xs text-[var(--text-muted)] text-[10px] uppercase tracking-wider">ID {recipe.id}</p>
                        </div>
                      </div>
                    </td>
                    <td className="table-cell hidden sm:table-cell">
                      <div className="flex flex-wrap gap-1">
                        {recipe.diet_type_names?.length ? (
                          recipe.diet_type_names.map((name) => (
                            <span key={name} className="badge-green">{name}</span>
                          ))
                        ) : (
                          <span className="badge-gray">Не указано</span>
                        )}
                      </div>
                    </td>
                    <td className="table-cell text-center hidden md:table-cell">
                      <span className="flex items-center justify-center gap-1 text-[var(--text-secondary)]">
                        <Clock className="w-3.5 h-3.5" />
                        {recipe.cook_time} мин
                      </span>
                    </td>
                    <td className="table-cell text-center hidden md:table-cell">
                      <span className="flex items-center justify-center gap-1 text-[var(--text-secondary)]">
                        <Flame className="w-3.5 h-3.5 text-orange-500" />
                        {recipe.total_calories}
                      </span>
                    </td>
                    <td className="table-cell text-right">
                      <div className="flex items-center justify-end gap-1">
                        <button
                          className="btn-ghost p-2 text-[var(--text-muted)]"
                          onClick={() => setPreviewId(recipe.id)}
                          title="Предпросмотр"
                        >
                          <Eye className="w-4 h-4" />
                        </button>
                        <button
                          className="btn-ghost p-2"
                          onClick={() => navigate(`/recipes/${recipe.id}`)}
                          title="Редактировать"
                        >
                          <Pencil className="w-4 h-4" />
                        </button>
                        <button
                          className="btn-ghost p-2 text-red-500 hover:text-red-600"
                          onClick={() => setDeleteTarget(recipe)}
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
        title="Удалить рецепт?"
        description={`Рецепт "${deleteTarget?.title}" будет удалён безвозвратно.`}
        onConfirm={() => deleteTarget && deleteMutation.mutate(deleteTarget.id)}
        onCancel={() => setDeleteTarget(null)}
        loading={deleteMutation.isPending}
      />

      <RecipePreviewModal
        open={!!previewId}
        onClose={() => setPreviewId(null)}
        loading={previewLoading || !previewRecipe}
        data={previewRecipe ? {
          title: previewRecipe.title,
          image_url: previewRecipe.image_url,
          servings: previewRecipe.servings,
          cook_time: previewRecipe.cook_time,
          total_calories: previewRecipe.total_calories,
          total_proteins: previewRecipe.total_proteins,
          total_fats: previewRecipe.total_fats,
          total_carbs: previewRecipe.total_carbs,
          ingredients: previewRecipe.ingredients,
          steps: previewRecipe.steps,
        } : undefined}
      />
    </div>
  )
}
