import { ChevronLeft, ChevronRight } from 'lucide-react'

interface PaginationProps {
  page: number
  totalPages: number
  onPageChange: (page: number) => void
}

export function Pagination({ page, totalPages, onPageChange }: PaginationProps) {
  if (totalPages <= 1) return null
  return (
    <div className="flex items-center gap-2">
      <button
        className="btn-ghost p-2"
        onClick={() => onPageChange(page - 1)}
        disabled={page <= 1}
      >
        <ChevronLeft className="w-4 h-4" />
      </button>
      <span className="text-sm text-[var(--text-secondary)]">
        Стр. <span className="font-medium text-[var(--text-primary)]">{page}</span> из{' '}
        <span className="font-medium text-[var(--text-primary)]">{totalPages}</span>
      </span>
      <button
        className="btn-ghost p-2"
        onClick={() => onPageChange(page + 1)}
        disabled={page >= totalPages}
      >
        <ChevronRight className="w-4 h-4" />
      </button>
    </div>
  )
}
