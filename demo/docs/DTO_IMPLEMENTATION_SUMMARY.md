# DTO Implementation Summary

## Scope
Tong hop cac thay doi DTO da duoc thuc hien cho use case he thong ban dong ho, bao gom request/response va mapping toi thieu de giu flow hien tai.

## Convention da bam theo
- DTO request: dung `@Data` + `jakarta.validation`.
- DTO response: dung `@Data` + `@Builder`.
- Khong tao DTO trung ten/trung vai tro.
- Chi them field co can cu tu entity/repository/controller/service hien co.

## DTO da chinh sua
- `src/main/java/com/example/demo/dtos/request/ProductRequest.java`
  - Them cac field alias cho use case tim kiem/nhap lieu: `color`, `size`, `specs`.
- `src/main/java/com/example/demo/dtos/response/ProductResponse.java`
  - Them field `color`, `size`, `specs` cho list/detail.
- `src/main/java/com/example/demo/dtos/request/UserRequest.java`
  - Bo sung `fullName` (required) de dong bo voi `User` entity.
- `src/main/java/com/example/demo/dtos/response/UserResponse.java`
  - Bo sung `fullName`.
- `src/main/java/com/example/demo/dtos/request/VoucherRequest.java`
  - Them validate cross-field: `validTo` phai sau `validFrom`.

## DTO request tao moi
### Staff
- `src/main/java/com/example/demo/dtos/request/StaffSearchRequest.java`
- `src/main/java/com/example/demo/dtos/request/StaffRequest.java`

### Product
- `src/main/java/com/example/demo/dtos/request/ProductSearchRequest.java`

### Warranty
- `src/main/java/com/example/demo/dtos/request/WarrantySearchRequest.java`
- `src/main/java/com/example/demo/dtos/request/WarrantyRequest.java`
- `src/main/java/com/example/demo/dtos/request/WarrantyProcessRequest.java`

### Customer
- `src/main/java/com/example/demo/dtos/request/CustomerSearchRequest.java`
- `src/main/java/com/example/demo/dtos/request/CustomerRequest.java`

### Supplier
- `src/main/java/com/example/demo/dtos/request/SupplierSearchRequest.java`
- `src/main/java/com/example/demo/dtos/request/SupplierRequest.java`

### Voucher / Promotion
- `src/main/java/com/example/demo/dtos/request/VoucherSearchRequest.java`
- `src/main/java/com/example/demo/dtos/request/VoucherStatusUpdateRequest.java`

### Report / Dashboard
- `src/main/java/com/example/demo/dtos/request/ReportFilterRequest.java`

## DTO response tao moi
### Staff
- `src/main/java/com/example/demo/dtos/response/StaffResponse.java`

### Customer
- `src/main/java/com/example/demo/dtos/response/CustomerResponse.java`

### Supplier
- `src/main/java/com/example/demo/dtos/response/SupplierResponse.java`

### Warranty
- `src/main/java/com/example/demo/dtos/response/WarrantyResponse.java`

### Voucher / Promotion
- `src/main/java/com/example/demo/dtos/response/VoucherResponse.java`

### Report / Dashboard
- `src/main/java/com/example/demo/dtos/response/DashboardSummaryResponse.java`
- `src/main/java/com/example/demo/dtos/response/RevenueByTimeResponse.java`
- `src/main/java/com/example/demo/dtos/response/OrdersByTimeResponse.java`
- `src/main/java/com/example/demo/dtos/response/TopSellingProductResponse.java`
- `src/main/java/com/example/demo/dtos/response/DashboardStatisticResponse.java`
- `src/main/java/com/example/demo/dtos/response/DashboardReportResponse.java`

## Mapper tao moi
- `src/main/java/com/example/demo/dtos/DtoMapper.java`
  - Mapping entity sang cac response DTO moi (Staff/Customer/Supplier/Product/Warranty/Voucher).

## Dieu chinh toi thieu de compile-flow
- `src/main/java/com/example/demo/controllers/UserController.java`
  - Mapping `fullName` vao entity/response.
- `src/main/java/com/example/demo/services/ProductService.java`
  - Ho tro map alias field moi (`color`, `size`, `specs`) vao cac field entity hien co.

## Validation da them
- Required fields: `@NotBlank`, `@NotNull`.
- Email format: `@Email`.
- Phone format VN: `@Pattern`.
- Numeric constraints: `@Min`, `@Max`.
- Voucher date range: `validTo > validFrom`.
- Warranty reject rule: bat buoc `rejectReason` khi status `REJECTED`.
- Report date range: `toDate` khong truoc `fromDate`.

## Ghi chu build
Da thuc hien compile check, hien con loi ngoai pham vi DTO:
- `src/main/java/com/example/demo/services/AuthService.java`
- `src/main/java/com/example/demo/entities/Customer.java`
- `src/main/java/com/example/demo/services/AccessControlService.java`

Cac loi nay khong den tu DTO moi/sua trong dot thay doi nay.

