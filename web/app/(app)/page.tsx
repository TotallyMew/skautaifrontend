import {
  ClipboardList,
  ArrowLeftRight,
  ChevronRight,
  Plus,
  Users,
  Flag,
  User,
  Package,
  Inbox,
} from "lucide-react"
import { Card, SectionHeader, StatusPill } from "@/components/ui"
import {
  activeUser,
  homeMetrics,
  tasks,
  inventoryScopes,
  requests,
} from "@/lib/mock-data"

const scopeIcons = [Users, Flag, User]

export default function HomePage() {
  return (
    <div className="space-y-8">
      {/* Overview hero */}
      <section className="overflow-hidden rounded-3xl bg-primary text-primary-foreground">
        <div className="p-6">
          <p className="text-xs font-medium uppercase tracking-wide opacity-70">
            Pagrindinė apžvalga
          </p>
          <h2 className="mt-1 text-2xl font-semibold text-balance">
            Labas, {activeUser.vocative}
          </h2>
          <p className="mt-1 text-sm opacity-80">Inventoriaus ir užduočių suvestinė</p>

          <div className="mt-5 grid grid-cols-3 gap-3">
            {homeMetrics.map((m) => (
              <div
                key={m.label}
                className="rounded-2xl bg-primary-foreground/10 p-3 text-center"
              >
                <p className="text-2xl font-semibold">{m.value}</p>
                <p className="text-xs opacity-80">{m.label}</p>
              </div>
            ))}
          </div>

          <div className="mt-5 flex flex-col gap-2 sm:flex-row">
            <button className="inline-flex items-center justify-center gap-2 rounded-xl bg-primary-foreground px-4 py-2.5 text-sm font-semibold text-primary">
              <ClipboardList className="size-4" aria-hidden />
              Atidaryti mano užduotis
            </button>
            <button className="inline-flex items-center justify-center gap-2 rounded-xl bg-primary-foreground/15 px-4 py-2.5 text-sm font-semibold text-primary-foreground">
              <ArrowLeftRight className="size-4" aria-hidden />
              Keisti tuntą
            </button>
          </div>
        </div>
      </section>

      {/* Tasks */}
      <section className="space-y-3">
        <SectionHeader
          title="Mano užduotys"
          subtitle="Trumpa svarbiausių veiksmų peržiūra."
          actionLabel="Žiūrėti visas"
        />
        <div className="space-y-2">
          {tasks.map((task) => (
            <Card key={task.id} className="flex items-center gap-3 p-3.5">
              <span className="flex size-11 shrink-0 items-center justify-center rounded-xl bg-secondary-container text-on-secondary-container">
                <Flag className="size-5" aria-hidden />
              </span>
              <div className="min-w-0 flex-1">
                <p className="truncate text-sm font-semibold">{task.title}</p>
                <p className="truncate text-xs text-muted-foreground">{task.subtitle}</p>
              </div>
              <StatusPill label={task.badge} tone={task.tone} />
              <ChevronRight className="size-5 text-muted-foreground" aria-hidden />
            </Card>
          ))}
        </div>
      </section>

      {/* Inventory scopes */}
      <section className="space-y-3">
        <SectionHeader
          title="Inventorius"
          subtitle="Greita prieiga prie tunto, vieneto ir asmeninio inventoriaus."
        />
        <div className="space-y-2.5">
          {inventoryScopes.map((scope, i) => {
            const Icon = scopeIcons[i] ?? Package
            return (
              <Card key={scope.id} className="flex items-center gap-3 p-3.5">
                <span className="flex size-11 shrink-0 items-center justify-center rounded-2xl bg-primary/10 text-primary">
                  <Icon className="size-5" aria-hidden />
                </span>
                <div className="min-w-0 flex-1">
                  <p className="text-xs font-medium text-primary">{scope.label}</p>
                  <p className="truncate text-sm font-semibold">{scope.title}</p>
                  <p className="text-xs text-muted-foreground">{scope.count} įrašų</p>
                </div>
                <button
                  className="flex size-10 items-center justify-center rounded-lg bg-primary/10 text-primary"
                  aria-label="Pridėti daiktą"
                >
                  <Plus className="size-5" />
                </button>
                <ChevronRight className="size-5 text-muted-foreground" aria-hidden />
              </Card>
            )
          })}
        </div>
      </section>

      {/* Requests */}
      <section className="space-y-3">
        <SectionHeader
          title="Prašymai"
          subtitle="Pirkimo, papildymo ir paėmimo užklausos vienoje vietoje."
        />
        <div className="space-y-2">
          {requests.map((req, i) => (
            <Card key={req.id} className="flex items-center gap-3 p-3.5">
              <span className="flex size-11 shrink-0 items-center justify-center rounded-xl bg-secondary-container text-on-secondary-container">
                {i === 0 ? (
                  <ClipboardList className="size-5" aria-hidden />
                ) : (
                  <Inbox className="size-5" aria-hidden />
                )}
              </span>
              <div className="min-w-0 flex-1">
                <p className="truncate text-sm font-semibold">{req.title}</p>
                <p className="truncate text-xs text-muted-foreground">{req.subtitle}</p>
              </div>
              {req.count != null ? <StatusPill label={String(req.count)} tone={req.tone} /> : null}
              <ChevronRight className="size-5 text-muted-foreground" aria-hidden />
            </Card>
          ))}
        </div>
      </section>
    </div>
  )
}
