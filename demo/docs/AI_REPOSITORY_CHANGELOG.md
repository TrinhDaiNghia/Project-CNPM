# AI Repository Changelog

Last updated: 2026-03-21
Scope: Repository layer and related query methods only.

## 1) Files created

- `src/main/java/com/example/demo/repositories/StaffRepository.java`
- `src/main/java/com/example/demo/repositories/WarrantyRepository.java`

## 2) Files updated

- `src/main/java/com/example/demo/repositories/ProductRepository.java`
- `src/main/java/com/example/demo/repositories/CustomerRepository.java`
- `src/main/java/com/example/demo/repositories/SupplierRepository.java`
- `src/main/java/com/example/demo/repositories/VoucherRepository.java`
- `src/main/java/com/example/demo/repositories/OrderRepository.java`

## 3) Method additions by use case

### Staff (`StaffRepository`)

- `findByStaffId(String staffId)`
- `searchStaff(String fullName, String email, String phone, Pageable pageable)`
- Duplicate checks:
  - `existsByEmail(String email)`
  - `existsByPhone(String phone)`
  - `existsByStaffId(String staffId)`
  - `existsByEmailAndIdNot(String email, String id)`
  - `existsByPhoneAndIdNot(String phone, String id)`
  - `existsByStaffIdAndIdNot(String staffId, String id)`
- Relation check before delete:
  - `existsRelatedRecords(String staffId)` (via `Notification.sender/receiver`)

### Product (`ProductRepository`)

- Multi-condition search:
  - `searchProducts(String name, String brand, String color, String size, String spec, ProductStatus status, Pageable pageable)`
- Duplicate checks:
  - `existsByNameAndCategoryIdAndIdNot(String name, String categoryId, String id)`
- Relation check before delete:
  - `existsRelatedTransactions(String productId)` (via `OrderItem.product`)

### Warranty (`WarrantyRepository`)

- List/detail/filter:
  - `findByStatus(WarrantyStatus status, Pageable pageable)`
  - `searchWarrantyRequests(String keyword, WarrantyStatus status, Pageable pageable)`
- Status update:
  - `updateStatus(String id, WarrantyStatus status)`
  - `updateStatusAndRejectReason(String id, WarrantyStatus status, String rejectReason)`

### Customer (`CustomerRepository`)

- Correct inherited-field lookups:
  - `findByUsername(String username)`
  - `findByEmail(String email)`
- Backward-compat wrappers:
  - `findByUserUsername(String username)` -> delegates to `findByUsername`
  - `findByUserEmail(String email)` -> delegates to `findByEmail`
- Multi-condition search:
  - `searchCustomers(String fullName, String email, String phone, String address, Pageable pageable)`
- Duplicate checks:
  - `existsByEmail(String email)`
  - `existsByPhone(String phone)`
  - `existsByEmailAndIdNot(String email, String id)`
  - `existsByPhoneAndIdNot(String phone, String id)`
- Relation check before delete:
  - `existsRelatedOrders(String customerId)`
- Report helper:
  - `countNewCustomersBetween(Date startDate, Date endDate)`

### Supplier (`SupplierRepository`)

- Duplicate checks:
  - `existsByNameAndIdNot(String name, String id)`
- Search:
  - `searchSuppliers(String keyword, String name, String contractInfo, String address, Pageable pageable)`
- Relation check before delete:
  - `existsRelatedRecords(String supplierId)` (via `ImportReceipt.supplier`)

### Voucher (`VoucherRepository`)

- Code uniqueness:
  - `existsByCode(String code)`
  - `existsByCodeAndIdNot(String code, String id)`
- Search/filter:
  - `searchVouchers(String keyword, VoucherStatus status, Pageable pageable)`
  - `findValidVouchersAt(Date atTime, VoucherStatus status)`
- Relation check:
  - `existsUsedInOrders(String voucherId)`
- Deactivate/disable support:
  - `updateStatus(String voucherId, VoucherStatus status)`

### Report queries (`OrderRepository` + `CustomerRepository`)

- In `OrderRepository`:
  - `sumRevenueBetween(Date startDate, Date endDate)`
  - `countOrdersBetween(Date startDate, Date endDate)`
  - `sumSoldQuantityBetween(Date startDate, Date endDate)`
  - `sumRevenueByDay(Date startDate, Date endDate)`
  - `countOrdersByDay(Date startDate, Date endDate)`
  - `countOrdersByMonth(Date startDate, Date endDate)`
  - `findTopSellingProducts(Date startDate, Date endDate, Pageable pageable)`
  - relation checks: `existsByCustomerId`, `existsByVoucherId`, `existsByProductId`
- In `CustomerRepository`:
  - `countNewCustomersBetween(Date startDate, Date endDate)`

## 4) Domain mapping assumptions used

- `Customer` and `Staff` inherit fields from `User` (`username`, `email`, `phone`, `fullName`, `address`), so repository queries target inherited columns directly.
- Warranty use case mapped to existing entity `Warranty` (no separate `WarrantyRequest` class in codebase).
- Supplier entity currently has `name`, `contractInfo`, `address` only; there is no supplier `email/phone` field in model.
- Voucher soft handling is implemented through `Voucher.status` update methods (no hard-delete strategy added at repository level).
- Report queries are added to existing repositories instead of creating a separate report repository, matching current architecture.

## 5) Known limitations / follow-up notes

- `WarrantyStatus` enum currently includes `RECEIVED`, `PROCESSING`, `COMPLETED`, `REJECTED`.
  - Use-case word "Approved" is mapped to status transition via `updateStatus(...)` (typically to `COMPLETED` in current model).
- Product relation check is currently based on sale-side relation (`OrderItem`).
  - If business wants import-side lock too, add an `ImportDetail`-based existence query in the same repository.

## 6) Compile status snapshot during this change

- Repository files are syntactically valid after fixes.
- Full project compile still fails due to pre-existing non-repository issues:
  - `src/main/java/com/example/demo/services/AuthService.java`
  - `src/main/java/com/example/demo/entities/Customer.java`
  - `src/main/java/com/example/demo/services/AccessControlService.java`
- These failures are outside repository scope and were intentionally not changed in this task.

