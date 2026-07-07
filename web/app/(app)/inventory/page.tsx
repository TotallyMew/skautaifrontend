"use client"

import { useMemo, useState } from "react"
import { Search, QrCode, Plus, MapPin, Package } from "lucide-react"
import { Card, StatusPill } from "@/components/ui"
import { inventoryItems, inventoryCategories, conditionLabels } from "@/lib/mock-data"

export default function InventoryPage() {
  const [query, setQuery] = useState("")
  const [category, setCategory] = useState("Visos")

  const filtered = useMemo(() => {
    return inventoryItems.filter((item) => {
      const matchesQuery =
        query.trim() === "" ||
        item.name.toLowerCase().includes(query.toLowerCase()) ||
        item.location.toLowerCase().includes(query.toLowerCase()) ||
        item.code.toLowerCase().includes(query.toLowerCase())
      const matchesCategory = category === "Visos" || item.category === category
      return matchesQuery && matchesCategory
    })
  }, [query, category])

  return (
    <div className="space-y-5">
      {/* Search bar */}
      <div className="flex items-center gap-2 rounded-2xl border border-border bg-card px-3.5 py-2.5">
        <Search className="size-5 shrink-0 text-muted-foreground" aria-hidden />
        <input
          value={query}
          onChange={(e) => setQuery(e.target.value)}
          placeholder="Ieškoti pagal pavadinimą, vietą ar pastabas"
          className="min-w-0 flex-1 bg-transparent text-sm outline-none placeholder:text-muted-foreground"
        />
        <button
          className="rounded-lg p-1.5 text-primary hover:bg-primary/10"
          aria-label="Skenuoti QR kodą"
        >
          <QrCode className="size-5" />
        </button>
      </div>

      {/* Category chips */}
      <div className="flex flex-wrap gap-2">
        {inventoryCategories.map((cat) => {
          const active = cat === category
          return (
            <button
              key={cat}
              onClick={() => setCategory(cat)}
              className={`rounded-full border px-3.5 py-1.5 text-sm font-medium transition-colors ${
                active
                  ? "border-primary bg-primary text-primary-foreground"
                  : "border-border bg-card text-foreground hover:bg-muted"
              }`}
            >
              {cat}
            </button>
          )
        })}
      </div>

      <div className="flex items-center justify-between">
        <p className="text-sm text-muted-foreground">
          {filtered.length} {filtered.length === 1 ? "daiktas" : "daiktai"}
        </p>
        <button className="inline-flex items-center gap-1.5 rounded-xl bg-primary px-3.5 py-2 text-sm font-semibold text-primary-foreground">
          <Plus className="size-4" aria-hidden />
          Pridėti daiktą
        </button>
      </div>

      {/* Item list */}
      <div className="grid gap-3 sm:grid-cols-2">
        {filtered.map((item) => {
          const condition = conditionLabels[item.condition]
          return (
            <Card key={item.id} className="p-4">
              <div className="flex items-start gap-3">
                <span className="flex size-11 shrink-0 items-center justify-center rounded-xl bg-secondary-container text-on-secondary-container">
                  <Package className="size-5" aria-hidden />
                </span>
                <div className="min-w-0 flex-1">
                  <div className="flex items-start justify-between gap-2">
                    <p className="text-sm font-semibold leading-tight text-balance">
                      {item.name}
                    </p>
                    <StatusPill label={condition.label} tone={condition.tone} />
                  </div>
                  <p className="mt-0.5 font-mono text-xs text-muted-foreground">{item.code}</p>
                </div>
              </div>

              <div className="mt-3 flex flex-wrap items-center gap-x-4 gap-y-1.5 text-xs text-muted-foreground">
                <span className="inline-flex items-center gap-1">
                  <MapPin className="size-3.5" aria-hidden />
                  {item.location}
                </span>
                <span className="rounded-md bg-muted px-2 py-0.5 font-medium text-foreground">
                  {item.category}
                </span>
                <span>Kiekis: {item.quantity}</span>
              </div>
              <p className="mt-2 text-xs text-muted-foreground">Kilmė: {item.origin}</p>
            </Card>
          )
        })}
      </div>

      {filtered.length === 0 ? (
        <Card className="p-10 text-center">
          <p className="text-sm font-medium">Nieko nerasta</p>
          <p className="mt-1 text-sm text-muted-foreground">
            Pabandyk pakeisti paiešką arba kategoriją.
          </p>
        </Card>
      ) : null}
    </div>
  )
}
