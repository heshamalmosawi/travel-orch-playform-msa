package com.sayedhesham.travelorch.apigateway_service.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.time.Instant;

@Component
public class RequestLoggingFilter implements WebFilter {

    private static final Logger log = LoggerFactory.getLogger(RequestLoggingFilter.class);

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        Instant startTime = Instant.now();

        String requestId = generateRequestId();
        exchange.getAttributes().put("requestId", requestId);
        exchange.getAttributes().put("startTime", startTime);

        logRequest(request, requestId);

        return chain.filter(exchange)
                .doFinally(signalType -> {
                    Instant endTime = Instant.now();
                    Duration duration = Duration.between(startTime, endTime);
                    ServerHttpResponse response = exchange.getResponse();
                    logResponse(request, response, requestId, duration);
                });
    }

    private void logRequest(ServerHttpRequest request, String requestId) {
        log.info("[{}] INCOMING REQUEST - Method: {}, Path: {}, Query: {}, Client: {}",
                requestId,
                request.getMethod(),
                request.getPath(),
                request.getQueryParams().isEmpty() ? "none" : request.getQueryParams(),
                getClientIp(request)
        );

        log.debug("[{}] Headers - Content-Type: {}, User-Agent: {}, Authorization: {}",
                requestId,
                request.getHeaders().getContentType(),
                request.getHeaders().getFirst("User-Agent"),
                request.getHeaders().getFirst("Authorization") != null ? "***" : "none"
        );
    }

    private void logResponse(ServerHttpRequest request, ServerHttpResponse response, String requestId, Duration duration) {
        log.info("[{}] OUTGOING RESPONSE - Status: {}, Method: {}, Path: {}, Duration: {}ms",
                requestId,
                response.getStatusCode(),
                request.getMethod(),
                request.getPath(),
                duration.toMillis()
        );

        if (!response.getStatusCode().is2xxSuccessful()) {
            log.warn("[{}] Non-successful response - Status: {}, Path: {}",
                    requestId,
                    response.getStatusCode(),
                    request.getPath()
            );
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

        return request.getRemoteAddress() != null
                ? request.getRemoteAddress().getAddress().getHostAddress()
                : "unknown";
    }

    private String generateRequestId() {
        return String.format("%08x", System.identityHashCode(Thread.currentThread()));
    }
}
