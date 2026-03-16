import { useState } from 'react'
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { Plus, Ruler, Pencil, Trash2, Check, X } from 'lucide-react'
import { getUnits, createUnit, updateUnit, deleteUnit } from '@/api/ingredients'
import { ConfirmDialog } from '@/components/ui/ConfirmDialog'
import { TableSkeleton } from '@/components/ui/Skeleton'
import { EmptyState } from '@/components/ui/EmptyState'
import { toast } from '@/components/ui/Toaster'
import type { Unit } from '@/types'

export function UnitsPage() {
  const queryClient = useQueryClient()
  const [editingId, setEditingId] = useState<number | null>(null)
  const [editingName, setEditingName] = useState('')
  const [editingIsBase, setEditingIsBase] = useState(false)
  const [newName, setNewName] = useState('')
  const [newIsBase, setNewIsBase] = useState(false)
  const [showNewForm, setShowNewForm] = useState(false)
  const [deleteTarget, setDeleteTarget] = useState<Unit | null>(null)

  const { data: units, isLoading } = useQuery({
    queryKey: ['units'],
    queryFn: getUnits,
  })

  const createMutation = useMutation({
    mutationFn: () => createUnit({ name: newName.trim(), is_base: newIsBase }),
    onSuccess: () => {
      toast.success('Единица создана')
      setNewName('')
      setNewIsBase(false)
      setShowNewForm(false)
      void queryClient.invalidateQueries({ queryKey: ['units'] })
    },
    onError: () => toast.error('Не удалось создать единицу'),
  })

  const updateMutation = useMutation({
    mutationFn: ({ id, name, is_base }: { id: number; name: string; is_base: boolean }) =>
      updateUnit(id, { name, is_base }),
    onSuccess: () => {
      toast.success('Единица обновлена')
      setEditingId(null)
      void queryClient.invalidateQueries({ queryKey: ['units'] })
    },
    onError: () => toast.error('Не удалось обновить единицу'),
  })

  const deleteMutation = useMutation({
    mutationFn: (id: number) => deleteUnit(id),
    onSuccess: () => {
      toast.success('Единица удалена')
      setDeleteTarget(null)
      void queryClient.invalidateQueries({ queryKey: ['units'] })
    },
    onError: () => toast.error('Не удалось удалить единицу'),
  })

  const startEdit = (unit: Unit) => {
    setEditingId(unit.id)
    setEditingName(unit.name)
    setEditingIsBase(unit.is_base)
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
    updateMutation.mutate({ id: editingId!, name: editingName.trim(), is_base: editingIsBase })
  }

  return (
    <div className="space-y-5 max-w-2xl">
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-2xl font-bold text-[var(--text-primary)]">Единицы измерения</h1>
          <p className="text-sm text-[var(--text-secondary)] mt-0.5">
            {units ? `Всего: ${units.length}` : 'Загрузка...'}
          </p>
        </div>
        <button className="btn-primary" onClick={() => setShowNewForm(true)}>
          <Plus className="w-4 h-4" />
          Добавить
        </button>
      </div>

      {showNewForm && (
        <div className="card p-4">
          <p className="text-sm font-medium text-[var(--text-primary)] mb-3">Новая единица</p>
          <div className="flex flex-wrap gap-2 items-center">
            <input
              className="input flex-1 min-w-[160px]"
              placeholder="Название единицы (г, кг, мл...)"
              value={newName}
              onChange={(e) => setNewName(e.target.value)}
              autoFocus
              onKeyDown={(e) => {
                if (e.key === 'Enter') createMutation.mutate()
                if (e.key === 'Escape') setShowNewForm(false)
              }}
            />
            <label className="flex items-center gap-2 cursor-pointer select-none">
              <input
                type="checkbox"
                checked={newIsBase}
                onChange={(e) => setNewIsBase(e.target.checked)}
                className="rounded border-[var(--border-color)] text-primary-600 focus:ring-primary-500"
              />
              <span className="text-sm text-[var(--text-primary)]">Базовая</span>
            </label>
            <button
              className="btn-primary"
              onClick={() => newName.trim() && createMutation.mutate()}
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
              <th className="table-header text-center">Базовая</th>
              <th className="table-header text-right">Действия</th>
            </tr>
          </thead>
          <tbody>
            {isLoading ? (
              <tr>
                <td colSpan={4}>
                  <TableSkeleton rows={6} cols={4} />
                </td>
              </tr>
            ) : !units?.length ? (
              <tr>
                <td colSpan={4}>
                  <EmptyState
                    icon={Ruler}
                    title="Нет единиц измерения"
                    description="Добавьте первую единицу измерения"
                    action={
                      <button className="btn-primary" onClick={() => setShowNewForm(true)}>
                        <Plus className="w-4 h-4" />
                        Добавить единицу
                      </button>
                    }
                  />
                </td>
              </tr>
            ) : (
              units.map((unit) => (
                <tr key={unit.id} className="border-b border-[var(--border-color)] last:border-0 hover:bg-[var(--bg-secondary)] transition-colors">
                  <td className="table-cell text-[var(--text-muted)] w-16">#{unit.id}</td>
                  <td className="table-cell">
                    {editingId === unit.id ? (
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
                      <span className="font-medium">{unit.name}</span>
                    )}
                  </td>
                  <td className="table-cell text-center">
                    {editingId === unit.id ? (
                      <input
                        type="checkbox"
                        checked={editingIsBase}
                        onChange={(e) => setEditingIsBase(e.target.checked)}
                        className="rounded border-[var(--border-color)] text-primary-600"
                      />
                    ) : (
                      unit.is_base ? (
                        <span className="badge-green">Да</span>
                      ) : (
                        <span className="badge-gray">Нет</span>
                      )
                    )}
                  </td>
                  <td className="table-cell text-right">
                    {editingId === unit.id ? (
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
                        <button className="btn-ghost p-2" onClick={() => startEdit(unit)}>
                          <Pencil className="w-4 h-4" />
                        </button>
                        <button
                          className="btn-ghost p-2 text-red-500 hover:text-red-600"
                          onClick={() => setDeleteTarget(unit)}
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
        title="Удалить единицу?"
        description={`Единица "${deleteTarget?.name}" будет удалена. Это может повлиять на конвертации ингредиентов.`}
        onConfirm={() => deleteTarget && deleteMutation.mutate(deleteTarget.id)}
        onCancel={() => setDeleteTarget(null)}
        loading={deleteMutation.isPending}
      />
    </div>
  )
}
