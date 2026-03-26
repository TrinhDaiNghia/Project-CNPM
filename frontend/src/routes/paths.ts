export const ROUTES = {
  login: '/dang-nhap',
  dashboard: '/',
  staffList: '/nhan-vien',
  staffCreate: '/nhan-vien/tao-moi',
  staffUpdate: '/nhan-vien/:id/chinh-sua',
  products: '/san-pham',
  warranty: '/bao-hanh',
  customers: '/khach-hang',
  suppliers: '/nha-cung-cap',
  reports: '/bao-cao',
  vouchers: '/khuyen-mai',
  forbidden: '/forbidden',
} as const

export function getStaffUpdatePath(id: string): string {
  return `/nhan-vien/${id}/chinh-sua`
}

