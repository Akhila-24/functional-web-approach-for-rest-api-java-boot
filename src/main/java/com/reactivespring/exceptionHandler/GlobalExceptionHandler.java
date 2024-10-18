package com.reactivespring.exceptionHandler;

import com.reactivespring.exception.ReviewDataException;
import com.reactivespring.exception.ReviewNotFoundException;
import org.springframework.boot.web.reactive.error.ErrorWebExceptionHandler;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@Component
public class GlobalExceptionHandler implements ErrorWebExceptionHandler {
    @Override
    public Mono<Void> handle(ServerWebExchange serverWebExchange, Throwable throwable) {
        DataBufferFactory dataBufferFactory = serverWebExchange.getResponse().bufferFactory();
        DataBuffer errorMessage = dataBufferFactory.wrap(throwable.getMessage().getBytes());
        if(throwable instanceof ReviewDataException){
            serverWebExchange.getResponse().setStatusCode(HttpStatus.BAD_REQUEST);
            return serverWebExchange.getResponse().writeWith(Mono.just(errorMessage));
        }
        if(throwable instanceof ReviewNotFoundException){
            serverWebExchange.getResponse().setStatusCode(HttpStatus.NOT_FOUND);
            return serverWebExchange.getResponse().writeWith(Mono.just(errorMessage));
        }
        serverWebExchange.getResponse().setStatusCode(HttpStatus.INTERNAL_SERVER_ERROR);
        return serverWebExchange.getResponse().writeWith(Mono.just(errorMessage));
    }
}
