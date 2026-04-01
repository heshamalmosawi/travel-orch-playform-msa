package com.sayedhesham.travelorch.common.repository.user;

import com.sayedhesham.travelorch.common.entity.user.User;
import com.sayedhesham.travelorch.common.enums.UserRole;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    
    Optional<User> findByUsername(String username);
    
    Optional<User> findByEmail(String email);
    
    boolean existsByUsername(String username);
    
    boolean existsByEmail(String email);
    
    List<User> findByRole(UserRole role);
    
    /**
     * Find all admin users
     */
    default List<User> findAllAdmins() {
        return findByRole(UserRole.ADMIN);
    }
}
