import { useState, useEffect } from 'react'
import { useNavigate, useParams } from 'react-router-dom'
import { useForm } from 'react-hook-form'
import { zodResolver } from '@hookform/resolvers/zod'
import { z } from 'zod'
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import {
  ArrowLeft,
  Save,
  Plus,
  Trash2,
  Image as ImageIcon,
  ChefHat,
  Clock,
  Users,
  Timer,
  Search,
} from 'lucide-react'
import {
  getRecipe,
  createRecipe,
  updateRecipe,
  addRecipeIngredient,
  deleteRecipeIngredient,
  addRecipeStep,
  updateRecipeStep,
  deleteRecipeStep,
} from '@/api/recipes'
import { getUnits } from '@/api/ingredients'
import { getDietTypes } from '@/api/users'
import { uploadImage } from '@/api/auth'
import { toast } from '@/components/ui/Toaster'
import { Skeleton } from '@/components/ui/Skeleton'
import { ConfirmDialog } from '@/components/ui/ConfirmDialog'
import { ImageSearchModal } from '@/components/ui/ImageSearchModal'
import { IngredientAutocomplete } from '@/components/ui/IngredientAutocomplete'
import type { RecipeIngredient, RecipeStep } from '@/types'

const recipeSchema = z.object({
  title: z.string().min(1, 'Введите название'),
  cook_time: z.number({ coerce: true }).min(1, 'Введите время приготовления'),
  servings: z.number({ coerce: true }).min(1, 'Введите количество порций'),
  image_url: z.string().optional(),
  diet_types: z.array(z.number()).optional(),
})

type RecipeFormData = z.infer<typeof recipeSchema>

export function RecipeFormPage() {
  const { id } = useParams()
  const navigate = useNavigate()
  const queryClient = useQueryClient()
  const isEdit = !!id
  const recipeId = id ? Number(id) : null

  const [ingredients, setIngredients] = useState<RecipeIngredient[]>([])
  const [steps, setSteps] = useState<RecipeStep[]>([])
  const [imageUploading, setImageUploading] = useState(false)
  const [deleteIngredientTarget, setDeleteIngredientTarget] = useState<RecipeIngredient | null>(null)
  const [deleteStepTarget, setDeleteStepTarget] = useState<RecipeStep | null>(null)

  // New ingredient form state
  const [newIngredient, setNewIngredient] = useState({
    ingredient: '',
    amount: '',
    unit: '',
  })
  const [addingIngredient, setAddingIngredient] = useState(false)

  // New step form state
  const [newStep, setNewStep] = useState({ description: '', timer: '' })
  const [addingStep, setAddingStep] = useState(false)
  const [imageSearchOpen, setImageSearchOpen] = useState(false)

  const { data: recipe, isLoading: recipeLoading } = useQuery({
    queryKey: ['recipe', recipeId],
    queryFn: () => getRecipe(recipeId!),
    enabled: !!recipeId,
  })

  const { data: units } = useQuery({
    queryKey: ['units'],
    queryFn: getUnits,
  })

  const { data: dietTypes } = useQuery({
    queryKey: ['diet-types'],
    queryFn: getDietTypes,
  })

  const {
    register,
    handleSubmit,
    setValue,
    watch,
    formState: { errors, isSubmitting },
  } = useForm<RecipeFormData>({
    resolver: zodResolver(recipeSchema),
    defaultValues: {
      diet_types: [],
    },
  })

  const watchedImageUrl = watch('image_url')
  const watchedDietTypes = watch('diet_types') ?? []

  useEffect(() => {
    if (recipe) {
      setValue('title', recipe.title)
      setValue('cook_time', recipe.cook_time)
      setValue('servings', recipe.servings)
      setValue('image_url', recipe.image_url)
      setValue('diet_types', recipe.diet_types)
      setIngredients(recipe.ingredients)
      setSteps([...recipe.steps].sort((a, b) => a.step_number - b.step_number))
    }
  }, [recipe, setValue])

  const saveMutation = useMutation({
    mutationFn: async (data: RecipeFormData) => {
      if (isEdit && recipeId) {
        return updateRecipe(recipeId, data)
      }
      return createRecipe(data)
    },
    onSuccess: (saved) => {
      toast.success(isEdit ? 'Рецепт обновлён' : 'Рецепт создан')
      void queryClient.invalidateQueries({ queryKey: ['recipes'] })
      if (!isEdit) {
        navigate(`/recipes/${saved.id}`)
      } else {
        void queryClient.invalidateQueries({ queryKey: ['recipe', recipeId] })
      }
    },
    onError: () => {
      toast.error('Не удалось сохранить рецепт')
    },
  })

  const handleImageUpload = async (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0]
    if (!file) return
    setImageUploading(true)
    try {
      const result = await uploadImage(file)
      setValue('image_url', result.url)
      toast.success('Изображение загружено')
    } catch {
      toast.error('Не удалось загрузить изображение')
    } finally {
      setImageUploading(false)
    }
  }

  const toggleDietType = (dtId: number) => {
    const current = watchedDietTypes
    if (current.includes(dtId)) {
      setValue('diet_types', current.filter((d) => d !== dtId))
    } else {
      setValue('diet_types', [...current, dtId])
    }
  }

  const handleAddIngredient = async () => {
    if (!recipeId) {
      toast.error('Сначала сохраните рецепт')
      return
    }
    if (!newIngredient.ingredient || !newIngredient.amount || !newIngredient.unit) {
      toast.error('Заполните все поля ингредиента')
      return
    }
    setAddingIngredient(true)
    try {
      const added = await addRecipeIngredient(recipeId, {
        ingredient: Number(newIngredient.ingredient),
        amount: newIngredient.amount,
        unit: Number(newIngredient.unit),
      })
      setIngredients((prev) => [...prev, added as RecipeIngredient])
      setNewIngredient({ ingredient: '', amount: '', unit: '' })
      toast.success('Ингредиент добавлен')
    } catch {
      toast.error('Не удалось добавить ингредиент')
    } finally {
      setAddingIngredient(false)
    }
  }

  const handleDeleteIngredient = async () => {
    if (!recipeId || !deleteIngredientTarget) return
    try {
      await deleteRecipeIngredient(recipeId, deleteIngredientTarget.id)
      setIngredients((prev) => prev.filter((i) => i.id !== deleteIngredientTarget.id))
      setDeleteIngredientTarget(null)
      toast.success('Ингредиент удалён')
    } catch {
      toast.error('Не удалось удалить ингредиент')
    }
  }

  const handleAddStep = async () => {
    if (!recipeId) {
      toast.error('Сначала сохраните рецепт')
      return
    }
    if (!newStep.description) {
      toast.error('Введите описание шага')
      return
    }
    setAddingStep(true)
    try {
      const added = await addRecipeStep(recipeId, {
        description: newStep.description,
        timer: newStep.timer ? Number(newStep.timer) : null,
        step_number: steps.length + 1,
      })
      setSteps((prev) => [...prev, added as RecipeStep])
      setNewStep({ description: '', timer: '' })
      toast.success('Шаг добавлен')
    } catch {
      toast.error('Не удалось добавить шаг')
    } finally {
      setAddingStep(false)
    }
  }

  const handleDeleteStep = async () => {
    if (!recipeId || !deleteStepTarget) return
    try {
      await deleteRecipeStep(recipeId, deleteStepTarget.id)
      setSteps((prev) => prev.filter((s) => s.id !== deleteStepTarget.id))
      setDeleteStepTarget(null)
      toast.success('Шаг удалён')
    } catch {
      toast.error('Не удалось удалить шаг')
    }
  }

  const handleUpdateStep = async (stepId: number, description: string) => {
    if (!recipeId) return
    try {
      await updateRecipeStep(recipeId, stepId, { description })
      setSteps((prev) =>
        prev.map((s) => (s.id === stepId ? { ...s, description } : s))
      )
    } catch {
      toast.error('Не удалось обновить шаг')
    }
  }

  if (recipeLoading) {
    return (
      <div className="space-y-4">
        <Skeleton className="h-8 w-48" />
        <div className="card p-6 space-y-4">
          {[...Array(5)].map((_, i) => <Skeleton key={i} className="h-10 w-full" />)}
        </div>
      </div>
    )
  }

  return (
    <div className="space-y-6 max-w-4xl">
      <div className="flex items-center gap-4">
        <button className="btn-ghost p-2" onClick={() => navigate('/recipes')}>
          <ArrowLeft className="w-5 h-5" />
        </button>
        <div>
          <h1 className="text-2xl font-bold text-[var(--text-primary)]">
            {isEdit ? 'Редактировать рецепт' : 'Новый рецепт'}
          </h1>
          {isEdit && <p className="text-sm text-[var(--text-muted)]">ID: {recipeId}</p>}
        </div>
      </div>

      <form onSubmit={handleSubmit((data) => saveMutation.mutate(data))}>
        <div className="card p-6 space-y-5">
          <h2 className="font-semibold text-[var(--text-primary)] flex items-center gap-2">
            <ChefHat className="w-4 h-4 text-primary-600" />
            Основная информация
          </h2>

          <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
            <div className="md:col-span-2">
              <label className="block text-sm font-medium text-[var(--text-primary)] mb-1.5">
                Название рецепта *
              </label>
              <input
                {...register('title')}
                className="input"
                placeholder="Введите название рецепта"
              />
              {errors.title && <p className="text-xs text-red-500 mt-1">{errors.title.message}</p>}
            </div>

            <div>
              <label className="block text-sm font-medium text-[var(--text-primary)] mb-1.5">
                <Clock className="w-3.5 h-3.5 inline mr-1" />
                Время приготовления (мин) *
              </label>
              <input
                {...register('cook_time', { valueAsNumber: true })}
                type="number"
                className="input"
                placeholder="30"
                min={1}
              />
              {errors.cook_time && <p className="text-xs text-red-500 mt-1">{errors.cook_time.message}</p>}
            </div>

            <div>
              <label className="block text-sm font-medium text-[var(--text-primary)] mb-1.5">
                <Users className="w-3.5 h-3.5 inline mr-1" />
                Количество порций *
              </label>
              <input
                {...register('servings', { valueAsNumber: true })}
                type="number"
                className="input"
                placeholder="4"
                min={1}
              />
              {errors.servings && <p className="text-xs text-red-500 mt-1">{errors.servings.message}</p>}
            </div>

            <div className="md:col-span-2">
              <label className="block text-sm font-medium text-[var(--text-primary)] mb-1.5">
                <ImageIcon className="w-3.5 h-3.5 inline mr-1" />
                Изображение
              </label>
              <div className="flex gap-2 items-start flex-wrap sm:flex-nowrap">
                <input
                  {...register('image_url')}
                  className="input flex-1 min-w-0"
                  placeholder="https://..."
                />
                <button
                  type="button"
                  className="btn-secondary flex-shrink-0"
                  onClick={() => setImageSearchOpen(true)}
                >
                  <Search className="w-4 h-4" />
                  Найти в интернете
                </button>
                <label className="btn-secondary cursor-pointer flex-shrink-0">
                  {imageUploading ? (
                    <span className="w-4 h-4 border-2 border-current/30 border-t-current rounded-full animate-spin" />
                  ) : (
                    <ImageIcon className="w-4 h-4" />
                  )}
                  Загрузить
                  <input type="file" accept="image/*" className="hidden" onChange={handleImageUpload} />
                </label>
              </div>
              {watchedImageUrl && (
                <img src={watchedImageUrl} alt="Preview" className="mt-2 h-32 rounded-lg object-cover" />
              )}
            </div>
          </div>

          <div>
            <label className="block text-sm font-medium text-[var(--text-primary)] mb-2">
              Типы диет
            </label>
            <div className="flex flex-wrap gap-2">
              {dietTypes?.map((dt) => (
                <button
                  key={dt.id}
                  type="button"
                  onClick={() => toggleDietType(dt.id)}
                  className={`px-3 py-1.5 rounded-lg text-sm font-medium border transition-colors ${
                    watchedDietTypes.includes(dt.id)
                      ? 'bg-primary-600 text-white border-primary-600'
                      : 'bg-[var(--bg-secondary)] text-[var(--text-secondary)] border-[var(--border-color)] hover:border-primary-400'
                  }`}
                >
                  {dt.name}
                </button>
              ))}
            </div>
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
              {isEdit ? 'Сохранить изменения' : 'Создать рецепт'}
            </button>
          </div>
        </div>
      </form>

      {/* Ingredients Section */}
      <div className="card p-6 space-y-4">
        <h2 className="font-semibold text-[var(--text-primary)]">Ингредиенты ({ingredients.length})</h2>

        {!isEdit && (
          <p className="text-sm text-amber-600 dark:text-amber-400 bg-amber-50 dark:bg-amber-900/20 border border-amber-200 dark:border-amber-800 rounded-lg px-4 py-2">
            Сначала создайте рецепт, затем добавьте ингредиенты
          </p>
        )}

        {ingredients.length > 0 && (
          <div className="border border-[var(--border-color)] rounded-lg overflow-hidden">
            <table className="w-full">
              <thead>
                <tr className="border-b border-[var(--border-color)]">
                  <th className="table-header text-left">Ингредиент</th>
                  <th className="table-header text-center">Количество</th>
                  <th className="table-header text-center">Единица</th>
                  <th className="table-header text-right"></th>
                </tr>
              </thead>
              <tbody>
                {ingredients.map((ing) => (
                  <tr key={ing.id} className="border-b border-[var(--border-color)] last:border-0">
                    <td className="table-cell font-medium">{ing.ingredient_name}</td>
                    <td className="table-cell text-center">{ing.amount}</td>
                    <td className="table-cell text-center">{ing.unit_name}</td>
                    <td className="table-cell text-right">
                      <button
                        className="btn-ghost p-1.5 text-red-500 hover:text-red-600"
                        onClick={() => setDeleteIngredientTarget(ing)}
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
            <p className="text-sm font-medium text-[var(--text-secondary)]">Добавить ингредиент</p>
            <div className="grid grid-cols-1 sm:grid-cols-3 gap-3">
              <IngredientAutocomplete
                value={newIngredient.ingredient}
                onChange={(id, _name) => setNewIngredient({ ...newIngredient, ingredient: id })}
              />
              <input
                className="input"
                type="number"
                placeholder="Количество"
                value={newIngredient.amount}
                onChange={(e) => setNewIngredient({ ...newIngredient, amount: e.target.value })}
                min={0}
                step="0.1"
              />
              <select
                className="input"
                value={newIngredient.unit}
                onChange={(e) => setNewIngredient({ ...newIngredient, unit: e.target.value })}
              >
                <option value="">Единица измерения</option>
                {units?.map((u) => (
                  <option key={u.id} value={u.id}>{u.name}</option>
                ))}
              </select>
            </div>
            <button
              className="btn-secondary"
              onClick={handleAddIngredient}
              disabled={addingIngredient}
            >
              {addingIngredient ? (
                <span className="w-4 h-4 border-2 border-current/30 border-t-current rounded-full animate-spin" />
              ) : (
                <Plus className="w-4 h-4" />
              )}
              Добавить
            </button>
          </div>
        )}
      </div>

      {/* Steps Section */}
      <div className="card p-6 space-y-4">
        <h2 className="font-semibold text-[var(--text-primary)]">Шаги приготовления ({steps.length})</h2>

        {!isEdit && (
          <p className="text-sm text-amber-600 dark:text-amber-400 bg-amber-50 dark:bg-amber-900/20 border border-amber-200 dark:border-amber-800 rounded-lg px-4 py-2">
            Сначала создайте рецепт, затем добавьте шаги
          </p>
        )}

        {steps.length > 0 && (
          <div className="space-y-3">
            {steps.map((step, idx) => (
              <div key={step.id} className="flex gap-3 p-4 border border-[var(--border-color)] rounded-lg">
                <div className="w-7 h-7 rounded-full bg-primary-600 text-white text-sm font-semibold flex items-center justify-center flex-shrink-0">
                  {idx + 1}
                </div>
                <div className="flex-1">
                  <textarea
                    className="input resize-none"
                    rows={2}
                    defaultValue={step.description}
                    onBlur={(e) => {
                      if (e.target.value !== step.description) {
                        void handleUpdateStep(step.id, e.target.value)
                      }
                    }}
                  />
                  {step.timer && (
                    <p className="text-xs text-[var(--text-muted)] mt-1 flex items-center gap-1">
                      <Timer className="w-3 h-3" />
                      Таймер: {step.timer} сек
                    </p>
                  )}
                </div>
                <button
                  className="btn-ghost p-1.5 text-red-500 hover:text-red-600 flex-shrink-0"
                  onClick={() => setDeleteStepTarget(step)}
                >
                  <Trash2 className="w-3.5 h-3.5" />
                </button>
              </div>
            ))}
          </div>
        )}

        {isEdit && (
          <div className="border border-dashed border-[var(--border-color)] rounded-lg p-4 space-y-3">
            <p className="text-sm font-medium text-[var(--text-secondary)]">Добавить шаг</p>
            <textarea
              className="input resize-none"
              rows={3}
              placeholder="Описание шага..."
              value={newStep.description}
              onChange={(e) => setNewStep({ ...newStep, description: e.target.value })}
            />
            <div className="flex items-center gap-3">
              <div className="flex items-center gap-2">
                <Timer className="w-4 h-4 text-[var(--text-muted)]" />
                <input
                  className="input w-32"
                  type="number"
                  placeholder="Таймер (сек)"
                  value={newStep.timer}
                  onChange={(e) => setNewStep({ ...newStep, timer: e.target.value })}
                  min={0}
                />
              </div>
              <button
                className="btn-secondary"
                onClick={handleAddStep}
                disabled={addingStep}
              >
                {addingStep ? (
                  <span className="w-4 h-4 border-2 border-current/30 border-t-current rounded-full animate-spin" />
                ) : (
                  <Plus className="w-4 h-4" />
                )}
                Добавить шаг
              </button>
            </div>
          </div>
        )}
      </div>

      <ConfirmDialog
        open={!!deleteIngredientTarget}
        title="Удалить ингредиент?"
        description={`Ингредиент "${deleteIngredientTarget?.ingredient_name}" будет удалён из рецепта.`}
        onConfirm={handleDeleteIngredient}
        onCancel={() => setDeleteIngredientTarget(null)}
      />

      <ConfirmDialog
        open={!!deleteStepTarget}
        title="Удалить шаг?"
        description="Шаг будет удалён из рецепта безвозвратно."
        onConfirm={handleDeleteStep}
        onCancel={() => setDeleteStepTarget(null)}
      />

      <ImageSearchModal
        open={imageSearchOpen}
        onClose={() => setImageSearchOpen(false)}
        onSelect={(url) => setValue('image_url', url)}
      />
    </div>
  )
}
