import {
  Home,
  Bell,
  ListTodo,
  Boxes,
  Package,
  CalendarCheck,
  CalendarDays,
  ShoppingCart,
  Inbox,
  MapPin,
  MailOpen,
  Users,
  Network,
  User,
  type LucideIcon,
} from "lucide-react"

export type NavItem = {
  label: string
  href: string
  icon: LucideIcon
  badge?: number
}

export type NavSection = {
  title: string
  items: NavItem[]
}

export const navSections: NavSection[] = [
  {
    title: "Greita prieiga",
    items: [
      { label: "Pradžia", href: "/", icon: Home },
      { label: "Pranešimai", href: "/", icon: Bell, badge: 3 },
      { label: "Mano užduotys", href: "/", icon: ListTodo },
      { label: "Inventorius", href: "/inventory", icon: Package },
      { label: "Komplektai", href: "/inventory", icon: Boxes },
      { label: "Rezervacijos", href: "/reservations", icon: CalendarCheck },
      { label: "Renginiai", href: "/events", icon: CalendarDays },
      { label: "Kalendorius", href: "/events", icon: CalendarDays },
      { label: "Pirkimai", href: "/", icon: ShoppingCart, badge: 2 },
      { label: "Paėmimai", href: "/", icon: Inbox },
    ],
  },
  {
    title: "Valdymas",
    items: [
      { label: "Lokacijos", href: "/", icon: MapPin },
      { label: "Kvietimai", href: "/", icon: MailOpen },
      { label: "Nariai", href: "/members", icon: Users },
      { label: "Vienetai", href: "/", icon: Network },
    ],
  },
  {
    title: "Paskyra",
    items: [
      { label: "Profilis", href: "/", icon: User },
    ],
  },
]

// Bottom navigation (mobile) mirrors BottomNavItem.all
export const bottomNav: NavItem[] = [
  { label: "Pradžia", href: "/", icon: Home },
  { label: "Inventorius", href: "/inventory", icon: Package },
  { label: "Rezervacijos", href: "/reservations", icon: CalendarCheck },
  { label: "Renginiai", href: "/events", icon: CalendarDays },
  { label: "Nariai", href: "/members", icon: Users },
]
