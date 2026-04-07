import { useState, useRef, useEffect, useCallback } from 'react'
import { createPortal } from 'react-dom'
import { Search, X } from 'lucide-react'
import api from '@/lib/axios'
import type { Ingredient } from '@/types'

interface IngredientAutocompleteProps {
  initialValue?: string
  value: string
  onChange: (id: string, name: string) => void
  placeholder?: string
  error?: boolean
}

export function IngredientAutocomplete({
  initialValue,
  value,
  onChange,
  placeholder = 'Поиск ингредиента...',
  error = false,
}: IngredientAutocompleteProps) {
  const [query, setQuery] = useState('')
  const [results, setResults] = useState<Ingredient[]>([])
  const [open, setOpen] = useState(false)
  const [loading, setLoading] = useState(false)
  const [selectedName, setSelectedName] = useState('')
  const [isFocused, setIsFocused] = useState(false)
  const [dropdownPosition, setDropdownPosition] = useState({ top: 0, left: 0, width: 0 })

  const containerRef = useRef<HTMLDivElement>(null)
  const inputRef = useRef<HTMLInputElement>(null)
  const timerRef = useRef<ReturnType<typeof setTimeout> | null>(null)

  // Обновление позиции выпадающего списка
  const updateDropdownPosition = useCallback(() => {
    if (inputRef.current) {
      const rect = inputRef.current.getBoundingClientRect()
      setDropdownPosition({
        top: rect.bottom + window.scrollY,
        left: rect.left + window.scrollX,
        width: rect.width,
      })
    }
  }, [])

  // При открытии списка обновляем позицию
  useEffect(() => {
    if (open) {
      updateDropdownPosition()
      window.addEventListener('scroll', updateDropdownPosition, true)
      window.addEventListener('resize', updateDropdownPosition)
    }
    return () => {
      window.removeEventListener('scroll', updateDropdownPosition, true)
      window.removeEventListener('resize', updateDropdownPosition)
    }
  }, [open, updateDropdownPosition])

  // Закрытие при клике вне
  useEffect(() => {
    const handleClick = (e: MouseEvent) => {
      if (containerRef.current && !containerRef.current.contains(e.target as Node)) {
        setOpen(false)
      }
    }
    document.addEventListener('mousedown', handleClick)
    return () => document.removeEventListener('mousedown', handleClick)
  }, [])

  // Установка начального названия, только если поле не в фокусе
  useEffect(() => {
    if (value && initialValue && !selectedName && !query && !isFocused) {
      setSelectedName(initialValue)
    }
    if (!value && (selectedName || query)) {
      setSelectedName('')
      setQuery('')
      setResults([])
    }
  }, [value, initialValue, selectedName, query, isFocused])

  const search = useCallback(async (q: string) => {
    if (!q.trim()) {
      setResults([])
      return
    }
    setLoading(true)
    try {
      const res = await api.get<{ results: Ingredient[] }>('/ingredients/', {
        params: { search: q, page_size: 10 },
      })
      setResults(res.data.results)
      setOpen(true)
    } catch {
      setResults([])
    } finally {
      setLoading(false)
    }
  }, [])

  const handleInput = (e: React.ChangeEvent<HTMLInputElement>) => {
    const q = e.target.value
    setQuery(q)
    if (value) {
      onChange('', '')
      setSelectedName('')
    }
    if (timerRef.current) clearTimeout(timerRef.current)
    timerRef.current = setTimeout(() => void search(q), 300)
  }

  const handleSelect = (ing: Ingredient) => {
    onChange(String(ing.id), ing.name)
    setSelectedName(ing.name)
    setQuery(ing.name)
    setResults([])
    setOpen(false)
    inputRef.current?.focus()
  }

  const handleClear = () => {
    onChange('', '')
    setSelectedName('')
    setQuery('')
    setResults([])
    setOpen(false)
    inputRef.current?.focus()
  }

  const handleFocus = () => {
    setIsFocused(true)
    if (selectedName && !query) {
      setQuery('')
    }
    // При фокусе обновляем позицию на случай, если прокрутка изменилась
    updateDropdownPosition()
  }

  const handleBlur = () => {
    setIsFocused(false)
    setTimeout(() => {
      if (!value && !query) {
        setResults([])
      }
      setOpen(false)
    }, 150)
  }

  const displayValue = isFocused ? query : selectedName || query

  return (
    <div className="relative" ref={containerRef}>
      <div className="relative">
        <Search className="absolute left-3 top-1/2 -translate-y-1/2 w-4 h-4 text-[var(--text-muted)] pointer-events-none" />
        <input
          ref={inputRef}
          type="text"
          className={`input pl-9 pr-8 ${selectedName ? 'text-[var(--text-primary)] font-medium' : ''} ${error ? 'border-red-500 focus:border-red-500' : ''}`}
          placeholder={placeholder}
          value={displayValue}
          onChange={handleInput}
          onFocus={handleFocus}
          onBlur={handleBlur}
        />
        {(selectedName || query) && (
          <button
            type="button"
            className="absolute right-2 top-1/2 -translate-y-1/2 text-[var(--text-muted)] hover:text-[var(--text-primary)] p-1"
            onClick={handleClear}
          >
            <X className="w-3.5 h-3.5" />
          </button>
        )}
      </div>

      {open && (results.length > 0 || loading) &&
        createPortal(
          <div
            className="fixed z-[1000] bg-[var(--bg-primary)] border border-[var(--border-color)] rounded-xl shadow-lg overflow-hidden"
            style={{
              top: dropdownPosition.top,
              left: dropdownPosition.left,
              width: dropdownPosition.width,
              maxHeight: '300px',
              overflowY: 'auto',
            }}
          >
            {loading && (
              <div className="px-3 py-2 text-sm text-[var(--text-muted)]">Поиск...</div>
            )}
            {results.map((ing) => (
              <button
                key={ing.id}
                type="button"
                className="w-full text-left px-3 py-2.5 text-sm hover:bg-[var(--bg-secondary)] flex items-center justify-between gap-2 transition-colors"
                onMouseDown={(e) => {
                  e.preventDefault()
                  handleSelect(ing)
                }}
              >
                <span className="text-[var(--text-primary)] font-medium">{ing.name}</span>
                {ing.category_name && (
                  <span className="text-xs text-[var(--text-muted)] flex-shrink-0">{ing.category_name}</span>
                )}
              </button>
            ))}
            {!loading && results.length === 0 && (
              <div className="px-3 py-2 text-sm text-[var(--text-muted)]">Ничего не найдено</div>
            )}
          </div>,
          document.body
        )}
    </div>
  )
}