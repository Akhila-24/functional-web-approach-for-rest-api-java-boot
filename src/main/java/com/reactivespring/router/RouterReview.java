package com.reactivespring.router;

import com.reactivespring.handler.ReviewHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.ServerResponse;

import static org.springframework.web.reactive.function.server.RouterFunctionDslKt.router;
import static org.springframework.web.reactive.function.server.RouterFunctions.route;

@Configuration
public class RouterReview {
    private ReviewHandler reviewHandler;

    public RouterReview(ReviewHandler reviewHandler) {
        this.reviewHandler = reviewHandler;
    }

    @Bean
    public RouterFunction<ServerResponse> handleRequests() {
        return route()
                .GET("/v1/helloworld", (serverRequest -> ServerResponse.ok().bodyValue("helloworld")))
                .POST("/v1/reviews/add",serverRequest ->reviewHandler.addReview(serverRequest) )
                .GET("/v1/reviews",serverRequest -> reviewHandler.getAllReviews())
                .PUT("/v1/reviews/update/{id}",serverRequest -> reviewHandler.updateReview(serverRequest))
                .DELETE("/v1/reviews/delete/{id}",serverRequest -> reviewHandler.deleteReview(serverRequest))
                .GET("/v1/reviews/{movieInfoId}",serverRequest -> reviewHandler.getReviewsByMovieInfoId(serverRequest))
                .build();
    }
}
