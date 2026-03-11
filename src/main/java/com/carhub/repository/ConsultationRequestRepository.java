package com.carhub.repository;

import com.carhub.entity.Admin;
import com.carhub.entity.ConsultationRequest;
import com.carhub.entity.Customer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface ConsultationRequestRepository extends JpaRepository<ConsultationRequest, Long> {
    List<ConsultationRequest> findByCustomer(Customer customer);
    List<ConsultationRequest> findByCustomerId(Long customerId);
    List<ConsultationRequest> findByAdmin(Admin admin);
    List<ConsultationRequest> findByAdminId(Long adminId);
    List<ConsultationRequest> findByStatus(String status);
    List<ConsultationRequest> findBySendDateBetween(LocalDateTime startDate, LocalDateTime endDate);
    List<ConsultationRequest> findByStatusOrderBySendDateDesc(String status);
}