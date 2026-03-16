import { useEffect, useState } from 'react'
import { useNavigate, useParams } from 'react-router-dom'
import { useForm } from 'react-hook-form'
import { zodResolver } from '@hookform/resolvers/zod'
import { z } from 'zod'
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { ArrowLeft, Save, Apple, Plus, Trash2 } from 'lucide-react'
import {
  getIngredient,
  createIngredient,
  updateIngredient,
  getCategories,
  getUnits,
} from '@/api/ingredients'
import api from '@/lib/axios'
import { toast } from '@/components/ui/Toaster'
import { Skeleton } from '@/components/ui/Skeleton'
import { ConfirmDialog } from '@/components/ui/ConfirmDialog'
import type { UnitConversion } from '@/types'

const ingredientSchema = z.object({
  name: z.string().min(1, 'Введите название'),
  category: z.number({ coerce: true }).nullable().optional(),
  nutrition: z
    .object({
      base_weight_g: z.number({ coerce: true }).min(0),
      protein: z.number({ coerce: true }).min(0),
      fat: z.number({ coerce: true }).min(0),
      carbs: z.number({ coerce: true }).min(0),
      calories: z.number({ coerce: true }).min(0),
    })
    .nullable()
    .optional(),
})

type IngredientFormData = z.infer<typeof ingredientSchema>

export function IngredientFormPage() {
  const { id } = useParams()
  const navigate = useNavigate()
  const queryClient = useQueryClient()
  const isEdit = !!id
  const ingredientId = id ? Number(id) : null

  const [unitConversions, setUnitConversions] = useState<UnitConversion[]>([])
  const [newConversion, setNewConversion] = useState({ unit: '', grams_per_unit: '' })
  const [addingConversion, setAddingConversion] = useState(false)
  const [deleteConversionTarget, setDeleteConversionTarget] = useState<UnitConversion | null>(null)
  const [hasNutrition, setHasNutrition] = useState(false)

  const { data: ingredient, isLoading } = useQuery({
    queryKey: ['ingredient', ingredientId],
    queryFn: () => getIngredient(ingredientId!),
    enabled: !!ingredientId,
  })

  const { data: categories } = useQuery({
    queryKey: ['categories'],
    queryFn: getCategories,
  })

  const { data: units } = useQuery({
    queryKey: ['units'],
    queryFn: getUnits,
  })

  const {
    register,
    handleSubmit,
    setValue,
    formState: { errors, isSubmitting },
  } = useForm<IngredientFormData>({
    resolver: zodResolver(ingredientSchema),
  })

  useEffect(() => {
    if (ingredient) {
      setValue('name', ingredient.name)
      setValue('category', ingredient.category ?? null)
      if (ingredient.nutrition) {
        setHasNutrition(true)
        setValue('nutrition.base_weight_g', Number(ingredient.nutrition.base_weight_g))
        setValue('nutrition.protein', Number(ingredient.nutrition.protein))
        setValue('nutrition.fat', Number(ingredient.nutrition.fat))
        setValue('nutrition.carbs', Number(ingredient.nutrition.carbs))
        setValue('nutrition.calories', ingredient.nutrition.calories)
      }
      setUnitConversions(ingredient.unit_conversions)
    }
  }, [ingredient, setValue])

  const saveMutation = useMutation({
    mutationFn: async (data: IngredientFormData) => {
      const payload = {
        name: data.name,
        category: data.category || null,
        nutrition: hasNutrition ? data.nutrition : null,
      }
      if (isEdit && ingredientId) {
        return updateIngredient(ingredientId, payload as unknown as Parameters<typeof updateIngredient>[1])
      }
      return createIngredient(payload as unknown as Parameters<typeof createIngredient>[0])
    },
    onSuccess: (saved) => {
      toast.success(isEdit ? 'Ингредиент обновлён' : 'Ингредиент создан')
      void queryClient.invalidateQueries({ queryKey: ['ingredients'] })
      if (!isEdit) {
        navigate(`/ingredients/${saved.id}`)
      } else {
        void queryClient.invalidateQueries({ queryKey: ['ingredient', ingredientId] })
      }
    },
    onError: () => {
      toast.error('Не удалось сохранить ингредиент')
    },
  })

  const handleAddConversion = async () => {
    if (!ingredientId) {
      toast.error('Сначала сохраните ингредиент')
      return
    }
    if (!newConversion.unit || !newConversion.grams_per_unit) {
      toast.error('Заполните все поля')
      return
    }
    setAddingConversion(true)
    try {
      const added = await api.post(`/ingredients/${ingredientId}/conversions/`, {
        unit: Number(newConversion.unit),
        grams_per_unit: newConversion.grams_per_unit,
      })
      setUnitConversions((prev) => [...prev, added.data as UnitConversion])
      setNewConversion({ unit: '', grams_per_unit: '' })
      toast.success('Конвертация добавлена')
    } catch {
      toast.error('Не удалось добавить конвертацию')
    } finally {
      setAddingConversion(false)
    }
  }

  const handleDeleteConversion = async () => {
    if (!ingredientId || !deleteConversionTarget) return
    try {
      await api.delete(`/ingredients/${ingredientId}/conversions/${deleteConversionTarget.id}/`)
      setUnitConversions((prev) => prev.filter((c) => c.id !== deleteConversionTarget.id))
      setDeleteConversionTarget(null)
      toast.success('Конвертация удалена')
    } catch {
      toast.error('Не удалось удалить конвертацию')
    }
  }

  if (isLoading) {
    return (
      <div className="space-y-4">
        <Skeleton className="h-8 w-48" />
        <div className="card p-6 space-y-4">
          {[...Array(4)].map((_, i) => <Skeleton key={i} className="h-10 w-full" />)}
        </div>
      </div>
    )
  }

  return (
    <div className="space-y-6 max-w-3xl">
      <div className="flex items-center gap-4">
        <button className="btn-ghost p-2" onClick={() => navigate('/ingredients')}>
          <ArrowLeft className="w-5 h-5" />
        </button>
        <div>
          <h1 className="text-2xl font-bold text-[var(--text-primary)]">
            {isEdit ? 'Редактировать ингредиент' : 'Новый ингредиент'}
          </h1>
          {isEdit && <p className="text-sm text-[var(--text-muted)]">ID: {ingredientId}</p>}
        </div>
      </div>

      <form onSubmit={handleSubmit((data) => saveMutation.mutate(data))}>
        <div className="card p-6 space-y-5">
          <h2 className="font-semibold text-[var(--text-primary)] flex items-center gap-2">
            <Apple className="w-4 h-4 text-green-600" />
            Основная информация
          </h2>

          <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
            <div className="md:col-span-2">
              <label className="block text-sm font-medium text-[var(--text-primary)] mb-1.5">
                Название *
              </label>
              <input
                {...register('name')}
                className="input"
                placeholder="Название ингредиента"
              />
              {errors.name && <p className="text-xs text-red-500 mt-1">{errors.name.message}</p>}
            </div>

            <div>
              <label className="block text-sm font-medium text-[var(--text-primary)] mb-1.5">
                Категория
              </label>
              <select {...register('category', { valueAsNumber: true })} className="input">
                <option value="">Без категории</option>
                {categories?.map((cat) => (
                  <option key={cat.id} value={cat.id}>{cat.name}</option>
                ))}
              </select>
            </div>
          </div>

          {/* Nutrition */}
          <div>
            <div className="flex items-center gap-3 mb-3">
              <label className="font-medium text-[var(--text-primary)] text-sm">Пищевая ценность</label>
              <button
                type="button"
                onClick={() => setHasNutrition(!hasNutrition)}
                className={`relative w-10 h-5 rounded-full transition-colors ${hasNutrition ? 'bg-primary-600' : 'bg-[var(--bg-tertiary)]'}`}
              >
                <span
                  className={`absolute top-0.5 w-4 h-4 bg-white rounded-full shadow transition-transform ${hasNutrition ? 'left-5' : 'left-0.5'}`}
                />
              </button>
            </div>

            {hasNutrition && (
              <div className="grid grid-cols-2 md:grid-cols-5 gap-3">
                <div>
                  <label className="block text-xs text-[var(--text-muted)] mb-1">Базовый вес (г)</label>
                  <input
                    {...register('nutrition.base_weight_g', { valueAsNumber: true })}
                    type="number"
                    className="input"
                    placeholder="100"
                    step="0.01"
                    min={0}
                  />
                </div>
                <div>
                  <label className="block text-xs text-[var(--text-muted)] mb-1">Белки (г)</label>
                  <input
                    {...register('nutrition.protein', { valueAsNumber: true })}
                    type="number"
                    className="input"
                    placeholder="0"
                    step="0.01"
                    min={0}
                  />
                </div>
                <div>
                  <label className="block text-xs text-[var(--text-muted)] mb-1">Жиры (г)</label>
                  <input
                    {...register('nutrition.fat', { valueAsNumber: true })}
                    type="number"
                    className="input"
                    placeholder="0"
                    step="0.01"
                    min={0}
                  />
                </div>
                <div>
                  <label className="block text-xs text-[var(--text-muted)] mb-1">Углеводы (г)</label>
                  <input
                    {...register('nutrition.carbs', { valueAsNumber: true })}
                    type="number"
                    className="input"
                    placeholder="0"
                    step="0.01"
                    min={0}
                  />
                </div>
                <div>
                  <label className="block text-xs text-[var(--text-muted)] mb-1">Калории</label>
                  <input
                    {...register('nutrition.calories', { valueAsNumber: true })}
                    type="number"
                    className="input"
                    placeholder="0"
                    step="0.01"
                    min={0}
                  />
                </div>
              </div>
            )}
          </div>

          <div className="flex justify-end pt-2">
            <button
              type="submit"
              className="btn-primary"
              disabled={isSubmitting || saveMutation.isPending}
            >
              {(isSubmitting || saveMutation.isPending) ? (
                <span className="w-4 h-4 border-2 border-white/30 border-t-white rounded-full animate-spin" />
              ) : (
                <Save className="w-4 h-4" />
              )}
              {isEdit ? 'Сохранить изменения' : 'Создать ингредиент'}
            </button>
          </div>
        </div>
      </form>

      {/* Unit Conversions */}
      <div className="card p-6 space-y-4">
        <h2 className="font-semibold text-[var(--text-primary)]">
          Конвертации единиц ({unitConversions.length})
        </h2>

        {!isEdit && (
          <p className="text-sm text-amber-600 dark:text-amber-400 bg-amber-50 dark:bg-amber-900/20 border border-amber-200 dark:border-amber-800 rounded-lg px-4 py-2">
            Сначала создайте ингредиент, затем добавьте конвертации
          </p>
        )}

        {unitConversions.length > 0 && (
          <div className="border border-[var(--border-color)] rounded-lg overflow-hidden">
            <table className="w-full">
              <thead>
                <tr className="border-b border-[var(--border-color)]">
                  <th className="table-header text-left">Единица</th>
                  <th className="table-header text-center">Грамм на единицу</th>
                  <th className="table-header text-right"></th>
                </tr>
              </thead>
              <tbody>
                {unitConversions.map((conv) => (
                  <tr key={conv.id} className="border-b border-[var(--border-color)] last:border-0">
                    <td className="table-cell font-medium">{conv.unit_name}</td>
                    <td className="table-cell text-center">{conv.grams_per_unit} г</td>
                    <td className="table-cell text-right">
                      <button
                        className="btn-ghost p-1.5 text-red-500 hover:text-red-600"
                        onClick={() => setDeleteConversionTarget(conv)}
                      >
                        <Trash2 className="w-3.5 h-3.5" />
                      </button>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}

        {isEdit && (
          <div className="border border-dashed border-[var(--border-color)] rounded-lg p-4 space-y-3">
            <p className="text-sm font-medium text-[var(--text-secondary)]">Добавить конвертацию</p>
            <div className="flex gap-3 flex-wrap">
              <select
                className="input flex-1 min-w-[150px]"
                value={newConversion.unit}
                onChange={(e) => setNewConversion({ ...newConversion, unit: e.target.value })}
              >
                <option value="">Выберите единицу</option>
                {units?.map((u) => (
                  <option key={u.id} value={u.id}>{u.name}</option>
                ))}
              </select>
              <input
                className="input w-40"
                type="number"
                placeholder="Грамм на единицу"
                value={newConversion.grams_per_unit}
                onChange={(e) => setNewConversion({ ...newConversion, grams_per_unit: e.target.value })}
                min={0}
                step="0.01"
              />
              <button
                className="btn-secondary"
                onClick={handleAddConversion}
                disabled={addingConversion}
              >
                {addingConversion ? (
                  <span className="w-4 h-4 border-2 border-current/30 border-t-current rounded-full animate-spin" />
                ) : (
                  <Plus className="w-4 h-4" />
                )}
                Добавить
              </button>
            </div>
          </div>
        )}
      </div>

      <ConfirmDialog
        open={!!deleteConversionTarget}
        title="Удалить конвертацию?"
        description={`Конвертация для единицы "${deleteConversionTarget?.unit_name}" будет удалена.`}
        onConfirm={handleDeleteConversion}
        onCancel={() => setDeleteConversionTarget(null)}
      />
    </div>
  )
}
