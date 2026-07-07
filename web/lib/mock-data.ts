// Mock data mirroring the Android app's domain (Skautų inventorius).
// Lithuanian labels match the original Jetpack Compose UI.

export type StatusTone = "ok" | "pending" | "warn" | "info" | "neutral"

export const activeUser = {
  name: "Gabija Petraitytė",
  vocative: "Gabija",
  role: "Vieneto vadovė",
  tuntas: "Vilniaus Aušros tuntas",
  unit: "Sakalų draugovė",
}

export const homeMetrics = [
  { label: "Veiksmai", value: "5" },
  { label: "Vieneto daiktai", value: "128" },
  { label: "Bendri daiktai", value: "342" },
]

export type Task = {
  id: string
  title: string
  subtitle: string
  tone: StatusTone
  badge: string
}

export const tasks: Task[] = [
  {
    id: "t1",
    title: "Patvirtinti palapinės rezervaciją",
    subtitle: "Sakalų draugovė • 3 vietos, 2 palapinės",
    tone: "pending",
    badge: "Laukia",
  },
  {
    id: "t2",
    title: "Peržiūrėti pridėtą inventorių",
    subtitle: "2 nauji daiktai laukia patvirtinimo",
    tone: "info",
    badge: "2",
  },
  {
    id: "t3",
    title: "Grąžinti virtuvės komplektą",
    subtitle: "Terminas — rytoj",
    tone: "warn",
    badge: "Vėluoja",
  },
]

export type InventoryScope = {
  id: string
  label: string
  title: string
  count: number
}

export const inventoryScopes: InventoryScope[] = [
  { id: "unit", label: "Vienetas", title: "Sakalų draugovė", count: 128 },
  { id: "shared", label: "Bendras", title: "Tunto bendras inventorius", count: 342 },
  { id: "personal", label: "Asmeninis", title: "Mano asmeniniai daiktai", count: 6 },
]

export type ConditionKey = "GOOD" | "DAMAGED" | "MISSING" | "UNDER_REPAIR" | "NEEDS_INSPECTION"

export const conditionLabels: Record<ConditionKey, { label: string; tone: StatusTone }> = {
  GOOD: { label: "Gera", tone: "ok" },
  DAMAGED: { label: "Sugadinta", tone: "warn" },
  MISSING: { label: "Pamesta", tone: "warn" },
  UNDER_REPAIR: { label: "Taisoma", tone: "pending" },
  NEEDS_INSPECTION: { label: "Reikia patikrinti", tone: "pending" },
}

export type InventoryItem = {
  id: string
  name: string
  code: string
  category: string
  quantity: number
  location: string
  origin: string
  condition: ConditionKey
}

export const inventoryItems: InventoryItem[] = [
  {
    id: "i1",
    name: "Keturvietė palapinė Ferrino",
    code: "INV-0142",
    category: "Stovyklavimas",
    quantity: 4,
    location: "Sandėlis A / Lentyna 2",
    origin: "Bendras sandėlis",
    condition: "GOOD",
  },
  {
    id: "i2",
    name: "Dujinė viryklė Campingaz",
    code: "INV-0088",
    category: "Virtuvė",
    quantity: 2,
    location: "Sandėlis A / Lentyna 5",
    origin: "Vieneto inventorius",
    condition: "NEEDS_INSPECTION",
  },
  {
    id: "i3",
    name: "Pirmosios pagalbos rinkinys",
    code: "INV-0210",
    category: "Pirmoji pagalba",
    quantity: 8,
    location: "Sandėlis B / Spinta 1",
    origin: "Bendras sandėlis",
    condition: "GOOD",
  },
  {
    id: "i4",
    name: "Miegmaišis -5°C",
    code: "INV-0311",
    category: "Stovyklavimas",
    quantity: 12,
    location: "Sandėlis A / Lentyna 3",
    origin: "Vieneto inventorius",
    condition: "DAMAGED",
  },
  {
    id: "i5",
    name: "Virvė alpinistinė 30 m",
    code: "INV-0056",
    category: "Įrankiai",
    quantity: 3,
    location: "Sandėlis B / Kabykla",
    origin: "Bendras sandėlis",
    condition: "UNDER_REPAIR",
  },
  {
    id: "i6",
    name: "Katilas 15 l nerūdijantis",
    code: "INV-0177",
    category: "Virtuvė",
    quantity: 5,
    location: "Sandėlis A / Lentyna 5",
    origin: "Vieneto inventorius",
    condition: "GOOD",
  },
]

export const inventoryCategories = [
  "Visos",
  "Stovyklavimas",
  "Virtuvė",
  "Įrankiai",
  "Pirmoji pagalba",
  "Uniformos",
]

export type Reservation = {
  id: string
  title: string
  requester: string
  dates: string
  items: number
  status: StatusTone
  statusLabel: string
}

export const reservations: Reservation[] = [
  {
    id: "r1",
    title: "Vasaros stovykla „Girios“",
    requester: "Sakalų draugovė",
    dates: "Liep. 12 – Liep. 20",
    items: 24,
    status: "ok",
    statusLabel: "Patvirtinta",
  },
  {
    id: "r2",
    title: "Savaitgalio žygis",
    requester: "Vilkiukų būrelis",
    dates: "Birž. 28 – Birž. 29",
    items: 9,
    status: "pending",
    statusLabel: "Laukia patvirtinimo",
  },
  {
    id: "r3",
    title: "Miesto akcija",
    requester: "Gabija Petraitytė",
    dates: "Birž. 15",
    items: 4,
    status: "info",
    statusLabel: "Išduota",
  },
  {
    id: "r4",
    title: "Mokymai vadovams",
    requester: "Tunto štabas",
    dates: "Geg. 30 – Geg. 31",
    items: 12,
    status: "neutral",
    statusLabel: "Grąžinta",
  },
]

export type SkautEvent = {
  id: string
  title: string
  place: string
  date: string
  attendees: number
  needs: number
  tone: StatusTone
  status: string
}

export const events: SkautEvent[] = [
  {
    id: "e1",
    title: "Vasaros stovykla „Girios“",
    place: "Aukštaitijos nacionalinis parkas",
    date: "Liepos 12–20",
    attendees: 48,
    needs: 32,
    tone: "info",
    status: "Planuojama",
  },
  {
    id: "e2",
    title: "Šv. Jurgio šventė",
    place: "Vilnius, Katedros aikštė",
    date: "Balandžio 23",
    attendees: 120,
    needs: 15,
    tone: "ok",
    status: "Patvirtinta",
  },
  {
    id: "e3",
    title: "Vadovų mokymai",
    place: "Tunto namai",
    date: "Gegužės 30–31",
    attendees: 18,
    needs: 6,
    tone: "pending",
    status: "Registracija",
  },
]

export type Member = {
  id: string
  name: string
  role: string
  unit: string
  unitTone: string
  email: string
}

export const members: Member[] = [
  {
    id: "m1",
    name: "Gabija Petraitytė",
    role: "Vieneto vadovė",
    unit: "Skautai",
    unitTone: "#e3c664",
    email: "gabija.p@skautai.lt",
  },
  {
    id: "m2",
    name: "Tomas Kazlauskas",
    role: "Draugininkas",
    unit: "Patyrę skautai",
    unitTone: "#e4b3ab",
    email: "tomas.k@skautai.lt",
  },
  {
    id: "m3",
    name: "Ugnė Vasiliauskaitė",
    role: "Narė",
    unit: "Vilkai",
    unitTone: "#e4ac76",
    email: "ugne.v@skautai.lt",
  },
  {
    id: "m4",
    name: "Mantas Jonaitis",
    role: "Vyr. skautas",
    unit: "Vyr. skautai",
    unitTone: "#c6b2d8",
    email: "mantas.j@skautai.lt",
  },
  {
    id: "m5",
    name: "Rūta Steponavičienė",
    role: "Tunto iždininkė",
    unit: "Gildija",
    unitTone: "#c3cac1",
    email: "ruta.s@skautai.lt",
  },
]

export type RequestItem = {
  id: string
  title: string
  subtitle: string
  count: number | null
  tone: StatusTone
}

export const requests: RequestItem[] = [
  {
    id: "q1",
    title: "Mano pirkimo prašymai",
    subtitle: "Kuriuos pats pateikei",
    count: 2,
    tone: "info",
  },
  {
    id: "q2",
    title: "Paėmimo prašymai",
    subtitle: "Paimti daiktus iš bendro tunto inventoriaus",
    count: null,
    tone: "info",
  },
]
