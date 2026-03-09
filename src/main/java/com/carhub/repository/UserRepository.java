package com.carhub.repository;

import com.carhub.entity.User;
import org.postgresql.core.NativeQuery;
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
    Optional<User> findByPhoneNumber(String phoneNumber);
    List<User> findByRole(User.Role role);
    List<User> findByFullNameContainingIgnoreCase(String fullName);
    boolean existsByUsername(String username);
    boolean existsByEmail(String email);

    @Query("Update User u SET u.password = :password WHERE u.email = :email")
    void updateNewPassword(@Param("email") String email, @Param("password") String password);
}