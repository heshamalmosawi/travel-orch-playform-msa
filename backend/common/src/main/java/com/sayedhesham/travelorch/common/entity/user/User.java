package com.sayedhesham.travelorch.common.entity.user;

import com.sayedhesham.travelorch.common.entity.base.BaseEntity;
import com.sayedhesham.travelorch.common.entity.rbac.Role;
import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

@Entity
@Table(name = "users", indexes = {
    @Index(name = "idx_users_email", columnList = "email"),
    @Index(name = "idx_users_username", columnList = "username")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User extends BaseEntity {

    @NotBlank
    @Size(max = 50)
    @Column(unique = true, nullable = false, length = 50)
    private String username;

    @NotBlank
    @Email
    @Size(max = 255)
    @Column(unique = true, nullable = false)
    private String email;

    @NotBlank
    @Column(name = "password_hash", nullable = false)
    private String passwordHash;

    @NotBlank
    @Size(max = 100)
    @Column(name = "first_name", nullable = false, length = 100)
    private String firstName;

    @NotBlank
    @Size(max = 100)
    @Column(name = "last_name", nullable = false, length = 100)
    private String lastName;

    @Size(max = 20)
    @Column(length = 20)
    private String phone;

    @Column(name = "date_of_birth")
    private LocalDate dateOfBirth;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "role_id", nullable = false)
    private Role role;

    public boolean isAdmin() {
        return role != null && "admin".equalsIgnoreCase(role.getName());
    }

    public boolean hasRole(String roleName) {
        return role != null && roleName.equalsIgnoreCase(role.getName());
    }

    public boolean hasPermission(String permissionName) {
        return role != null && role.getPermissions().stream()
                .anyMatch(permission -> permissionName.equalsIgnoreCase(permission.getName()));
    }
}