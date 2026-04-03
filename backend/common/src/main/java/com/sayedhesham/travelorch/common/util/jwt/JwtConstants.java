package com.sayedhesham.travelorch.common.util.jwt;

public final class JwtConstants {

    private JwtConstants() {
        throw new UnsupportedOperationException("Utility class cannot be instantiated");
    }

    public static final String AUTHORIZATION_HEADER = "Authorization";
    public static final String BEARER_PREFIX = "Bearer ";

    public static final String ROLES_CLAIM = "roles";
    public static final String SERVICE_CLAIM = "service";
    public static final String TOKEN_TYPE_CLAIM = "token_type";

    public static final String ACCESS_TOKEN_TYPE = "access";
    public static final String REFRESH_TOKEN_TYPE = "refresh";
}
