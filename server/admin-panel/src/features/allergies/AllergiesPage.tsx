import { useState } from 'react'
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { Plus, ShieldAlert, Pencil, Trash2, Check, X } from 'lucide-react'
import { getAllergies, createAllergy, updateAllergy, deleteAllergy } from '@/api/users'
import { ConfirmDialog } from '@/components/ui/ConfirmDialog'
import { TableSkeleton } from '@/components/ui/Skeleton'
import { EmptyState } from '@/components/ui/EmptyState'
import { toast } from '@/components/ui/Toaster'
import type { Allergy } from '@/types'

export function AllergiesPage() {
  const queryClient = useQueryClient()
  const [editingId, setEditingId] = useState<number | null>(null)
  const [editingName, setEditingName] = useState('')
  const [newName, setNewName] = useState('')
  const [showNewForm, setShowNewForm] = useState(false)
  const [deleteTarget, setDeleteTarget] = useState<Allergy | null>(null)

  const { data: allergies, isLoading } = useQuery({
    queryKey: ['allergies'],
    queryFn: getAllergies,
  })

  const createMutation = useMutation({
    mutationFn: (name: string) => createAllergy({ name }),
    onSuccess: () => {
      toast.success('Аллерген создан')
      setNewName('')
      setShowNewForm(false)
      void queryClient.invalidateQueries({ queryKey: ['allergies'] })
    },
    onError: () => toast.error('Не удалось создать аллерген'),
  })

  const updateMutation = useMutation({
    mutationFn: ({ id, name }: { id: number; name: string }) => updateAllergy(id, { name }),
    onSuccess: () => {
      toast.success('Аллерген обновлён')
      setEditingId(null)
      void queryClient.invalidateQueries({ queryKey: ['allergies'] })
    },
    onError: () => toast.error('Не удалось обновить аллерген'),
  })

  const deleteMutation = useMutation({
    mutationFn: (id: number) => deleteAllergy(id),
    onSuccess: () => {
      toast.success('Аллерген удалён')
      setDeleteTarget(null)
      void queryClient.invalidateQueries({ queryKey: ['allergies'] })
    },
    onError: () => toast.error('Не удалось удалить аллерген'),
  })

  const startEdit = (allergy: Allergy) => {
    setEditingId(allergy.id)
    setEditingName(allergy.name)
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
          <h1 className="text-2xl font-bold text-[var(--text-primary)]">Аллергены</h1>
          <p className="text-sm text-[var(--text-secondary)] mt-0.5">
            {allergies ? `Всего: ${allergies.length}` : 'Загрузка...'}
          </p>
        </div>
        <button className="btn-primary" onClick={() => setShowNewForm(true)}>
          <Plus className="w-4 h-4" />
          Добавить
        </button>
      </div>

      {showNewForm && (
        <div className="card p-4">
          <p className="text-sm font-medium text-[var(--text-primary)] mb-3">Новый аллерген</p>
          <div className="flex gap-2">
            <input
              className="input flex-1"
              placeholder="Название (например: Глютен, Лактоза, Орехи...)"
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
                  <TableSkeleton rows={5} cols={3} />
                </td>
              </tr>
            ) : !allergies?.length ? (
              <tr>
                <td colSpan={3}>
                  <EmptyState
                    icon={ShieldAlert}
                    title="Нет аллергенов"
                    description="Добавьте аллергены (Глютен, Лактоза, Орехи, Рыба...)"
                    action={
                      <button className="btn-primary" onClick={() => setShowNewForm(true)}>
                        <Plus className="w-4 h-4" />
                        Добавить аллерген
                      </button>
                    }
                  />
                </td>
              </tr>
            ) : (
              allergies.map((allergy) => (
                <tr
                  key={allergy.id}
                  className="border-b border-[var(--border-color)] last:border-0 hover:bg-[var(--bg-secondary)] transition-colors"
                >
                  <td className="table-cell text-[var(--text-muted)] w-16">#{allergy.id}</td>
                  <td className="table-cell">
                    {editingId === allergy.id ? (
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
                        <ShieldAlert className="w-4 h-4 text-red-500 flex-shrink-0" />
                        <span className="font-medium">{allergy.name}</span>
                      </div>
                    )}
                  </td>
                  <td className="table-cell text-right">
                    {editingId === allergy.id ? (
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
                        <button className="btn-ghost p-2" onClick={() => startEdit(allergy)}>
                          <Pencil className="w-4 h-4" />
                        </button>
                        <button
                          className="btn-ghost p-2 text-red-500 hover:text-red-600"
                          onClick={() => setDeleteTarget(allergy)}
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
        title="Удалить аллерген?"
        description={`Аллерген "${deleteTarget?.name}" будет удалён. Пользователи с этой аллергией потеряют привязку.`}
        onConfirm={() => deleteTarget && deleteMutation.mutate(deleteTarget.id)}
        onCancel={() => setDeleteTarget(null)}
        loading={deleteMutation.isPending}
      />
    </div>
  )
}
