package com.sayedhesham.travelorch.payment_service.config;

import com.stripe.StripeClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;

@Configuration
public class StripeConfig {

    private static final Logger log = LoggerFactory.getLogger(StripeConfig.class);

    @Value("${stripe.secret-key}")
    private String stripeSecretKey;

    @Bean
    public StripeClient stripeClient() {
        if (!StringUtils.hasText(stripeSecretKey)) {
            throw new IllegalStateException(
                    "Stripe secret key is not configured. "
                    + "Ensure HashiCorp Vault is properly configured and the secret is stored at "
                    + "'secret/travel-system/stripe' (key: stripe.secret-key). "
                    + "Set VAULT_ENABLED=true and provide VAULT_TOKEN to enable Vault integration, "
                    + "or set STRIPE_SECRET_KEY environment variable for local development."
            );
        }

        if (!stripeSecretKey.startsWith("sk_test_") && !stripeSecretKey.startsWith("sk_live_")) {
            throw new IllegalStateException(
                    "Invalid Stripe secret key format. Key must start with 'sk_test_' or 'sk_live_'."
            );
        }

        log.info("StripeClient initialized successfully (mode: {})",
                stripeSecretKey.startsWith("sk_test_") ? "TEST" : "LIVE");

        return StripeClient.builder()
                .setApiKey(stripeSecretKey)
                .build();
    }
}
