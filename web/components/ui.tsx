import type { ReactNode } from "react"
import type { StatusTone } from "@/lib/mock-data"

const toneClasses: Record<StatusTone, string> = {
  ok: "bg-ok text-on-ok",
  pending: "bg-pending text-on-pending",
  warn: "bg-warn text-on-warn",
  info: "bg-info text-on-info",
  neutral: "bg-secondary-container text-on-secondary-container",
}

export function StatusPill({
  label,
  tone = "neutral",
}: {
  label: string
  tone?: StatusTone
}) {
  return (
    <span
      className={`inline-flex items-center rounded-full px-2.5 py-0.5 text-xs font-medium ${toneClasses[tone]}`}
    >
      {label}
    </span>
  )
}

export function Card({
  children,
  className = "",
}: {
  children: ReactNode
  className?: string
}) {
  return (
    <div
      className={`rounded-2xl border border-border bg-card text-card-foreground ${className}`}
    >
      {children}
    </div>
  )
}

export function SectionHeader({
  title,
  subtitle,
  actionLabel,
}: {
  title: string
  subtitle?: string
  actionLabel?: string
}) {
  return (
    <div className="flex items-end justify-between gap-4">
      <div className="space-y-1">
        <h2 className="text-base font-semibold text-foreground text-balance">{title}</h2>
        {subtitle ? (
          <p className="text-sm text-muted-foreground text-pretty">{subtitle}</p>
        ) : null}
      </div>
      {actionLabel ? (
        <button className="shrink-0 text-sm font-medium text-primary hover:underline">
          {actionLabel}
        </button>
      ) : null}
    </div>
  )
}
