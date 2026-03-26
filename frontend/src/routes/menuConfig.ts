import {
  BarChart3,
  Package,
  Percent,
  ShieldCheck,
  Truck,
  UserRoundCog,
  Users,
} from 'lucide-react'
import { ROUTES } from '@/routes/paths'
import { Role } from '@/types/role'

export interface MenuItemConfig {
  label: string
  to: string
  allowedRoles: Role[]
  icon: typeof Users
}

export const MENU_ITEMS: MenuItemConfig[] = [
  {
    label: 'Nhan vien',
    to: ROUTES.staffList,
    allowedRoles: [Role.OWNER],
    icon: UserRoundCog,
  },
  {
    label: 'San pham',
    to: ROUTES.products,
    allowedRoles: [Role.OWNER, Role.STAFF],
    icon: Package,
  },
  {
    label: 'Bao hanh',
    to: ROUTES.warranty,
    allowedRoles: [Role.OWNER, Role.STAFF],
    icon: ShieldCheck,
  },
  {
    label: 'Khach hang',
    to: ROUTES.customers,
    allowedRoles: [Role.OWNER, Role.STAFF],
    icon: Users,
  },
  {
    label: 'Nha cung cap',
    to: ROUTES.suppliers,
    allowedRoles: [Role.OWNER, Role.ADMIN],
    icon: Truck,
  },
  {
    label: 'Bao cao va Thong ke',
    to: ROUTES.reports,
    allowedRoles: [Role.OWNER],
    icon: BarChart3,
  },
  {
    label: 'Khuyen mai',
    to: ROUTES.vouchers,
    allowedRoles: [Role.OWNER],
    icon: Percent,
  },
]


