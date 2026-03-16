import { useState } from 'react'
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { Plus, Tag, Pencil, Trash2, Check, X } from 'lucide-react'
import { getCategories, createCategory, updateCategory, deleteCategory } from '@/api/ingredients'
import { ConfirmDialog } from '@/components/ui/ConfirmDialog'
import { TableSkeleton } from '@/components/ui/Skeleton'
import { EmptyState } from '@/components/ui/EmptyState'
import { toast } from '@/components/ui/Toaster'
import type { IngredientCategory } from '@/types'

export function CategoriesPage() {
  const queryClient = useQueryClient()
  const [editingId, setEditingId] = useState<number | null>(null)
  const [editingName, setEditingName] = useState('')
  const [newName, setNewName] = useState('')
  const [showNewForm, setShowNewForm] = useState(false)
  const [deleteTarget, setDeleteTarget] = useState<IngredientCategory | null>(null)

  const { data: categories, isLoading } = useQuery({
    queryKey: ['categories'],
    queryFn: getCategories,
  })

  const createMutation = useMutation({
    mutationFn: (name: string) => createCategory({ name }),
    onSuccess: () => {
      toast.success('Категория создана')
      setNewName('')
      setShowNewForm(false)
      void queryClient.invalidateQueries({ queryKey: ['categories'] })
    },
    onError: () => toast.error('Не удалось создать категорию'),
  })

  const updateMutation = useMutation({
    mutationFn: ({ id, name }: { id: number; name: string }) => updateCategory(id, { name }),
    onSuccess: () => {
      toast.success('Категория обновлена')
      setEditingId(null)
      void queryClient.invalidateQueries({ queryKey: ['categories'] })
    },
    onError: () => toast.error('Не удалось обновить категорию'),
  })

  const deleteMutation = useMutation({
    mutationFn: (id: number) => deleteCategory(id),
    onSuccess: () => {
      toast.success('Категория удалена')
      setDeleteTarget(null)
      void queryClient.invalidateQueries({ queryKey: ['categories'] })
    },
    onError: () => toast.error('Не удалось удалить категорию'),
  })

  const startEdit = (cat: IngredientCategory) => {
    setEditingId(cat.id)
    setEditingName(cat.name)
  }

  const cancelEdit = () => {
    setEditingId(null)
    setEditingName('')
  }

  const saveEdit = () => {
    if (!editingName.trim()) {
      toast.error('Введите название')
      return
    }
    updateMutation.mutate({ id: editingId!, name: editingName.trim() })
  }

  return (
    <div className="space-y-5 max-w-2xl">
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-2xl font-bold text-[var(--text-primary)]">Категории ингредиентов</h1>
          <p className="text-sm text-[var(--text-secondary)] mt-0.5">
            {categories ? `Всего: ${categories.length}` : 'Загрузка...'}
          </p>
        </div>
        <button className="btn-primary" onClick={() => setShowNewForm(true)}>
          <Plus className="w-4 h-4" />
          Добавить
        </button>
      </div>

      {showNewForm && (
        <div className="card p-4">
          <p className="text-sm font-medium text-[var(--text-primary)] mb-3">Новая категория</p>
          <div className="flex gap-2">
            <input
              className="input flex-1"
              placeholder="Название категории"
              value={newName}
              onChange={(e) => setNewName(e.target.value)}
              autoFocus
              onKeyDown={(e) => {
                if (e.key === 'Enter') createMutation.mutate(newName.trim())
                if (e.key === 'Escape') setShowNewForm(false)
              }}
            />
            <button
              className="btn-primary"
              onClick={() => newName.trim() && createMutation.mutate(newName.trim())}
              disabled={createMutation.isPending}
            >
              <Check className="w-4 h-4" />
            </button>
            <button className="btn-secondary" onClick={() => setShowNewForm(false)}>
              <X className="w-4 h-4" />
            </button>
          </div>
        </div>
      )}

      <div className="card overflow-hidden">
        <table className="w-full">
          <thead>
            <tr className="border-b border-[var(--border-color)]">
              <th className="table-header text-left">ID</th>
              <th className="table-header text-left">Название</th>
              <th className="table-header text-right">Действия</th>
            </tr>
          </thead>
          <tbody>
            {isLoading ? (
              <tr>
                <td colSpan={3}>
                  <TableSkeleton rows={6} cols={3} />
                </td>
              </tr>
            ) : !categories?.length ? (
              <tr>
                <td colSpan={3}>
                  <EmptyState
                    icon={Tag}
                    title="Нет категорий"
                    description="Добавьте первую категорию ингредиентов"
                    action={
                      <button className="btn-primary" onClick={() => setShowNewForm(true)}>
                        <Plus className="w-4 h-4" />
                        Добавить категорию
                      </button>
                    }
                  />
                </td>
              </tr>
            ) : (
              categories.map((cat) => (
                <tr key={cat.id} className="border-b border-[var(--border-color)] last:border-0 hover:bg-[var(--bg-secondary)] transition-colors">
                  <td className="table-cell text-[var(--text-muted)] w-16">#{cat.id}</td>
                  <td className="table-cell">
                    {editingId === cat.id ? (
                      <input
                        className="input py-1"
                        value={editingName}
                        onChange={(e) => setEditingName(e.target.value)}
                        autoFocus
                        onKeyDown={(e) => {
                          if (e.key === 'Enter') saveEdit()
                          if (e.key === 'Escape') cancelEdit()
                        }}
                      />
                    ) : (
                      <span className="font-medium">{cat.name}</span>
                    )}
                  </td>
                  <td className="table-cell text-right">
                    {editingId === cat.id ? (
                      <div className="flex items-center justify-end gap-1">
                        <button
                          className="btn-ghost p-1.5 text-primary-600"
                          onClick={saveEdit}
                          disabled={updateMutation.isPending}
                        >
                          <Check className="w-4 h-4" />
                        </button>
                        <button className="btn-ghost p-1.5" onClick={cancelEdit}>
                          <X className="w-4 h-4" />
                        </button>
                      </div>
                    ) : (
                      <div className="flex items-center justify-end gap-1">
                        <button className="btn-ghost p-2" onClick={() => startEdit(cat)}>
                          <Pencil className="w-4 h-4" />
                        </button>
                        <button
                          className="btn-ghost p-2 text-red-500 hover:text-red-600"
                          onClick={() => setDeleteTarget(cat)}
                        >
                          <Trash2 className="w-4 h-4" />
                        </button>
                      </div>
                    )}
                  </td>
                </tr>
              ))
            )}
          </tbody>
        </table>
      </div>

      <ConfirmDialog
        open={!!deleteTarget}
        title="Удалить категорию?"
        description={`Категория "${deleteTarget?.name}" будет удалена безвозвратно. Ингредиенты этой категории станут без категории.`}
        onConfirm={() => deleteTarget && deleteMutation.mutate(deleteTarget.id)}
        onCancel={() => setDeleteTarget(null)}
        loading={deleteMutation.isPending}
      />
    </div>
  )
}
