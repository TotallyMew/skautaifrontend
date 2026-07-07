"use client"

import Link from "next/link"
import { usePathname } from "next/navigation"
import { useState } from "react"
import { Menu, X, RefreshCw, CloudOff, Tent } from "lucide-react"
import { navSections, bottomNav } from "./nav-config"
import { activeUser } from "@/lib/mock-data"

const pageTitles: Record<string, string> = {
  "/": "Pradžia",
  "/inventory": "Inventorius",
  "/reservations": "Rezervacijos",
  "/events": "Renginiai",
  "/members": "Nariai",
}

function isActive(pathname: string, href: string) {
  if (href === "/") return pathname === "/"
  return pathname.startsWith(href)
}

function SidebarContent({ pathname }: { pathname: string }) {
  return (
    <div className="flex h-full flex-col gap-4 overflow-y-auto p-4">
      <div className="rounded-2xl bg-primary-container p-4 text-on-primary-container">
        <div className="flex items-center gap-2">
          <Tent className="size-5" aria-hidden />
          <span className="text-base font-semibold">Skautų Inventorius</span>
        </div>
        <p className="mt-2 text-sm opacity-80">{activeUser.name}</p>
        <p className="text-xs font-medium opacity-70">{activeUser.tuntas}</p>
      </div>

      {navSections.map((section) => (
        <nav key={section.title} className="space-y-1" aria-label={section.title}>
          <p className="px-3 pb-1 text-xs font-semibold uppercase tracking-wide text-muted-foreground">
            {section.title}
          </p>
          {section.items.map((item) => {
            const active = isActive(pathname, item.href)
            const Icon = item.icon
            return (
              <Link
                key={item.label}
                href={item.href}
                className={`flex items-center gap-3 rounded-xl px-3 py-2 text-sm font-medium transition-colors ${
                  active
                    ? "bg-secondary-container text-on-secondary-container"
                    : "text-foreground hover:bg-muted"
                }`}
              >
                <Icon className="size-5 shrink-0" aria-hidden />
                <span className="flex-1">{item.label}</span>
                {item.badge ? (
                  <span className="inline-flex min-w-5 items-center justify-center rounded-full bg-accent px-1.5 text-xs font-semibold text-accent-foreground">
                    {item.badge}
                  </span>
                ) : null}
              </Link>
            )
          })}
        </nav>
      ))}
    </div>
  )
}

export function AppShell({ children }: { children: React.ReactNode }) {
  const pathname = usePathname()
  const [open, setOpen] = useState(false)
  const title = pageTitles[pathname] ?? "Skautų Inventorius"

  return (
    <div className="min-h-screen bg-background">
      {/* Desktop sidebar */}
      <aside className="fixed inset-y-0 left-0 hidden w-72 border-r border-border bg-card lg:block">
        <SidebarContent pathname={pathname} />
      </aside>

      {/* Mobile drawer */}
      {open ? (
        <div className="fixed inset-0 z-40 lg:hidden">
          <div
            className="absolute inset-0 bg-foreground/40"
            onClick={() => setOpen(false)}
            aria-hidden
          />
          <div className="absolute inset-y-0 left-0 w-72 rounded-r-3xl bg-card shadow-xl">
            <button
              onClick={() => setOpen(false)}
              className="absolute right-3 top-3 rounded-full p-2 text-muted-foreground hover:bg-muted"
              aria-label="Uždaryti meniu"
            >
              <X className="size-5" />
            </button>
            <SidebarContent pathname={pathname} />
          </div>
        </div>
      ) : null}

      <div className="lg:pl-72">
        {/* Top bar */}
        <header className="sticky top-0 z-30 flex h-16 items-center gap-3 border-b border-border bg-background/90 px-4 backdrop-blur">
          <button
            onClick={() => setOpen(true)}
            className="rounded-full p-2 text-foreground hover:bg-muted lg:hidden"
            aria-label="Atidaryti meniu"
          >
            <Menu className="size-5" />
          </button>
          <h1 className="flex-1 text-lg font-semibold">{title}</h1>
          <span className="hidden items-center gap-1.5 rounded-full bg-pending px-3 py-1 text-xs font-medium text-on-pending sm:inline-flex">
            <RefreshCw className="size-3.5" aria-hidden />2 sinchronizuojama
          </span>
          <span className="inline-flex items-center gap-1.5 rounded-full bg-muted px-3 py-1 text-xs font-medium text-muted-foreground">
            <CloudOff className="size-3.5" aria-hidden />
            Demonstracinis
          </span>
          <div
            className="flex size-9 items-center justify-center rounded-full bg-primary text-sm font-semibold text-primary-foreground"
            title={activeUser.name}
          >
            GP
          </div>
        </header>

        <main className="mx-auto max-w-5xl px-4 pb-24 pt-6 lg:pb-10">{children}</main>
      </div>

      {/* Mobile bottom nav */}
      <nav className="fixed inset-x-0 bottom-0 z-30 flex border-t border-border bg-card lg:hidden">
        {bottomNav.map((item) => {
          const active = isActive(pathname, item.href)
          const Icon = item.icon
          return (
            <Link
              key={item.label}
              href={item.href}
              className={`flex flex-1 flex-col items-center gap-1 py-2 text-[11px] font-medium ${
                active ? "text-primary" : "text-muted-foreground"
              }`}
            >
              <span
                className={`flex h-7 w-12 items-center justify-center rounded-full ${
                  active ? "bg-secondary-container" : ""
                }`}
              >
                <Icon className="size-5" aria-hidden />
              </span>
              {item.label}
            </Link>
          )
        })}
      </nav>
    </div>
  )
}
