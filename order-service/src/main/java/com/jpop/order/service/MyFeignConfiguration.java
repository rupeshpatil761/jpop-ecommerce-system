package com.jpop.order.service;

import io.github.resilience4j.feign.FeignDecorators;
import io.github.resilience4j.feign.Resilience4jFeign;
import io.github.resilience4j.ratelimiter.RateLimiter;
import io.github.resilience4j.ratelimiter.RateLimiterConfig;
import io.github.resilience4j.ratelimiter.RateLimiterRegistry;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryConfig;
import io.github.resilience4j.retry.RetryRegistry;
import org.springframework.context.annotation.Bean;

import java.time.Duration;

public class MyFeignConfiguration {

    @Bean
    public Resilience4jFeign.Builder feignBuilder() {
        FeignDecorators decorators = FeignDecorators.builder()
                .withRetry(productServiceRetry())
                .withFallback(new FeignFallback())
                //.withRateLimiter(productServiceRateLimiter())
                .build();
        return Resilience4jFeign.builder(decorators);
    }

    public Retry productServiceRetry() {
        RetryConfig config = RetryConfig.custom()
                .maxAttempts(7)
                .waitDuration(Duration.ofMillis(15000))
                .failAfterMaxAttempts(true)
                .build();
        Retry snowRetry = RetryRegistry.of(config).retry("productService");
        snowRetry.getEventPublisher().onRetry(event -> System.out.println("productServiceRetry event: "+event));
        return snowRetry;
    }

    public RateLimiter productServiceRateLimiter() {
        RateLimiterConfig config = RateLimiterConfig.custom()
                .limitForPeriod(2)
                .limitRefreshPeriod(Duration.ofMillis(4000))
                .timeoutDuration(Duration.ofMillis(10000))
                .build();
        return RateLimiterRegistry.of(config).rateLimiter("productService");
    }
}
