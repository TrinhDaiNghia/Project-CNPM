package com.example.demo.services;

import com.example.demo.dtos.response.CartItemResponse;
import com.example.demo.dtos.response.CartProductResponse;
import com.example.demo.dtos.response.CartResponse;
import com.example.demo.entities.Cart;
import com.example.demo.entities.CartItem;
import com.example.demo.entities.Customer;
import com.example.demo.entities.Product;
import com.example.demo.exceptions.ResourceNotFoundException;
import com.example.demo.repositories.CartRepository;
import com.example.demo.repositories.CustomerRepository;
import com.example.demo.repositories.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class CartService {

    private final CartRepository cartRepository;
    private final CustomerRepository customerRepository;
    private final ProductRepository productRepository;
    private final AccessControlService accessControlService;

    @Transactional(readOnly = true)
    public CartResponse getOrCreateCart(String customerId) {
        return toResponse(getOrCreateCartEntity(customerId));
    }

    @Transactional(readOnly = true)
    public CartResponse getOrCreateCartResponse(String customerId) {
        return getOrCreateCart(customerId);
    }

    private Cart getOrCreateCartEntity(String customerId) {
        accessControlService.requireCustomerAccess(customerId);
        return cartRepository.findByCustomerId(customerId).orElseGet(() -> {
            Customer customer = customerRepository.findById(customerId)
                    .orElseThrow(() -> new ResourceNotFoundException("Customer not found: " + customerId));
            Cart cart = Cart.builder().customer(customer).build();
            return cartRepository.save(cart);
        });
    }

    public CartResponse addItem(String customerId, String productId, int quantity) {
        if (quantity <= 0) {
            throw new IllegalArgumentException("Quantity must be greater than 0");
        }

        Cart cart = getOrCreateCartEntity(customerId);
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found: " + productId));

        cart.getItems().stream()
                .filter(item -> item.getProduct().getId().equals(productId))
                .findFirst()
                .ifPresentOrElse(
                        item -> {
                            int current = item.getQuantity() == null ? 0 : item.getQuantity();
                            int nextQuantity = current + quantity;
                            item.setQuantity(nextQuantity);
                            item.setSubTotal((item.getProduct().getPrice() == null ? 0L : item.getProduct().getPrice()) * nextQuantity);
                        },
                        () -> cart.getItems().add(
                                CartItem.builder()
                                        .cart(cart)
                                        .product(product)
                                        .quantity(quantity)
                                        .subTotal((product.getPrice() == null ? 0L : product.getPrice()) * quantity)
                                        .build()
                        )
                );

        recalcTotal(cart);
        return toResponse(cartRepository.save(cart));
    }

    public CartResponse addItemResponse(String customerId, String productId, int quantity) {
        return addItem(customerId, productId, quantity);
    }

    public CartResponse updateItemQuantity(String customerId, String productId, int quantity) {
        Cart cart = getOrCreateCartEntity(customerId);
        if (quantity <= 0) {
            return removeItem(customerId, productId);
        }

        cart.getItems().stream()
                .filter(item -> item.getProduct().getId().equals(productId))
                .findFirst()
                .ifPresent(item -> {
                    item.setQuantity(quantity);
                    long price = item.getProduct() == null || item.getProduct().getPrice() == null ? 0L : item.getProduct().getPrice();
                    item.setSubTotal(price * quantity);
                });

        recalcTotal(cart);
        return toResponse(cartRepository.save(cart));
    }

    public CartResponse updateItemQuantityResponse(String customerId, String productId, int quantity) {
        return updateItemQuantity(customerId, productId, quantity);
    }

    public CartResponse removeItem(String customerId, String productId) {
        Cart cart = getOrCreateCartEntity(customerId);
        cart.getItems().removeIf(item -> item.getProduct().getId().equals(productId));
        recalcTotal(cart);
        return toResponse(cartRepository.save(cart));
    }

    public CartResponse removeItemResponse(String customerId, String productId) {
        return removeItem(customerId, productId);
    }

    public void clearCart(String customerId) {
        Cart cart = getOrCreateCartEntity(customerId);
        cart.getItems().clear();
        cart.setTotalAmount(0L);
        cartRepository.save(cart);
    }

    private void recalcTotal(Cart cart) {
        long total = cart.getItems().stream()
                .mapToLong(item -> {
                    long price = item.getProduct() == null || item.getProduct().getPrice() == null ? 0L : item.getProduct().getPrice();
                    int quantity = item.getQuantity() == null ? 0 : item.getQuantity();
                    return price * quantity;
                })
                .sum();
        cart.setTotalAmount(total);
    }

    private CartResponse toResponse(Cart cart) {
        List<CartItemResponse> items = cart.getItems().stream()
                .map(this::toItemResponse)
                .toList();

        return CartResponse.builder()
                .id(cart.getId())
                .totalAmount(cart.getTotalAmount() == null ? 0L : cart.getTotalAmount())
                .customerId(cart.getCustomer() == null ? null : cart.getCustomer().getId())
                .items(items)
                .build();
    }

    private CartItemResponse toItemResponse(CartItem item) {
        int quantity = item.getQuantity() == null ? 0 : item.getQuantity();
        long unitPrice = item.getProduct() == null || item.getProduct().getPrice() == null ? 0L : item.getProduct().getPrice();
        long subTotal = item.getSubTotal() == null ? unitPrice * quantity : item.getSubTotal();

        return CartItemResponse.builder()
                .id(item.getId())
                .quantity(quantity)
                .subTotal(subTotal)
                .product(toProductResponse(item.getProduct()))
                .build();
    }

    private CartProductResponse toProductResponse(Product product) {
        if (product == null) {
            return null;
        }
        return CartProductResponse.builder()
                .id(product.getId())
                .name(product.getName())
                .price(product.getPrice())
                .build();
    }
}
