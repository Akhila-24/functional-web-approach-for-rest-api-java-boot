package com.reactivespring.routes;

import com.reactivespring.domain.Review;
import com.reactivespring.exceptionHandler.GlobalExceptionHandler;
import com.reactivespring.handler.ReviewHandler;
import com.reactivespring.repository.ReviewReactiveRepository;
import com.reactivespring.router.RouterReview;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.when;

@WebFluxTest
@ContextConfiguration(classes = {RouterReview.class, ReviewHandler.class, GlobalExceptionHandler.class})
@AutoConfigureWebTestClient
public class ReviewsUnitTest {
    @Autowired
    private WebTestClient webTestClient;

    @MockBean
    private ReviewReactiveRepository reviewReactiveRepository;

    @Test
    void getHelloWorld() {
        webTestClient.get().uri("/v1/helloworld")
                .exchange()
                .expectStatus()
                .is2xxSuccessful()
                .expectBody(String.class)
                .consumeWith(response -> {
                    String responseBody = response.getResponseBody();
                    assertEquals(responseBody, "helloworld");
                });
    }

    @Test
    void addReview() {
        Review review = new Review("r1", 3L, "super movie", 5.0);
        when(reviewReactiveRepository.save(isA(Review.class))).thenReturn(Mono.just(review));
        webTestClient.post().uri("/v1/reviews/add").
                bodyValue(review)
                .exchange()
                .expectStatus()
                .is2xxSuccessful()
                .expectBody(Review.class)
                .consumeWith(response -> {
                    Review responseBody = response.getResponseBody();
                    assert responseBody != null;
                    assert responseBody.getReviewId() != null;
                    assertEquals(responseBody.getComment(), "super movie");
                });
    }

    @Test
    void addReview_validation() {
        Review review = new Review("r1", null, "super movie", -5.0);
        when(reviewReactiveRepository.save(isA(Review.class))).thenReturn(Mono.just(review));
        webTestClient.post().uri("/v1/reviews/add").
                bodyValue(review)
                .exchange()
                .expectStatus()
                .isBadRequest().expectBody(String.class)
                .isEqualTo("movieinfoid should not be null,rating.negative : please pass a non-negative value");
    }

    @Test
    void getAllReviews() {
        Review review = new Review(null, 3L, "super movie", 5.0);
        when(reviewReactiveRepository.findAll()).thenReturn(Flux.fromIterable(List.of(review)));
        webTestClient.get().uri("/v1/reviews")
                .exchange()
                .expectStatus()
                .is2xxSuccessful()
                .expectBodyList(Review.class)
                .hasSize(1);
    }

    @Test
    void update_existing_review() {
        Review review = new Review("r1", 1L, "super movie", 5.0);
        Review update_review = new Review("r1", 1L, "updated_comment", 5.0);
        when(reviewReactiveRepository.findById("r1")).thenReturn(Mono.just(review));
        when(reviewReactiveRepository.save(update_review)).thenReturn(Mono.just(update_review));
        webTestClient.put().uri("/v1/reviews/update/{id}", "r1").bodyValue(update_review)
                .exchange()
                .expectStatus()
                .is2xxSuccessful()
                .expectBody(Review.class)
                .consumeWith(result -> {
                    Review responseBody = result.getResponseBody();
                    assertEquals(responseBody.getComment(), "updated_comment");
                });
    }
    @Test
    void update_existing_review_not_found() {
        Review update_review = new Review("r1", 1L, "updated_comment", 5.0);
        when(reviewReactiveRepository.findById("r1")).thenReturn(Mono.empty());
        when(reviewReactiveRepository.save(update_review)).thenReturn(Mono.just(update_review));
        webTestClient.put().uri("/v1/reviews/update/{id}", "r1").bodyValue(update_review)
                .exchange()
                .expectStatus()
                .isNotFound()
                .expectBody(String.class)
                .isEqualTo("review not found");
    }


    @Test
    void delete_existing_review() {
        Review review = new Review("r1", 1L, "super movie", 5.0);
        when(reviewReactiveRepository.findById("r1")).thenReturn(Mono.just(review));
        when(reviewReactiveRepository.deleteById("r1")).thenReturn(Mono.empty());
        webTestClient.delete().uri("/v1/reviews/delete/{id}", "r1")
                .exchange()
                .expectStatus()
                .isNoContent();
    }

    @Test
    void getReviewsByMovieInfoId() {
        Review review_1 = new Review("r1", 1L, "super movie", 5.0);
        when(reviewReactiveRepository.findByMovieInfoId(1L)).thenReturn(Flux.fromIterable(List.of(review_1)));
        webTestClient.get().uri("/v1/reviews/{movieInfoId}", 1L)
                .exchange()
                .expectStatus()
                .is2xxSuccessful().expectBodyList(Review.class).consumeWith(response -> {
                    List<Review> responseBody = response.getResponseBody();
                    assert responseBody != null;
                    responseBody.forEach(review -> assertEquals(review.getMovieInfoId(), 1L));
                });
    }
}
