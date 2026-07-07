import { Mail, Plus } from "lucide-react"
import { Card } from "@/components/ui"
import { members } from "@/lib/mock-data"

function initials(name: string) {
  return name
    .split(" ")
    .map((p) => p[0])
    .slice(0, 2)
    .join("")
}

export default function MembersPage() {
  return (
    <div className="space-y-5">
      <div className="flex items-center justify-between">
        <p className="text-sm text-muted-foreground">{members.length} nariai</p>
        <button className="inline-flex items-center gap-1.5 rounded-xl bg-primary px-3.5 py-2 text-sm font-semibold text-primary-foreground">
          <Plus className="size-4" aria-hidden />
          Pakviesti narį
        </button>
      </div>

      <div className="space-y-2.5">
        {members.map((m) => (
          <Card key={m.id} className="flex items-center gap-3.5 p-3.5">
            <span
              className="flex size-11 shrink-0 items-center justify-center rounded-full text-sm font-semibold text-on-secondary-container"
              style={{ backgroundColor: m.unitTone }}
            >
              {initials(m.name)}
            </span>
            <div className="min-w-0 flex-1">
              <p className="truncate text-sm font-semibold">{m.name}</p>
              <p className="truncate text-xs text-muted-foreground">{m.role}</p>
              <p className="mt-1 inline-flex items-center gap-1.5 text-xs text-muted-foreground">
                <Mail className="size-3.5" aria-hidden />
                {m.email}
              </p>
            </div>
            <span
              className="shrink-0 rounded-full px-2.5 py-1 text-xs font-medium text-on-secondary-container"
              style={{ backgroundColor: m.unitTone }}
            >
              {m.unit}
            </span>
          </Card>
        ))}
      </div>
    </div>
  )
}
