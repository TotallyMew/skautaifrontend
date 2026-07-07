import Link from "next/link"
import { AtSign, Lock, Tent } from "lucide-react"

export default function LoginPage() {
  return (
    <main className="flex min-h-screen items-center justify-center bg-background px-4 py-10">
      <div className="w-full max-w-md">
        {/* Hero */}
        <div className="rounded-3xl bg-primary p-6 text-primary-foreground">
          <div className="flex items-center gap-2">
            <Tent className="size-6" aria-hidden />
            <span className="font-semibold">Skautų Inventorius</span>
          </div>
          <h1 className="mt-4 text-2xl font-semibold text-balance">
            Prisijunk prie savo tunto inventoriaus
          </h1>
          <p className="mt-2 text-sm opacity-80 text-pretty">
            Vienoje vietoje matysi bendrą tunto, vieneto ir savo siūlomą inventorių.
          </p>
        </div>

        {/* Card */}
        <div className="mt-4 rounded-3xl border border-border bg-card p-6">
          <h2 className="text-lg font-semibold">Prisijungimas</h2>

          <div className="mt-4 space-y-4">
            <label className="block">
              <span className="mb-1.5 block text-sm font-medium">El. paštas</span>
              <div className="flex items-center gap-2 rounded-xl border border-input bg-background px-3 py-2.5">
                <AtSign className="size-4 text-muted-foreground" aria-hidden />
                <input
                  type="email"
                  placeholder="vardas@skautai.lt"
                  className="min-w-0 flex-1 bg-transparent text-sm outline-none placeholder:text-muted-foreground"
                />
              </div>
            </label>

            <label className="block">
              <span className="mb-1.5 block text-sm font-medium">Slaptažodis</span>
              <div className="flex items-center gap-2 rounded-xl border border-input bg-background px-3 py-2.5">
                <Lock className="size-4 text-muted-foreground" aria-hidden />
                <input
                  type="password"
                  placeholder="••••••••"
                  className="min-w-0 flex-1 bg-transparent text-sm outline-none placeholder:text-muted-foreground"
                />
              </div>
            </label>

            <div className="text-right">
              <button className="text-sm font-medium text-primary hover:underline">
                Pamiršote slaptažodį?
              </button>
            </div>

            <Link
              href="/"
              className="flex w-full items-center justify-center rounded-xl bg-primary px-4 py-3 text-sm font-semibold text-primary-foreground"
            >
              Prisijungti
            </Link>

            <div className="space-y-1 pt-1 text-center text-sm text-muted-foreground">
              <p>
                Turite pakvietimą?{" "}
                <button className="font-medium text-primary hover:underline">
                  Susikurkite paskyrą
                </button>
              </p>
              <p>
                Tunto dar nėra?{" "}
                <button className="font-medium text-primary hover:underline">
                  Užregistruokite jį
                </button>
              </p>
            </div>
          </div>
        </div>
      </div>
    </main>
  )
}
