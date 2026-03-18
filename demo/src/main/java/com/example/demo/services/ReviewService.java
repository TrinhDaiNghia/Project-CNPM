package com.example.demo.services;

import com.example.demo.dtos.request.ReviewRequest;
import com.example.demo.entities.Customer;
import com.example.demo.entities.Product;
import com.example.demo.entities.Review;
import com.example.demo.exceptions.ResourceNotFoundException;
import com.example.demo.repositories.CustomerRepository;
import com.example.demo.repositories.ProductRepository;
import com.example.demo.repositories.ReviewRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional
public class ReviewService {

    private final ReviewRepository reviewRepository;
    private final CustomerRepository customerRepository;
    private final ProductRepository productRepository;
    private final AccessControlService accessControlService;

    public Review createReview(ReviewRequest request) {
        accessControlService.requireCustomerAccess(request.getCustomerId());
        if (reviewRepository.existsByCustomerIdAndProductId(request.getCustomerId(), request.getProductId())) {
            throw new IllegalStateException("Review already exists for this customer and product");
        }

        Customer customer = customerRepository.findById(request.getCustomerId())
                .orElseThrow(() -> new ResourceNotFoundException("Customer not found: " + request.getCustomerId()));
        Product product = productRepository.findById(request.getProductId())
                .orElseThrow(() -> new ResourceNotFoundException("Product not found: " + request.getProductId()));

        Review review = new Review();
        review.setCustomer(customer);
        review.setProduct(product);
        review.setRating(request.getRating());
        review.setComment(request.getComment());
        return reviewRepository.save(review);
    }

    public Review updateReview(String id, ReviewRequest request) {
        Review existing = reviewRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Review not found: " + id));
        accessControlService.requireCustomerAccess(existing.getCustomer().getId());

        if (!existing.getCustomer().getId().equals(request.getCustomerId())
                || !existing.getProduct().getId().equals(request.getProductId())) {
            throw new IllegalStateException("Changing customerId/productId of a review is not allowed");
        }

        existing.setRating(request.getRating());
        existing.setComment(request.getComment());
        return reviewRepository.save(existing);
    }

    public void deleteReview(String id) {
        Review review = reviewRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Review not found: " + id));
        accessControlService.requireCustomerAccess(review.getCustomer().getId());
        reviewRepository.delete(review);
    }

    @Transactional(readOnly = true)
    public Optional<Review> findById(String id) {
        return reviewRepository.findById(id)
                .map(review -> {
                    accessControlService.requireCustomerAccess(review.getCustomer().getId());
                    return review;
                });
    }

    @Transactional(readOnly = true)
    public List<Review> findByProductId(String productId) {
        return reviewRepository.findByProductId(productId);
    }

    @Transactional(readOnly = true)
    public List<Review> findByCustomerId(String customerId) {
        accessControlService.requireCustomerAccess(customerId);
        return reviewRepository.findByCustomerId(customerId);
    }

    @Transactional(readOnly = true)
    public Double getAverageRating(String productId) {
        return reviewRepository.findAverageRatingByProductId(productId);
    }
}
