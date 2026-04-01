package com.sayedhesham.travelorch.common.enums;

/**
 * Simple user roles for the travel platform.
 * Used directly in User entity - no separate Role table needed.
 */
public enum UserRole {
    USER,   // Regular user - can book travels, manage own profile
    ADMIN   // Administrator - full system access, manage users/travels/payments
}
