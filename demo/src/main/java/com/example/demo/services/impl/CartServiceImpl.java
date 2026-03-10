package com.example.demo.services.impl;

import com.example.demo.entities.Cart;
import com.example.demo.entities.CartItem;
import com.example.demo.entities.Customer;
import com.example.demo.entities.Product;
import com.example.demo.repositories.CartRepository;
import com.example.demo.repositories.CustomerRepository;
import com.example.demo.repositories.ProductRepository;
import com.example.demo.services.CartService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class CartServiceImpl implements CartService {

    private final CartRepository cartRepository;
    private final CustomerRepository customerRepository;
    private final ProductRepository productRepository;

    @Override
    public Cart getOrCreateCart(String customerId) {
        return cartRepository.findByCustomerId(customerId).orElseGet(() -> {
            Customer customer = customerRepository.findById(customerId)
                    .orElseThrow(() -> new RuntimeException("Customer not found: " + customerId));
            Cart cart = Cart.builder().customer(customer).build();
            return cartRepository.save(cart);
        });
    }

    @Override
    public Cart addItem(String customerId, String productId, int quantity) {
        Cart cart = getOrCreateCart(customerId);
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new RuntimeException("Product not found: " + productId));

        cart.getItems().stream()
                .filter(item -> item.getProduct().getId().equals(productId))
                .findFirst()
                .ifPresentOrElse(
                        item -> item.setQuantity(item.getQuantity() + quantity),
                        () -> cart.getItems().add(
                                CartItem.builder()
                                        .cart(cart)
                                        .product(product)
                                        .quantity(quantity)
                                        .subTotal(product.getPrice() * quantity)
                                        .build()
                        )
                );
        recalcTotal(cart);
        return cartRepository.save(cart);
    }

    @Override
    public Cart updateItemQuantity(String customerId, String productId, int quantity) {
        Cart cart = getOrCreateCart(customerId);
        if (quantity <= 0) {
            return removeItem(customerId, productId);
        }
        cart.getItems().stream()
                .filter(item -> item.getProduct().getId().equals(productId))
                .findFirst()
                .ifPresent(item -> {
                    item.setQuantity(quantity);
                    item.setSubTotal(item.getProduct().getPrice() * quantity);
                });
        recalcTotal(cart);
        return cartRepository.save(cart);
    }

    @Override
    public Cart removeItem(String customerId, String productId) {
        Cart cart = getOrCreateCart(customerId);
        cart.getItems().removeIf(item -> item.getProduct().getId().equals(productId));
        recalcTotal(cart);
        return cartRepository.save(cart);
    }

    @Override
    public void clearCart(String customerId) {
        Cart cart = getOrCreateCart(customerId);
        cart.getItems().clear();
        cart.setTotalAmount(0L);
        cartRepository.save(cart);
    }

    private void recalcTotal(Cart cart) {
        long total = cart.getItems().stream()
                .mapToLong(item -> item.getProduct().getPrice() * item.getQuantity())
                .sum();
        cart.setTotalAmount(total);
    }
}
