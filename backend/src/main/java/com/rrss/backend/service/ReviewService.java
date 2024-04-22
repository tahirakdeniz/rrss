package com.rrss.backend.service;

import com.rrss.backend.dto.*;
import com.rrss.backend.model.*;
import com.rrss.backend.repository.*;
import com.rrss.backend.util.UserUtil;
import org.springframework.stereotype.Service;

import java.security.Principal;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
public class ReviewService {
    private final ReviewFormRepository reviewFormRepository;
    private final ProductCategoryRepository productCategoryRepository;
    private final ReviewFieldRepository reviewFieldRepository;
    private final UserUtil userUtil;
    private final ReviewRepository reviewRepository;
    private final ProductRepository productRepository;
    private final FieldScoreRepository fieldScoreRepository;

    public ReviewService(ReviewFormRepository reviewFormRepository, ProductCategoryRepository productCategoryRepository, ReviewFieldRepository reviewFieldRepository, UserUtil userUtil, ReviewRepository reviewRepository, ProductRepository productRepository, FieldScoreRepository fieldScoreRepository) {
        this.reviewFormRepository = reviewFormRepository;
        this.productCategoryRepository = productCategoryRepository;
        this.reviewFieldRepository = reviewFieldRepository;
        this.userUtil = userUtil;
        this.reviewRepository = reviewRepository;
        this.productRepository = productRepository;
        this.fieldScoreRepository = fieldScoreRepository;
    }

    public ReviewFormDto createReviewForm(ReviewFormRequest reviewFormRequest) {
        return ReviewFormDto.convert(
                reviewFormRepository.save(
                        new ReviewForm(
                                reviewFormRequest.name(),
                                productCategoryRepository.findByName(reviewFormRequest.productCategoryName())
                                        .orElseThrow(() -> new RuntimeException("Product category not found"))
                        )
                )
        );

    }

    public ReviewFieldDto addReviewField(Long id, ReviewFieldRequest reviewFieldRequest) {
        ReviewForm reviewForm = reviewFormRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Review form not found"));


        ReviewField reviewField = reviewFieldRepository.save(
                new ReviewField(
                        null,
                        reviewFieldRequest.label(),
                        reviewFieldRequest.minScore(),
                        reviewFieldRequest.maxScore()
                )
        );

        reviewForm.getFields().add(reviewField);
        reviewFormRepository.save(reviewForm);

        return ReviewFieldDto.convert(reviewField);
    }



    public ReviewFormDto getReviewForm(String productCategoryName) {
        return ReviewFormDto.convert(
                reviewFormRepository.findByProductTypeName(productCategoryName)
                        .orElseThrow(() -> new RuntimeException("Review form not found"))
        );
    }

    public String deleteReviewField(Long id, Long fieldId) {
        ReviewForm reviewForm = reviewFormRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Review form not found"));

        ReviewField reviewField = reviewFieldRepository.findById(fieldId)
                .orElseThrow(() -> new RuntimeException("Review field not found"));

        reviewForm.getFields().remove(reviewField);
        reviewFormRepository.save(reviewForm);

        reviewFieldRepository.delete(reviewField);

        return "Review field deleted";
    }

    public ReviewFieldDto updateReviewField(Long id, Long fieldId, ReviewFieldRequest reviewFieldRequest) {
        ReviewForm reviewForm = reviewFormRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Review form not found"));

        ReviewField reviewField = reviewFieldRepository.findById(fieldId)
                .orElseThrow(() -> new RuntimeException("Review field not found"));

        reviewForm.getFields().remove(reviewField);

        ReviewField newReviewField = reviewFieldRepository.save(
                new ReviewField(
                    reviewField.getId(),
                    reviewFieldRequest.label(),
                    reviewFieldRequest.minScore(),
                    reviewFieldRequest.maxScore()
                )
        );

        reviewForm.getFields().add(newReviewField);

        return ReviewFieldDto.convert(newReviewField);
    }


    public ReviewDto submitReview(Principal currentUser, Long productId, ReviewSubmitRequest reviewSubmitRequest) {
        var user = userUtil.extractUser(currentUser);

        // Check if the user has bought the product
        boolean boughtProduct = user.getPurchases().stream()
                .anyMatch(purchase -> purchase.getItems().stream()
                        .anyMatch(item -> Objects.equals(item.getProduct().getId(), productId)));

        if (!boughtProduct) {
            throw new RuntimeException("User has not bought the product");
        }

        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new RuntimeException("Product not found"));

        var review = reviewRepository.save(
                new Review(
                        null,
                        product,
                        user,
                        reviewSubmitRequest.comment()
                )
        );

        reviewRepository.save(review);

        List<FieldScore> fieldScores = reviewSubmitRequest
                .fieldScores()
                .stream()
                .map(scoreDto ->
                        fieldScoreRepository.save(
                                new FieldScore(
                                    new FieldScoreId(0L, scoreDto.reviewFieldDto().id()), // Assuming fieldId is the ID for ReviewField
                                    review,
                                    reviewFieldRepository.findById(scoreDto.reviewFieldDto().id())
                                        .orElseThrow(() -> new RuntimeException("Review field not found")),
                                    scoreDto.score()
                                )
                        )
                )
                .toList();

        return ReviewDto.convert(review);

    }
}