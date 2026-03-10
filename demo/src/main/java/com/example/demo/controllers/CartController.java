package com.example.demo.controllers;

import com.example.demo.entities.Cart;
import com.example.demo.services.CartService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/cart")
@RequiredArgsConstructor
public class CartController {

    private final CartService cartService;

    @GetMapping("/{customerId}")
    public ResponseEntity<Cart> getCart(@PathVariable String customerId) {
        return ResponseEntity.ok(cartService.getOrCreateCart(customerId));
    }

    @PostMapping("/{customerId}/items")
    public ResponseEntity<Cart> addItem(
            @PathVariable String customerId,
            @RequestParam String productId,
            @RequestParam int quantity) {
        return ResponseEntity.ok(cartService.addItem(customerId, productId, quantity));
    }

    @PutMapping("/{customerId}/items/{productId}")
    public ResponseEntity<Cart> updateItem(
            @PathVariable String customerId,
            @PathVariable String productId,
            @RequestParam int quantity) {
        return ResponseEntity.ok(cartService.updateItemQuantity(customerId, productId, quantity));
    }

    @DeleteMapping("/{customerId}/items/{productId}")
    public ResponseEntity<Cart> removeItem(
            @PathVariable String customerId,
            @PathVariable String productId) {
        return ResponseEntity.ok(cartService.removeItem(customerId, productId));
    }

    @DeleteMapping("/{customerId}/clear")
    public ResponseEntity<Void> clearCart(@PathVariable String customerId) {
        cartService.clearCart(customerId);
        return ResponseEntity.noContent().build();
    }
}
