package com.sayedhesham.travelorch.payment_service.service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.mockito.Answers.RETURNS_DEEP_STUBS;
import static org.mockito.ArgumentMatchers.any;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;

import com.sayedhesham.travelorch.common.entity.payment.PaymentMethod;
import com.sayedhesham.travelorch.common.entity.payment.PaymentTransaction;
import com.sayedhesham.travelorch.common.entity.travel.Travel;
import com.sayedhesham.travelorch.common.entity.user.User;
import com.sayedhesham.travelorch.common.enums.PaymentProvider;
import com.sayedhesham.travelorch.common.enums.PaymentStatus;
import com.sayedhesham.travelorch.common.repository.payment.PaymentMethodRepository;
import com.sayedhesham.travelorch.common.repository.payment.PaymentTransactionRepository;
import com.sayedhesham.travelorch.common.repository.travel.TravelRepository;
import com.sayedhesham.travelorch.common.repository.user.UserRepository;
import com.sayedhesham.travelorch.payment_service.dto.PaymentTransactionCreateRequest;
import com.stripe.StripeClient;
import com.stripe.model.PaymentIntent;

import reactor.test.StepVerifier;

@ExtendWith(MockitoExtension.class)
class PaymentTransactionServiceTest {

    @Mock
    private PaymentTransactionRepository paymentTransactionRepository;

    @Mock
    private PaymentMethodRepository paymentMethodRepository;

    @Mock
    private TravelRepository travelRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private TransactionTemplate transactionTemplate;

    @Mock(answer = RETURNS_DEEP_STUBS)
    private StripeClient stripeClient;

    @InjectMocks
    private PaymentTransactionService paymentTransactionService;

    private User testUser;
    private User otherUser;
    private Travel testTravel;
    private PaymentMethod testPaymentMethod;
    private PaymentTransaction testTransaction;
    private PaymentIntent testPaymentIntent;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setId(1L);
        testUser.setUsername("testuser");

        otherUser = new User();
        otherUser.setId(2L);
        otherUser.setUsername("otheruser");

        testTravel = new Travel();
        testTravel.setId(1L);
        testTravel.setTitle("Test Trip");
        testTravel.setUser(testUser);

        testPaymentMethod = new PaymentMethod();
        testPaymentMethod.setId(1L);
        testPaymentMethod.setProvider(PaymentProvider.stripe);
        testPaymentMethod.setName("Stripe Test");
        testPaymentMethod.setIsTestMode(true);

        testTransaction = new PaymentTransaction();
        testTransaction.setId(100L);
        testTransaction.setTravel(testTravel);
        testTransaction.setPaymentMethod(testPaymentMethod);
        testTransaction.setAmount(new BigDecimal("100.00"));
        testTransaction.setCurrency("USD");
        testTransaction.setStatus(PaymentStatus.pending);
        testTransaction.setProviderTransactionId("pi_test_123");
        testTransaction.setPaymentIntentId("pi_test_123");
        testTransaction.setCreatedAt(LocalDateTime.now());

        testPaymentIntent = new PaymentIntent();
        testPaymentIntent.setId("pi_test_123");
        testPaymentIntent.setStatus("requires_payment_method");
    }

    private void setupTransactionTemplateInvocation() {
        when(transactionTemplate.execute(any())).thenAnswer(invocation -> {
            TransactionCallback<?> callback = invocation.getArgument(0);
            return callback.doInTransaction(null);
        });
    }

    @Test
    void getAllTransactions_Success() {
        setupTransactionTemplateInvocation();
        when(paymentTransactionRepository.findAll()).thenReturn(java.util.List.of(testTransaction));

        StepVerifier.create(paymentTransactionService.getAllTransactions())
                .expectNextMatches(response -> {
                    assertEquals(100L, response.getId());
                    assertEquals(new BigDecimal("100.00"), response.getAmount());
                    assertEquals("USD", response.getCurrency());
                    assertEquals(PaymentStatus.pending, response.getStatus());
                    assertEquals("pi_test_123", response.getPaymentIntentId());
                    assertEquals(1L, response.getTravelId());
                    return true;
                })
                .verifyComplete();
    }

    @Test
    void getAllTransactions_EmptyList() {
        setupTransactionTemplateInvocation();
        when(paymentTransactionRepository.findAll()).thenReturn(java.util.List.of());

        StepVerifier.create(paymentTransactionService.getAllTransactions())
                .verifyComplete();
    }

    @Test
    void getTransactionById_AsOwner_Success() {
        setupTransactionTemplateInvocation();
        when(paymentTransactionRepository.findById(100L)).thenReturn(Optional.of(testTransaction));
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));

        StepVerifier.create(paymentTransactionService.getTransactionById(100L, "testuser"))
                .expectNextMatches(response -> {
                    assertEquals(100L, response.getId());
                    assertEquals(new BigDecimal("100.00"), response.getAmount());
                    assertEquals(PaymentStatus.pending, response.getStatus());
                    return true;
                })
                .verifyComplete();
    }

    @Test
    void getTransactionById_NotFound() {
        setupTransactionTemplateInvocation();
        when(paymentTransactionRepository.findById(99L)).thenReturn(Optional.empty());

        StepVerifier.create(paymentTransactionService.getTransactionById(99L, "testuser"))
                .expectErrorMatches(throwable
                        -> throwable instanceof IllegalArgumentException
                && throwable.getMessage().equals("Transaction not found with id: 99")
                )
                .verify();
    }

    @Test
    void getTransactionById_NotOwner_NoPermission_ThrowsSecurity() {
        setupTransactionTemplateInvocation();
        when(paymentTransactionRepository.findById(100L)).thenReturn(Optional.of(testTransaction));
        when(userRepository.findByUsername("otheruser")).thenReturn(Optional.of(otherUser));

        StepVerifier.create(paymentTransactionService.getTransactionById(100L, "otheruser"))
                .expectErrorMatches(throwable
                        -> throwable instanceof SecurityException
                && throwable.getMessage().equals("You do not have permission to view this transaction")
                )
                .verify();
    }

    @Test
    void getTransactionsByUser_AsOwner_Success() {
        setupTransactionTemplateInvocation();
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));
        when(paymentTransactionRepository.findByTravelUserId(1L)).thenReturn(java.util.List.of(testTransaction));

        StepVerifier.create(paymentTransactionService.getTransactionsByUser(1L, "testuser"))
                .expectNextMatches(response -> {
                    assertEquals(100L, response.getId());
                    assertEquals(1L, response.getTravelId());
                    return true;
                })
                .verifyComplete();
    }

    @Test
    void getTransactionsByUser_NotOwner_NoPermission_ThrowsSecurity() {
        setupTransactionTemplateInvocation();
        when(userRepository.findByUsername("otheruser")).thenReturn(Optional.of(otherUser));

        StepVerifier.create(paymentTransactionService.getTransactionsByUser(1L, "otheruser"))
                .expectErrorMatches(throwable
                        -> throwable instanceof SecurityException
                && throwable.getMessage().equals("You do not have permission to view these transactions")
                )
                .verify();
    }

    @Test
    void createTransaction_Success() {
        setupTransactionTemplateInvocation();
        when(travelRepository.findById(1L)).thenReturn(Optional.of(testTravel));
        when(paymentMethodRepository.findByProviderAndIsTestMode(PaymentProvider.stripe, true))
                .thenReturn(Optional.of(testPaymentMethod));
        when(paymentTransactionRepository.save(any(PaymentTransaction.class))).thenAnswer(invocation -> {
            PaymentTransaction t = invocation.getArgument(0);
            t.setId(100L);
            t.setCreatedAt(LocalDateTime.now());
            return t;
        });

        try {
            when(stripeClient.v1().paymentIntents().create(any(com.stripe.param.PaymentIntentCreateParams.class)))
                    .thenReturn(testPaymentIntent);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        PaymentTransactionCreateRequest request = PaymentTransactionCreateRequest.builder()
                .amount(new BigDecimal("100.00"))
                .currency("USD")
                .travelId(1L)
                .build();

        StepVerifier.create(paymentTransactionService.createTransaction(request))
                .expectNextMatches(response -> {
                    assertNotNull(response.getId());
                    assertEquals(new BigDecimal("100.00"), response.getAmount());
                    assertEquals("USD", response.getCurrency());
                    assertEquals(PaymentStatus.pending, response.getStatus());
                    assertEquals("pi_test_123", response.getPaymentIntentId());
                    assertEquals(1L, response.getTravelId());
                    return true;
                })
                .verifyComplete();

        verify(paymentTransactionRepository).save(any(PaymentTransaction.class));
    }

    @Test
    void createTransaction_DefaultCurrency() {
        setupTransactionTemplateInvocation();
        when(travelRepository.findById(1L)).thenReturn(Optional.of(testTravel));
        when(paymentMethodRepository.findByProviderAndIsTestMode(PaymentProvider.stripe, true))
                .thenReturn(Optional.of(testPaymentMethod));
        when(paymentTransactionRepository.save(any(PaymentTransaction.class))).thenAnswer(invocation -> {
            PaymentTransaction t = invocation.getArgument(0);
            t.setId(101L);
            t.setCreatedAt(LocalDateTime.now());
            return t;
        });

        try {
            when(stripeClient.v1().paymentIntents().create(any(com.stripe.param.PaymentIntentCreateParams.class)))
                    .thenReturn(testPaymentIntent);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        PaymentTransactionCreateRequest request = PaymentTransactionCreateRequest.builder()
                .amount(new BigDecimal("50.00"))
                .travelId(1L)
                .build();

        StepVerifier.create(paymentTransactionService.createTransaction(request))
                .expectNextMatches(response -> {
                    assertEquals("USD", response.getCurrency());
                    return true;
                })
                .verifyComplete();
    }

    @Test
    void createTransaction_TravelNotFound() {
        setupTransactionTemplateInvocation();
        when(travelRepository.findById(99L)).thenReturn(Optional.empty());

        PaymentTransactionCreateRequest request = PaymentTransactionCreateRequest.builder()
                .amount(new BigDecimal("100.00"))
                .currency("USD")
                .travelId(99L)
                .build();

        StepVerifier.create(paymentTransactionService.createTransaction(request))
                .expectErrorMatches(throwable
                        -> throwable instanceof IllegalArgumentException
                && throwable.getMessage().equals("Travel not found with id: 99")
                )
                .verify();
    }

    @Test
    void createTransaction_AutoCreatesPaymentMethod() {
        setupTransactionTemplateInvocation();
        when(travelRepository.findById(1L)).thenReturn(Optional.of(testTravel));
        when(paymentMethodRepository.findByProviderAndIsTestMode(PaymentProvider.stripe, true))
                .thenReturn(Optional.empty());
        when(paymentMethodRepository.save(any(PaymentMethod.class))).thenAnswer(invocation -> {
            PaymentMethod pm = invocation.getArgument(0);
            pm.setId(1L);
            return pm;
        });
        when(paymentTransactionRepository.save(any(PaymentTransaction.class))).thenAnswer(invocation -> {
            PaymentTransaction t = invocation.getArgument(0);
            t.setId(102L);
            t.setCreatedAt(LocalDateTime.now());
            return t;
        });

        try {
            when(stripeClient.v1().paymentIntents().create(any(com.stripe.param.PaymentIntentCreateParams.class)))
                    .thenReturn(testPaymentIntent);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        PaymentTransactionCreateRequest request = PaymentTransactionCreateRequest.builder()
                .amount(new BigDecimal("75.00"))
                .currency("EUR")
                .travelId(1L)
                .build();

        StepVerifier.create(paymentTransactionService.createTransaction(request))
                .expectNextMatches(response -> {
                    assertNotNull(response.getId());
                    assertEquals("EUR", response.getCurrency());
                    return true;
                })
                .verifyComplete();

        verify(paymentMethodRepository).save(any(PaymentMethod.class));
    }
}
