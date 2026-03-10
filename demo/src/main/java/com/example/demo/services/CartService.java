package com.example.demo.services;

import com.example.demo.entities.Cart;
import com.example.demo.entities.CartItem;

public interface CartService {

    Cart getOrCreateCart(String customerId);

    Cart addItem(String customerId, String productId, int quantity);

    Cart updateItemQuantity(String customerId, String productId, int quantity);

    Cart removeItem(String customerId, String productId);

    void clearCart(String customerId);
}
