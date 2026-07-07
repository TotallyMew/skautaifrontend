import { MapPin, CalendarDays, Users, ListChecks, Plus } from "lucide-react"
import { Card, StatusPill } from "@/components/ui"
import { events } from "@/lib/mock-data"

export default function EventsPage() {
  return (
    <div className="space-y-5">
      <div className="flex items-center justify-between">
        <p className="text-sm text-muted-foreground">{events.length} artėjantys renginiai</p>
        <button className="inline-flex items-center gap-1.5 rounded-xl bg-primary px-3.5 py-2 text-sm font-semibold text-primary-foreground">
          <Plus className="size-4" aria-hidden />
          Naujas renginys
        </button>
      </div>

      <div className="grid gap-4 sm:grid-cols-2">
        {events.map((e) => (
          <Card key={e.id} className="overflow-hidden">
            <div className="flex items-center justify-between bg-secondary-container px-4 py-2.5 text-on-secondary-container">
              <span className="inline-flex items-center gap-1.5 text-xs font-semibold">
                <CalendarDays className="size-4" aria-hidden />
                {e.date}
              </span>
              <StatusPill label={e.status} tone={e.tone} />
            </div>
            <div className="p-4">
              <h3 className="text-base font-semibold text-balance">{e.title}</h3>
              <p className="mt-1 inline-flex items-center gap-1.5 text-xs text-muted-foreground">
                <MapPin className="size-3.5" aria-hidden />
                {e.place}
              </p>
              <div className="mt-4 grid grid-cols-2 gap-2">
                <div className="rounded-xl bg-muted p-3">
                  <p className="inline-flex items-center gap-1.5 text-xs text-muted-foreground">
                    <Users className="size-3.5" aria-hidden />
                    Dalyviai
                  </p>
                  <p className="mt-0.5 text-lg font-semibold">{e.attendees}</p>
                </div>
                <div className="rounded-xl bg-muted p-3">
                  <p className="inline-flex items-center gap-1.5 text-xs text-muted-foreground">
                    <ListChecks className="size-3.5" aria-hidden />
                    Poreikiai
                  </p>
                  <p className="mt-0.5 text-lg font-semibold">{e.needs}</p>
                </div>
              </div>
            </div>
          </Card>
        ))}
      </div>
    </div>
  )
}
