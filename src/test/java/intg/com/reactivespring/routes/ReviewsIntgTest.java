package com.reactivespring.routes;

import com.reactivespring.domain.Review;
import com.reactivespring.repository.ReviewReactiveRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.testcontainers.containers.MongoDBContainer;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.util.List;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureWebTestClient
@ActiveProfiles("test")
class ReviewsIntgTest {

    @Autowired
    ReviewReactiveRepository reviewReactiveRepository;
    @Autowired
    private WebTestClient webTestClient;
    static final MongoDBContainer mongoDBContainer = new MongoDBContainer("mongo:latest")
            .withExposedPorts(27017);

    static {
        mongoDBContainer.start();
    }

    @DynamicPropertySource
    static void setProperties(DynamicPropertyRegistry registry) {
        String mongoUri = mongoDBContainer.getReplicaSetUrl();
        System.out.println("Mongo URI: " + mongoUri); // Log the URI
        registry.add("spring.data.mongodb.uri", () -> mongoUri);
        registry.add("spring.data.mongodb.database", () -> "testdb");
    }


    @BeforeEach
    public void setUp() {
        reviewReactiveRepository.deleteAll().block();
        List<Review> reviews = List.of(new Review("r1", 1L, "super", 5.0), new Review(null, 2L, "average", 4.0));
        reviewReactiveRepository.saveAll(reviews).blockLast();
    }

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
        Review review = new Review(null, 3L, "super movie", 5.0);
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
    void getAllReviews() {
        webTestClient.get().uri("/v1/reviews")
                .exchange()
                .expectStatus()
                .is2xxSuccessful()
                .expectBodyList(Review.class)
                .hasSize(2);
    }

    @Test
    void update_existing_review() {
        Review update_review = new Review(null, 1L, "updated_comment", 5.0);
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
    void delete_existing_review() {
        webTestClient.delete().uri("/v1/reviews/delete/{id}", "r1")
                .exchange()
                .expectStatus()
                .isNoContent();
    }

    @Test
    void getReviewsByMovieInfoId() {
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
