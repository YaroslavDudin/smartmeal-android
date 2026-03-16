import { useParams, useNavigate } from 'react-router-dom'
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { ArrowLeft, Save, User, Mail, Calendar, Utensils, ShieldAlert } from 'lucide-react'
import { useState, useEffect } from 'react'
import { getUser, updateUser, getDietTypes, getAllergies } from '@/api/users'
import { Skeleton } from '@/components/ui/Skeleton'
import { toast } from '@/components/ui/Toaster'
import { formatDateTime } from '@/lib/utils'

export function UserDetailPage() {
  const { id } = useParams()
  const navigate = useNavigate()
  const queryClient = useQueryClient()
  const userId = Number(id)

  const [isEditing, setIsEditing] = useState(false)
  const [editDietType, setEditDietType] = useState<number | null>(null)
  const [editAllergies, setEditAllergies] = useState<number[]>([])
  const [editPortionSize, setEditPortionSize] = useState<number>(1)
  const [editIsActive, setEditIsActive] = useState(true)

  const { data: user, isLoading } = useQuery({
    queryKey: ['user', userId],
    queryFn: () => getUser(userId),
  })

  const { data: dietTypes } = useQuery({
    queryKey: ['diet-types'],
    queryFn: getDietTypes,
  })

  const { data: allergies } = useQuery({
    queryKey: ['allergies'],
    queryFn: getAllergies,
  })

  useEffect(() => {
    if (user) {
      setEditDietType(user.diet_type)
      setEditAllergies(user.allergies)
      setEditPortionSize(user.portion_size)
      setEditIsActive(user.is_active)
    }
  }, [user])

  const updateMutation = useMutation({
    mutationFn: () =>
      updateUser(userId, {
        diet_type: editDietType,
        allergies: editAllergies,
        portion_size: editPortionSize,
        is_active: editIsActive,
      }),
    onSuccess: () => {
      toast.success('Данные пользователя обновлены')
      setIsEditing(false)
      void queryClient.invalidateQueries({ queryKey: ['user', userId] })
      void queryClient.invalidateQueries({ queryKey: ['users'] })
    },
    onError: () => toast.error('Не удалось обновить данные'),
  })

  const toggleAllergy = (allergyId: number) => {
    setEditAllergies((prev) =>
      prev.includes(allergyId) ? prev.filter((a) => a !== allergyId) : [...prev, allergyId]
    )
  }

  if (isLoading) {
    return (
      <div className="space-y-4 max-w-3xl">
        <Skeleton className="h-8 w-48" />
        <div className="card p-6 space-y-4">
          {[...Array(6)].map((_, i) => <Skeleton key={i} className="h-10 w-full" />)}
        </div>
      </div>
    )
  }

  if (!user) {
    return (
      <div className="flex flex-col items-center justify-center py-16">
        <p className="text-[var(--text-muted)]">Пользователь не найден</p>
        <button className="btn-secondary mt-4" onClick={() => navigate('/users')}>
          <ArrowLeft className="w-4 h-4" />
          Назад
        </button>
      </div>
    )
  }

  const displayName = user.first_name
    ? `${user.first_name} ${user.last_name}`.trim()
    : user.username || user.email

  return (
    <div className="space-y-6 max-w-3xl">
      <div className="flex items-center gap-4">
        <button className="btn-ghost p-2" onClick={() => navigate('/users')}>
          <ArrowLeft className="w-5 h-5" />
        </button>
        <div>
          <h1 className="text-2xl font-bold text-[var(--text-primary)]">{displayName}</h1>
          <p className="text-sm text-[var(--text-muted)]">ID: {user.id}</p>
        </div>
        <div className="ml-auto">
          {isEditing ? (
            <div className="flex gap-2">
              <button className="btn-secondary" onClick={() => setIsEditing(false)}>
                Отмена
              </button>
              <button
                className="btn-primary"
                onClick={() => updateMutation.mutate()}
                disabled={updateMutation.isPending}
              >
                {updateMutation.isPending ? (
                  <span className="w-4 h-4 border-2 border-white/30 border-t-white rounded-full animate-spin" />
                ) : (
                  <Save className="w-4 h-4" />
                )}
                Сохранить
              </button>
            </div>
          ) : (
            <button className="btn-secondary" onClick={() => setIsEditing(true)}>
              Редактировать
            </button>
          )}
        </div>
      </div>

      {/* Basic info */}
      <div className="card p-6">
        <h2 className="font-semibold text-[var(--text-primary)] mb-4 flex items-center gap-2">
          <User className="w-4 h-4 text-violet-500" />
          Основная информация
        </h2>
        <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
          <div className="flex items-start gap-3">
            <User className="w-4 h-4 text-[var(--text-muted)] mt-0.5 flex-shrink-0" />
            <div>
              <p className="text-xs text-[var(--text-muted)]">Имя</p>
              <p className="text-sm font-medium text-[var(--text-primary)]">
                {user.first_name || user.last_name ? `${user.first_name} ${user.last_name}`.trim() : '—'}
              </p>
            </div>
          </div>
          <div className="flex items-start gap-3">
            <User className="w-4 h-4 text-[var(--text-muted)] mt-0.5 flex-shrink-0" />
            <div>
              <p className="text-xs text-[var(--text-muted)]">Логин</p>
              <p className="text-sm font-medium text-[var(--text-primary)]">{user.username || '—'}</p>
            </div>
          </div>
          <div className="flex items-start gap-3">
            <Mail className="w-4 h-4 text-[var(--text-muted)] mt-0.5 flex-shrink-0" />
            <div>
              <p className="text-xs text-[var(--text-muted)]">Email</p>
              <p className="text-sm font-medium text-[var(--text-primary)]">{user.email}</p>
            </div>
          </div>
          <div className="flex items-start gap-3">
            <Calendar className="w-4 h-4 text-[var(--text-muted)] mt-0.5 flex-shrink-0" />
            <div>
              <p className="text-xs text-[var(--text-muted)]">Дата регистрации</p>
              <p className="text-sm font-medium text-[var(--text-primary)]">{formatDateTime(user.date_joined)}</p>
            </div>
          </div>
          <div className="flex items-start gap-3">
            <div className="w-4 h-4 mt-0.5 flex-shrink-0" />
            <div>
              <p className="text-xs text-[var(--text-muted)]">Роль</p>
              <div className="flex gap-1 mt-0.5">
                {user.is_superuser && <span className="badge-blue">Superuser</span>}
                <span className={user.is_active ? 'badge-green' : 'badge-red'}>
                  {user.is_active ? 'Активен' : 'Неактивен'}
                </span>
              </div>
            </div>
          </div>
        </div>
      </div>

      {/* Editable settings */}
      <div className="card p-6">
        <h2 className="font-semibold text-[var(--text-primary)] mb-4 flex items-center gap-2">
          <Utensils className="w-4 h-4 text-primary-600" />
          Настройки питания
        </h2>
        <div className="space-y-5">
          {isEditing ? (
            <>
              <div>
                <label className="block text-sm font-medium text-[var(--text-primary)] mb-1.5">
                  Статус аккаунта
                </label>
                <div className="flex items-center gap-3">
                  <button
                    type="button"
                    onClick={() => setEditIsActive(!editIsActive)}
                    className={`relative w-10 h-5 rounded-full transition-colors ${editIsActive ? 'bg-primary-600' : 'bg-[var(--bg-tertiary)]'}`}
                  >
                    <span
                      className={`absolute top-0.5 w-4 h-4 bg-white rounded-full shadow transition-transform ${editIsActive ? 'left-5' : 'left-0.5'}`}
                    />
                  </button>
                  <span className="text-sm text-[var(--text-secondary)]">
                    {editIsActive ? 'Активен' : 'Заблокирован'}
                  </span>
                </div>
              </div>

              <div>
                <label className="block text-sm font-medium text-[var(--text-primary)] mb-1.5">
                  Размер порции
                </label>
                <input
                  type="number"
                  className="input w-32"
                  value={editPortionSize}
                  onChange={(e) => setEditPortionSize(Number(e.target.value))}
                  min={1}
                  max={10}
                />
              </div>

              <div>
                <label className="block text-sm font-medium text-[var(--text-primary)] mb-2">
                  Тип диеты
                </label>
                <div className="flex flex-wrap gap-2">
                  <button
                    type="button"
                    onClick={() => setEditDietType(null)}
                    className={`px-3 py-1.5 rounded-lg text-sm font-medium border transition-colors ${
                      editDietType === null
                        ? 'bg-primary-600 text-white border-primary-600'
                        : 'bg-[var(--bg-secondary)] text-[var(--text-secondary)] border-[var(--border-color)]'
                    }`}
                  >
                    Не указано
                  </button>
                  {dietTypes?.map((dt) => (
                    <button
                      key={dt.id}
                      type="button"
                      onClick={() => setEditDietType(dt.id)}
                      className={`px-3 py-1.5 rounded-lg text-sm font-medium border transition-colors ${
                        editDietType === dt.id
                          ? 'bg-primary-600 text-white border-primary-600'
                          : 'bg-[var(--bg-secondary)] text-[var(--text-secondary)] border-[var(--border-color)]'
                      }`}
                    >
                      {dt.name}
                    </button>
                  ))}
                </div>
              </div>

              <div>
                <label className="block text-sm font-medium text-[var(--text-primary)] mb-2 flex items-center gap-1.5">
                  <ShieldAlert className="w-3.5 h-3.5" />
                  Аллергии
                </label>
                <div className="flex flex-wrap gap-2">
                  {allergies?.map((allergy) => (
                    <button
                      key={allergy.id}
                      type="button"
                      onClick={() => toggleAllergy(allergy.id)}
                      className={`px-3 py-1.5 rounded-lg text-sm font-medium border transition-colors ${
                        editAllergies.includes(allergy.id)
                          ? 'bg-red-600 text-white border-red-600'
                          : 'bg-[var(--bg-secondary)] text-[var(--text-secondary)] border-[var(--border-color)]'
                      }`}
                    >
                      {allergy.name}
                    </button>
                  ))}
                </div>
              </div>
            </>
          ) : (
            <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
              <div>
                <p className="text-xs text-[var(--text-muted)]">Тип диеты</p>
                <p className="text-sm font-medium text-[var(--text-primary)] mt-0.5">
                  {user.diet_type_name || 'Не указано'}
                </p>
              </div>
              <div>
                <p className="text-xs text-[var(--text-muted)]">Размер порции</p>
                <p className="text-sm font-medium text-[var(--text-primary)] mt-0.5">
                  {user.portion_size}
                </p>
              </div>
              <div className="md:col-span-2">
                <p className="text-xs text-[var(--text-muted)] mb-1.5">Аллергии</p>
                {user.allergies.length > 0 ? (
                  <div className="flex flex-wrap gap-1">
                    {allergies
                      ?.filter((a) => user.allergies.includes(a.id))
                      .map((a) => (
                        <span key={a.id} className="badge-red">{a.name}</span>
                      ))}
                  </div>
                ) : (
                  <p className="text-sm text-[var(--text-muted)]">Не указано</p>
                )}
              </div>
            </div>
          )}
        </div>
      </div>
    </div>
  )
}
