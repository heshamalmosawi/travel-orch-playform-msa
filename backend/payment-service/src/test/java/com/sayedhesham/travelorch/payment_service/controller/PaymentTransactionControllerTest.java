package com.sayedhesham.travelorch.payment_service.controller;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.mockito.ArgumentMatchers.any;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.test.web.reactive.server.WebTestClient;

import com.sayedhesham.travelorch.common.enums.PaymentStatus;
import com.sayedhesham.travelorch.payment_service.dto.PaymentTransactionCreateRequest;
import com.sayedhesham.travelorch.payment_service.dto.PaymentTransactionResponse;
import com.sayedhesham.travelorch.payment_service.service.PaymentTransactionService;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@ExtendWith(MockitoExtension.class)
class PaymentTransactionControllerTest {

    @Mock
    private PaymentTransactionService paymentTransactionService;

    @InjectMocks
    private PaymentTransactionController paymentTransactionController;

    private WebTestClient webTestClient;
    private PaymentTransactionResponse transactionResponse;

    @BeforeEach
    void setUp() {
        webTestClient = WebTestClient.bindToController(paymentTransactionController)
                .webFilter((exchange, chain)
                        -> chain.filter(exchange)
                        .contextWrite(ReactiveSecurityContextHolder.withAuthentication(getAuthentication()))
                )
                .build();

        transactionResponse = PaymentTransactionResponse.builder()
                .id(100L)
                .amount(new BigDecimal("100.00"))
                .currency("USD")
                .status(PaymentStatus.pending)
                .providerTransactionId("pi_test_123")
                .paymentIntentId("pi_test_123")
                .travelId(1L)
                .createdAt(LocalDateTime.now())
                .build();
    }

    private UsernamePasswordAuthenticationToken getAuthentication() {
        return new UsernamePasswordAuthenticationToken(
                "admin", null,
                List.of(new SimpleGrantedAuthority("ROLE_ADMIN"))
        );
    }

    @Test
    void getAllTransactions_Success() {
        when(paymentTransactionService.getAllTransactions()).thenReturn(Flux.just(transactionResponse));

        webTestClient.get()
                .uri("/transactions")
                .exchange()
                .expectStatus().isOk()
                .expectBodyList(PaymentTransactionResponse.class)
                .hasSize(1)
                .value(responses -> {
                    PaymentTransactionResponse response = responses.getFirst();
                    assertEquals(100L, response.getId());
                    assertEquals(new BigDecimal("100.00"), response.getAmount());
                    assertEquals(PaymentStatus.pending, response.getStatus());
                });
    }

    @Test
    void getAllTransactions_EmptyList() {
        when(paymentTransactionService.getAllTransactions()).thenReturn(Flux.empty());

        webTestClient.get()
                .uri("/transactions")
                .exchange()
                .expectStatus().isOk()
                .expectBodyList(PaymentTransactionResponse.class)
                .hasSize(0);
    }

    @Test
    void getTransactionById_Success() {
        when(paymentTransactionService.getTransactionById(100L, "admin")).thenReturn(Mono.just(transactionResponse));

        webTestClient.get()
                .uri("/transactions/100")
                .exchange()
                .expectStatus().isOk()
                .expectBody(PaymentTransactionResponse.class)
                .value(response -> {
                    assertEquals(100L, response.getId());
                    assertEquals("pi_test_123", response.getPaymentIntentId());
                });
    }

    @Test
    void getTransactionById_NotFound() {
        when(paymentTransactionService.getTransactionById(99L, "admin"))
                .thenReturn(Mono.error(new IllegalArgumentException("Transaction not found with id: 99")));

        webTestClient.get()
                .uri("/transactions/99")
                .exchange()
                .expectStatus().isNotFound();
    }

    @Test
    void getTransactionsByUser_Success() {
        when(paymentTransactionService.getTransactionsByUser(1L, "admin")).thenReturn(Flux.just(transactionResponse));

        webTestClient.get()
                .uri("/transactions/user/1")
                .exchange()
                .expectStatus().isOk()
                .expectBodyList(PaymentTransactionResponse.class)
                .hasSize(1)
                .value(responses -> {
                    assertEquals(100L, responses.getFirst().getId());
                    assertEquals(1L, responses.getFirst().getTravelId());
                });
    }

    @Test
    void getTransactionsByUser_Forbidden() {
        when(paymentTransactionService.getTransactionsByUser(2L, "admin"))
                .thenReturn(Flux.error(new SecurityException("You do not have permission to view these transactions")));

        webTestClient.get()
                .uri("/transactions/user/2")
                .exchange()
                .expectStatus().isForbidden();
    }

    @Test
    void createTransaction_Success() {
        PaymentTransactionCreateRequest request = PaymentTransactionCreateRequest.builder()
                .amount(new BigDecimal("100.00"))
                .currency("USD")
                .travelId(1L)
                .build();

        PaymentTransactionResponse createdResponse = PaymentTransactionResponse.builder()
                .id(101L)
                .amount(new BigDecimal("100.00"))
                .currency("USD")
                .status(PaymentStatus.pending)
                .providerTransactionId("pi_test_456")
                .paymentIntentId("pi_test_456")
                .travelId(1L)
                .createdAt(LocalDateTime.now())
                .build();

        when(paymentTransactionService.createTransaction(any(PaymentTransactionCreateRequest.class)))
                .thenReturn(Mono.just(createdResponse));

        webTestClient.post()
                .uri("/transactions")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .exchange()
                .expectStatus().isCreated()
                .expectBody(PaymentTransactionResponse.class)
                .value(response -> {
                    assertEquals(101L, response.getId());
                    assertEquals(new BigDecimal("100.00"), response.getAmount());
                    assertEquals("pi_test_456", response.getPaymentIntentId());
                });
    }
}
