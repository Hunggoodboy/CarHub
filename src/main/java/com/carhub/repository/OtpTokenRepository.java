package com.carhub.repository;

import com.carhub.entity.OtpToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface OtpTokenRepository extends JpaRepository<OtpToken, Long> {
    Optional<OtpToken> findFirstByEmailOrderByCreatedAtDesc(String email);
    void deleteByEmail(String email);
}
