package com.example.demo.services;

import com.example.demo.dtos.request.CancelOrderRequest;
import com.example.demo.dtos.request.OrderRequest;
import com.example.demo.dtos.request.QrPaymentPrepareRequest;
import com.example.demo.dtos.response.OrderItemResponse;
import com.example.demo.dtos.response.OrderResponse;
import com.example.demo.dtos.response.OrderStatusHistoryResponse;
import com.example.demo.dtos.response.QrPaymentResponse;
import com.example.demo.dtos.response.QrPaymentStatusResponse;
import com.example.demo.entities.Customer;
import com.example.demo.entities.Order;
import com.example.demo.entities.OrderItem;
import com.example.demo.entities.OrderStatusHistory;
import com.example.demo.entities.Payment;
import com.example.demo.entities.Product;
import com.example.demo.entities.Shipping;
import com.example.demo.entities.Voucher;
import com.example.demo.entities.enums.OrderStatus;
import com.example.demo.entities.enums.PaymentMethod;
import com.example.demo.entities.enums.PaymentStatus;
import com.example.demo.exceptions.ResourceNotFoundException;
import com.example.demo.repositories.CustomerRepository;
import com.example.demo.repositories.OrderRepository;
import com.example.demo.repositories.OrderStatusHistoryRepository;
import com.example.demo.repositories.ProductRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Date;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Service
@Slf4j
@RequiredArgsConstructor
@Transactional
public class OrderService {

    private static final String REFUND_PROCESSING_MESSAGE = "Hoàn tiền sẽ được xử lý trong 3-7 ngày làm việc.";
    private static final long DEFAULT_CANCEL_WINDOW_HOURS = 24L;
    private static final Set<OrderStatus> CUSTOMER_CAN_CANCEL_STATUSES = EnumSet.of(OrderStatus.PENDING, OrderStatus.CONFIRMED);
    private static final String QR_PAYMENT_STATUS_PENDING = "PENDING";
    private static final String QR_PAYMENT_STATUS_SUCCESS = "SUCCESS";
    private static final String QR_PAYMENT_STATUS_WRONG_AMOUNT = "WRONG_AMOUNT";
    private static final String QR_PAYMENT_STATUS_CANCELLED = "CANCELLED";
    private static final String QR_BASE_URL = "https://qr.sepay.vn/img";

    private final OrderRepository orderRepository;
    private final CustomerRepository customerRepository;
    private final ProductRepository productRepository;
    private final VoucherService voucherService;
    private final AccessControlService accessControlService;
    private final CartService cartService;
    private final OrderStatusHistoryRepository historyRepository;
    private final NotificationService notificationService;
    private final JavaMailSender mailSender;
    private final ObjectMapper objectMapper;
    private final ConcurrentMap<String, PendingQrPaymentSession> pendingQrPaymentSessions = new ConcurrentHashMap<>();
    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(8))
            .followRedirects(HttpClient.Redirect.NORMAL)
            .build();

    @Value("${app.qr.account-number:${payment.qr.account-number:0359537981}}")
    private String qrAccountNumber;

    @Value("${app.qr.bank-code:${payment.qr.bank-code:MB}}")
    private String qrBankCode;

    @Value("${app.rio.poll-url:${payment.qr.verify-url:https://thanhdat050625.onrender.com/api/rio/cnpm/sent}}")
    private String qrVerifyUrl;

    @Value("${app.rio.timeout-ms:8000}")
    private long qrVerifyTimeoutMs;

    @Value("${payment.qr.session-ttl-minutes:30}")
    private long qrSessionTtlMinutes;

    @Value("${app.mail.from:${spring.mail.username:}}")
    private String mailFromAddress;

    @Value("${order.cancel.self-window-hours:24}")
    private long selfCancelWindowHours;

    @Transactional
    public OrderResponse createOrder(OrderRequest request) {
        validatePaymentBeforeCreation(request);
        accessControlService.requireCustomerAccess(request.getCustomerId());

        Customer customer = customerRepository.findById(request.getCustomerId())
                .orElseThrow(() -> new ResourceNotFoundException("Customer not found: " + request.getCustomerId()));

        Order order = Order.builder()
                .customer(customer)
                .note(normalizeText(request.getNote()))
                .shippingAddress(resolveShippingAddress(request, customer))
                .build();

        List<OrderItem> orderItems = new ArrayList<>();
        long totalAmount = 0L;

        for (OrderRequest.OrderItemRequest itemReq : request.getItems()) {
            Product product = productRepository.findById(itemReq.getProductId())
                    .orElseThrow(() -> new ResourceNotFoundException("Product not found: " + itemReq.getProductId()));

            int quantity = itemReq.getQuantity();
            int availableStock = product.getStockQuantity() == null ? 0 : product.getStockQuantity();
            if (quantity > availableStock) {
                throw new IllegalArgumentException(
                        "Sản phẩm " + product.getName() + " chỉ còn " + availableStock + " trong kho"
                );
            }

            product.setStockQuantity(availableStock - quantity);

            long unitPrice = product.getPrice() == null ? 0L : product.getPrice();
            long subTotal = unitPrice * quantity;

            OrderItem orderItem = OrderItem.builder()
                    .order(order)
                    .product(product)
                    .quantity(quantity)
                    .subTotal(subTotal)
                    .build();
            orderItems.add(orderItem);
            totalAmount += subTotal;
        }

        order.setOrderItems(orderItems);

        if (StringUtils.hasText(request.getVoucherCode())) {
            Voucher voucher = voucherService.consumeVoucher(request.getVoucherCode());
            order.setVoucher(voucher);
            long discount = totalAmount * voucher.getDiscountPercent() / 100;
            totalAmount -= discount;
        }

        totalAmount = Math.max(totalAmount, 0L);
        order.setTotalAmount(totalAmount);

        Payment payment = buildPayment(request, order, totalAmount);
        Shipping shipping = buildShipping(request, order, customer);
        order.setPayment(payment);
        order.setShipping(shipping);

        Order savedOrder = orderRepository.save(order);
        appendHistory(savedOrder, OrderStatus.PENDING, "Đơn hàng được tạo", customer.getUsername());

        try {
            notificationService.sendOrderSuccessNotification(customer, savedOrder.getId());
        } catch (Exception ex) {
            log.warn("Order success notification failed for order {}", savedOrder.getId(), ex);
        }

        try {
            notificationService.notifyStoreAboutNewOrder(customer, savedOrder.getId());
        } catch (Exception ex) {
            log.warn("Store notification for new order {} failed", savedOrder.getId(), ex);
        }

        return toOrderResponse(savedOrder);
    }

    public QrPaymentResponse prepareQrPayment(QrPaymentPrepareRequest request) {
        cleanupExpiredQrSessions();
        accessControlService.requireCustomerAccess(request.getCustomerId());

        customerRepository.findById(request.getCustomerId())
                .orElseThrow(() -> new ResourceNotFoundException("Customer not found: " + request.getCustomerId()));

        OrderRequest orderRequest = buildOrderRequestForQr(request);
        long totalAmount = calculateTotalAmount(orderRequest);

        String orderId = UUID.randomUUID().toString();
        pendingQrPaymentSessions.put(
                orderId,
                new PendingQrPaymentSession(orderId, request.getCustomerId(), orderRequest, totalAmount, Instant.now())
        );

        return QrPaymentResponse.builder()
                .orderId(orderId)
                .accountNumber(qrAccountNumber)
                .bankCode(qrBankCode)
                .amount(totalAmount)
                .description(orderId)
                .qrUrl(buildQrUrl(orderId, totalAmount))
                .build();
    }

    public QrPaymentStatusResponse verifyQrPayment(String orderId) {
        cleanupExpiredQrSessions();
        PendingQrPaymentSession session = pendingQrPaymentSessions.get(orderId);
        if (session == null) {
            throw new ResourceNotFoundException("QR payment session not found: " + orderId);
        }

        accessControlService.requireCustomerAccess(session.customerId());

        synchronized (session) {
            if (session.isExpired(resolveQrSessionTtlMinutes())) {
                session.cancel("Phiên thanh toán đã hết hạn.");
            }

            if (session.isCancelled()) {
                return buildQrStatusResponse(session, QR_PAYMENT_STATUS_CANCELLED, session.getLastMessage(), null);
            }

            if (session.isCompleted()) {
                OrderResponse order = findOrderResponse(session.getCreatedOrderId());
                return buildQrStatusResponse(session, QR_PAYMENT_STATUS_SUCCESS, "Thanh toán thành công.", order);
            }

            RioPaymentResult rioPayment = queryRioPayment(orderId);
            session.setReceivedAmount(rioPayment.amount());

            if (!rioPayment.paid()) {
                return buildQrStatusResponse(session, QR_PAYMENT_STATUS_PENDING, "Chưa nhận được thanh toán.", null);
            }

            if (rioPayment.amount() != session.expectedAmount()) {
                return buildQrStatusResponse(
                        session,
                        QR_PAYMENT_STATUS_WRONG_AMOUNT,
                        "Đã nhận thanh toán nhưng sai số tiền. Vui lòng chuyển đúng số tiền của đơn hàng.",
                        null
                );
            }

            OrderResponse createdOrder = createOrder(buildPaidOrderRequest(session.orderRequest()));
            cartService.clearCart(session.customerId(), true);
            session.complete(createdOrder.getId(), rioPayment.amount());
            return buildQrStatusResponse(session, QR_PAYMENT_STATUS_SUCCESS, "Thanh toán thành công.", createdOrder);
        }
    }

    public void cancelQrPayment(String orderId) {
        cleanupExpiredQrSessions();
        PendingQrPaymentSession session = pendingQrPaymentSessions.get(orderId);
        if (session == null) {
            throw new ResourceNotFoundException("QR payment session not found: " + orderId);
        }

        accessControlService.requireCustomerAccess(session.customerId());

        synchronized (session) {
            if (session.isCompleted()) {
                throw new IllegalStateException("Đơn hàng đã được thanh toán thành công, không thể hủy phiên.");
            }
            session.cancel("Bạn đã hủy thanh toán.");
        }
    }

    @Transactional
    public OrderResponse updateOrderStatus(String id, OrderStatus status) {
        accessControlService.requirePrivilegedRole();
        if (status == OrderStatus.CANCELLED) {
            throw new IllegalStateException("Use /api/orders/{id}/cancel to cancel an order");
        }

        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found: " + id));

        validateStatusTransition(order.getStatus(), status);

        order.setStatus(status);
        if (status == OrderStatus.SHIPPING && order.getShipping() != null && order.getShipping().getTrackingDate() == null) {
            order.getShipping().setTrackingDate(new Date());
        }
        order = orderRepository.save(order);

        appendHistory(order, status, "Cập nhật bởi nhân viên", getCurrentUsername());

        try {
            String senderId = accessControlService.getCurrentUserOrThrow().getId();
            switch (status) {
                case CONFIRMED -> notificationService.sendOrderConfirmedNotification(senderId, order.getCustomer(), order.getId());
                case DELIVERED -> notificationService.sendOrderDeliveredNotification(senderId, order.getCustomer(), order.getId());
                default -> notificationService.sendOrderStatusUpdateNotification(
                        senderId,
                        order.getCustomer(),
                        order.getId(),
                        null,
                        status.name()
                );
            }
        } catch (Exception ex) {
            log.warn("Order status notification failed for order {}", order.getId(), ex);
        }

        return toOrderResponse(order);
    }

    private void validateStatusTransition(OrderStatus current, OrderStatus target) {
        boolean isValid = switch (current) {
            case PENDING -> target == OrderStatus.CONFIRMED || target == OrderStatus.CANCELLED;
            case CONFIRMED -> target == OrderStatus.SHIPPING || target == OrderStatus.CANCELLED;
            case SHIPPING -> target == OrderStatus.DELIVERED;
            case DELIVERED -> target == OrderStatus.COMPLETED || target == OrderStatus.RETURNED;
            default -> false;
        };

        if (!isValid) {
            throw new IllegalStateException(
                    String.format("Cannot transition from %s to %s", current, target)
            );
        }
    }

    private String getCurrentUsername() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return auth != null ? auth.getName() : "system";
    }

    @Transactional(readOnly = true)
    public Optional<OrderResponse> findById(String id) {
        return orderRepository.findById(id)
                .map(order -> {
                    accessControlService.requireCustomerAccess(order.getCustomer().getId());
                    return toOrderResponse(order);
                });
    }

    @Transactional(readOnly = true)
    public List<Order> findByCustomerId(String customerId) {
        accessControlService.requireCustomerAccess(customerId);
        return orderRepository.findByCustomerId(customerId);
    }

    @Transactional(readOnly = true)
    public Page<OrderResponse> findByCustomerId(String customerId, Pageable pageable) {
        accessControlService.requireCustomerAccess(customerId);
        Page<Order> orderPage = orderRepository.findByCustomerId(customerId, pageable);
        List<OrderResponse> responses = orderPage.getContent().stream()
                .map(this::toOrderResponse)
                .toList();
        return new PageImpl<>(responses, pageable, orderPage.getTotalElements());
    }

    @Transactional(readOnly = true)
    public List<Order> findByStatus(OrderStatus status) {
        accessControlService.requirePrivilegedRole();
        return orderRepository.findByStatus(status);
    }

    public OrderResponse toOrderResponse(Order order) {
        List<OrderItemResponse> itemResponses = order.getOrderItems().stream()
                .map(item -> OrderItemResponse.builder()
                        .id(item.getId())
                        .productId(item.getProduct().getId())
                        .productName(item.getProduct().getName())
                        .quantity(item.getQuantity())
                        .subTotal(item.getSubTotal())
                        .build())
                .toList();

        List<OrderStatusHistory> historyEntries = historyRepository.findByOrderIdOrderByChangedAtAsc(order.getId());

        List<OrderStatusHistoryResponse> timeline = historyEntries.stream()
                .map(h -> OrderStatusHistoryResponse.builder()
                        .status(h.getStatus().name())
                        .note(h.getNote())
                        .changedAt(h.getChangedAt())
                        .changedBy(h.getChangedBy())
                        .build())
                .toList();

        if (timeline.isEmpty()) {
            timeline = List.of(
                    OrderStatusHistoryResponse.builder()
                            .status(order.getStatus().name())
                            .note("Đơn hàng hiện tại")
                            .changedAt(order.getOrderDate())
                            .changedBy(order.getCustomer() != null ? order.getCustomer().getUsername() : "system")
                            .build()
            );
        }

        OrderResponse.PaymentResponse paymentResponse = null;
        if (order.getPayment() != null) {
            paymentResponse = OrderResponse.PaymentResponse.builder()
                    .method(order.getPayment().getMethod())
                    .status(order.getPayment().getStatus())
                    .isPaid(order.getPayment().getIsPaid())
                    .paymentDate(order.getPayment().getPaymentDate())
                    .amount(order.getPayment().getAmount())
                    .build();
        }

        OrderResponse.ShippingResponse shippingResponse = null;
        if (order.getShipping() != null) {
            shippingResponse = OrderResponse.ShippingResponse.builder()
                    .trackingNumber(order.getShipping().getTrackingNumber())
                    .trackingDate(order.getShipping().getTrackingDate())
                    .carrierName(order.getShipping().getCarrierName())
                    .carrierPhone(order.getShipping().getCarrierPhone())
                    .estimatedDelivery(order.getShipping().getEstimatedDelivery())
                    .build();
        }

        String latestCancellationNote = historyEntries.stream()
                .filter(item -> item.getStatus() == OrderStatus.CANCELLED)
                .reduce((first, second) -> second)
                .map(OrderStatusHistory::getNote)
                .orElse(null);

        return OrderResponse.builder()
                .id(order.getId())
                .orderDate(order.getOrderDate())
                .totalAmount(order.getTotalAmount())
                .note(order.getNote())
                .shippingAddress(order.getShippingAddress())
                .status(order.getStatus())
                .customerId(order.getCustomer().getId())
                .customerUsername(order.getCustomer().getUsername())
                .customerFullName(order.getCustomer().getFullName())
                .customerPhone(order.getCustomer().getPhone())
                .customerAddress(order.getCustomer().getAddress())
                .voucherCode(order.getVoucher() != null ? order.getVoucher().getCode() : null)
                .orderItems(itemResponses)
                .timeline(timeline)
                .payment(paymentResponse)
                .shipping(shippingResponse)
                .canCancel(isSelfCancelable(order))
                .canRequestCancel(order.getStatus() == OrderStatus.SHIPPING)
                .refundRequired(isRefundRequired(order))
                .refundMessage(isRefundRequired(order) ? REFUND_PROCESSING_MESSAGE : null)
                .cancellationReason(extractCancellationReason(latestCancellationNote))
                .cancellationNote(latestCancellationNote)
                .build();
    }

    @Transactional(readOnly = true)
    public Page<OrderResponse> findAllOrders(Pageable pageable) {
        accessControlService.requirePrivilegedRole();
        Page<Order> orderPage = orderRepository.findAll(pageable);
        List<OrderResponse> responses = orderPage.getContent().stream()
                .map(this::toOrderResponse)
                .toList();
        return new PageImpl<>(responses, pageable, orderPage.getTotalElements());
    }

    @Transactional
    public OrderResponse cancelOrder(String id, CancelOrderRequest request) {
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found: " + id));

        accessControlService.requireCustomerAccess(order.getCustomer().getId());
        validateCustomerCancellation(order);

        String reason = normalizeCancelReason(request != null ? request.getReason() : null);
        String note = normalizeText(request != null ? request.getNote() : null);
        boolean paidOrder = isPaidOrder(order);

        boolean restockIssue = restockOrderItems(order);

        order.setStatus(OrderStatus.CANCELLED);
        if (order.getPayment() != null && paidOrder) {
            order.getPayment().setStatus(PaymentStatus.PROCESSING);
        }
        order = orderRepository.save(order);

        String historyNote = buildCancellationHistoryNote(reason, note, restockIssue);
        appendHistory(order, OrderStatus.CANCELLED, historyNote, getCurrentUsername());

        try {
            notificationService.notifyCustomerAboutOrderCancellation(order.getCustomer(), order.getId(), reason, paidOrder);
        } catch (Exception ex) {
            log.warn("Customer cancel notification failed for order {}", order.getId(), ex);
        }

        try {
            notificationService.notifyStoreAboutCustomerCancellation(
                    order.getCustomer(),
                    order.getId(),
                    reason,
                    paidOrder,
                    restockIssue
            );
        } catch (Exception ex) {
            log.warn("Store cancel notification failed for order {}", order.getId(), ex);
        }

        sendCancellationEmailAsync(order, reason, note, paidOrder, restockIssue);

        return toOrderResponse(order);
    }

    @Transactional
    public OrderResponse requestCancelForShippingOrder(String id, CancelOrderRequest request) {
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found: " + id));

        accessControlService.requireCustomerAccess(order.getCustomer().getId());

        if (order.getStatus() == OrderStatus.CANCELLED) {
            throw new IllegalStateException("Đơn hàng này đã được hủy.");
        }
        if (order.getStatus() != OrderStatus.SHIPPING) {
            throw new IllegalStateException("Chỉ có thể gửi yêu cầu hủy khi đơn hàng đang giao.");
        }

        String reason = normalizeCancelReason(request != null ? request.getReason() : null);
        String note = normalizeText(request != null ? request.getNote() : null);
        String historyNote = "Yêu cầu hủy khi đang giao. Lý do: " + reason;
        if (StringUtils.hasText(note)) {
            historyNote += "; Ghi chú: " + note;
        }

        appendHistory(order, order.getStatus(), historyNote, getCurrentUsername());

        try {
            notificationService.notifyStoreAboutCancellationRequest(order.getCustomer(), order.getId(), reason, note);
        } catch (Exception ex) {
            log.warn("Store cancellation request notification failed for order {}", order.getId(), ex);
        }

        return toOrderResponse(order);
    }

    private String buildCancellationHistoryNote(String reason, String note, boolean restockIssue) {
        StringBuilder builder = new StringBuilder("Lý do: ").append(reason);
        if (StringUtils.hasText(note)) {
            builder.append("; Ghi chú: ").append(note);
        }
        if (restockIssue) {
            builder.append("; Cảnh báo: lỗi hoàn kho, cần xử lý thủ công.");
        }
        return builder.toString();
    }

    private void validateCustomerCancellation(Order order) {
        if (order.getStatus() == OrderStatus.CANCELLED) {
            throw new IllegalStateException("Đơn hàng này đã được hủy.");
        }
        if (order.getStatus() == OrderStatus.SHIPPING) {
            throw new IllegalStateException("Đơn hàng đang được giao. Bạn không thể tự hủy, vui lòng gửi yêu cầu hủy đến nhân viên.");
        }
        if (order.getStatus() == OrderStatus.DELIVERED
                || order.getStatus() == OrderStatus.COMPLETED
                || order.getStatus() == OrderStatus.RETURNED) {
            throw new IllegalStateException("Đơn hàng đã giao/hoàn tất không thể hủy. Vui lòng liên hệ hotline để được hỗ trợ.");
        }
        if (!CUSTOMER_CAN_CANCEL_STATUSES.contains(order.getStatus())) {
            throw new IllegalStateException("Không thể hủy đơn ở trạng thái: " + order.getStatus());
        }
        if (!isWithinSelfCancelWindow(order)) {
            throw new IllegalStateException("Đã quá thời gian cho phép hủy đơn tự động. Vui lòng liên hệ hotline để được hỗ trợ.");
        }
    }

    private boolean restockOrderItems(Order order) {
        boolean hasIssue = false;
        for (OrderItem item : order.getOrderItems()) {
            try {
                Product product = item.getProduct();
                if (product == null) {
                    throw new IllegalStateException("Missing product on order item " + item.getId());
                }
                int currentStock = product.getStockQuantity() == null ? 0 : product.getStockQuantity();
                int quantity = item.getQuantity() == null ? 0 : item.getQuantity();
                product.setStockQuantity(currentStock + Math.max(quantity, 0));
            } catch (Exception ex) {
                hasIssue = true;
                log.error("Failed to restock product for order item {}", item.getId(), ex);
            }
        }
        return hasIssue;
    }

    private boolean isSelfCancelable(Order order) {
        return CUSTOMER_CAN_CANCEL_STATUSES.contains(order.getStatus()) && isWithinSelfCancelWindow(order);
    }

    private boolean isWithinSelfCancelWindow(Order order) {
        if (order.getOrderDate() == null) {
            return true;
        }
        long configuredWindowHours = selfCancelWindowHours > 0 ? selfCancelWindowHours : DEFAULT_CANCEL_WINDOW_HOURS;
        Instant deadline = order.getOrderDate().toInstant().plus(configuredWindowHours, ChronoUnit.HOURS);
        return !Instant.now().isAfter(deadline);
    }

    private boolean isRefundRequired(Order order) {
        return order.getStatus() == OrderStatus.CANCELLED && isPaidOrder(order);
    }

    private boolean isPaidOrder(Order order) {
        return order.getPayment() != null && Boolean.TRUE.equals(order.getPayment().getIsPaid());
    }

    private void validatePaymentBeforeCreation(OrderRequest request) {
        OrderRequest.PaymentRequest paymentRequest = request.getPayment();
        if (paymentRequest == null) {
            throw new IllegalArgumentException("Payment information is required.");
        }
        if (paymentRequest.getMethod() != PaymentMethod.BANK_TRANSFER) {
            throw new IllegalArgumentException("Only BANK_TRANSFER is supported.");
        }
        if (!Boolean.TRUE.equals(paymentRequest.getIsPaid())) {
            throw new IllegalArgumentException("Order can only be created after successful payment.");
        }
        if (paymentRequest.getStatus() != PaymentStatus.COMPLETED) {
            throw new IllegalArgumentException("Payment status must be COMPLETED.");
        }
    }

    private OrderRequest buildOrderRequestForQr(QrPaymentPrepareRequest request) {
        OrderRequest orderRequest = new OrderRequest();
        orderRequest.setCustomerId(request.getCustomerId());
        orderRequest.setNote(request.getNote());
        orderRequest.setShippingAddress(request.getShippingAddress());
        orderRequest.setVoucherCode(request.getVoucherCode());
        orderRequest.setItems(copyOrderItems(request.getItems()));
        orderRequest.setShipping(copyShipping(request.getShipping()));
        return orderRequest;
    }

    private OrderRequest buildPaidOrderRequest(OrderRequest source) {
        OrderRequest copied = deepCopyOrderRequest(source);
        OrderRequest.PaymentRequest paymentRequest = new OrderRequest.PaymentRequest();
        paymentRequest.setMethod(PaymentMethod.BANK_TRANSFER);
        paymentRequest.setStatus(PaymentStatus.COMPLETED);
        paymentRequest.setIsPaid(true);
        paymentRequest.setPaymentDate(new Date());
        copied.setPayment(paymentRequest);
        return copied;
    }

    private OrderRequest deepCopyOrderRequest(OrderRequest source) {
        OrderRequest copied = new OrderRequest();
        copied.setCustomerId(source.getCustomerId());
        copied.setNote(source.getNote());
        copied.setShippingAddress(source.getShippingAddress());
        copied.setVoucherCode(source.getVoucherCode());
        copied.setItems(copyOrderItems(source.getItems()));
        copied.setShipping(copyShipping(source.getShipping()));
        return copied;
    }

    private List<OrderRequest.OrderItemRequest> copyOrderItems(List<OrderRequest.OrderItemRequest> items) {
        if (items == null) {
            return List.of();
        }
        return items.stream().map(item -> {
            OrderRequest.OrderItemRequest copiedItem = new OrderRequest.OrderItemRequest();
            copiedItem.setProductId(item.getProductId());
            copiedItem.setQuantity(item.getQuantity());
            return copiedItem;
        }).toList();
    }

    private OrderRequest.ShippingRequest copyShipping(OrderRequest.ShippingRequest shipping) {
        if (shipping == null) {
            return null;
        }
        OrderRequest.ShippingRequest copied = new OrderRequest.ShippingRequest();
        copied.setTrackingNumber(shipping.getTrackingNumber());
        copied.setCarrierName(shipping.getCarrierName());
        copied.setCarrierPhone(shipping.getCarrierPhone());
        copied.setEstimatedDelivery(shipping.getEstimatedDelivery());
        copied.setFullName(shipping.getFullName());
        copied.setPhone(shipping.getPhone());
        copied.setProvince(shipping.getProvince());
        copied.setDistrict(shipping.getDistrict());
        copied.setWard(shipping.getWard());
        copied.setDetailAddress(shipping.getDetailAddress());
        return copied;
    }

    private long calculateTotalAmount(OrderRequest request) {
        long totalAmount = 0L;
        for (OrderRequest.OrderItemRequest itemReq : request.getItems()) {
            Product product = productRepository.findById(itemReq.getProductId())
                    .orElseThrow(() -> new ResourceNotFoundException("Product not found: " + itemReq.getProductId()));

            int quantity = itemReq.getQuantity() == null ? 0 : itemReq.getQuantity();
            int availableStock = product.getStockQuantity() == null ? 0 : product.getStockQuantity();
            if (quantity <= 0) {
                throw new IllegalArgumentException("Product quantity must be greater than 0.");
            }
            if (quantity > availableStock) {
                throw new IllegalArgumentException("Product " + product.getName() + " only has " + availableStock + " in stock.");
            }

            long unitPrice = product.getPrice() == null ? 0L : product.getPrice();
            totalAmount += unitPrice * quantity;
        }

        if (StringUtils.hasText(request.getVoucherCode())) {
            Voucher voucher = voucherService.applyVoucher(request.getVoucherCode());
            long discount = totalAmount * voucher.getDiscountPercent() / 100;
            totalAmount -= discount;
        }

        return Math.max(totalAmount, 0L);
    }

    private String buildQrUrl(String orderId, long amount) {
        String description = URLEncoder.encode(orderId, StandardCharsets.UTF_8);
        return QR_BASE_URL + "?acc=" + qrAccountNumber + "&bank=" + qrBankCode + "&amount=" + amount + "&des=" + description;
    }

    private long resolveQrSessionTtlMinutes() {
        return qrSessionTtlMinutes > 0 ? qrSessionTtlMinutes : 30L;
    }

    private void cleanupExpiredQrSessions() {
        long ttlMinutes = resolveQrSessionTtlMinutes();
        pendingQrPaymentSessions.entrySet().removeIf(entry -> entry.getValue().isExpired(ttlMinutes));
    }

    private QrPaymentStatusResponse buildQrStatusResponse(
            PendingQrPaymentSession session,
            String status,
            String message,
            OrderResponse order
    ) {
        return QrPaymentStatusResponse.builder()
                .orderId(session.orderId())
                .status(status)
                .expectedAmount(session.expectedAmount())
                .receivedAmount(session.getReceivedAmount())
                .message(message)
                .order(order)
                .build();
    }

    private OrderResponse findOrderResponse(String orderId) {
        if (!StringUtils.hasText(orderId)) {
            return null;
        }
        return orderRepository.findById(orderId).map(this::toOrderResponse).orElse(null);
    }

    private RioPaymentResult queryRioPayment(String orderId) {
        try {
            String payload = objectMapper.writeValueAsString(Map.of("order_id", orderId));
            long timeoutMs = qrVerifyTimeoutMs > 0 ? qrVerifyTimeoutMs : 8000L;
            HttpRequest request = HttpRequest.newBuilder(URI.create(qrVerifyUrl))
                    .timeout(Duration.ofMillis(timeoutMs))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(payload))
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                log.warn("QR verify gateway responded {} for order {}", response.statusCode(), orderId);
                return new RioPaymentResult(false, 0L);
            }
            JsonNode body = objectMapper.readTree(response.body());
            boolean paid = body.path("status").asBoolean(false);
            long amount = body.path("amount").asLong(0L);
            return new RioPaymentResult(paid, amount);
        } catch (Exception ex) {
            log.warn("Failed to verify QR payment for order {}", orderId, ex);
            return new RioPaymentResult(false, 0L);
        }
    }

    private record RioPaymentResult(boolean paid, long amount) {
    }

    private enum QrSessionState {
        PENDING,
        COMPLETED,
        CANCELLED
    }

    private static final class PendingQrPaymentSession {
        private final String orderId;
        private final String customerId;
        private final OrderRequest orderRequest;
        private final long expectedAmount;
        private final Instant createdAt;
        private QrSessionState state = QrSessionState.PENDING;
        private String createdOrderId;
        private Long receivedAmount;
        private String lastMessage;

        private PendingQrPaymentSession(
                String orderId,
                String customerId,
                OrderRequest orderRequest,
                long expectedAmount,
                Instant createdAt
        ) {
            this.orderId = orderId;
            this.customerId = customerId;
            this.orderRequest = orderRequest;
            this.expectedAmount = expectedAmount;
            this.createdAt = createdAt;
        }

        private String orderId() {
            return orderId;
        }

        private String customerId() {
            return customerId;
        }

        private OrderRequest orderRequest() {
            return orderRequest;
        }

        private long expectedAmount() {
            return expectedAmount;
        }

        private boolean isExpired(long ttlMinutes) {
            return createdAt.plus(ttlMinutes, ChronoUnit.MINUTES).isBefore(Instant.now());
        }

        private void complete(String orderId, Long amount) {
            this.state = QrSessionState.COMPLETED;
            this.createdOrderId = orderId;
            this.receivedAmount = amount;
            this.lastMessage = "Payment completed.";
        }

        private void cancel(String message) {
            this.state = QrSessionState.CANCELLED;
            this.lastMessage = message;
        }

        private boolean isCompleted() {
            return this.state == QrSessionState.COMPLETED;
        }

        private boolean isCancelled() {
            return this.state == QrSessionState.CANCELLED;
        }

        private void setReceivedAmount(Long receivedAmount) {
            this.receivedAmount = receivedAmount;
        }

        private Long getReceivedAmount() {
            return receivedAmount;
        }

        private String getCreatedOrderId() {
            return createdOrderId;
        }

        private String getLastMessage() {
            return lastMessage;
        }
    }

    private Payment buildPayment(OrderRequest request, Order order, long amount) {
        OrderRequest.PaymentRequest paymentRequest = request.getPayment();
        PaymentMethod method = paymentRequest != null && paymentRequest.getMethod() != null
                ? paymentRequest.getMethod()
                : PaymentMethod.BANK_TRANSFER;

        if (method != PaymentMethod.BANK_TRANSFER) {
            throw new IllegalArgumentException("Chỉ hỗ trợ thanh toán chuyển khoản QR.");
        }

        boolean isPaid = paymentRequest != null && Boolean.TRUE.equals(paymentRequest.getIsPaid());
        if (!isPaid) {
            throw new IllegalArgumentException("Đơn hàng chỉ được tạo sau khi thanh toán thành công.");
        }

        PaymentStatus status = paymentRequest != null && paymentRequest.getStatus() != null
                ? paymentRequest.getStatus()
                : PaymentStatus.COMPLETED;
        if (status != PaymentStatus.COMPLETED) {
            throw new IllegalArgumentException("Trạng thái thanh toán phải là COMPLETED.");
        }

        Date paymentDate = paymentRequest != null && paymentRequest.getPaymentDate() != null
                ? paymentRequest.getPaymentDate()
                : (isPaid ? new Date() : null);

        return Payment.builder()
                .order(order)
                .amount(amount)
                .method(method)
                .status(status)
                .isPaid(isPaid)
                .paymentDate(paymentDate)
                .build();
    }

    private Shipping buildShipping(OrderRequest request, Order order, Customer customer) {
        OrderRequest.ShippingRequest shippingRequest = request.getShipping();

        String carrierPhone = null;
        if (shippingRequest != null) {
            carrierPhone = normalizeText(shippingRequest.getCarrierPhone());
            if (!StringUtils.hasText(carrierPhone)) {
                carrierPhone = normalizeText(shippingRequest.getPhone());
            }
        }
        if (!StringUtils.hasText(carrierPhone)) {
            carrierPhone = normalizeText(customer.getPhone());
        }

        return Shipping.builder()
                .order(order)
                .trackingNumber(shippingRequest != null ? normalizeText(shippingRequest.getTrackingNumber()) : null)
                .carrierName(shippingRequest != null ? normalizeText(shippingRequest.getCarrierName()) : null)
                .carrierPhone(carrierPhone)
                .estimatedDelivery(shippingRequest != null ? shippingRequest.getEstimatedDelivery() : null)
                .build();
    }

    private String resolveShippingAddress(OrderRequest request, Customer customer) {
        if (StringUtils.hasText(request.getShippingAddress())) {
            return request.getShippingAddress().trim();
        }

        OrderRequest.ShippingRequest shipping = request.getShipping();
        if (shipping != null) {
            List<String> addressParts = new ArrayList<>();
            if (StringUtils.hasText(shipping.getDetailAddress())) {
                addressParts.add(shipping.getDetailAddress().trim());
            }
            if (StringUtils.hasText(shipping.getWard())) {
                addressParts.add(shipping.getWard().trim());
            }
            if (StringUtils.hasText(shipping.getDistrict())) {
                addressParts.add(shipping.getDistrict().trim());
            }
            if (StringUtils.hasText(shipping.getProvince())) {
                addressParts.add(shipping.getProvince().trim());
            }
            if (!addressParts.isEmpty()) {
                return String.join(", ", addressParts);
            }
        }

        return normalizeText(customer.getAddress());
    }

    private String normalizeText(String input) {
        if (!StringUtils.hasText(input)) {
            return null;
        }
        return input.trim();
    }

    private String normalizeCancelReason(String rawReason) {
        if (!StringUtils.hasText(rawReason)) {
            return "Khác";
        }
        String normalized = rawReason.trim().toUpperCase();
        return switch (normalized) {
            case "WRONG_PRODUCT" -> "Đặt nhầm sản phẩm";
            case "BETTER_PRICE" -> "Tìm thấy giá tốt hơn";
            case "DONT_NEED_ANYMORE" -> "Không cần nữa";
            case "CHANGED_MIND" -> "Thay đổi ý định";
            case "DELIVERY_TOO_LONG" -> "Thời gian giao hàng quá lâu";
            case "OTHER" -> "Khác";
            default -> rawReason.trim();
        };
    }

    private String extractCancellationReason(String note) {
        if (!StringUtils.hasText(note)) {
            return null;
        }
        String prefix = "Lý do:";
        if (!note.startsWith(prefix)) {
            return null;
        }
        String body = note.substring(prefix.length()).trim();
        int noteIndex = body.indexOf(";");
        return noteIndex >= 0 ? body.substring(0, noteIndex).trim() : body;
    }

    private void appendHistory(Order order, OrderStatus status, String note, String changedBy) {
        OrderStatusHistory history = OrderStatusHistory.builder()
                .order(order)
                .status(status)
                .note(note)
                .changedBy(changedBy)
                .build();
        historyRepository.save(history);
    }

    private void sendCancellationEmailAsync(
            Order order,
            String reason,
            String note,
            boolean paidOrder,
            boolean restockIssue
    ) {
        String email = order.getCustomer() != null ? order.getCustomer().getEmail() : null;
        if (!StringUtils.hasText(email)) {
            return;
        }

        String customerName = StringUtils.hasText(order.getCustomer().getFullName())
                ? order.getCustomer().getFullName()
                : order.getCustomer().getUsername();

        SimpleMailMessage message = new SimpleMailMessage();
        if (StringUtils.hasText(mailFromAddress)) {
            message.setFrom(mailFromAddress.trim());
        }
        message.setTo(email.trim());
        message.setSubject("Xác nhận hủy đơn hàng #" + order.getId());

        StringBuilder body = new StringBuilder();
        body.append("Xin chào ").append(customerName).append(",\n\n")
                .append("Đơn hàng #").append(order.getId()).append(" đã được hủy thành công.\n")
                .append("Lý do: ").append(reason).append(".\n");

        if (StringUtils.hasText(note)) {
            body.append("Ghi chú: ").append(note).append("\n");
        }
        if (paidOrder) {
            body.append(REFUND_PROCESSING_MESSAGE).append("\n");
        }
        if (restockIssue) {
            body.append("Lưu ý: hệ thống đang xử lý sự cố hoàn kho, nhân viên sẽ kiểm tra thủ công.\n");
        }

        body.append("\nTrân trọng,\nChronolux Team");
        message.setText(body.toString());

        new Thread(() -> {
            try {
                mailSender.send(message);
                log.info("Cancellation email sent to {} for order {}", email, order.getId());
            } catch (MailException ex) {
                log.warn("Failed to send cancellation email to {} for order {}", email, order.getId(), ex);
            } catch (Exception ex) {
                log.warn("Unexpected error while sending cancellation email to {} for order {}", email, order.getId(), ex);
            }
        }, "order-cancel-email-sender").start();
    }
}

