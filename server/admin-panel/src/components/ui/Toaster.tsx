import { useEffect, useState } from 'react'
import { X, CheckCircle, AlertCircle, Info } from 'lucide-react'
import { cn } from '@/lib/utils'

export type ToastType = 'success' | 'error' | 'info'

export interface Toast {
  id: string
  message: string
  type: ToastType
}

type ToastListener = (toast: Toast) => void
const listeners: ToastListener[] = []

export const toast = {
  success: (message: string) => emit({ id: crypto.randomUUID(), message, type: 'success' }),
  error: (message: string) => emit({ id: crypto.randomUUID(), message, type: 'error' }),
  info: (message: string) => emit({ id: crypto.randomUUID(), message, type: 'info' }),
}

function emit(t: Toast) {
  listeners.forEach((l) => l(t))
}

export function Toaster() {
  const [toasts, setToasts] = useState<Toast[]>([])

  useEffect(() => {
    const handler = (t: Toast) => {
      setToasts((prev) => [...prev, t])
      setTimeout(() => {
        setToasts((prev) => prev.filter((x) => x.id !== t.id))
      }, 4000)
    }
    listeners.push(handler)
    return () => {
      const idx = listeners.indexOf(handler)
      if (idx >= 0) listeners.splice(idx, 1)
    }
  }, [])

  const remove = (id: string) => setToasts((prev) => prev.filter((x) => x.id !== id))

  return (
    <div className="fixed top-4 right-4 z-50 flex flex-col gap-2 max-w-sm w-full pointer-events-none">
      {toasts.map((t) => (
        <div
          key={t.id}
          className={cn(
            'flex items-start gap-3 rounded-xl border p-4 shadow-lg pointer-events-auto',
            t.type === 'success' && 'bg-[var(--bg-primary)] border-primary-200 dark:border-primary-800',
            t.type === 'error' && 'bg-[var(--bg-primary)] border-red-200 dark:border-red-800',
            t.type === 'info' && 'bg-[var(--bg-primary)] border-blue-200 dark:border-blue-800',
          )}
        >
          {t.type === 'success' && <CheckCircle className="w-5 h-5 text-primary-600 flex-shrink-0 mt-0.5" />}
          {t.type === 'error' && <AlertCircle className="w-5 h-5 text-red-600 flex-shrink-0 mt-0.5" />}
          {t.type === 'info' && <Info className="w-5 h-5 text-blue-600 flex-shrink-0 mt-0.5" />}
          <p className="text-sm text-[var(--text-primary)] flex-1">{t.message}</p>
          <button
            onClick={() => remove(t.id)}
            className="text-[var(--text-muted)] hover:text-[var(--text-primary)] transition-colors flex-shrink-0"
          >
            <X className="w-4 h-4" />
          </button>
        </div>
      ))}
    </div>
  )
}
