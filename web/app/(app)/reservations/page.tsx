import { CalendarRange, Package, User, Plus } from "lucide-react"
import { Card, StatusPill } from "@/components/ui"
import { reservations } from "@/lib/mock-data"

export default function ReservationsPage() {
  return (
    <div className="space-y-5">
      <div className="flex items-center justify-between">
        <p className="text-sm text-muted-foreground">{reservations.length} rezervacijos</p>
        <button className="inline-flex items-center gap-1.5 rounded-xl bg-primary px-3.5 py-2 text-sm font-semibold text-primary-foreground">
          <Plus className="size-4" aria-hidden />
          Nauja rezervacija
        </button>
      </div>

      <div className="space-y-3">
        {reservations.map((r) => (
          <Card key={r.id} className="p-4">
            <div className="flex items-start justify-between gap-3">
              <h3 className="text-sm font-semibold text-balance">{r.title}</h3>
              <StatusPill label={r.statusLabel} tone={r.status} />
            </div>
            <div className="mt-3 flex flex-wrap items-center gap-x-5 gap-y-1.5 text-xs text-muted-foreground">
              <span className="inline-flex items-center gap-1.5">
                <User className="size-3.5" aria-hidden />
                {r.requester}
              </span>
              <span className="inline-flex items-center gap-1.5">
                <CalendarRange className="size-3.5" aria-hidden />
                {r.dates}
              </span>
              <span className="inline-flex items-center gap-1.5">
                <Package className="size-3.5" aria-hidden />
                {r.items} daiktai
              </span>
            </div>
          </Card>
        ))}
      </div>
    </div>
  )
}
