package com.example.demo.services;

import com.example.demo.dtos.request.ReviewRequest;
import com.example.demo.dtos.response.ReviewResponse;
import com.example.demo.entities.Customer;
import com.example.demo.entities.Product;
import com.example.demo.entities.Review;
import com.example.demo.exceptions.ResourceNotFoundException;
import com.example.demo.repositories.CustomerRepository;
import com.example.demo.repositories.ProductRepository;
import com.example.demo.repositories.ReviewRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
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

    public ReviewResponse createReview(ReviewRequest request) {
        accessControlService.requireCustomerAccess(request.getCustomerId());

        if (reviewRepository.existsByCustomerIdAndProductId(request.getCustomerId(), request.getProductId())) {
            throw new IllegalStateException("Bạn đã đánh giá sản phẩm này rồi.");
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
        try {
            return toResponse(reviewRepository.save(review));
        } catch (DataIntegrityViolationException ex) {
            throw new IllegalStateException("Bạn đã đánh giá sản phẩm này rồi.");
        }
    }

    public ReviewResponse updateReview(String id, ReviewRequest request) {
        Review existing = reviewRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Review not found: " + id));
        accessControlService.requireCustomerAccess(existing.getCustomer().getId());

        if (!existing.getCustomer().getId().equals(request.getCustomerId())
                || !existing.getProduct().getId().equals(request.getProductId())) {
            throw new IllegalStateException("Changing customerId/productId of a review is not allowed");
        }

        existing.setRating(request.getRating());
        existing.setComment(request.getComment());
        return toResponse(reviewRepository.save(existing));
    }

    public void deleteReview(String id) {
        Review review = reviewRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Review not found: " + id));
        accessControlService.requireCustomerAccess(review.getCustomer().getId());
        reviewRepository.delete(review);
    }

    @Transactional(readOnly = true)
    public Optional<ReviewResponse> findById(String id) {
        return reviewRepository.findById(id)
                .map(review -> {
                    accessControlService.requireCustomerAccess(review.getCustomer().getId());
                    return toResponse(review);
                });
    }

    @Transactional(readOnly = true)
    public List<ReviewResponse> findByProductId(String productId) {
        return reviewRepository.findByProductId(productId).stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public List<ReviewResponse> findByCustomerId(String customerId) {
        accessControlService.requireCustomerAccess(customerId);
        return reviewRepository.findByCustomerId(customerId).stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public Double getAverageRating(String productId) {
        Double avg = reviewRepository.findAverageRatingByProductId(productId);
        return avg != null ? avg : 0.0;
    }

    private ReviewResponse toResponse(Review review) {
        return ReviewResponse.builder()
                .id(review.getId())
                .rating(review.getRating())
                .comment(review.getComment())
                .createdAt(review.getCreatedAt())
                .customerId(review.getCustomer() != null ? review.getCustomer().getId() : null)
                .customerUsername(
                    review.getCustomer() != null
                        ? review.getCustomer().getUsername()
                        : null
                )
                .productId(review.getProduct() != null ? review.getProduct().getId() : null)
                .productName(review.getProduct() != null ? review.getProduct().getName() : null)
                .build();
    }
}
