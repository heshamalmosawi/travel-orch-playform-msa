package com.sayedhesham.travelorch.common.repository.user;

import com.sayedhesham.travelorch.common.entity.user.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    
    Optional<User> findByUsername(String username);
    
    Optional<User> findByEmail(String email);
    
    boolean existsByUsername(String username);
    
    boolean existsByEmail(String email);
    
    @Query("SELECT u FROM User u JOIN u.roles r WHERE r.name = :roleName")
    List<User> findByRoles_Name(@Param("roleName") String roleName);
    
    /**
     * Find all admin users
     */
    default List<User> findAllAdmins() {
        return findByRoles_Name("admin");
    }
}
