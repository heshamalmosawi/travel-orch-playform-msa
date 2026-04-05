package com.sayedhesham.travelorch.apigateway_service.config;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.Refill;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.core.Ordered;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilterChain;
import org.springframework.web.server.WebFilter;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class RateLimiterFilter implements WebFilter, Ordered {

    private static final Logger log = LoggerFactory.getLogger(RateLimiterFilter.class);

    private final Map<String, Bucket> buckets = new ConcurrentHashMap<>();

    private final int capacity;
    private final Duration duration;

    public RateLimiterFilter(
            @Value("${rate.limit.capacity:5}") int capacity,
            @Value("${rate.limit.duration:10}") int durationSeconds) {
        this.capacity = capacity;
        this.duration = Duration.ofSeconds(durationSeconds);
        log.info("RateLimiter initialized: capacity={}, duration={}s", capacity, durationSeconds);
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        String ip = getClientIp(exchange.getRequest());
        log.debug("Processing request from IP: {}", ip);
        
        Bucket bucket = buckets.computeIfAbsent(ip, key -> createBucket());
        log.debug("Bucket for IP {}: available tokens={}", ip, bucket.getAvailableTokens());

        if (bucket.tryConsume(1)) {
            log.debug("Request allowed for IP {}: tokens remaining={}", ip, bucket.getAvailableTokens());
            return chain.filter(exchange);
        } else {
            log.warn("Rate limit exceeded for IP {}", ip);
            ServerHttpResponse response = exchange.getResponse();
            response.setStatusCode(HttpStatus.TOO_MANY_REQUESTS);
            response.getHeaders().setContentType(MediaType.APPLICATION_JSON);
            
            String body = "{\"message\":\"Rate limit exceeded. Please try again later.\"}";
            return response.writeWith(Mono.just(response.bufferFactory().wrap(body.getBytes())));
        }
    }

    private String getClientIp(ServerHttpRequest request) {
        String xForwardedFor = request.getHeaders().getFirst("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }
        
        String xRealIp = request.getHeaders().getFirst("X-Real-IP");
        if (xRealIp != null && !xRealIp.isEmpty()) {
            return xRealIp;
        }
        
        var remoteAddress = request.getRemoteAddress();
        if (remoteAddress != null && remoteAddress.getAddress() != null) {
            return remoteAddress.getAddress().getHostAddress();
        }
        return "unknown";
    }

    @Override
    public int getOrder() {
        return Ordered.LOWEST_PRECEDENCE - 1;
    }

    private Bucket createBucket() {
        Bandwidth limit = Bandwidth.classic(capacity, Refill.intervally(capacity, duration));
        return Bucket.builder()
            .addLimit(limit)
            .build();
    }
}
