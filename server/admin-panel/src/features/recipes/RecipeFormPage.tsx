import { AxiosError } from 'axios'
import { useState, useEffect } from 'react'
import { useNavigate, useParams } from 'react-router-dom'
import { useForm, useFieldArray, Controller } from 'react-hook-form'
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
  Video,
} from 'lucide-react'
import {
  getRecipe,
  createRecipe,
  updateRecipe,
  addRecipeIngredient,
  updateRecipeIngredient,
  deleteRecipeIngredient,
  addRecipeStep,
  updateRecipeStep,
  deleteRecipeStep,
} from '@/api/recipes'
import { getUnits } from '@/api/ingredients'
import { getDietTypes } from '@/api/users'
import { getMealTypes } from '@/api/menus'
import { toast } from '@/components/ui/Toaster'
import { Skeleton } from '@/components/ui/Skeleton'
import { ConfirmDialog } from '@/components/ui/ConfirmDialog'
import { RecipePhonePreview } from '@/components/ui/RecipePreviewModal'
import { IngredientAutocomplete } from '@/components/ui/IngredientAutocomplete'


const ingredientSchema = z.object({
  id: z.number().optional(),
  ingredient: z.number({ invalid_type_error: 'Выберите ингредиент из выпадающего списка' }).min(1, 'Выберите ингредиент'),
  amount: z.number({ coerce: true }).min(0.01, 'Укажите количество').positive('Количество должно быть > 0'),
  unit: z.number({ coerce: true, invalid_type_error: 'Выберите единицу измерения' }).min(1, 'Выберите единицу'),
})
const stepSchema = z.object({
  id: z.number().optional(),
  description: z.string().min(1, 'Введите описание шага'),
  timer: z.number({ coerce: true }).min(0, 'Таймер не может быть отрицательным').nullable().default(null),
  image_url: z.union([z.string(), z.instanceof(File), z.null()]).optional().nullable(),
  video_url: z.union([z.string(), z.instanceof(File), z.null()]).optional().nullable(),
})

const recipeSchema = z.object({
  title: z.string({ required_error: 'Введите название' }).min(1),
  cook_time: z.number({ coerce: true, invalid_type_error: 'Введите время приготовления' }).positive('Время должно быть > 0'),
  servings: z.number({ coerce: true, invalid_type_error: 'Введите количество порций' }).min(1, 'Минимум 1').max(20, 'Максимум 20'),
  meal_types: z.array(z.number({ coerce: true })).min(1, 'Выберите хотя бы один прием пищи'),
  diet_types: z.array(z.number({ coerce: true })).min(1, 'Выберите хотя бы один тип питания'),
  image_url: z.union([z.string(), z.instanceof(File), z.null()]).optional().nullable(),
  ingredients: z.array(ingredientSchema),
  steps: z.array(stepSchema),
})

type RecipeFormData = z.infer<typeof recipeSchema>
type RecipeIngredientForm = RecipeFormData['ingredients'][0]
type RecipeStepForm = RecipeFormData['steps'][0]

function FieldError({ msg }: { msg?: string }) {
  if (!msg) return null
  return <p className="text-xs text-red-500 mt-1">{msg}</p>
}

export function RecipeFormPage() {
  const { id } = useParams()
  const navigate = useNavigate()
  const queryClient = useQueryClient()
  const isEdit = !!id
  const recipeId = id ? Number(id) : null

  // Данные с сервера
  const { data: recipe, isLoading: recipeLoading } = useQuery({
    queryKey: ['recipe', recipeId],
    queryFn: () => getRecipe(recipeId!),
    enabled: !!recipeId,
  })
  const { data: units } = useQuery({ queryKey: ['units'], queryFn: getUnits })
  const { data: dietTypes } = useQuery({ queryKey: ['diet-types'], queryFn: getDietTypes })
  const { data: mealTypes } = useQuery({ queryKey: ['meal-types'], queryFn: getMealTypes })

  // Основная форма
  const {
    register,
    control,
    handleSubmit,
    setValue,
    watch,
    formState: { errors, isSubmitting },
  } = useForm<RecipeFormData>({
    resolver: zodResolver(recipeSchema),
    defaultValues: {
      diet_types: [6],
      meal_types: [],
      ingredients: [],
      steps: [],
      image_url: null,
    },
  })

  // Массивы для динамических полей
  const {
    fields: ingredientFields,
    append: appendIngredient,
    remove: removeIngredient,
  } = useFieldArray({
    control,
    name: 'ingredients',
    keyName: 'fieldId',
  })

  const {
    fields: stepFields,
    append: appendStep,
    remove: removeStep,
  } = useFieldArray({
    control,
    name: 'steps',
    keyName: 'fieldId',
  })

  // Отслеживаем значения
  const watchedDietTypes = watch('diet_types') ?? []
  const watchedMealTypes = watch('meal_types') ?? []
  const watchedRecipeImage = watch('image_url')

  // Сохраняем исходные данные рецепта (для сравнения)
  const [initialRecipe, setInitialRecipe] = useState<RecipeFormData | null>(null)

  // Локальный стейт для превью основного фото
  const [recipeImagePreview, setRecipeImagePreview] = useState<string>('')
  // Храним временные URL для превью шагов (ключ - "img_index" или "vid_index")
  const [stepPreviewUrls, setStepPreviewUrls] = useState<Map<string, string>>(new Map())
  // Общие ошибки сервера (не привязанные к конкретному полю)
  const [serverErrors, setServerErrors] = useState<{ general?: string[] }>({})

  // При загрузке рецепта заполняем форму и сохраняем исходное состояние
  useEffect(() => {
    if (recipe) {
      setValue('title', recipe.title)
      setValue('cook_time', recipe.cook_time)
      setValue('servings', recipe.servings)
      setValue('diet_types', recipe.diet_types)
      setValue('meal_types', recipe.meal_types)
      setValue(
        'ingredients',
        recipe.ingredients.map(ing => ({
          id: ing.id,
          ingredient: ing.ingredient,
          amount: ing.amount,
          unit: ing.unit,
        }))
      )
      setValue(
        'steps',
        recipe.steps
          .sort((a, b) => a.step_number - b.step_number)
          .map(step => ({
            id: step.id,
            description: step.description,
            timer: step.timer ?? null,
            image_url: step.image_url,
            video_url: step.video_url,
          }))
      )
      setValue('image_url', recipe.image_url)

      setInitialRecipe({
        title: recipe.title,
        cook_time: recipe.cook_time,
        servings: recipe.servings,
        diet_types: recipe.diet_types,
        meal_types: recipe.meal_types,
        image_url: recipe.image_url,
        ingredients: recipe.ingredients.map(ing => ({
          id: ing.id,
          ingredient: ing.ingredient,
          amount: ing.amount,
          unit: ing.unit,
        })),
        steps: recipe.steps.map(step => ({
          id: step.id,
          description: step.description,
          timer: step.timer,
          image_url: step.image_url,
          video_url: step.video_url,
        })),
      })

      if (recipe.image_url) {
        setRecipeImagePreview(recipe.image_url)
      }
    } else {
      setInitialRecipe(null)
      setRecipeImagePreview('')
    }
    // Очищаем предыдущие ошибки при загрузке нового рецепта
    setServerErrors({})
  }, [recipe, setValue])

  // Обработка выбора файла для основного фото
  const handleRecipeImageFileChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0]
    if (!file) return
    setValue('image_url', file)
    // Создаём временный URL для превью
    if (recipeImagePreview && !recipeImagePreview.startsWith('blob:')) {
      URL.revokeObjectURL(recipeImagePreview)
    }
    const previewUrl = URL.createObjectURL(file)
    setRecipeImagePreview(previewUrl)
  }

  // Обработка выбора медиа для шага
  const handleStepMediaChange = (index: number, type: 'image' | 'video', file: File) => {
    const url = URL.createObjectURL(file)
    const key = `${type === 'image' ? 'img' : 'vid'}_${index}`
    setStepPreviewUrls(prev => new Map(prev).set(key, url))
    if (type === 'image') {
      setValue(`steps.${index}.image_url`, file, { shouldDirty: true })
    } else {
      setValue(`steps.${index}.video_url`, file, { shouldDirty: true })
    }
  }

  // Функция для получения URL для отображения медиа шага
  const getStepMediaUrl = (index: number, type: 'image' | 'video', originalUrl: any): string | null => {
    const key = `${type === 'image' ? 'img' : 'vid'}_${index}`
    if (stepPreviewUrls.has(key)) {
      return stepPreviewUrls.get(key)!
    }
    if (typeof originalUrl === 'string') {
      return originalUrl
    }
    return null
  }

  // Удаление шага с очисткой временного URL
  const handleDeleteStep = (index: number) => {
    // Очищаем временные URL медиа, если есть
    ;['img', 'vid'].forEach(type => {
      const key = `${type}_${index}`
      const url = stepPreviewUrls.get(key)
      if (url) URL.revokeObjectURL(url)
      setStepPreviewUrls(prev => {
        const newMap = new Map(prev)
        newMap.delete(key)
        return newMap
      })
    })
    removeStep(index)
    setDeleteStepIndex(null)
  }

  // Очистка всех временных URL при размонтировании
  useEffect(() => {
    return () => {
      if (recipeImagePreview && recipeImagePreview.startsWith('blob:')) {
        URL.revokeObjectURL(recipeImagePreview)
      }
      stepPreviewUrls.forEach(url => URL.revokeObjectURL(url))
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [])

  // Переключение типов
  const toggleDietType = (dtId: number) => {
    const cur = watchedDietTypes
    setValue('diet_types', cur.includes(dtId) ? cur.filter(d => d !== dtId) : [...cur, dtId])
  }

  const toggleMealType = (mtId: number) => {
    const cur = watchedMealTypes
    setValue('meal_types', cur.includes(mtId) ? cur.filter(m => m !== mtId) : [...cur, mtId])
  }

  // Состояние для диалогов удаления
  const [deleteIngredientIndex, setDeleteIngredientIndex] = useState<number | null>(null)
  const [deleteStepIndex, setDeleteStepIndex] = useState<number | null>(null)

  // Мутация сохранения
  const saveMutation = useMutation({
    mutationFn: async (data: RecipeFormData) => {
      let savedRecipe: Awaited<ReturnType<typeof getRecipe>>

      // Определяем, изменились ли основные поля рецепта
      const recipeChanged =
        !isEdit ||
        !initialRecipe ||
        data.title !== initialRecipe.title ||
        data.cook_time !== initialRecipe.cook_time ||
        data.servings !== initialRecipe.servings ||
        JSON.stringify([...data.diet_types].sort()) !== JSON.stringify([...initialRecipe.diet_types].sort()) ||
        JSON.stringify([...data.meal_types].sort()) !== JSON.stringify([...initialRecipe.meal_types].sort()) ||
        (data.image_url instanceof File) // если выбран новый файл

      if (recipeChanged) {
        const formData = new FormData()
        formData.append('title', data.title)
        formData.append('cook_time', String(data.cook_time))
        formData.append('servings', String(data.servings))
        if (data.meal_types.length === 0) formData.append('meal_types', '')
        else data.meal_types.forEach(mt => formData.append('meal_types', String(mt)))
        if (data.diet_types.length === 0) formData.append('diet_types', '')
        else data.diet_types.forEach(dt => formData.append('diet_types', String(dt)))
        if (data.image_url instanceof File) {
          formData.append('image_url', data.image_url)
        }
        if (!isEdit) {
          savedRecipe = await createRecipe(formData)
        } else {
          savedRecipe = await updateRecipe(recipeId!, formData)
        }
      } else {
        savedRecipe = recipe!
      }

      const currentRecipeId = savedRecipe.id

      // 2. Обработка ингредиентов
      const currentIngredients = data.ingredients
      const originalIngredients = initialRecipe?.ingredients ?? []

      const addedIngredients: RecipeIngredientForm[] = []
      const updatedIngredients: { id: number; data: Partial<RecipeIngredientForm> }[] = []
      const removedIngredientIds: number[] = []

      const originalMap = new Map<number, RecipeIngredientForm>()
      originalIngredients.forEach(ing => {
        if (ing.id) originalMap.set(ing.id, ing)
      })

      currentIngredients.forEach(ing => {
        if (ing.id === undefined) {
          addedIngredients.push(ing)
        } else {
          const original = originalMap.get(ing.id)
          if (original) {
            if (
              original.ingredient !== ing.ingredient ||
              original.amount !== ing.amount ||
              original.unit !== ing.unit
            ) {
              updatedIngredients.push({
                id: ing.id,
                data: {
                  ingredient: ing.ingredient,
                  amount: ing.amount,
                  unit: ing.unit,
                },
              })
            }
            originalMap.delete(ing.id)
          }
        }
      })
      originalMap.forEach((_, id) => removedIngredientIds.push(id))

      for (const ing of addedIngredients) {
        await addRecipeIngredient(currentRecipeId, {
          ingredient: ing.ingredient,
          amount: String(ing.amount),
          unit: ing.unit,
        })
      }
      for (const upd of updatedIngredients) {
        await updateRecipeIngredient(currentRecipeId, upd.id, {
          amount: String(upd.data.amount),
          unit: upd.data.unit,
        })
      }
      for (const id of removedIngredientIds) {
        await deleteRecipeIngredient(currentRecipeId, id)
      }

      // 3. Обработка шагов
      const currentSteps = data.steps
      const originalSteps = initialRecipe?.steps ?? []

      const addedSteps: RecipeStepForm[] = []
      const updatedSteps: { id: number; data: Partial<RecipeStepForm> }[] = []
      const removedStepIds: number[] = []

      const originalStepMap = new Map<number, RecipeStepForm>()
      originalSteps.forEach(step => {
        if (step.id) originalStepMap.set(step.id, step)
      })

      currentSteps.forEach(step => {
        if (step.id === undefined) {
          addedSteps.push(step)
        } else {
          const original = originalStepMap.get(step.id)
          if (original) {
            const imageChanged = step.image_url instanceof File ||
              (typeof step.image_url === 'string' && step.image_url !== original.image_url)
            const videoChanged = step.video_url instanceof File ||
              (typeof step.video_url === 'string' && step.video_url !== original.video_url)
            if (
              original.description !== step.description ||
              original.timer !== step.timer ||
              imageChanged ||
              videoChanged
            ) {
              updatedSteps.push({
                id: step.id,
                data: {
                  description: step.description,
                  timer: step.timer,
                  image_url: step.image_url,
                  video_url: step.video_url,
                },
              })
            }
            originalStepMap.delete(step.id)
          }
        }
      })
      originalStepMap.forEach((_, id) => removedStepIds.push(id))

      // Добавление новых шагов (step_number не передаём, сервер сам определит порядок)
      for (const step of addedSteps) {
        const formData = new FormData()
        formData.append('description', step.description)
        if (step.timer) formData.append('timer', String(step.timer))
        if (step.image_url instanceof File) {
          formData.append('image_url', step.image_url)
        } else if (typeof step.image_url === 'string' && step.image_url) {
          formData.append('image_url', step.image_url)
        }
        if (step.video_url instanceof File) {
          formData.append('video_url', step.video_url)
        } else if (typeof step.video_url === 'string' && step.video_url) {
          formData.append('video_url', step.video_url)
        }
        await addRecipeStep(currentRecipeId, formData)
      }
      // Обновление существующих шагов
      for (const upd of updatedSteps) {
        const formData = new FormData()
        if (upd.data.description !== undefined) formData.append('description', upd.data.description)
        if (upd.data.timer !== undefined) formData.append('timer', String(upd.data.timer ?? ''))
        if (upd.data.image_url instanceof File) {
          formData.append('image_url', upd.data.image_url)
        } else if (typeof upd.data.image_url === 'string' && upd.data.image_url) {
          formData.append('image_url', upd.data.image_url)
        }
        if (upd.data.video_url instanceof File) {
          formData.append('video_url', upd.data.video_url)
        } else if (typeof upd.data.video_url === 'string' && upd.data.video_url) {
          formData.append('video_url', upd.data.video_url)
        }
        if ([...formData.keys()].length > 0) {
          await updateRecipeStep(currentRecipeId, upd.id, formData)
        }
      }
      // Удаление шагов
      for (const id of removedStepIds) {
        await deleteRecipeStep(currentRecipeId, id)
      }

      return savedRecipe
    },
    onSuccess: saved => {
      toast.success(isEdit ? 'Рецепт обновлён' : 'Рецепт создан')
      queryClient.invalidateQueries({ queryKey: ['recipes'] })
      setServerErrors({}) // очищаем ошибки
      if (!isEdit) {
        navigate(`/recipes/${saved.id}`)
      } else {
        queryClient.invalidateQueries({ queryKey: ['recipe', recipeId] })
      }
    },
    onError: (error: unknown) => {
      console.error('Ошибка сохранения:', error);
      if (error instanceof AxiosError && error.response) {
        const { data } = error.response;
        let errorMessages: string[] = [];

        if (typeof data === 'string') {
          // Сервер вернул HTML или текст (например, ошибка Django)
          // Пробуем извлечь сообщение об ошибке из текста
          if (data.includes('ValidationError')) {
            const match = data.match(/\{.*\}/);
            if (match) {
              const jsonStr = match[0].replace(/'/g, '"');
              errorMessages = [jsonStr];
            }
          } else {
            errorMessages = [data.substring(0, 200)];
          }
        } else if (data && typeof data === 'object') {
          // Ожидаемый JSON от DRF
          if (data.__all__) {
            errorMessages = Array.isArray(data.__all__) ? data.__all__ : [data.__all__];
          } else if (data.non_field_errors) {
            errorMessages = Array.isArray(data.non_field_errors) ? data.non_field_errors : [data.non_field_errors];
          } else {
            errorMessages = Object.entries(data).flatMap(([field, msgs]) => {
              const msgsArray = Array.isArray(msgs) ? msgs : [String(msgs)];
              return msgsArray.map(msg => `${field}: ${msg}`);
            });
          }
        }

        setServerErrors({ general: errorMessages });
        toast.error('Ошибка сохранения. Проверьте правильность заполнения.');
      } else {
        toast.error('Не удалось сохранить рецепт');
      }
    }
  })

  const onSubmit = (data: RecipeFormData) => {
    if (isEdit && initialRecipe) {
      const isRecipeChanged =
        data.title !== initialRecipe.title ||
        data.cook_time !== initialRecipe.cook_time ||
        data.servings !== initialRecipe.servings ||
        JSON.stringify([...data.diet_types].sort()) !== JSON.stringify([...initialRecipe.diet_types].sort()) ||
        JSON.stringify([...data.meal_types].sort()) !== JSON.stringify([...initialRecipe.meal_types].sort()) ||
        (data.image_url instanceof File)

      const ingredientsChanged =
        JSON.stringify(data.ingredients) !== JSON.stringify(initialRecipe.ingredients)

      const stepsChanged =
        JSON.stringify(data.steps.map(s => ({ 
          ...s, 
          image_url: typeof s.image_url === 'string' ? s.image_url : (s.image_url instanceof File ? 'FILE' : null),
          video_url: typeof s.video_url === 'string' ? s.video_url : (s.video_url instanceof File ? 'FILE' : null)
        }))) !==
        JSON.stringify(initialRecipe.steps.map(s => ({ ...s, image_url: s.image_url, video_url: s.video_url })))

      if (!isRecipeChanged && !ingredientsChanged && !stepsChanged) {
        toast.info('Нет изменений для сохранения')
        return
      }
    }
    saveMutation.mutate(data)
  }

  if (recipeLoading) {
    return (
      <div className="space-y-4">
        <Skeleton className="h-8 w-48" />
        <div className="card p-6 space-y-4">
          {[...Array(5)].map((_, i) => (
            <Skeleton key={i} className="h-10 w-full" />
          ))}
        </div>
      </div>
    )
  }

  // Динамические данные для предпросмотра (используем форму и исходные данные для названий)
  const previewData = recipe ? {
    title: recipe.title,
    image_url: recipeImagePreview || (typeof watchedRecipeImage === 'string' ? watchedRecipeImage : ''),
    servings: recipe.servings,
    cook_time: recipe.cook_time,
    total_calories: recipe.total_calories,
    total_proteins: recipe.total_proteins,
    total_fats: recipe.total_fats,
    total_carbs: recipe.total_carbs,
    ingredients: recipe.ingredients,
    steps: recipe.steps,
  } : undefined

  return (
    <div className="flex gap-8 items-start">
      <div className="flex-1 min-w-0 space-y-6">
        {/* Заголовок */}
        <div className="flex items-center gap-4">
          <button className="btn-ghost p-2" onClick={() => navigate('/recipes')}>
            <ArrowLeft className="w-5 h-5" />
          </button>
          <div className="flex-1">
            <h1 className="text-2xl font-bold text-[var(--text-primary)]">
              {isEdit ? 'Редактировать рецепт' : 'Новый рецепт'}
            </h1>
            {isEdit && <p className="text-sm text-[var(--text-muted)]">ID: {recipeId}</p>}
          </div>
        </div>

        <form onSubmit={handleSubmit(onSubmit)}>
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
                <input {...register('title')} className="input" placeholder="Введите название рецепта" />
                <FieldError msg={errors.title?.message} />
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
                <FieldError msg={errors.cook_time?.message} />
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
                <FieldError msg={errors.servings?.message} />
              </div>

              <div className="md:col-span-2">
                <label className="block text-sm font-medium text-[var(--text-primary)] mb-1.5">
                  <ImageIcon className="w-3.5 h-3.5 inline mr-1" />
                  Изображение
                </label>
                <div className="flex gap-2 items-start">
                  <label className="btn-secondary cursor-pointer flex-shrink-0">
                    <ImageIcon className="w-4 h-4" />
                    Загрузить
                    <input
                      type="file"
                      accept="image/*"
                      className="hidden"
                      onChange={handleRecipeImageFileChange}
                    />
                  </label>
                </div>
                {recipeImagePreview && (
                  <img
                    src={recipeImagePreview}
                    alt="Preview"
                    className="mt-2 h-32 rounded-lg object-cover"
                  />
                )}
              </div>
            </div>

            {/* Типы диет */}
            <div>
              <label className="block text-sm font-medium text-[var(--text-primary)] mb-2">
                Типы диет
              </label>
              <div className="flex flex-wrap gap-2">
                {dietTypes?.map(dt => (
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
              <FieldError msg={errors.diet_types?.message} />
            </div>

            {/* Приёмы пищи */}
            <div>
              <label className="block text-sm font-medium text-[var(--text-primary)] mb-2">
                Приёмы пищи
              </label>
              <div className="flex flex-wrap gap-2">
                {mealTypes?.map(mt => (
                  <button
                    key={mt.id}
                    type="button"
                    onClick={() => toggleMealType(mt.id)}
                    className={`px-3 py-1.5 rounded-lg text-sm font-medium border transition-colors ${
                      watchedMealTypes.includes(mt.id)
                        ? 'bg-primary-600 text-white border-primary-600'
                        : 'bg-[var(--bg-secondary)] text-[var(--text-secondary)] border-[var(--border-color)] hover:border-primary-400'
                    }`}
                  >
                    {mt.name}
                  </button>
                ))}
              </div>
              <FieldError msg={errors.meal_types?.message} />
            </div>

            {/* Ингредиенты */}
            <div className="space-y-4">
              <h2 className="font-semibold text-[var(--text-primary)]">
                Ингредиенты ({ingredientFields.length})
              </h2>

              {!isEdit && (
                <p className="text-sm text-amber-600 dark:text-amber-400 bg-amber-50 dark:bg-amber-900/20 border border-amber-200 dark:border-amber-800 rounded-lg px-4 py-2">
                  Сначала создайте рецепт, затем добавьте ингредиенты
                </p>
              )}

              {ingredientFields.length > 0 && (
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
                      {ingredientFields.map((field, index) => {
                        const error = (errors.ingredients as any)?.[index]
                        const initialIngredientName = (() => {
                          if (!isEdit || !recipe) return undefined
                          const ingredientId = field.id // id ингредиента в БД (если есть)
                          if (!ingredientId) return undefined
                          const found = recipe.ingredients.find(ing => ing.id === ingredientId)
                          return found?.ingredient_name
                        })()
                        return (
                          <tr key={field.fieldId} className="border-b border-[var(--border-color)] last:border-0">
                            <td className="table-cell">
                              <Controller
                                control={control}
                                name={`ingredients.${index}.ingredient`}
                                render={({ field: controllerField }) => (
                                  <IngredientAutocomplete
                                    initialValue={initialIngredientName}
                                    value={String(controllerField.value)}
                                    onChange={(id) => {
                                      controllerField.onChange(Number(id))
                                    }}
                                    error={!!error?.ingredient}
                                  />
                                )}
                              />
                              <FieldError msg={error?.ingredient?.message} />
                            </td>
                            <td className="table-cell">
                              <input
                                type="number"
                                className="input w-24 mx-auto block"
                                {...register(`ingredients.${index}.amount`, { valueAsNumber: true })}
                                step="0.01"
                                min={0}
                              />
                              <FieldError msg={error?.amount?.message} />
                            </td>
                            <td className="table-cell">
                              <select
                                className="input"
                                {...register(`ingredients.${index}.unit`, { valueAsNumber: true })}
                              >
                                <option value="">Единица</option>
                                {units?.map(u => (
                                  <option key={u.id} value={u.id}>{u.name}</option>
                                ))}
                              </select>
                              <FieldError msg={error?.unit?.message} />
                            </td>
                            <td className="table-cell text-right">
                              <button
                                type="button"
                                className="btn-ghost p-1.5 text-red-500 hover:text-red-600"
                                onClick={() => setDeleteIngredientIndex(index)}
                                title="Удалить"
                              >
                                <Trash2 className="w-3.5 h-3.5" />
                              </button>
                            </td>
                          </tr>
                        )
                      })}
                    </tbody>
                  </table>
                </div>
              )}

              {isEdit && (
                <button
                  type="button"
                  className="btn-secondary"
                  onClick={() =>
                    appendIngredient({
                      ingredient: 0,
                      amount: 0,
                      unit: 0,
                    })
                  }
                >
                  <Plus className="w-4 h-4" /> Добавить ингредиент
                </button>
              )}
            </div>

            {/* Шаги приготовления */}
            <div className="space-y-4">
              <h2 className="font-semibold text-[var(--text-primary)]">
                Шаги приготовления ({stepFields.length})
              </h2>

              {!isEdit && (
                <p className="text-sm text-amber-600 dark:text-amber-400 bg-amber-50 dark:bg-amber-900/20 border border-amber-200 dark:border-amber-800 rounded-lg px-4 py-2">
                  Сначала создайте рецепт, затем добавьте шаги
                </p>
              )}

              {stepFields.map((field, index) => {
                const error = (errors.steps as any)?.[index]
                const displayImageUrl = getStepImageUrl(field, index)
                return (
                  <div key={field.fieldId} className="flex gap-3 p-4 border border-[var(--border-color)] rounded-lg">
                    <div className="w-7 h-7 rounded-full bg-primary-600 text-white text-sm font-semibold flex items-center justify-center flex-shrink-0">
                      {index + 1}
                    </div>
                    <div className="flex-1 space-y-2">
                      <textarea
                        className="input resize-none w-full"
                        rows={2}
                        {...register(`steps.${index}.description`)}
                      />
                      <FieldError msg={error?.description?.message} />
                      <div className="flex items-center gap-3 flex-wrap">
                        <div className="flex items-center gap-2">
                          <Timer className="w-3.5 h-3.5 text-[var(--text-muted)]" />
                          <input
                            type="number"
                            className="input w-28"
                            placeholder="Таймер (мин)"
                            {...register(`steps.${index}.timer`, { valueAsNumber: true })}
                          />
                        </div>
                        <div className="flex gap-2">
                          <label className="btn-ghost text-xs cursor-pointer flex items-center gap-1">
                            <ImageIcon className="w-3 h-3" />
                            {getStepMediaUrl(index, 'image', field.image_url) ? 'Сменить фото' : 'Загрузить фото'}
                            <input
                              type="file"
                              accept="image/*"
                              className="hidden"
                              onChange={e => {
                                const file = e.target.files?.[0]
                                if (file) handleStepMediaChange(index, 'image', file)
                              }}
                            />
                          </label>
                          <label className="btn-ghost text-xs cursor-pointer flex items-center gap-1">
                            <Video className="w-3 h-3" />
                            {getStepMediaUrl(index, 'video', field.video_url) ? 'Сменить видео' : 'Загрузить видео'}
                            <input
                              type="file"
                              accept="video/*"
                              className="hidden"
                              onChange={e => {
                                const file = e.target.files?.[0]
                                if (file) handleStepMediaChange(index, 'video', file)
                              }}
                            />
                          </label>
                        </div>
                      </div>
                      <FieldError msg={error?.timer?.message} />
                      
                      <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
                        {getStepMediaUrl(index, 'image', field.image_url) && (
                          <div className="space-y-1">
                            <p className="text-[10px] uppercase font-bold text-gray-400">Фото шага</p>
                            <img
                              src={getStepMediaUrl(index, 'image', field.image_url)!}
                              alt={`Шаг ${index + 1}`}
                              className="h-32 w-full rounded-lg object-cover border"
                            />
                          </div>
                        )}
                        {getStepMediaUrl(index, 'video', field.video_url) && (
                          <div className="space-y-1">
                            <p className="text-[10px] uppercase font-bold text-gray-400">Видео шага</p>
                            <video
                              src={getStepMediaUrl(index, 'video', field.video_url)!}
                              className="h-32 w-full rounded-lg object-cover border bg-black"
                              controls
                            />
                          </div>
                        )}
                      </div>
                    </div>
                    <button
                      type="button"
                      className="btn-ghost p-1.5 text-red-500 hover:text-red-600 flex-shrink-0"
                      onClick={() => setDeleteStepIndex(index)}
                    >
                      <Trash2 className="w-3.5 h-3.5" />
                    </button>
                  </div>
                )
              })}

              {isEdit && (
                <button
                  type="button"
                  className="btn-secondary"
                  onClick={() =>
                    appendStep({
                      description: '',
                      timer: null,
                      image_url: null,
                      video_url: null,
                    })
                  }
                >
                  <Plus className="w-4 h-4" /> Добавить шаг
                </button>
              )}
            </div>

            {/* Отображение общих ошибок сервера перед кнопкой */}
            {serverErrors.general && serverErrors.general.length > 0 && (
              <div className="bg-red-50 dark:bg-red-900/20 border border-red-200 dark:border-red-800 rounded-lg p-3">
                <p className="text-sm font-medium text-red-800 dark:text-red-200 mb-1">Ошибки сохранения:</p>
                <ul className="text-xs text-red-700 dark:text-red-300 list-disc pl-5 space-y-0.5">
                  {serverErrors.general.map((err, i) => (
                    <li key={i}>{err}</li>
                  ))}
                </ul>
              </div>
            )}

            <div className="flex justify-end pt-2">
              <button
                type="submit"
                className="btn-primary"
                disabled={isSubmitting || saveMutation.isPending}
              >
                {isSubmitting || saveMutation.isPending ? (
                  <span className="w-4 h-4 border-2 border-white/30 border-t-white rounded-full animate-spin" />
                ) : (
                  <Save className="w-4 h-4" />
                )}
                {isEdit ? 'Сохранить изменения' : 'Создать рецепт'}
              </button>
            </div>
          </div>
        </form>
      </div>

      {/* Превью (только для редактирования) */}
      {isEdit && (
        <div className="hidden xl:block sticky top-6 flex-shrink-0">
          <p className="text-xs text-[var(--text-muted)] text-center mb-2">Предпросмотр</p>
          <RecipePhonePreview data={previewData} />
        </div>
      )}

      {/* Диалоги удаления */}
      <ConfirmDialog
        open={deleteIngredientIndex !== null}
        title="Удалить ингредиент?"
        description="Ингредиент будет удалён из рецепта."
        onConfirm={() => {
          if (deleteIngredientIndex !== null) {
            removeIngredient(deleteIngredientIndex)
            setDeleteIngredientIndex(null)
          }
        }}
        onCancel={() => setDeleteIngredientIndex(null)}
      />

      <ConfirmDialog
        open={deleteStepIndex !== null}
        title="Удалить шаг?"
        description="Шаг будет удалён из рецепта безвозвратно."
        onConfirm={() => {
          if (deleteStepIndex !== null) {
            handleDeleteStep(deleteStepIndex)
          }
        }}
        onCancel={() => setDeleteStepIndex(null)}
      />
    </div>
  )
}
