package com.reactivespring.handler;

import com.reactivespring.domain.Review;
import com.reactivespring.exception.ReviewDataException;
import com.reactivespring.exception.ReviewNotFoundException;
import com.reactivespring.repository.ReviewReactiveRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import javax.validation.ConstraintViolation;
import javax.validation.Validator;
import java.util.Set;
import java.util.stream.Collectors;

@Component
public class ReviewHandler {

    @Autowired
    private Validator validator;
    private ReviewReactiveRepository reviewReactiveRepository;

    public ReviewHandler(ReviewReactiveRepository reviewReactiveRepository) {
        this.reviewReactiveRepository = reviewReactiveRepository;
    }

    public Mono<ServerResponse> addReview(ServerRequest serverRequest) {
        return serverRequest.bodyToMono(Review.class)
                .doOnNext(this::validate)
                .flatMap(reviewReactiveRepository::save)
                .flatMap(ServerResponse.ok()::bodyValue);
    }

    private void validate(Review review) {
        Set<ConstraintViolation<Review>> violations = validator.validate(review);
        if (!violations.isEmpty()) {
            String errors = violations
                    .stream()
                    .map(ConstraintViolation::getMessage)
                    .sorted()
                    .collect(Collectors.joining(","));
            throw new ReviewDataException(errors);
        }
    }

    public Mono<ServerResponse> getAllReviews() {
        return ServerResponse.ok().body(reviewReactiveRepository.findAll(), Review.class);
    }

    public Mono<ServerResponse> updateReview(ServerRequest serverRequest) {
        String reviewId = serverRequest.pathVariable("id");
        Mono<Review> existingReview = reviewReactiveRepository.findById(reviewId)
                .switchIfEmpty(Mono.error(new ReviewNotFoundException("review not found")));
        return existingReview.flatMap(existing_review -> serverRequest.bodyToMono(Review.class).map(reqreview -> {
            existing_review.setReviewId(reqreview.getReviewId());
            existing_review.setComment(reqreview.getComment());
            existing_review.setRating(reqreview.getRating());
            existing_review.setMovieInfoId(reqreview.getMovieInfoId());
            return existing_review;
        })).flatMap(reviewReactiveRepository::save).flatMap(savedreview -> ServerResponse.ok()
                .bodyValue(savedreview));
//                .switchIfEmpty(ServerResponse.notFound().build());
    }

    public Mono<ServerResponse> deleteReview(ServerRequest serverRequest) {
        String reviewId = serverRequest.pathVariable("id");
        Mono<Review> existingReview = reviewReactiveRepository.findById(reviewId);
        return existingReview.flatMap(review -> reviewReactiveRepository.deleteById(reviewId))
                .then(ServerResponse.noContent().build());
    }

    public Mono<ServerResponse> getReviewsByMovieInfoId(ServerRequest serverRequest) {
        Long movieInfoId = Long.valueOf(serverRequest.pathVariable("movieInfoId"));
        Flux<Review> reviews = reviewReactiveRepository.findByMovieInfoId(movieInfoId);
        return reviews.collectList().flatMap(list_of_reviews -> ServerResponse.ok().bodyValue(list_of_reviews));
    }
}
