import { useState, useRef } from 'react'
import { Search, X, Check, Loader2, ImageOff } from 'lucide-react'
import { searchImages, ImageSearchRateLimitError } from '@/api/auth'
import type { ImageSearchResult } from '@/api/auth'

interface ImageSearchModalProps {
  open: boolean
  onClose: () => void
  onSelect: (url: string) => void
}

export function ImageSearchModal({ open, onClose, onSelect }: ImageSearchModalProps) {
  const [query, setQuery] = useState('')
  const [images, setImages] = useState<ImageSearchResult[]>([])
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState('')
  const [isRateLimit, setIsRateLimit] = useState(false)
  const [selected, setSelected] = useState<string | null>(null)
  const [failedImages, setFailedImages] = useState<Set<string>>(new Set())
  const inputRef = useRef<HTMLInputElement>(null)

  if (!open) return null

  const handleSearch = async () => {
    const q = query.trim()
    if (!q) return
    setLoading(true)
    setError('')
    setIsRateLimit(false)
    setImages([])
    setSelected(null)
    setFailedImages(new Set())
    try {
      const result = await searchImages(q, 12)
      if (result.images.length === 0) {
        setError('Ничего не найдено. Попробуйте другой запрос.')
      }
      setImages(result.images ?? [])
    } catch (e: unknown) {
      if (e instanceof ImageSearchRateLimitError) {
        setIsRateLimit(true)
        setError((e as ImageSearchRateLimitError).message)
      } else {
        setError('Ошибка поиска. Попробуйте позже.')
      }
    } finally {
      setLoading(false)
    }
  }

  const handleKeyDown = (e: React.KeyboardEvent) => {
    if (e.key === 'Enter') void handleSearch()
  }

  const handleConfirm = () => {
    if (selected) {
      onSelect(selected)
      handleClose()
    }
  }

  const handleClose = () => {
    setQuery('')
    setImages([])
    setSelected(null)
    setError('')
    setIsRateLimit(false)
    setFailedImages(new Set())
    onClose()
  }

  const markFailed = (url: string) => {
    setFailedImages((prev) => new Set(prev).add(url))
  }

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-4">
      <div className="absolute inset-0 bg-black/60 backdrop-blur-sm" onClick={handleClose} />
      <div className="relative bg-[var(--bg-primary)] rounded-2xl border border-[var(--border-color)] shadow-2xl w-full max-w-3xl max-h-[90vh] flex flex-col">

        {/* Header */}
        <div className="flex items-center justify-between px-5 py-4 border-b border-[var(--border-color)]">
          <h2 className="font-semibold text-[var(--text-primary)] text-lg">Поиск изображения</h2>
          <button className="btn-ghost p-1.5" onClick={handleClose}>
            <X className="w-5 h-5" />
          </button>
        </div>

        {/* Search bar */}
        <div className="px-5 py-4 border-b border-[var(--border-color)]">
          <div className="flex gap-2">
            <div className="relative flex-1">
              <Search className="absolute left-3 top-1/2 -translate-y-1/2 w-4 h-4 text-[var(--text-muted)]" />
              <input
                ref={inputRef}
                className="input pl-9"
                type="text"
                placeholder="Введите название блюда на английском..."
                value={query}
                onChange={(e) => setQuery(e.target.value)}
                onKeyDown={handleKeyDown}
                autoFocus
              />
            </div>
            <button
              className="btn-primary px-5"
              onClick={() => void handleSearch()}
              disabled={loading || !query.trim()}
            >
              {loading ? <Loader2 className="w-4 h-4 animate-spin" /> : <Search className="w-4 h-4" />}
              Найти
            </button>
          </div>
        </div>

        {/* Results */}
        <div className="flex-1 overflow-y-auto p-5">
          {loading && (
            <div className="flex flex-col items-center justify-center py-16 gap-3">
              <Loader2 className="w-8 h-8 text-primary-600 animate-spin" />
              <p className="text-sm text-[var(--text-secondary)]">Ищем изображения...</p>
            </div>
          )}

          {error && !loading && (
            <div className="flex flex-col items-center justify-center py-16 gap-3">
              <ImageOff className="w-8 h-8 text-[var(--text-muted)]" />
              <p className="text-sm text-[var(--text-secondary)]">{error}</p>
            </div>
          )}

          {!loading && !error && images.length === 0 && (
            <div className="flex flex-col items-center justify-center py-16 gap-2 text-center">
              <Search className="w-10 h-10 text-[var(--text-muted)]" />
              <p className="text-sm text-[var(--text-secondary)]">
                Введите запрос и нажмите "Найти"
              </p>
              <p className="text-xs text-[var(--text-muted)]">
                Совет: лучше искать на английском языке
              </p>
            </div>
          )}

          {images.length > 0 && (
            <div className="grid grid-cols-2 sm:grid-cols-3 md:grid-cols-4 gap-3">
              {images.map((img) => {
                const isFailed = failedImages.has(img.url)
                const isSelected = selected === img.url
                if (isFailed) return null
                return (
                  <button
                    key={img.url}
                    type="button"
                    onClick={() => setSelected(isSelected ? null : img.url)}
                    className={`
                      relative rounded-xl overflow-hidden aspect-square border-2 transition-all duration-150
                      ${isSelected
                        ? 'border-primary-500 ring-2 ring-primary-500/30 scale-[0.97]'
                        : 'border-transparent hover:border-primary-300 hover:scale-[0.98]'
                      }
                    `}
                  >
                    <img
                      src={img.thumbnail || img.url}
                      alt={img.title}
                      className="w-full h-full object-cover bg-[var(--bg-tertiary)]"
                      onError={() => markFailed(img.url)}
                    />
                    {isSelected && (
                      <div className="absolute inset-0 bg-primary-600/20 flex items-center justify-center">
                        <div className="w-8 h-8 rounded-full bg-primary-600 flex items-center justify-center shadow-lg">
                          <Check className="w-5 h-5 text-white" />
                        </div>
                      </div>
                    )}
                  </button>
                )
              })}
            </div>
          )}
        </div>

        {/* Footer */}
        <div className="px-5 py-4 border-t border-[var(--border-color)] flex items-center justify-between">
          <p className="text-sm text-[var(--text-muted)]">
            {selected ? 'Изображение выбрано' : images.length > 0 ? 'Нажмите на изображение для выбора' : ''}
          </p>
          <div className="flex gap-3">
            <button className="btn-secondary" onClick={handleClose}>
              Отмена
            </button>
            <button
              className="btn-primary"
              onClick={handleConfirm}
              disabled={!selected}
            >
              <Check className="w-4 h-4" />
              Вставить
            </button>
          </div>
        </div>
      </div>
    </div>
  )
}
