import { Navigate, Route, Routes } from 'react-router-dom'
import { RouteGuard } from '@/app/router/RouteGuard'
import { ForbiddenPage } from '@/features/auth/pages/ForbiddenPage'
import { LoginPage } from '@/features/auth/pages/LoginPage'
import { CustomersPage } from '@/features/customers/pages/CustomersPage'
import { DashboardPage } from '@/features/dashboard/pages/DashboardPage'
import { ProductsPage } from '@/features/products/pages/ProductsPage'
import { ReportsPage } from '@/features/reports/pages/ReportsPage'
import { StaffCreatePage } from '@/features/staff/pages/StaffCreatePage'
import { StaffListPage } from '@/features/staff/pages/StaffListPage'
import { StaffUpdatePage } from '@/features/staff/pages/StaffUpdatePage'
import { SuppliersPage } from '@/features/suppliers/pages/SuppliersPage'
import { VouchersPage } from '@/features/vouchers/pages/VouchersPage'
import { WarrantyPage } from '@/features/warranty/pages/WarrantyPage'
import { DashboardLayout } from '@/layouts/DashboardLayout'
import { ROUTES } from '@/routes/paths'
import { Role } from '@/types/role'

export function AppRouter() {
  return (
    <Routes>
      <Route element={<LoginPage />} path={ROUTES.login} />
      <Route element={<ForbiddenPage />} path={ROUTES.forbidden} />

      <Route element={<RouteGuard />}>
        <Route element={<DashboardLayout />}>
          <Route element={<DashboardPage />} path={ROUTES.dashboard} />

          <Route element={<RouteGuard allowedRoles={[Role.OWNER]} />}>
            <Route element={<StaffListPage />} path={ROUTES.staffList} />
            <Route element={<StaffCreatePage />} path={ROUTES.staffCreate} />
            <Route element={<StaffUpdatePage />} path={ROUTES.staffUpdate} />
            <Route element={<ReportsPage />} path={ROUTES.reports} />
            <Route element={<VouchersPage />} path={ROUTES.vouchers} />
          </Route>

          <Route element={<RouteGuard allowedRoles={[Role.OWNER, Role.STAFF]} />}>
            <Route element={<ProductsPage />} path={ROUTES.products} />
            <Route element={<WarrantyPage />} path={ROUTES.warranty} />
            <Route element={<CustomersPage />} path={ROUTES.customers} />
          </Route>

          <Route element={<RouteGuard allowedRoles={[Role.OWNER, Role.ADMIN]} />}>
            <Route element={<SuppliersPage />} path={ROUTES.suppliers} />
          </Route>
        </Route>
      </Route>

      <Route element={<Navigate replace to={ROUTES.dashboard} />} path="*" />
    </Routes>
  )
}

