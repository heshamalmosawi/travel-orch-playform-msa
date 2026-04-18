package com.sayedhesham.travelorch.travel_service.config;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.access.PermissionEvaluator;
import org.springframework.security.access.expression.method.DefaultMethodSecurityExpressionHandler;
import org.springframework.security.config.annotation.method.configuration.EnableReactiveMethodSecurity;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.web.server.SecurityWebFilterChain;

import com.sayedhesham.travelorch.common.repository.user.UserRepository;
import com.sayedhesham.travelorch.common.util.jwt.JwtUtil;
import com.sayedhesham.travelorch.travel_service.security.CustomPermissionEvaluator;
import com.sayedhesham.travelorch.travel_service.security.JwtSecurityContextRepository;

@Configuration
@EnableWebFluxSecurity
@EnableReactiveMethodSecurity
public class SecurityConfig {

    @Bean
    public SecurityWebFilterChain securityWebFilterChain(ServerHttpSecurity http, JwtUtil jwtUtil) {
        http
            .csrf(csrf -> csrf.disable())
            .securityContextRepository(new JwtSecurityContextRepository(jwtUtil))
            .authorizeExchange(auth -> auth
                .anyExchange().authenticated()
            );

        return http.build();
    }

    @Bean
    public PermissionEvaluator permissionEvaluator(UserRepository userRepository) {
        return new CustomPermissionEvaluator(userRepository);
    }

    @Bean
    static BeanPostProcessor methodSecurityExpressionHandlerPostProcessor(PermissionEvaluator permissionEvaluator) {
        return new BeanPostProcessor() {
            @Override
            public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
                if (bean instanceof DefaultMethodSecurityExpressionHandler handler) {
                    handler.setPermissionEvaluator(permissionEvaluator);
                }
                return bean;
            }
        };
    }
}
