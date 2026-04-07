import { X, Timer } from 'lucide-react'
import type { RecipeIngredient, RecipeStep } from '@/types'

export interface PreviewData {
  title: string
  image_url?: string
  servings: number
  cook_time: number
  total_calories: number | string
  total_proteins: number | string
  total_fats: number | string
  total_carbs: number | string
  ingredients: RecipeIngredient[]
  steps: RecipeStep[]
}

function PhoneBase({ children }: { children: React.ReactNode }) {
  return (
    <div
      className="relative rounded-[38px] overflow-hidden flex-shrink-0"
      style={{
        width: 300,
        height: 620,
        background: '#1a1a1a',
        boxShadow: '0 0 0 7px #1a1a1a, 0 0 0 9px #333, 0 20px 60px rgba(0,0,0,0.5)',
      }}
    >
      {/* Notch */}
      <div className="absolute top-0 left-1/2 -translate-x-1/2 w-20 h-4 bg-[#1a1a1a] rounded-b-2xl z-10" />

      {/* Screen */}
      <div className="absolute inset-[3px] rounded-[35px] overflow-hidden bg-[#FAFAF5]">
        {children}

        {/* Bottom nav */}
        <div
          className="absolute bottom-0 left-0 right-0 bg-white border-t border-gray-100 flex items-center justify-around"
          style={{ height: 46 }}
        >
          {['Меню', 'Продукты', 'Статистика', 'Профиль'].map((label) => (
            <div key={label} className="flex flex-col items-center gap-0.5">
              <div className="w-3.5 h-3.5 rounded bg-gray-200" />
              <span className="text-[8px] text-gray-400">{label}</span>
            </div>
          ))}
        </div>
      </div>
    </div>
  )
}

export function RecipePhonePreview({ data }: { data: PreviewData | undefined }) {
  if (data === undefined) {
    return (
      <PhoneBase>
        <div style={{
          width: '100%',
          height: '100%',
          background: '#e0e0e0',
          display: 'flex',
          flexDirection: 'column',
          alignItems: 'center',
          justifyContent: 'center',
          gap: 12,
        }}>
          <svg width="40" height="40" viewBox="0 0 24 24" fill="none" stroke="#aaa" strokeWidth="1.5">
            <circle cx="12" cy="12" r="10" />
            <line x1="12" y1="8" x2="12" y2="12" />
            <line x1="12" y1="16" x2="12.01" y2="16" />
          </svg>
          <span style={{ fontSize: 13, color: '#888', textAlign: 'center', padding: '0 32px', lineHeight: 1.5 }}>
            Не удалось загрузить превью
          </span>
        </div>
      </PhoneBase>
    )
  }
  const sortedSteps = [...data.steps].sort((a, b) => a.step_number - b.step_number)

  return (
    <PhoneBase>
      <div className="h-full overflow-y-auto" style={{ scrollbarWidth: 'none' }}>
        {/* Hero image */}
        <div className="relative">
          {data.image_url ? (
            <img
              src={data.image_url}
              alt={data.title}
              className="w-full object-cover"
              style={{ height: 190 }}
            />
          ) : (
            <div
              className="w-full flex items-center justify-center bg-gray-200"
              style={{ height: 190 }}
            >
              <span className="text-gray-400 text-xs">Нет фото</span>
            </div>
          )}
          <div className="absolute top-6 left-0 right-0 flex justify-between px-3">
            <div className="w-7 h-7 bg-white/85 backdrop-blur-sm rounded-full flex items-center justify-center shadow text-gray-700 text-base font-medium leading-none">
              +
            </div>
            <div className="w-7 h-7 bg-white/85 backdrop-blur-sm rounded-full flex items-center justify-center shadow text-gray-600 text-sm">
              ☆
            </div>
          </div>
        </div>

        {/* Content */}
        <div className="px-3 pt-3 pb-16 bg-[#FAFAF5]">
          {/* Title + servings */}
          <div className="flex items-start justify-between gap-2 mb-2">
            <h1 className="text-xs font-bold text-gray-900 leading-snug flex-1">
              {data.title || 'Название рецепта'}
            </h1>
            <span className="text-[#F4A922] font-bold text-xs whitespace-nowrap">
              {data.servings} порц.
            </span>
          </div>

          {/* Nutrition */}
          <div
            className="grid grid-cols-4 rounded-xl mb-3 py-2"
            style={{ background: '#FFF3D0' }}
          >
            {[
              { label: 'ккал', value: Math.round(Number(data.total_calories)) },
              { label: 'белки', value: Number(data.total_proteins).toFixed(1) },
              { label: 'жиры', value: Number(data.total_fats).toFixed(1) },
              { label: 'углев.', value: Number(data.total_carbs).toFixed(1) },
            ].map(({ label, value }, i, arr) => (
              <div
                key={label}
                className={`text-center ${i < arr.length - 1 ? 'border-r border-[#F4A922]/30' : ''}`}
              >
                <div className="text-xs font-bold text-gray-900">{value}</div>
                <div className="text-[9px] text-gray-500">{label}</div>
              </div>
            ))}
          </div>

          {/* Ingredients */}
          {data.ingredients.length > 0 && (
            <div className="rounded-2xl px-3 py-2.5 mb-3" style={{ background: '#FFF3D0' }}>
              <h2 className="font-bold text-gray-900 text-xs mb-1.5">Продукты</h2>
              <div className="space-y-1">
                {data.ingredients.map((ing) => (
                  <div key={ing.id} className="text-[11px] text-gray-800">
                    {ing.ingredient_name}{' '}
                    <span className="text-gray-500">
                      - {ing.amount} {ing.unit_name}
                    </span>
                  </div>
                ))}
              </div>
            </div>
          )}

          {/* Steps */}
          {sortedSteps.length > 0 && (
            <div>
              <h2 className="font-bold text-gray-900 text-xs mb-2">
                Пошаговое фото рецепта
              </h2>
              <div className="space-y-2.5">
                {sortedSteps.map((step) => (
                  <div key={step.id} className="flex gap-2">
                    {step.image_url ? (
                      <img
                        src={step.image_url}
                        alt={`Шаг ${step.step_number}`}
                        className="rounded-xl object-cover flex-shrink-0"
                        style={{ width: 76, height: 60 }}
                      />
                    ) : (
                      <div
                        className="rounded-xl bg-gray-100 flex-shrink-0 flex items-center justify-center"
                        style={{ width: 76, height: 60 }}
                      >
                        <span className="text-gray-300 text-[9px]">нет фото</span>
                      </div>
                    )}
                    <div className="flex-1 min-w-0">
                      <span
                        className="inline-block text-[9px] font-semibold px-1.5 py-0.5 rounded-full mb-0.5"
                        style={{ background: '#FFF3D0', color: '#C47A00' }}
                      >
                        Шаг {step.step_number}
                      </span>
                      <p className="text-[10px] text-gray-700 leading-relaxed">
                        {step.description}
                      </p>
                      {!!step.timer && (
                        <p className="text-[9px] text-gray-400 mt-0.5 flex items-center gap-0.5">
                          <Timer className="w-2 h-2" />
                          {step.timer} минут
                        </p>
                      )}
                    </div>
                  </div>
                ))}
              </div>
            </div>
          )}

          {data.ingredients.length === 0 && sortedSteps.length === 0 && (
            <div className="text-center py-6 text-gray-400 text-[11px]">
              Добавьте ингредиенты и шаги
            </div>
          )}
        </div>
      </div>
    </PhoneBase>
  )
}

interface RecipePreviewModalProps {
  open: boolean
  onClose: () => void
  data?: PreviewData
  loading?: boolean
}

export function RecipePreviewModal({ open, onClose, data, loading }: RecipePreviewModalProps) {
  if (!open) return null

  return (
    <div
      className="fixed inset-0 z-50 flex items-center justify-center bg-black/70 p-4"
      onClick={onClose}
    >
      <div className="relative flex flex-col items-center" onClick={(e) => e.stopPropagation()}>
        <button
          onClick={onClose}
          className="absolute -top-9 right-0 text-white/70 hover:text-white transition-colors"
        >
          <X className="w-6 h-6" />
        </button>

        <p className="text-white/40 text-[11px] mb-2">Предпросмотр в приложении</p>

        {loading || !data ? (
          <div
            className="rounded-[38px] flex items-center justify-center bg-[#1a1a1a]"
            style={{ width: 300, height: 620 }}
          >
            <span className="w-8 h-8 border-2 border-white/20 border-t-white/70 rounded-full animate-spin" />
          </div>
        ) : (
          <RecipePhonePreview data={data} />
        )}
      </div>
    </div>
  )
}
