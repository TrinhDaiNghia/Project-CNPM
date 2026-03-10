package com.example.demo.services;

import com.example.demo.entities.Review;

import java.util.List;
import java.util.Optional;

public interface ReviewService {

    Review createReview(Review review);

    Review updateReview(String id, Review review);

    void deleteReview(String id);

    Optional<Review> findById(String id);

    List<Review> findByProductId(String productId);

    List<Review> findByCustomerId(String customerId);

    Double getAverageRating(String productId);
}
