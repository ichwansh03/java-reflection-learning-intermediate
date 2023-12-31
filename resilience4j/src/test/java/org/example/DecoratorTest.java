package org.example;

import io.github.resilience4j.bulkhead.Bulkhead;
import io.github.resilience4j.bulkhead.BulkheadConfig;
import io.github.resilience4j.decorators.Decorators;
import io.github.resilience4j.ratelimiter.RateLimiter;
import io.github.resilience4j.ratelimiter.RateLimiterConfig;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryConfig;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.function.Supplier;

@Slf4j
public class DecoratorTest {

    @SneakyThrows
    public void slow() {
        log.info("Slow");
        Thread.sleep(1_000L);
        throw new IllegalArgumentException("Error");
    }

    @SneakyThrows
    public String sayhello(){
        log.info("say hello");
        Thread.sleep(1000L);
        throw new IllegalArgumentException("SayHello");
    }

    @Test
    void testDecoratorsWithFallback() {
        RateLimiter rateLimiter = RateLimiter.of("ichwan-ratelimiter", RateLimiterConfig.custom()
                .limitForPeriod(5)
                .limitRefreshPeriod(Duration.ofMinutes(1))
                .build());

        Retry retry = Retry.of("ichwan-retry", RetryConfig.custom()
                .maxAttempts(10)
                .waitDuration(Duration.ofMillis(100))
                .build());

        Bulkhead bulkhead = Bulkhead.of("ichwan-bulkhead", BulkheadConfig.custom()
                .maxWaitDuration(Duration.ofSeconds(5))
                .maxConcurrentCalls(10)
                .build());

        Supplier<String> decorate = Decorators.ofSupplier(() -> sayhello())
                .withRetry(retry)
                .withRateLimiter(rateLimiter)
                .withBulkhead(bulkhead)
                .withFallback(throwable -> "hello world")
                .decorate();

        System.out.println(decorate.get());
    }

    @Test
    void testDecorators() throws InterruptedException {
        RateLimiter rateLimiter = RateLimiter.of("ichwan-ratelimiter", RateLimiterConfig.custom()
                .limitForPeriod(5)
                .limitRefreshPeriod(Duration.ofMinutes(1))
                .build());

        Retry retry = Retry.of("ichwan-retry", RetryConfig.custom()
                .maxAttempts(10)
                .waitDuration(Duration.ofMillis(100))
                .build());

        Bulkhead bulkhead = Bulkhead.of("ichwan-bulkhead", BulkheadConfig.custom()
                .maxWaitDuration(Duration.ofSeconds(5))
                .maxConcurrentCalls(10)
                .build());

        Runnable runnable = Decorators.ofRunnable(() -> slow())
                .withRetry(retry)
                .withRateLimiter(rateLimiter)
                .withBulkhead(bulkhead)
                .decorate();

        for (int i = 0; i < 100; i++) {
            new Thread(runnable).start();
        }

        Thread.sleep(10_000L);
    }
}
