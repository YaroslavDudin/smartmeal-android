import { useState } from 'react'
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { Plus, Leaf, Pencil, Trash2, Check, X } from 'lucide-react'
import { getDietTypes, createDietType, updateDietType, deleteDietType } from '@/api/users'
import { ConfirmDialog } from '@/components/ui/ConfirmDialog'
import { TableSkeleton } from '@/components/ui/Skeleton'
import { EmptyState } from '@/components/ui/EmptyState'
import { toast } from '@/components/ui/Toaster'
import type { DietType } from '@/types'

export function DietTypesPage() {
  const queryClient = useQueryClient()
  const [editingId, setEditingId] = useState<number | null>(null)
  const [editingName, setEditingName] = useState('')
  const [newName, setNewName] = useState('')
  const [showNewForm, setShowNewForm] = useState(false)
  const [deleteTarget, setDeleteTarget] = useState<DietType | null>(null)

  const { data: dietTypes, isLoading } = useQuery({
    queryKey: ['diet-types'],
    queryFn: getDietTypes,
  })

  const createMutation = useMutation({
    mutationFn: (name: string) => createDietType({ name }),
    onSuccess: () => {
      toast.success('Тип диеты создан')
      setNewName('')
      setShowNewForm(false)
      void queryClient.invalidateQueries({ queryKey: ['diet-types'] })
    },
    onError: () => toast.error('Не удалось создать тип диеты'),
  })

  const updateMutation = useMutation({
    mutationFn: ({ id, name }: { id: number; name: string }) => updateDietType(id, { name }),
    onSuccess: () => {
      toast.success('Тип диеты обновлён')
      setEditingId(null)
      void queryClient.invalidateQueries({ queryKey: ['diet-types'] })
    },
    onError: () => toast.error('Не удалось обновить тип диеты'),
  })

  const deleteMutation = useMutation({
    mutationFn: (id: number) => deleteDietType(id),
    onSuccess: () => {
      toast.success('Тип диеты удалён')
      setDeleteTarget(null)
      void queryClient.invalidateQueries({ queryKey: ['diet-types'] })
    },
    onError: () => toast.error('Не удалось удалить тип диеты'),
  })

  const startEdit = (dt: DietType) => {
    setEditingId(dt.id)
    setEditingName(dt.name)
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
          <h1 className="text-2xl font-bold text-[var(--text-primary)]">Типы диет</h1>
          <p className="text-sm text-[var(--text-secondary)] mt-0.5">
            {dietTypes ? `Всего: ${dietTypes.length}` : 'Загрузка...'}
          </p>
        </div>
        <button className="btn-primary" onClick={() => setShowNewForm(true)}>
          <Plus className="w-4 h-4" />
          Добавить
        </button>
      </div>

      {showNewForm && (
        <div className="card p-4">
          <p className="text-sm font-medium text-[var(--text-primary)] mb-3">Новый тип диеты</p>
          <div className="flex gap-2">
            <input
              className="input flex-1"
              placeholder="Название (например: Веганская, Кето...)"
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
              <th className="table-header text-left">#</th>
              <th className="table-header text-left">Название</th>
              <th className="table-header text-right">Действия</th>
            </tr>
          </thead>
          <tbody>
            {isLoading ? (
              <tr>
                <td colSpan={3}>
                  <TableSkeleton rows={5} cols={3} />
                </td>
              </tr>
            ) : !dietTypes?.length ? (
              <tr>
                <td colSpan={3}>
                  <EmptyState
                    icon={Leaf}
                    title="Нет типов диет"
                    description="Добавьте первый тип диеты (Веганская, Кето, Безглютеновая...)"
                    action={
                      <button className="btn-primary" onClick={() => setShowNewForm(true)}>
                        <Plus className="w-4 h-4" />
                        Добавить тип диеты
                      </button>
                    }
                  />
                </td>
              </tr>
            ) : (
              dietTypes.map((dt, index) => (
                <tr
                  key={dt.id}
                  className="border-b border-[var(--border-color)] last:border-0 hover:bg-[var(--bg-secondary)] transition-colors"
                >
                  <td className="table-cell text-[var(--text-muted)] w-16">{index + 1}</td>
                  <td className="table-cell">
                    {editingId === dt.id ? (
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
                      <div className="flex items-center gap-2">
                        <Leaf className="w-4 h-4 text-primary-600 flex-shrink-0" />
                        <span className="font-medium">{dt.name}</span>
                      </div>
                    )}
                  </td>
                  <td className="table-cell text-right">
                    {editingId === dt.id ? (
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
                        <button className="btn-ghost p-2" onClick={() => startEdit(dt)}>
                          <Pencil className="w-4 h-4" />
                        </button>
                        <button
                          className="btn-ghost p-2 text-red-500 hover:text-red-600"
                          onClick={() => setDeleteTarget(dt)}
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
        title="Удалить тип диеты?"
        description={`Тип диеты "${deleteTarget?.name}" будет удалён. Пользователи с этим типом диеты потеряют привязку.`}
        onConfirm={() => deleteTarget && deleteMutation.mutate(deleteTarget.id)}
        onCancel={() => setDeleteTarget(null)}
        loading={deleteMutation.isPending}
      />
    </div>
  )
}
