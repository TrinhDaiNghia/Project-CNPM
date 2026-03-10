package com.example.demo.services.impl;

import com.example.demo.entities.Review;
import com.example.demo.repositories.ReviewRepository;
import com.example.demo.services.ReviewService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional
public class ReviewServiceImpl implements ReviewService {

    private final ReviewRepository reviewRepository;

    @Override
    public Review createReview(Review review) {
        if (reviewRepository.existsByCustomerIdAndProductId(
                review.getCustomer().getId(), review.getProduct().getId())) {
            throw new IllegalStateException("Review already exists for this customer and product");
        }
        return reviewRepository.save(review);
    }

    @Override
    public Review updateReview(String id, Review review) {
        review.setId(id);
        return reviewRepository.save(review);
    }

    @Override
    public void deleteReview(String id) {
        reviewRepository.deleteById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Review> findById(String id) {
        return reviewRepository.findById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Review> findByProductId(String productId) {
        return reviewRepository.findByProductId(productId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Review> findByCustomerId(String customerId) {
        return reviewRepository.findByCustomerId(customerId);
    }

    @Override
    @Transactional(readOnly = true)
    public Double getAverageRating(String productId) {
        return reviewRepository.findAverageRatingByProductId(productId);
    }
}
