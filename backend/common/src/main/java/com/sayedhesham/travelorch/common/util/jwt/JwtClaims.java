package com.sayedhesham.travelorch.common.util.jwt;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;
import java.util.Set;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class JwtClaims {
    private String username;
    private Set<String> roles;
    private boolean isService;
    private Date issuedAt;
    private Date expiration;
    private String tokenType;
}
