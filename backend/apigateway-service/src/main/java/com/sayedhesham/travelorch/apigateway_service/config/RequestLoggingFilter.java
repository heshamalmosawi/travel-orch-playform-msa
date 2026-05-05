package com.sayedhesham.travelorch.apigateway_service.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.core.Ordered;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

@Component
public class RequestLoggingFilter implements WebFilter, Ordered {

    private static final Logger log = LoggerFactory.getLogger(RequestLoggingFilter.class);
    private static final String CORRELATION_ID_HEADER = "X-Correlation-ID";
    private static final String CORRELATION_ID_KEY = "correlationId";

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        Instant startTime = Instant.now();

        String correlationId = resolveCorrelationId(exchange.getRequest());
        exchange.getAttributes().put(CORRELATION_ID_KEY, correlationId);
        exchange.getAttributes().put("startTime", startTime);

        ServerHttpRequest mutatedRequest = exchange.getRequest().mutate()
                .header(CORRELATION_ID_HEADER, correlationId)
                .build();
        ServerWebExchange mutatedExchange = exchange.mutate().request(mutatedRequest).build();

        MDC.put(CORRELATION_ID_KEY, correlationId);
        logRequest(mutatedExchange.getRequest(), correlationId);

        return chain.filter(mutatedExchange)
                .doFinally(signalType -> {
                    try {
                        Instant endTime = Instant.now();
                        Duration duration = Duration.between(startTime, endTime);
                        ServerHttpResponse response = mutatedExchange.getResponse();
                        logResponse(mutatedExchange.getRequest(), response, correlationId, duration);
                    } finally {
                        MDC.remove(CORRELATION_ID_KEY);
                    }
                });
    }

    private String resolveCorrelationId(ServerHttpRequest request) {
        String existing = request.getHeaders().getFirst(CORRELATION_ID_HEADER);
        if (existing != null && !existing.isBlank()) {
            return existing;
        }
        return UUID.randomUUID().toString();
    }

    private void logRequest(ServerHttpRequest request, String correlationId) {
        log.info("[{}] INCOMING REQUEST - Method: {}, Path: {}, Query: {}, Client: {}",
                correlationId,
                request.getMethod(),
                request.getPath(),
                request.getQueryParams().isEmpty() ? "none" : request.getQueryParams(),
                getClientIp(request)
        );

        if (log.isDebugEnabled()) {
            log.debug("[{}] Headers - Content-Type: {}, User-Agent: {}, Authorization: {}",
                    correlationId,
                    request.getHeaders().getContentType(),
                    request.getHeaders().getFirst("User-Agent"),
                    request.getHeaders().getFirst("Authorization") != null ? "***" : "none"
            );
        }
    }

    private void logResponse(ServerHttpRequest request, ServerHttpResponse response, String correlationId, Duration duration) {
        var statusCode = response.getStatusCode();
        log.info("[{}] OUTGOING RESPONSE - Status: {}, Method: {}, Path: {}, Duration: {}ms",
                correlationId,
                statusCode != null ? statusCode : "unknown",
                request.getMethod(),
                request.getPath(),
                duration.toMillis()
        );

        if (statusCode != null && !statusCode.is2xxSuccessful()) {
            log.warn("[{}] Non-successful response - Status: {}, Path: {}",
                    correlationId,
                    statusCode,
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

        var remoteAddress = request.getRemoteAddress();
        if (remoteAddress != null && remoteAddress.getAddress() != null) {
            return remoteAddress.getAddress().getHostAddress();
        }
        return "unknown";
    }

    @Override
    public int getOrder() {
        return Ordered.LOWEST_PRECEDENCE - 2;
    }
}
